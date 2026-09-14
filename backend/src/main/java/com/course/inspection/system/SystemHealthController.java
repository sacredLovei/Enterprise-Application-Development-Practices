package com.course.inspection.system;

import com.course.inspection.storage.HdfsClient;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * S80：系统健康接口（总览页监控面板数据源）。
 * 四组件探测：Mongo ping、ES cluster health、Kafka 消费组 LAG、HDFS 根目录可达。
 */
@RestController
public class SystemHealthController {

    private static final Logger log = LoggerFactory.getLogger(SystemHealthController.class);

    private static final List<String> GROUPS =
            List.of("biz-storage-consumer", "biz-alarm-consumer", "biz-task-consumer", "biz-image-consumer");

    private final MongoTemplate mongo;
    private final co.elastic.clients.elasticsearch.ElasticsearchClient es;
    private final HdfsClient hdfs;

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaServers;

    public SystemHealthController(MongoTemplate mongo,
                                  co.elastic.clients.elasticsearch.ElasticsearchClient es,
                                  HdfsClient hdfs) {
        this.mongo = mongo;
        this.es = es;
        this.hdfs = hdfs;
    }

    @GetMapping("/api/system/health")
    public Map<String, Object> health() {
        Map<String, Object> out = new HashMap<>();
        out.put("ts", Instant.now().toString());

        // Mongo
        try {
            mongo.executeCommand("{ ping: 1 }");
            out.put("mongo", "up");
        } catch (Exception e) {
            out.put("mongo", "down");
        }

        // ES
        try {
            out.put("es", es.cluster().health().status().jsonValue());
        } catch (Exception e) {
            out.put("es", "down");
        }

        // HDFS
        try {
            out.put("hdfs", hdfs.exists("/inspection") ? "up" : "down");
        } catch (Exception e) {
            out.put("hdfs", "down");
        }

        // Kafka LAG
        out.put("kafkaLag", kafkaLag());

        return out;
    }

    private List<Map<String, Object>> kafkaLag() {
        List<Map<String, Object>> result = new ArrayList<>();
        Properties p = new Properties();
        p.put("bootstrap.servers", kafkaServers);
        p.put("request.timeout.ms", "8000");
        try (AdminClient admin = AdminClient.create(p)) {
            for (String group : GROUPS) {
                try {
                    ListConsumerGroupOffsetsResult offsets = admin.listConsumerGroupOffsets(group);
                    Map<TopicPartition, OffsetAndMetadata> committed =
                            offsets.partitionsToOffsetAndMetadata().get(8, TimeUnit.SECONDS);
                    if (committed == null || committed.isEmpty()) {
                        result.add(Map.of("group", group, "lag", 0L));
                        continue;
                    }
                    Map<TopicPartition, OffsetSpec> specs = new HashMap<>();
                    committed.keySet().forEach(tp -> specs.put(tp, OffsetSpec.latest()));
                    ListOffsetsResult ends = admin.listOffsets(specs);
                    long lag = 0;
                    for (Map.Entry<TopicPartition, OffsetSpec> e : specs.entrySet()) {
                        long end = ends.partitionResult(e.getKey()).get(8, TimeUnit.SECONDS).offset();
                        lag += end - committed.get(e.getKey()).offset();
                    }
                    result.add(Map.of("group", group, "lag", lag));
                } catch (Exception e) {
                    log.warn("LAG 查询失败 group={}: {}", group, e.getMessage());
                    result.add(Map.of("group", group, "lag", -1L));
                }
            }
        } catch (Exception e) {
            log.error("Kafka AdminClient 不可用: {}", e.getMessage());
        }
        return result;
    }
}

package com.course.inspection.alarm;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.StringReader;

/**
 * 启动时幂等创建 ES 索引（设计报告 5.2.5(1)）。
 * mapping 口径见 STATE 注册表与设计报告 4.5.2：dynamic=strict、geo_point、
 * keyword 精确字段、description 为 text（本环境未装 ik 插件，中文按 standard 分词降级）。
 */
@Component
public class AlarmIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AlarmIndexInitializer.class);

    public static final String INDEX = "inspection_alarm_v1";

    private static final String MAPPING_JSON = """
            {
              "settings": {
                "number_of_shards": 1,
                "number_of_replicas": 0,
                "refresh_interval": "1s"
              },
              "mappings": {
                "dynamic": "strict",
                "properties": {
                  "alarmId":      { "type": "keyword" },
                  "deviceId":     { "type": "keyword" },
                  "deviceType":   { "type": "keyword" },
                  "alarmType":    { "type": "keyword" },
                  "level":        { "type": "keyword" },
                  "status":       { "type": "keyword" },
                  "description":  { "type": "text" },
                  "location":     { "type": "geo_point" },
                  "occurredTime": { "type": "date" },
                  "snapshotPath": { "type": "keyword", "index": false }
                }
              }
            }
            """;

    private final ElasticsearchClient client;

    public AlarmIndexInitializer(ElasticsearchClient client) {
        this.client = client;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean exists = client.indices().exists(e -> e.index(INDEX)).value();
            if (exists) {
                log.info("ES 索引已存在，跳过创建: {}", INDEX);
                return;
            }
            client.indices().create(c -> c.index(INDEX).withJson(new StringReader(MAPPING_JSON)));
            log.info("ES 索引创建成功: {}", INDEX);
        } catch (Exception e) {
            log.error("ES 索引初始化失败（服务不可达时后端仍可启动，告警仅写 Mongo）", e);
        }
    }
}

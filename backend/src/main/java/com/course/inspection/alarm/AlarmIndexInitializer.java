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
 * keyword 精确字段、description 为 text（S63 起 analyzer=ik_smart，中文分词检索；索引升版 v2）。
 */
@Component
public class AlarmIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AlarmIndexInitializer.class);

    /** S63：v3 索引启用 ik_smart 中文分词（v1/v2 保留作回滚基线；v2 为映射损坏的中间产物，见风险 #37）。 */
    public static final String INDEX = "inspection_alarm_v3";

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
                  "description":  { "type": "text", "analyzer": "ik_smart" },
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
        // S63 修复（风险 #37）：ES 可能晚于后端就绪，重试 2 分钟再放弃——否则索引永远不被创建，
        // 后续 _reindex 会以动态映射自动建出字段类型错误的索引
        for (int attempt = 1; attempt <= 24; attempt++) {
            try {
                boolean exists = client.indices().exists(e -> e.index(INDEX)).value();
                if (exists) {
                    log.info("ES 索引已存在，跳过创建: {}", INDEX);
                    return;
                }
                client.indices().create(c -> c.index(INDEX).withJson(new StringReader(MAPPING_JSON)));
                log.info("ES 索引创建成功: {}", INDEX);
                return;
            } catch (Exception e) {
                log.warn("ES 索引初始化第 {}/24 次失败（服务不可达时后端仍可启动，告警仅写 Mongo）: {}",
                        attempt, e.getMessage());
                try {
                    Thread.sleep(5_000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.error("ES 索引初始化重试耗尽，索引 {} 未创建——需人工执行 mapping 创建（见 docker/init/）", INDEX);
    }
}

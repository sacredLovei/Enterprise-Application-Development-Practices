package com.course.inspection.alarm;

import com.course.inspection.common.TopicConst;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * S78：死信主题（inspection.dlq）查看与重放。
 * 每次操作使用独立消费组 + earliest，保证"从头看/从头放"；重放后提交位点。
 * 注意：无法解析的消息重放后仍会再次失败回到死信（属预期，工具用于恢复"瞬时故障"类死信）。
 */
@Service
public class DlqService {

    private final KafkaTemplate<String, String> kafka;

    @Value("${spring.kafka.bootstrap-servers}")
    private String servers;

    public DlqService(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    private KafkaConsumer<String, String> newConsumer(String groupSuffix) {
        Properties p = new Properties();
        p.put("bootstrap.servers", servers);
        p.put("group.id", "dlq-tool-" + groupSuffix);
        p.put("key.deserializer", StringDeserializer.class.getName());
        p.put("value.deserializer", StringDeserializer.class.getName());
        p.put("enable.auto.commit", "false");
        p.put("auto.offset.reset", "earliest");
        p.put("max.poll.records", "500");
        return new KafkaConsumer<>(p);
    }

    /** 预览死信前 n 条（不提交位点，不影响重放）。 */
    public List<Map<String, Object>> peek(int n) {
        List<Map<String, Object>> out = new ArrayList<>();
        try (KafkaConsumer<String, String> c = newConsumer("peek")) {
            c.subscribe(List.of(TopicConst.DLQ));
            c.poll(Duration.ofSeconds(2));        // 触发分区分配
            c.seekToBeginning(c.assignment());
            int got = 0;
            long deadline = System.currentTimeMillis() + 10_000;
            while (got < n && System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> rs = c.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> r : rs) {
                    out.add(Map.of("partition", r.partition(), "offset", r.offset(),
                            "key", r.key() == null ? "" : r.key(), "value", r.value()));
                    got++;
                    if (got >= n) {
                        break;
                    }
                }
            }
        }
        return out;
    }

    /** 从头重放全部死信到 inspection.alarm，返回重放条数。 */
    public int replayAll() {
        int count = 0;
        try (KafkaConsumer<String, String> c = newConsumer("replay")) {
            c.subscribe(List.of(TopicConst.DLQ));
            c.poll(Duration.ofMillis(500));       // 触发分区分配
            c.seekToBeginning(c.assignment());
            long deadline = System.currentTimeMillis() + 30_000;
            boolean seenAny = false;
            while (System.currentTimeMillis() < deadline) {
                ConsumerRecords<String, String> rs = c.poll(Duration.ofMillis(1000));
                if (rs.isEmpty()) {
                    if (seenAny) {
                        break;                    // 已处理过记录后再空轮询 → 全部完成
                    }
                    continue;                     // seek 后的首次空轮询属复位抖动，继续等待
                }
                seenAny = true;
                for (ConsumerRecord<String, String> r : rs) {
                    kafka.send(TopicConst.INSPECTION_ALARM, r.key() == null ? "dlq-replay" : r.key(), r.value());
                    count++;
                }
                c.commitSync();
            }
        }
        return count;
    }
}

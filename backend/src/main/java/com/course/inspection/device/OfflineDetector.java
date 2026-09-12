package com.course.inspection.device;

import com.course.inspection.common.Json;
import com.course.inspection.common.TopicConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 离线检测（设计报告 FR-1.5 / 图 3-4）：
 * 每 5 秒扫描心跳超时（15s = 3 个心跳周期）的在线设备 → 置 OFFLINE 并产生 DEVICE_OFFLINE 告警。
 * 告警走 Kafka 进入统一告警链路（三写），保持单一数据通路。
 */
@Component
@EnableScheduling
public class OfflineDetector {

    private static final Logger log = LoggerFactory.getLogger(OfflineDetector.class);

    private static final long TIMEOUT_SECONDS = 15;

    private final MongoTemplate mongo;
    private final KafkaTemplate<String, String> kafka;

    public OfflineDetector(MongoTemplate mongo, KafkaTemplate<String, String> kafka) {
        this.mongo = mongo;
        this.kafka = kafka;
    }

    @Scheduled(fixedDelay = 5_000)
    public void detect() {
        Instant deadline = Instant.now().minusSeconds(TIMEOUT_SECONDS);
        Query query = Query.query(Criteria.where("status").is("ONLINE")
                .and("lastHeartbeat").lt(deadline));
        List<DeviceDoc> offline = mongo.find(query, DeviceDoc.class);

        for (DeviceDoc d : offline) {
            // 条件更新原子生效：仅"本次真的把 ONLINE 改成 OFFLINE"的实例才发告警，
            // 避免双后端实例定时器并发造成重复 DEVICE_OFFLINE（S30 排错实录）
            var result = mongo.updateFirst(
                    Query.query(Criteria.where("deviceId").is(d.getDeviceId()).and("status").is("ONLINE")),
                    new Update().set("status", "OFFLINE").set("currentTaskId", null),
                    DeviceDoc.class);
            if (result.getModifiedCount() == 0) {
                continue;
            }

            // 取设备最后上报位置作为告警坐标（供地理检索；无遥测时回退 0,0）
            DeviceStatusDoc last = mongo.findOne(
                    Query.query(Criteria.where("deviceId").is(d.getDeviceId()))
                            .with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "ts")),
                    DeviceStatusDoc.class);
            double lng = last == null ? 0.0 : last.getLng();
            double lat = last == null ? 0.0 : last.getLat();

            // DEVICE_OFFLINE 告警走统一告警链路
            String alarmId = "ALM-" + UUID.randomUUID().toString().substring(0, 8);
            String msg = Json.toJson(new java.util.HashMap<String, Object>() {{
                put("alarmId", alarmId);
                put("deviceId", d.getDeviceId());
                put("deviceType", d.getDeviceType());
                put("alarmType", "DEVICE_OFFLINE");
                put("level", "WARN");
                put("description", "设备心跳超时，判定离线");
                put("lng", lng);
                put("lat", lat);
                put("occurredTime", System.currentTimeMillis());
            }});
            kafka.send(TopicConst.INSPECTION_ALARM, d.getDeviceId(), msg);
            log.warn("设备心跳超时置为离线 deviceId={} lastHeartbeat={}", d.getDeviceId(), d.getLastHeartbeat());
        }
    }
}

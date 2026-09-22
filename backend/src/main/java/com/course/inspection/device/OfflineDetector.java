package com.course.inspection.device;

import com.course.inspection.alarm.AlarmDoc;
import com.course.inspection.alarm.AlarmEsDoc;
import com.course.inspection.alarm.AlarmSearchService;
import com.course.inspection.common.Json;
import com.course.inspection.common.TopicConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 *
 * S103 告警体系重构（用户需求）：
 * - DEVICE_OFFLINE 属设备运维类（AlarmCategories.DEVICE），产生时 WARN、主页不显示；
 * - {@link #escalateAndResolve()}：持续离线超过 {@link #OFFLINE_ESCALATE_MINUTES}（默认 3 分钟，
 *   电量耗尽/过热等均为正常运维范畴，只有"异常离线过久"才升级）→ 告警升级 CRITICAL 进入主页；
 * - 设备恢复上线 → 未终态的 DEVICE_OFFLINE 告警自动 RESOLVED（告警自愈，无需人工清理）。
 * 升级/恢复均为幂等条件更新，双实例并发安全。
 */
@Component
@EnableScheduling
public class OfflineDetector {

    private static final Logger log = LoggerFactory.getLogger(OfflineDetector.class);

    private static final long TIMEOUT_SECONDS = 15;

    /** 异常离线升级阈值（分钟）：超过该时长仍未恢复心跳才升级 CRITICAL。 */
    @Value("${alarm.offline-escalate-minutes:3}")
    private long offlineEscalateMinutes;

    private final MongoTemplate mongo;
    private final KafkaTemplate<String, String> kafka;
    private final AlarmSearchService searchService;

    public OfflineDetector(MongoTemplate mongo, KafkaTemplate<String, String> kafka,
                           AlarmSearchService searchService) {
        this.mongo = mongo;
        this.kafka = kafka;
        this.searchService = searchService;
    }

    @Scheduled(fixedDelay = 5_000)
    public void detect() {
        Instant deadline = Instant.now().minusSeconds(TIMEOUT_SECONDS);
        Query query = Query.query(Criteria.where("status").is("ONLINE")
                .and("lastHeartbeat").lt(deadline));
        List<DeviceDoc> offline = mongo.find(query, DeviceDoc.class);

        for (DeviceDoc d : offline) {
            markOffline(d.getDeviceId(), "设备心跳超时，判定离线");
        }
    }

    /**
     * S103：离线告警分级升级 + 恢复自动关闭（每 30 秒）。
     * 升级：DEVICE_OFFLINE 且 WARN 且非终态 且 occurredTime 距今超阈值 → CRITICAL；
     * 恢复：DEVICE_OFFLINE 非终态 且 对应设备已恢复 ONLINE → RESOLVED。
     */
    @Scheduled(fixedDelay = 30_000)
    public void escalateAndResolve() {
        Instant threshold = Instant.now().minusSeconds(offlineEscalateMinutes * 60);

        // ---- 升级：WARN 且发生时间早于阈值（持续离线过久）----
        Query upgradeQuery = Query.query(Criteria.where("alarmType").is("DEVICE_OFFLINE")
                .and("level").is("WARN")
                .and("status").in("PENDING", "CONFIRMED")
                .and("occurredTime").lt(threshold));
        for (AlarmDoc doc : mongo.find(upgradeQuery, AlarmDoc.class)) {
            var result = mongo.updateFirst(
                    Query.query(Criteria.where("alarmId").is(doc.getAlarmId()).and("level").is("WARN")),
                    new Update().set("level", "CRITICAL")
                            .set("description", doc.getDescription() + "；异常离线超过 "
                                    + offlineEscalateMinutes + " 分钟仍未恢复，升级为严重告警"),
                    AlarmDoc.class);
            if (result.getModifiedCount() > 0) {
                AlarmDoc updated = mongo.findById(doc.getAlarmId(), AlarmDoc.class);
                if (updated != null) {
                    searchService.index(AlarmEsDoc.from(updated));
                    log.warn("离线告警升级 CRITICAL alarmId={} deviceId={}",
                            doc.getAlarmId(), doc.getDeviceId());
                }
            }
        }

        // ---- 恢复自动关闭：非终态离线告警，且对应设备已恢复 ONLINE ----
        Query openQuery = Query.query(Criteria.where("alarmType").is("DEVICE_OFFLINE")
                .and("status").in("PENDING", "CONFIRMED"));
        for (AlarmDoc doc : mongo.find(openQuery, AlarmDoc.class)) {
            DeviceDoc dev = mongo.findById(doc.getDeviceId(), DeviceDoc.class);
            if (dev == null || !"ONLINE".equals(dev.getStatus())) {
                continue;
            }
            var result = mongo.updateFirst(
                    Query.query(Criteria.where("alarmId").is(doc.getAlarmId())
                            .and("status").in("PENDING", "CONFIRMED")),
                    new Update().set("status", "RESOLVED")
                            .set("description", doc.getDescription() + "；设备已恢复上线，告警自动关闭"),
                    AlarmDoc.class);
            if (result.getModifiedCount() > 0) {
                AlarmDoc updated = mongo.findById(doc.getAlarmId(), AlarmDoc.class);
                if (updated != null) {
                    searchService.index(AlarmEsDoc.from(updated));
                    log.info("设备恢复上线，离线告警自动关闭 alarmId={} deviceId={}",
                            doc.getAlarmId(), doc.getDeviceId());
                }
            }
        }
    }

    /**
     * 置 OFFLINE + 产生 DEVICE_OFFLINE 告警（幂等：仅 ONLINE→OFFLINE 的条件更新生效者发告警）。
     * 自然失联由 detect() 按 15s 阈值调用；手动下线由 DeviceController 指令驱动 5s 后调用（S50 用户要求提速）。
     */
    public boolean markOffline(String deviceId, String description) {
        var result = mongo.updateFirst(
                Query.query(Criteria.where("deviceId").is(deviceId).and("status").is("ONLINE")),
                new Update().set("status", "OFFLINE").set("currentTaskId", null),
                DeviceDoc.class);
        if (result.getModifiedCount() == 0) {
            return false;
        }

        DeviceDoc d = mongo.findById(deviceId, DeviceDoc.class);

        // 取设备最后上报位置作为告警坐标（供地理检索；无遥测时回退 0,0）
        DeviceStatusDoc last = mongo.findOne(
                Query.query(Criteria.where("deviceId").is(deviceId))
                        .with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "ts")),
                DeviceStatusDoc.class);
        double lng = last == null ? 0.0 : last.getLng();
        double lat = last == null ? 0.0 : last.getLat();

        // DEVICE_OFFLINE 告警走统一告警链路（S103：WARN 设备运维类，主页不显示；
        // 持续离线过久由 escalateAndResolve() 升级 CRITICAL）
        String alarmId = "ALM-" + UUID.randomUUID().toString().substring(0, 8);
        String msg = Json.toJson(new java.util.HashMap<String, Object>() {{
            put("alarmId", alarmId);
            put("deviceId", d.getDeviceId());
            put("deviceType", d.getDeviceType());
            put("alarmType", "DEVICE_OFFLINE");
            put("level", "WARN");
            put("description", description);
            put("lng", lng);
            put("lat", lat);
            put("occurredTime", System.currentTimeMillis());
        }});
        kafka.send(TopicConst.INSPECTION_ALARM, d.getDeviceId(), msg);
        log.warn("设备置为离线 deviceId={} reason={}", d.getDeviceId(), description);
        return true;
    }
}

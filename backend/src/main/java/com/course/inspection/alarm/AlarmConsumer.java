package com.course.inspection.alarm;

import com.course.inspection.common.AlarmMsg;
import com.course.inspection.common.Json;
import com.course.inspection.common.TopicConst;
import com.course.inspection.storage.EvidenceImageGenerator;
import com.course.inspection.storage.HdfsClient;
import com.course.inspection.storage.PathBuilder;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * 告警消费者（设计报告 5.2.2(4)，FR-2.6 幂等 / FR-3.1 证据归档）：
 * HDFS 证据图 → MongoDB 权威记录（alarmId upsert 幂等）→ ES 检索副本。
 * 仅 HDFS/Mongo 失败才抛异常重试（转死信）；ES 失败只记日志，由对账补偿（D-5）。
 */
@Component
public class AlarmConsumer {

    private static final Logger log = LoggerFactory.getLogger(AlarmConsumer.class);

    private final AlarmStore store;
    private final AlarmSearchService searchService;
    private final HdfsClient hdfs;
    private final PathBuilder paths;
    private final EvidenceImageGenerator evidence;
    private final com.course.inspection.task.TaskService taskService;

    public AlarmConsumer(AlarmStore store, AlarmSearchService searchService,
                         HdfsClient hdfs, PathBuilder paths, EvidenceImageGenerator evidence,
                         com.course.inspection.task.TaskService taskService) {
        this.store = store;
        this.searchService = searchService;
        this.hdfs = hdfs;
        this.paths = paths;
        this.evidence = evidence;
        this.taskService = taskService;
    }

    @KafkaListener(topics = TopicConst.INSPECTION_ALARM,
            groupId = TopicConst.GROUP_ALARM,
            containerFactory = "manualAckFactory")
    public void onAlarm(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            AlarmMsg msg = Json.fromJson(record.value(), AlarmMsg.class);

            // 幂等：已入库直接确认，避免重复生成证据图/重复写
            if (store.exists(msg.alarmId())) {
                log.warn("重复告警，幂等跳过 alarmId={}", msg.alarmId());
                ack.acknowledge();
                return;
            }

            // 1) HDFS 证据图（S30：仿真无真实图像，生成占位证据图打通归档链路）
            String snapshotPath = null;
            try {
                EvidenceImageGenerator.Evidence ev = evidence.generate(
                        msg.alarmId(), msg.alarmType(), msg.description(), msg.occurredTime());
                String path = paths.build("alarm_snapshot", msg.deviceId(),
                        Instant.ofEpochMilli(msg.occurredTime()), ev.ext());
                snapshotPath = hdfs.upload(path, ev.bytes());
            } catch (Exception e) {
                log.error("HDFS 证据图归档失败 alarmId={}，转重试/死信", msg.alarmId(), e);
                throw e;   // HDFS 失败：重试（超限转死信）
            }

            // 2) MongoDB 权威记录
            AlarmDoc doc = new AlarmDoc();
            doc.setAlarmId(msg.alarmId());
            doc.setDeviceId(msg.deviceId());
            doc.setDeviceType(msg.deviceType());
            doc.setAlarmType(msg.alarmType());
            doc.setLevel(msg.level());
            doc.setDescription(msg.description());
            doc.setLng(msg.lng());
            doc.setLat(msg.lat());
            doc.setOccurredTime(Instant.ofEpochMilli(msg.occurredTime()));
            doc.setSnapshotPath(snapshotPath);
            doc.setStatus("PENDING");
            store.upsert(doc);

            // 3) ES 检索副本（失败不回滚，D-5）
            try {
                searchService.index(AlarmEsDoc.from(doc));
            } catch (Exception e) {
                log.error("ES 写入失败，等待对账补偿 alarmId={}", msg.alarmId(), e);
            }

            // 4) S61 复核派单：新告警（DEVICE_OFFLINE 除外）触发就近机器狗复核；
            //    派单失败不影响告警落库（仅日志）。历史 10k 告警不重放，不会批量派单。
            if (!"DEVICE_OFFLINE".equals(msg.alarmType())) {
                try {
                    taskService.dispatchReview(msg.alarmId(), msg.lng(), msg.lat());
                } catch (Exception e) {
                    log.warn("复核派单失败（不影响告警落库） alarmId={}: {}", msg.alarmId(), e.getMessage());
                }
            }

            log.info("告警三写完成 alarmId={} type={} snapshot={}",
                    msg.alarmId(), msg.alarmType(), snapshotPath);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("告警消费失败（不提交，等待重投/死信） partition={} offset={}",
                    record.partition(), record.offset(), e);
            throw new org.springframework.kafka.KafkaException("alarm consume failed", e);
        }
    }
}

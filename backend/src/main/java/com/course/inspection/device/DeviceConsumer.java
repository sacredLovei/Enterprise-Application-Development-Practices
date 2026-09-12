package com.course.inspection.device;

import com.course.inspection.common.HeartbeatMsg;
import com.course.inspection.common.Json;
import com.course.inspection.common.RobotTelemetryMsg;
import com.course.inspection.common.UavTelemetryMsg;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 存储消费组（biz-storage-consumer）：
 * 心跳 → device 集合 upsert（幂等）；遥测 → device_status 时序集合。
 * 手动提交 offset：业务成功才提交（设计报告 5.2.2(3)，防消息丢失）。
 */
@Component
public class DeviceConsumer {

    private static final Logger log = LoggerFactory.getLogger(DeviceConsumer.class);

    private final DeviceStore store;

    public DeviceConsumer(DeviceStore store) {
        this.store = store;
    }

    @KafkaListener(topics = "device.heartbeat", groupId = "biz-storage-consumer")
    public void onHeartbeat(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            HeartbeatMsg msg = Json.fromJson(record.value(), HeartbeatMsg.class);
            store.upsertHeartbeat(msg);
            log.info("心跳落库 deviceId={} battery={}", msg.deviceId(), msg.battery());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("心跳消费失败，不提交 offset 等待重投 partition={} offset={}",
                    record.partition(), record.offset(), e);
            // 不提交 offset：由监听容器重投（S30 升级为死信兜底）
        }
    }

    @KafkaListener(topics = "uav.telemetry", groupId = "biz-storage-consumer")
    public void onTelemetry(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            UavTelemetryMsg msg = Json.fromJson(record.value(), UavTelemetryMsg.class);
            store.saveTelemetry(msg);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("遥测消费失败，不提交 offset 等待重投 partition={} offset={}",
                    record.partition(), record.offset(), e);
        }
    }

    /** 机器狗遥测（S40 补上：此前未消费导致地图无机器狗位置）。 */
    @KafkaListener(topics = "robot.telemetry", groupId = "biz-storage-consumer")
    public void onRobotTelemetry(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            RobotTelemetryMsg msg = Json.fromJson(record.value(), RobotTelemetryMsg.class);
            store.saveRobotTelemetry(msg);
            ack.acknowledge();
        } catch (Exception e) {
            log.error("机器狗遥测消费失败，不提交 offset 等待重投 partition={} offset={}",
                    record.partition(), record.offset(), e);
        }
    }
}

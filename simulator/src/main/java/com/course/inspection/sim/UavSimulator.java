package com.course.inspection.sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 无人机仿真（设计报告 5.2.1）：
 * - 心跳 5s 一次（FR-1.2）：证明在线；
 * - 遥测 2s 一次（FR-1.3）：位置在园区中心附近做小步漂移，模拟连续移动而非随机跳点。
 * 消息以 deviceId 为 key：同设备消息进同一分区，保证有序（设计报告 4.4.2）。
 */
@Component
public class UavSimulator {

    private static final Logger log = LoggerFactory.getLogger(UavSimulator.class);

    private static final double CENTER_LNG = 116.3974;
    private static final double CENTER_LAT = 39.9092;

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${sim.device-id:UAV-001}")
    private String deviceId;

    private int battery = 100;
    private double lng = CENTER_LNG;
    private double lat = CENTER_LAT;

    public UavSimulator(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    /** 心跳：5 秒一次（FR-1.2）。 */
    @Scheduled(fixedRate = 5_000)
    public void heartbeat() {
        battery = Math.max(0, battery - 1);
        HeartbeatMsg msg = new HeartbeatMsg(deviceId, "UAV", battery, null, System.currentTimeMillis());
        send("device.heartbeat", msg);
    }

    /** 遥测：2 秒一次（FR-1.3）。 */
    @Scheduled(fixedRate = 2_000)
    public void telemetry() {
        lng += (Math.random() - 0.5) * 0.0005;
        lat += (Math.random() - 0.5) * 0.0005;
        UavTelemetryMsg msg = new UavTelemetryMsg(
                deviceId,
                round(lng), round(lat),
                round(80 + Math.random() * 10),   // 高度 m
                round(8 + Math.random() * 4),      // 速度 m/s
                battery,
                System.currentTimeMillis());
        send("uav.telemetry", msg);
    }

    private void send(String topic, Object payload) {
        try {
            kafka.send(topic, deviceId, om.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("仿真消息发送失败 topic={}", topic, e);
        }
    }

    private static double round(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }
}

package com.course.inspection.sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 仿真设备基类（设计报告 5.2.1）：
 * - 心跳 5s（FR-1.2）；电量单调下降，归零触发 BATTERY_LOW 告警并回充（S31 电量循环）；
 * - 故障注入：BATTERY_DROP（电量骤降）/ COMM_OFFLINE（停止心跳，验证离线检测）。
 * 子类实现设备类型专属的遥测与告警逻辑。
 */
public abstract class DeviceSimulator {

    protected static final Logger log = LoggerFactory.getLogger(DeviceSimulator.class);

    private static final long RECHARGE_MILLIS = 30_000;   // 回充耗时 30 秒（演示用）

    protected final KafkaTemplate<String, String> kafka;
    protected final ObjectMapper om = new ObjectMapper();
    protected final TelemetryGenerator track = new TelemetryGenerator(116.3974, 39.9092);

    @Value("${sim.device-id:UAV-001}")
    protected String deviceId;

    @Value("${sim.device-type:UAV}")
    protected String deviceType;

    protected volatile int battery = 100;
    private volatile boolean recharging = false;
    private volatile boolean batteryLowFired = false;
    private volatile long rechargeDeadline = 0;

    private final AtomicBoolean heartbeatEnabled = new AtomicBoolean(true);

    protected DeviceSimulator(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    /** 心跳：5 秒一次（FR-1.2）；COMM_OFFLINE 注入后停止。 */
    @Scheduled(fixedRate = 5_000)
    public final void heartbeat() {
        if (!heartbeatEnabled.get()) {
            return;
        }
        tickBattery();
        HeartbeatMsg msg = new HeartbeatMsg(deviceId, deviceType, battery, null, System.currentTimeMillis());
        send("device.heartbeat", msg);
    }

    /** 电量循环：消耗 → 归零触发告警 → 回充（S31 新增，解决 S21 遗留项 #20①）。 */
    private void tickBattery() {
        long now = System.currentTimeMillis();
        if (recharging) {
            if (now >= rechargeDeadline) {
                battery = 100;
                recharging = false;
                batteryLowFired = false;
                log.info("回充完成 deviceId={} battery=100", deviceId);
            }
            return;
        }
        if (battery > 0) {
            battery = Math.max(0, battery - 1);
        }
        if (battery == 0 && !batteryLowFired) {
            batteryLowFired = true;
            recharging = true;
            rechargeDeadline = now + RECHARGE_MILLIS;
            emitAlarm("BATTERY_LOW", "WARN", "电量耗尽，自动返航回充");
        }
    }

    /** 故障注入（FR-1.4）。COMM_OFFLINE 与 BATTERY_DROP 通用；OVERHEAT 由机器狗实现。 */
    public void injectFault(String type) {
        switch (type) {
            case "COMM_OFFLINE" -> {
                heartbeatEnabled.set(false);
                log.info("故障注入 COMM_OFFLINE：心跳已停止 deviceId={}", deviceId);
            }
            case "BATTERY_DROP" -> {
                battery = Math.min(battery, 10);
                log.info("故障注入 BATTERY_DROP：电量骤降至 {} deviceId={}", battery, deviceId);
            }
            default -> log.warn("未知故障类型: {}", type);
        }
    }

    protected void emitAlarm(String alarmType, String level, String description) {
        AlarmMsg msg = new AlarmMsg(
                "ALM-" + UUID.randomUUID().toString().substring(0, 8),
                deviceId, deviceType, alarmType, level, description,
                track.currentLng(), track.currentLat(),
                System.currentTimeMillis());
        send("inspection.alarm", msg);
        log.info("告警产生 {} {} {} deviceId={}", alarmType, level, description, deviceId);
    }

    protected void send(String topic, Object payload) {
        try {
            kafka.send(topic, deviceId, om.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("仿真消息发送失败 topic={}", topic, e);
        }
    }

    protected double[] advance(double speed, double intervalSec) {
        return track.next(speed, intervalSec);
    }
}

package com.course.inspection.sim;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 无人机仿真（设计报告 5.2.1）：
 * 遥测 2s（高度/速度，FR-1.3）；随机产生周界入侵告警（高空广域发现的业务语义）。
 * S33：任务行为——定点复核/区域覆盖为空中直飞/空中扫掠，返航直线回基地。
 */
@Component
@ConditionalOnProperty(name = "sim.device-type", havingValue = "UAV")
public class UavSimulator extends DeviceSimulator {

    private final Random random = new Random();

    public UavSimulator(KafkaTemplate<String, String> kafka) {
        super(kafka);
    }

    /** 遥测：2 秒一次（FR-1.3）。速度 12 m/s。通信中断时停发（S40 修复）。 */
    @Scheduled(fixedRate = 2_000)
    public void telemetry() {
        if (!isCommUp()) {
            return;
        }
        double[] pos = advance(12, 2);
        UavTelemetryMsg msg = new UavTelemetryMsg(
                deviceId, pos[0], pos[1],
                round(80 + Math.random() * 10),   // 高度 m
                round(12 + Math.random() * 2),    // 速度 m/s
                battery,
                System.currentTimeMillis());
        send("uav.telemetry", msg);
        checkTaskCompletion();
    }

    /** 随机隐患发现：每 30 秒以约 15% 概率产生周界入侵告警（演示数据，FR-1.4 语义）。 */
    @Scheduled(fixedDelay = 30_000)
    public void patrolScan() {
        if (!isCommUp() || random.nextDouble() >= 0.15) {
            return;
        }
        emitAlarm("PERIMETER_BREACH", "CRITICAL", "周界检测到疑似人员活动，待地面复核");
    }

    /** 定点复核：空中直飞目标（用户定制：无人机直线飞无妨）。 */
    @Override
    protected void beginReview(TaskCommandMsg cmd) {
        double tlng = cmd.targetLng() == null ? track.currentLng() : cmd.targetLng();
        double tlat = cmd.targetLat() == null ? track.currentLat() : cmd.targetLat();
        track.goTo(tlng, tlat);
    }

    /** 区域覆盖：空中弓字扫掠。 */
    @Override
    protected void beginSweep(TaskCommandMsg cmd) {
        double clng = cmd.targetLng() == null ? track.currentLng() : cmd.targetLng();
        double clat = cmd.targetLat() == null ? track.currentLat() : cmd.targetLat();
        track.startSweep(clng, clat);
    }

    /** 返航：直线回基地。 */
    @Override
    protected void beginHome() {
        track.goHome();
    }

    private static double round(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }
}

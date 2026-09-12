package com.course.inspection.sim;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 机器狗仿真（设计报告 5.2.1）：
 * 遥测 2s（红外测温/环境传感，FR-1.3）；支持 OVERHEAT 故障注入。
 * S33（用户定制）：机器狗不能飞——点到点任务走园区地面路网最短路径（GroundNetwork BFS），
 * 区域覆盖为地面弓字扫掠（步行），返航沿路网最短路径回基地。
 */
@Component
@ConditionalOnProperty(name = "sim.device-type", havingValue = "ROBOT_DOG")
public class RobotDogSimulator extends DeviceSimulator {

    /** 红外温度（℃），OVERHEAT 注入后拉高。 */
    private volatile double irTemp = 48.0;
    private volatile long overheatUntil = 0;
    private volatile boolean overheatFired = false;

    public RobotDogSimulator(KafkaTemplate<String, String> kafka) {
        super(kafka);
    }

    /** 遥测：2 秒一次（FR-1.3）。地面行进速度 3.5 m/s（真实机器狗跑动速度，S33 调整便于观察）。通信中断时停发（S40 修复）。 */
    @Scheduled(fixedRate = 2_000)
    public void telemetry() {
        if (!isCommUp()) {
            return;
        }
        double[] pos = advance(3.5, 2);
        long now = System.currentTimeMillis();
        if (now < overheatUntil) {
            irTemp = 110 + Math.random() * 15;
            if (!overheatFired) {
                overheatFired = true;
                emitAlarm("DEVICE_OVERHEAT", "WARN", "设备红外温度异常升高");
            }
        } else {
            irTemp = 45 + Math.random() * 8;
        }
        RobotTelemetryMsg msg = new RobotTelemetryMsg(
                deviceId, pos[0], pos[1],
                round(irTemp),
                round(25 + Math.random() * 3),    // 环境温度
                round(50 + Math.random() * 10),   // 湿度
                round(Math.random() * 0.05),      // 烟雾
                round(Math.random() * 0.03),      // 可燃气
                round(3.5 + Math.random() * 0.5),
                battery,
                now);        send("robot.telemetry", msg);
        checkTaskCompletion();
    }

    /** 定点复核：沿地面路网最短路径奔向目标（不能飞，用户定制）。 */
    @Override
    protected void beginReview(TaskCommandMsg cmd) {
        double tlng = cmd.targetLng() == null ? track.currentLng() : cmd.targetLng();
        double tlat = cmd.targetLat() == null ? track.currentLat() : cmd.targetLat();
        track.followPath(GroundNetwork.shortestPath(track.currentLng(), track.currentLat(), tlng, tlat));
    }

    /** 区域覆盖：地面步行弓字扫掠。 */
    @Override
    protected void beginSweep(TaskCommandMsg cmd) {
        double clng = cmd.targetLng() == null ? track.currentLng() : cmd.targetLng();
        double clat = cmd.targetLat() == null ? track.currentLat() : cmd.targetLat();
        track.startSweep(clng, clat);
    }

    /** 返航：沿地面路网最短路径回基地。 */
    @Override
    protected void beginHome() {
        track.followPath(GroundNetwork.shortestPath(
                track.currentLng(), track.currentLat(),
                TelemetryGenerator.HOME_LNG, TelemetryGenerator.HOME_LAT));
    }

    /** OVERHEAT 故障注入（FR-1.4）：红外温度拉高 30 秒并触发告警。 */
    @Override
    public void injectFault(String type) {
        if ("OVERHEAT".equals(type)) {
            overheatUntil = System.currentTimeMillis() + 30_000;
            overheatFired = false;
            log.info("故障注入 OVERHEAT：红外温度拉高 deviceId={}", deviceId);
            return;
        }
        super.injectFault(type);
    }

    private static double round(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }
}

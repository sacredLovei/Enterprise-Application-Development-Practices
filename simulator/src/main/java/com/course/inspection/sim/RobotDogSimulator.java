package com.course.inspection.sim;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 机器狗仿真（设计报告 5.2.1）：
 * 遥测 2s（红外测温/环境传感，FR-1.3）；支持 OVERHEAT 故障注入（DEVICE_OVERHEAT 告警）。
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

    /** 遥测：2 秒一次（FR-1.3）。地面行进速度 1.2 m/s。 */
    @Scheduled(fixedRate = 2_000)
    public void telemetry() {
        double[] pos = advance(1.2, 2);
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
                round(1.2 + Math.random() * 0.3),
                battery,
                now);
        send("robot.telemetry", msg);
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

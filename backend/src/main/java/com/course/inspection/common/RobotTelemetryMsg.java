package com.course.inspection.common;

/** 机器狗遥测消息（robot.telemetry 主题）。与 simulator 工程同名 record 结构一致。 */
public record RobotTelemetryMsg(
        String deviceId,
        double lng,
        double lat,
        double irMaxTemp,
        double ambientTemp,
        double humidity,
        double smoke,
        double gas,
        double speed,
        int battery,
        long ts
) {
}

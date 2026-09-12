package com.course.inspection.sim;

/** 机器狗遥测消息（robot.telemetry 主题）。lng/lat 顺序即 GeoJSON [经度, 纬度]。 */
public record RobotTelemetryMsg(
        String deviceId,
        double lng,
        double lat,
        double irMaxTemp,      // 红外最高温 ℃
        double ambientTemp,    // 环境温度 ℃
        double humidity,       // 湿度 %
        double smoke,          // 烟雾浓度
        double gas,            // 可燃气浓度
        double speed,
        int battery,
        long ts
) {
}

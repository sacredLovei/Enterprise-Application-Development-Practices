package com.course.inspection.sim;

/** 无人机遥测消息契约（uav.telemetry 主题）。lng/lat 顺序即 GeoJSON [经度, 纬度]。 */
public record UavTelemetryMsg(
        String deviceId,
        double lng,
        double lat,
        double altitude,
        double speed,
        int battery,
        long ts
) {
}

package com.course.inspection.common;

/** 无人机遥测消息（uav.telemetry 主题）。lng/lat 顺序即 GeoJSON [经度, 纬度]。 */
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

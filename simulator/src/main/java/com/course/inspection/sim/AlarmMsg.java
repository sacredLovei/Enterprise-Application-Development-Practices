package com.course.inspection.sim;

/** 告警消息（inspection.alarm 主题，设计报告 FR-1.4 / 4.5.1 alarm 集合来源）。 */
public record AlarmMsg(
        String alarmId,
        String deviceId,
        String deviceType,
        String alarmType,      // BATTERY_LOW / DEVICE_OVERHEAT / PERIMETER_BREACH
        String level,          // WARN / CRITICAL
        String description,
        double lng,
        double lat,
        long occurredTime
) {
}

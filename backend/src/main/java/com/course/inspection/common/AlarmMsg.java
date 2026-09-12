package com.course.inspection.common;

/** 告警消息契约（inspection.alarm 主题）。与 simulator 工程同名 record 结构一致。 */
public record AlarmMsg(
        String alarmId,
        String deviceId,
        String deviceType,
        String alarmType,
        String level,
        String description,
        double lng,
        double lat,
        long occurredTime
) {
}

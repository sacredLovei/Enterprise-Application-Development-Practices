package com.course.inspection.sim;

/** 心跳消息契约（device.heartbeat 主题）。与 backend 工程同名 record 结构一致（S30 统一契约模块）。 */
public record HeartbeatMsg(
        String deviceId,
        String deviceType,
        int battery,
        String currentTaskId,
        long ts
) {
}

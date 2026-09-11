package com.course.inspection.common;

/**
 * 设备心跳消息（device.heartbeat 主题）。
 * 与 simulator 工程中的同名 record 结构一致——S30 前两工程各持一份，S30 引入公共契约模块。
 */
public record HeartbeatMsg(
        String deviceId,
        String deviceType,
        int battery,
        String currentTaskId,
        long ts
) {
}

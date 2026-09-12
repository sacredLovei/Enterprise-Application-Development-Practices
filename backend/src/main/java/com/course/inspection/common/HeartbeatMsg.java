package com.course.inspection.common;

/**
 * 设备心跳消息（device.heartbeat 主题）。
 * 与 simulator 工程中的同名 record 结构一致——S30 前两工程各持一份，S30 引入公共契约模块。
 * S61：新增 lng/lat（心跳携带位置，供设备台账 location 刷新与 2dsphere 就近派单）；旧消息缺省为 null，消费端兼容。
 */
public record HeartbeatMsg(
        String deviceId,
        String deviceType,
        int battery,
        String currentTaskId,
        Double lng,
        Double lat,
        long ts
) {
}

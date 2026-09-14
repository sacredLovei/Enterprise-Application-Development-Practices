package com.course.inspection.sim;

/** 影像元数据消息契约（inspection.image.meta 主题）。与 backend 工程同名 record 结构一致（S72）。 */
public record ImageMetaMsg(
        String imageId,
        String deviceId,
        String deviceType,
        String imageType,    // uav_patrol / dog_infrared
        double lng,
        double lat,
        long ts,
        String alarmId       // 关联告警编号；巡逻照片为 null
) {
}

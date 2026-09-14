package com.course.inspection.common;

/**
 * 影像元数据消息（inspection.image.meta 主题，S72 打通 BUG-008 设计差异）。
 * 与 simulator 工程同名 record 结构一致：设备拍摄照片的"信息卡片"（图片本体仍由平台生成入 HDFS）。
 */
public record ImageMetaMsg(
        String imageId,
        String deviceId,
        String deviceType,
        String imageType,    // uav_patrol / dog_infrared（口径见 STATE 注册表 HDFS 路径 imageType）
        double lng,
        double lat,
        long ts,
        String alarmId       // 关联告警编号；巡逻照片为 null
) {
}

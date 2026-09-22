package com.course.inspection.alarm;

import java.util.Set;

/**
 * S103 告警分类（用户需求：设备类默认隐藏、查询可见；新增烟火告警）：
 * 以告警类型的静态映射派生分类，不新增存储字段——存量数据天然兼容。
 *
 * - SECURITY 安防类（默认显示）：周界入侵、烟火告警——需要人工立即介入；
 * - DEVICE 设备运维类（默认隐藏）：过热 / 低电 / 离线——正常运维事件，
 *   仅"异常离线过久"升级 CRITICAL 后进入主页（详见 OfflineDetector）。
 */
public final class AlarmCategories {

    public static final String SECURITY = "SECURITY";
    public static final String DEVICE = "DEVICE";

    public static final Set<String> SECURITY_TYPES = Set.of("PERIMETER_BREACH", "FIRE_SMOKE");
    public static final Set<String> DEVICE_TYPES = Set.of("DEVICE_OVERHEAT", "BATTERY_LOW", "DEVICE_OFFLINE");

    private AlarmCategories() {
    }

    public static String categoryOf(String alarmType) {
        return SECURITY_TYPES.contains(alarmType) ? SECURITY : DEVICE;
    }
}

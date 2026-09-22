package com.course.inspection.common;

/**
 * 园区边界（S104，用户需求：派任务的地点限定在园区内，不可外派）。
 * 大型工业园区：lng 116.3952~116.4000 × lat 39.9076~39.9110（约 425m × 380m）。
 * 坐标口径与仿真器 TelemetryGenerator 巡逻环线 / 前端 campus.js 保持一致。
 */
public final class CampusBounds {

    public static final double MIN_LNG = 116.3952;
    public static final double MAX_LNG = 116.4000;
    public static final double MIN_LAT = 39.9076;
    public static final double MAX_LAT = 39.9110;

    private CampusBounds() {
    }

    /** 目标点是否在园区范围内（含边界）。 */
    public static boolean contains(Double lng, Double lat) {
        if (lng == null || lat == null) {
            return false;
        }
        return lng >= MIN_LNG && lng <= MAX_LNG && lat >= MIN_LAT && lat <= MAX_LAT;
    }
}

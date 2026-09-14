package com.course.inspection.sim;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S77：遥测轨迹生成器单测（航点插值 + 速度推进）。 */
class TelemetryGeneratorTest {

    @Test
    void advanceMovesBySpeedTimesInterval() {
        TelemetryGenerator t = new TelemetryGenerator(116.3974, 39.9092);
        double lng0 = t.currentLng();
        double lat0 = t.currentLat();
        double[] next = t.next(3.5, 2.0);   // 3.5 m/s × 2s = 7m
        double dLng = next[0] - lng0;
        double dLat = next[1] - lat0;
        double meters = Math.sqrt(dLng * dLng + dLat * dLat)
                * 111_000 * Math.cos(Math.toRadians(lat0));   // 粗略换算（度→米）
        // 移动距离应接近 7m（插值航段长度不一，允许 0~15m 量级）
        assertTrue(meters > 0, "应发生位移");
        assertTrue(meters < 20, "位移量级异常: " + meters + "m");
    }

    @Test
    void patrolLoopKeepsDeviceInsideCampus() {
        TelemetryGenerator t = new TelemetryGenerator(116.3974, 39.9092);
        for (int i = 0; i < 300; i++) {
            t.next(12, 2.0);
        }
        // 巡逻 10 分钟后仍落在园区 bbox 附近（口径：园区中心 116.3974/39.9092，半径 ~1km）
        assertTrue(Math.abs(t.currentLng() - 116.3974) < 0.02, "lng 越界: " + t.currentLng());
        assertTrue(Math.abs(t.currentLat() - 39.9092) < 0.02, "lat 越界: " + t.currentLat());
    }

    @Test
    void goToEventuallyArrives() {
        TelemetryGenerator t = new TelemetryGenerator(116.3974, 39.9092);
        t.goTo(116.3969, 39.9101);
        for (int i = 0; i < 300 && !t.pointArrived(); i++) {
            t.next(3.5, 2.0);
        }
        assertTrue(t.pointArrived(), "300 步内应到达目标");
        assertEquals(116.3969, t.currentLng(), 1e-6);
        assertEquals(39.9101, t.currentLat(), 1e-6);
    }
}

package com.course.inspection.sim;

import java.util.List;
import java.util.Random;

/**
 * 连续轨迹生成器（设计报告 5.2.1(1)）：
 * 在航点间线性插值并叠加高斯噪声模拟定位漂移，避免"随机跳点"失真。
 */
public class TelemetryGenerator {

    /** 航点序列（园区周界巡航线），lng/lat 顺序与 GeoJSON 一致。 */
    private static final List<double[]> WAYPOINTS = List.of(
            new double[]{116.3968, 39.9088},
            new double[]{116.3982, 39.9088},
            new double[]{116.3982, 39.9102},
            new double[]{116.3968, 39.9102}
    );

    private final Random random = new Random();
    private int waypointIndex = 0;
    private double progress = 0.0;
    private double lng;
    private double lat;

    public TelemetryGenerator(double startLng, double startLat) {
        this.lng = startLng;
        this.lat = startLat;
    }

    /** 每步推进轨迹：按速度推进航点插值，返回 [lng, lat]。 */
    public synchronized double[] next(double speedMetersPerSec, double intervalSec) {
        double[] from = WAYPOINTS.get(waypointIndex);
        double[] to = WAYPOINTS.get((waypointIndex + 1) % WAYPOINTS.size());
        double segmentMeters = distanceMeters(from, to);
        double step = (speedMetersPerSec * intervalSec) / segmentMeters;
        progress += step;
        if (progress >= 1.0) {
            progress = 0.0;
            waypointIndex = (waypointIndex + 1) % WAYPOINTS.size();
            from = WAYPOINTS.get(waypointIndex);
            to = WAYPOINTS.get((waypointIndex + 1) % WAYPOINTS.size());
        }
        lng = from[0] + (to[0] - from[0]) * progress;
        lat = from[1] + (to[1] - from[1]) * progress;

        // 高斯噪声模拟定位漂移（约 ±3 米）
        double latNoise = random.nextGaussian() * 3.0 / 111_000d;
        double lngNoise = random.nextGaussian() * 3.0 / (111_000d * Math.cos(Math.toRadians(lat)));
        return new double[]{round(lng + lngNoise), round(lat + latNoise)};
    }

    public double currentLng() {
        return lng;
    }

    public double currentLat() {
        return lat;
    }

    private static double distanceMeters(double[] a, double[] b) {
        double dLat = Math.toRadians(b[1] - a[1]);
        double dLng = Math.toRadians(b[0] - a[0]);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(a[1])) * Math.cos(Math.toRadians(b[1]))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * 6371000 * Math.asin(Math.sqrt(h));
    }

    private static double round(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }
}

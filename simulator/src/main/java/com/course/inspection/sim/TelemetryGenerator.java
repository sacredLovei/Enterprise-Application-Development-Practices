package com.course.inspection.sim;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 多模式轨迹生成器（S33）：真实任务行为
 * PATROL 周界巡线一圈 / PATH 沿路网点序列（机器狗地面最短路径）/
 * GOTO 直飞目标（无人机）/ LOITER 目标点盘旋 / SWEEP 区域弓字扫掠 / HOME 直线返航。
 */
public class TelemetryGenerator {

    public enum Mode { PATROL, PATH, GOTO, LOITER, SWEEP, HOME }

    private static final double ARRIVE_METERS = 10.0;
    /** 基地（返航目的地）。S104：基地位于园区西南内侧。 */
    public static final double HOME_LNG = 116.3956;
    public static final double HOME_LAT = 39.9082;

    /**
     * 周界航点（S104：大型工业园区巡逻环线，8 点含内凹路口，顺时针一圈）。
     * 园区范围 lng 116.3952~116.4000 × lat 39.9076~39.9110（约 425m × 380m）。
     */
    private static final List<double[]> WAYPOINTS = List.of(
            new double[]{116.3954, 39.9078},
            new double[]{116.3998, 39.9078},
            new double[]{116.3998, 39.9108},
            new double[]{116.3976, 39.9108},
            new double[]{116.3976, 39.9092},
            new double[]{116.3964, 39.9092},
            new double[]{116.3964, 39.9108},
            new double[]{116.3954, 39.9108});

    private final Random random = new Random();

    private Mode mode = Mode.PATROL;
    private double lng;
    private double lat;
    private int waypointIndex = random.nextInt(WAYPOINTS.size());
    private double progress = random.nextDouble();
    private int loopsCompleted = 0;
    private boolean loopDoneFlag = false;

    private double targetLng;
    private double targetLat;
    private List<double[]> path = new ArrayList<>();
    private int pathIndex = 0;
    private boolean pointArrivedFlag = false;
    private boolean sweepFinishedFlag = false;
    private boolean sweepInited = false;
    private boolean sweepModeFlag = false;

    public TelemetryGenerator(double startLng, double startLat) {
        this.lng = startLng;
        this.lat = startLat;
    }

    public synchronized double[] next(double speed, double intervalSec) {
        switch (mode) {
            case GOTO -> stepGoto(speed, intervalSec);
            case HOME -> stepGoto(speed, intervalSec);
            case PATH, SWEEP -> stepPath(speed, intervalSec);
            case LOITER -> loiter();
            default -> stepPatrol(speed, intervalSec);
        }
        return new double[]{round(lng), round(lat)};
    }

    // ---------- 模式切换入口 ----------

    /** 周界巡逻一圈（重置计数）。 */
    public synchronized void startPatrolLoop() {
        mode = Mode.PATROL;
        loopDoneFlag = false;
        sweepModeFlag = false;
        pathIndex = 0;
    }

    /** 无人机直飞目标。 */
    public synchronized void goTo(double tlng, double tlat) {
        targetLng = tlng;
        targetLat = tlat;
        pointArrivedFlag = false;
        sweepModeFlag = false;
        mode = Mode.GOTO;
    }

    /** 沿给定路网点序列行进（机器狗地面最短路径）。 */
    public synchronized void followPath(List<double[]> pts) {
        path = new ArrayList<>(pts);
        pathIndex = 0;
        pointArrivedFlag = false;
        sweepModeFlag = false;
        mode = Mode.PATH;
        if (!path.isEmpty()) {
            targetLng = path.get(path.size() - 1)[0];
            targetLat = path.get(path.size() - 1)[1];
        }
    }

    /** 直线返航回基地。 */
    public synchronized void goHome() {
        goTo(HOME_LNG, HOME_LAT);
        mode = Mode.HOME;
    }

    /** 区域弓字扫掠：以目标为中心的矩形（360m×240m，行距 40m）。 */
    public synchronized void startSweep(double clng, double clat) {
        sweepBaseLng = clng;
        sweepBaseLat = clat;
        sweepInited = false;
        sweepFinishedFlag = false;
        pointArrivedFlag = false;
        sweepModeFlag = true;
        mode = Mode.SWEEP;
    }

    // ---------- 完成判定 ----------

    public synchronized boolean pointArrived() {
        return pointArrivedFlag;
    }

    public synchronized boolean sweepFinished() {
        return sweepFinishedFlag;
    }

    public synchronized boolean patrolLoopDone() {
        return loopDoneFlag;
    }

    public double currentLng() {
        return lng;
    }

    public double currentLat() {
        return lat;
    }

    // ---------- 各模式步进 ----------

    private void stepPatrol(double speed, double intervalSec) {
        double[] from = WAYPOINTS.get(waypointIndex);
        double[] to = WAYPOINTS.get((waypointIndex + 1) % WAYPOINTS.size());
        double segment = distanceMeters(lng, lat, to[0], to[1]);
        double step = speed * intervalSec;
        if (segment <= step) {
            lng = to[0];
            lat = to[1];
            waypointIndex = (waypointIndex + 1) % WAYPOINTS.size();
            if (waypointIndex == 0) {
                loopDoneFlag = true;
            }
            return;
        }
        double f = step / segment;
        lng += (to[0] - lng) * f;
        lat += (to[1] - lat) * f;
    }

    private void stepGoto(double speed, double intervalSec) {
        double d = distanceMeters(lng, lat, targetLng, targetLat);
        double step = speed * intervalSec;
        if (d <= ARRIVE_METERS || d <= step) {
            lng = targetLng;
            lat = targetLat;
            mode = Mode.LOITER;
            pointArrivedFlag = true;
            return;
        }
        double f = step / d;
        lng += (targetLng - lng) * f;
        lat += (targetLat - lat) * f;
    }

    private void stepPath(double speed, double intervalSec) {
        if (!sweepInited && mode == Mode.SWEEP) {
            initSweepPath();
            sweepInited = true;
        }
        if (pathIndex >= path.size()) {
            mode = Mode.LOITER;
            pointArrivedFlag = true;
            if (sweepModeFlag) {
                sweepFinishedFlag = true;
            }
            return;
        }
        double[] p = path.get(pathIndex);
        double d = distanceMeters(lng, lat, p[0], p[1]);
        double step = speed * intervalSec;
        if (d <= step) {
            lng = p[0];
            lat = p[1];
            pathIndex++;
            return;
        }
        double f = step / d;
        lng += (p[0] - lng) * f;
        lat += (p[1] - lat) * f;
    }

    private double sweepBaseLng;
    private double sweepBaseLat;

    private void initSweepPath() {
        double latStep = 40.0 / 111_000d;
        double lngSpan = 200.0 / (111_000d * Math.cos(Math.toRadians(sweepBaseLat)));
        double lng0 = sweepBaseLng - lngSpan / 2;
        double lng1 = sweepBaseLng + lngSpan / 2;
        int rows = 4;
        path = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            double rowLat = sweepBaseLat + r * latStep;
            if (r % 2 == 0) {
                path.add(new double[]{lng0, rowLat});
                path.add(new double[]{lng1, rowLat});
            } else {
                path.add(new double[]{lng1, rowLat});
                path.add(new double[]{lng0, rowLat});
            }
        }
        pathIndex = 0;
    }

    private void loiter() {
        double radius = 0.00006;
        double a = System.currentTimeMillis() / 1000.0;
        lng = targetLng + radius * Math.cos(a);
        lat = targetLat + radius * Math.sin(a);
    }

    private static double distanceMeters(double aLng, double aLat, double bLng, double bLat) {
        double dLat = Math.toRadians(bLat - aLat);
        double dLng = Math.toRadians(bLng - aLng);
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(aLat)) * Math.cos(Math.toRadians(bLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * 6371000 * Math.asin(Math.sqrt(h));
    }

    private static double round(double v) {
        return Math.round(v * 1_000_000d) / 1_000_000d;
    }
}

package com.course.inspection.storage;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** S77：HDFS 路径规范单测（口径：/inspection/{type}/{yyyy}/{MM}/{dd}/{deviceId}/{uuid12}.{ext}）。 */
class PathBuilderTest {

    private final PathBuilder paths = new PathBuilder();

    @Test
    void buildsExpectedLayout() {
        String path = paths.build("alarm_snapshot", "UAV-001",
                Instant.parse("2026-09-14T02:00:00Z"), "png");
        assertTrue(path.matches(
                "/inspection/alarm_snapshot/2026/09/14/UAV-001/[a-f0-9]{12}\\.png"),
                "path=" + path);
    }

    @Test
    void usesDeviceIdAndExt() {
        String path = paths.build("manual_review", "ROBOT-002",
                Instant.parse("2026-09-14T02:00:00Z"), "jpg");
        assertTrue(path.contains("/ROBOT-002/"));
        assertTrue(path.endsWith(".jpg"));
    }

    @Test
    void uuidPartDiffersPerCall() {
        String a = paths.build("uav_patrol", "UAV-001", Instant.now(), "png");
        String b = paths.build("uav_patrol", "UAV-001", Instant.now(), "png");
        assertFalse(a.equals(b), "两次生成路径应不同（随机 uuid 段）");
    }
}

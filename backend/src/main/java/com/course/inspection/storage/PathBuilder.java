package com.course.inspection.storage;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/** HDFS 路径规范（设计报告 4.4.4 / STATE 注册表）：/inspection/{type}/{yyyy}/{MM}/{dd}/{deviceId}/{uuid}.{ext} */
@Component
public class PathBuilder {

    public String build(String imageType, String deviceId, Instant time, String ext) {
        LocalDate d = time.atZone(ZoneId.of("Asia/Shanghai")).toLocalDate();
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return String.format("/inspection/%s/%04d/%02d/%02d/%s/%s.%s",
                imageType, d.getYear(), d.getMonthValue(), d.getDayOfMonth(),
                deviceId, uuid, ext);
    }
}

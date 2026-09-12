package com.course.inspection.alarm;

import com.course.inspection.storage.EvidenceImageGenerator;
import com.course.inspection.storage.HdfsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * S63 维护接口：证据图重生成。
 * 背景（风险 #38）：后端镜像原缺 CJK 字体，历史告警证据图中的中文描述被 AWT 画成占位方块；
 * 字体修复后新告警正常，本接口将存量中文描述证据图按原 HDFS 路径覆盖重生成（WebHDFS overwrite=true）。
 */
@RestController
public class EvidenceMaintenanceController {

    private static final Logger log = LoggerFactory.getLogger(EvidenceMaintenanceController.class);

    /** 中文描述过滤（Java 正则，避开 Mongo PCRE 不支持 Unicode 转义的口径坑）。 */
    private static final java.util.regex.Pattern CJK = java.util.regex.Pattern.compile("[\\u4e00-\\u9fff]");

    private final MongoTemplate mongo;
    private final EvidenceImageGenerator evidence;
    private final HdfsClient hdfs;

    public EvidenceMaintenanceController(MongoTemplate mongo, EvidenceImageGenerator evidence, HdfsClient hdfs) {
        this.mongo = mongo;
        this.evidence = evidence;
        this.hdfs = hdfs;
    }

    /** POST /api/maintenance/regenerate-evidence：重生成含中文描述的告警证据图（覆盖原路径，幂等）。 */
    @PostMapping("/api/maintenance/regenerate-evidence")
    public Map<String, Object> regenerate() {
        long start = System.currentTimeMillis();
        List<AlarmDoc> alarms = mongo.find(Query.query(Criteria.where("snapshotPath").ne(null)), AlarmDoc.class);
        int ok = 0;
        int fail = 0;
        int skipped = 0;
        for (AlarmDoc a : alarms) {
            if (a.getDescription() == null || !CJK.matcher(a.getDescription()).find()) {
                skipped++;
                continue;
            }
            try {
                EvidenceImageGenerator.Evidence ev = evidence.generate(
                        a.getAlarmId(), a.getAlarmType(), a.getDescription(),
                        a.getOccurredTime().toEpochMilli());
                hdfs.upload(a.getSnapshotPath(), ev.bytes());
                ok++;
            } catch (Exception e) {
                fail++;
                log.warn("证据图重生成失败 alarmId={}: {}", a.getAlarmId(), e.getMessage());
            }
        }
        log.info("证据图重生成完成 total={} ok={} fail={} skipped={} elapsedMs={}",
                alarms.size(), ok, fail, skipped, System.currentTimeMillis() - start);
        return Map.of("total", alarms.size(), "ok", ok, "fail", fail, "skipped", skipped,
                "elapsedMs", System.currentTimeMillis() - start);
    }
}

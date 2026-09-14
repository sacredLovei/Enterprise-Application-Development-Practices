package com.course.inspection.alarm;

import com.course.inspection.storage.EvidenceImageGenerator;
import com.course.inspection.storage.HdfsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final AlarmReconcileService reconcile;
    private final co.elastic.clients.elasticsearch.ElasticsearchClient esClient;
    private final DlqService dlq;

    public EvidenceMaintenanceController(MongoTemplate mongo, EvidenceImageGenerator evidence,
                                         HdfsClient hdfs, AlarmReconcileService reconcile,
                                         co.elastic.clients.elasticsearch.ElasticsearchClient esClient,
                                         DlqService dlq) {
        this.mongo = mongo;
        this.evidence = evidence;
        this.hdfs = hdfs;
        this.reconcile = reconcile;
        this.esClient = esClient;
        this.dlq = dlq;
    }

    /** S78：预览死信主题前 n 条。 */
    @GetMapping("/api/maintenance/dlq/peek")
    public List<Map<String, Object>> dlqPeek(@RequestParam(defaultValue = "5") int n) {
        return dlq.peek(n);
    }

    /** S78：从头重放全部死信到 inspection.alarm。 */
    @PostMapping("/api/maintenance/dlq/replay-all")
    public Map<String, Object> dlqReplayAll() {
        return Map.of("replayed", dlq.replayAll());
    }

    /**
     * S74（用户反馈）：清理测试注入数据——S50/S62/S63/S72 各轮验收注入的告警
     * （前缀 pt3k-/tc017-/v06/fontfix-）污染演示数据，从 Mongo 权威、ES 副本、
     * 关联复核任务与任务日志中一并删除。自然告警（ALM-*）保留。
     */
    @PostMapping("/api/maintenance/cleanup-test-data")
    public Map<String, Object> cleanupTestData() {
        List<String> prefixes = List.of("pt3k-", "tc017-", "v06", "fontfix-");
        Criteria or = new Criteria().orOperator(prefixes.stream()
                .map(p -> Criteria.where("alarmId").regex("^" + java.util.regex.Pattern.quote(p)))
                .toArray(Criteria[]::new));
        List<AlarmDoc> victims = mongo.find(Query.query(or), AlarmDoc.class);
        Set<String> ids = victims.stream().map(AlarmDoc::getAlarmId).collect(java.util.stream.Collectors.toSet());
        long alarms = ids.size();

        long tasks = 0;
        long taskLogs = 0;
        long esDeleted = 0;
        if (!ids.isEmpty()) {
            mongo.remove(Query.query(or), AlarmDoc.class);
            // 关联复核任务与任务日志
            List<com.course.inspection.task.TaskDoc> ts = mongo.find(
                    Query.query(Criteria.where("alarmId").in(ids)),
                    com.course.inspection.task.TaskDoc.class);
            Set<String> taskIds = ts.stream().map(com.course.inspection.task.TaskDoc::getTaskId)
                    .collect(java.util.stream.Collectors.toSet());
            if (!taskIds.isEmpty()) {
                tasks = mongo.remove(Query.query(Criteria.where("taskId").in(taskIds)),
                        com.course.inspection.task.TaskDoc.class).getDeletedCount();
                taskLogs = mongo.remove(Query.query(Criteria.where("taskId").in(taskIds)),
                        com.course.inspection.task.TaskLogDoc.class).getDeletedCount();
            }
            // ES 副本：按前缀 delete_by_query（alarmId 为 keyword，前缀查询命中）
            try {
                for (String p : prefixes) {
                    esDeleted += esClient.deleteByQuery(d -> d.index(AlarmIndexInitializer.INDEX)
                            .query(q -> q.prefix(pq -> pq.field("alarmId").value(p)))).deleted();
                }
            } catch (Exception e) {
                log.error("测试数据 ES 清理失败（Mongo 已清，ES 由对账兜底）: {}", e.getMessage());
            }
        }
        log.info("测试数据清理完成 alarms={} tasks={} taskLogs={} esDeleted={}",
                alarms, tasks, taskLogs, esDeleted);
        return Map.of("alarms", alarms, "tasks", tasks, "taskLogs", taskLogs, "esDeleted", esDeleted);
    }

    /** S70：手动触发对账（验收用；平时由 5 分钟定时任务执行）。 */
    @PostMapping("/api/maintenance/reconcile")
    public AlarmReconcileService.ReconcileResult reconcileNow() {
        return reconcile.run();
    }

    /** S74：全量对账（30 天窗口），修复历史存量缺口。 */
    @PostMapping("/api/maintenance/reconcile-full")
    public AlarmReconcileService.ReconcileResult reconcileFull() {
        return reconcile.runFull();
    }

    /** S70：最近一次对账结果。 */
    @GetMapping("/api/maintenance/reconcile-status")
    public AlarmReconcileService.ReconcileResult reconcileStatus() {
        AlarmReconcileService.ReconcileResult r = reconcile.lastResult();
        if (r == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.NOT_FOUND, "尚未执行过对账");
        }
        return r;
    }

    /** POST /api/maintenance/regenerate-evidence：重生成告警证据图（覆盖原路径，幂等）。
     *  S63 起用于中文字体回填；S82 起 all=true 全量重生成（分辨率升级 1280x720）。 */
    @PostMapping("/api/maintenance/regenerate-evidence")
    public Map<String, Object> regenerate(@RequestParam(defaultValue = "false") boolean all) {
        long start = System.currentTimeMillis();
        List<AlarmDoc> alarms = mongo.find(Query.query(Criteria.where("snapshotPath").ne(null)), AlarmDoc.class);
        int ok = 0;
        int fail = 0;
        int skipped = 0;
        for (AlarmDoc a : alarms) {
            if (!all && (a.getDescription() == null || !CJK.matcher(a.getDescription()).find())) {
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

    /** S82：重生成存量复核红外图（覆盖原路径，分辨率升级 1280x720）。 */
    @PostMapping("/api/maintenance/regenerate-review-images")
    public Map<String, Object> regenerateReviewImages() {
        long start = System.currentTimeMillis();
        List<AlarmDoc> alarms = mongo.find(
                Query.query(Criteria.where("review.imagePath").ne(null)), AlarmDoc.class);
        int ok = 0;
        int fail = 0;
        for (AlarmDoc a : alarms) {
            try {
                EvidenceImageGenerator.Evidence ev = evidence.generateReview(
                        a.getAlarmId(), a.getReview().getConclusion(), a.getReview().getReviewerDeviceId());
                hdfs.upload(a.getReview().getImagePath(), ev.bytes());
                ok++;
            } catch (Exception e) {
                fail++;
                log.warn("复核图重生成失败 alarmId={}: {}", a.getAlarmId(), e.getMessage());
            }
        }
        log.info("复核图重生成完成 total={} ok={} fail={} elapsedMs={}",
                alarms.size(), ok, fail, System.currentTimeMillis() - start);
        return Map.of("total", alarms.size(), "ok", ok, "fail", fail,
                "elapsedMs", System.currentTimeMillis() - start);
    }
}

package com.course.inspection.alarm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * S70：Mongo↔ES 对账补偿（D-5 承诺落地）。
 * 每 5 分钟比对"最近 6 小时"窗口（窗口内文档数远小于 ES from+size 10,000 上限，可直接分页）：
 * Mongo 权威账本的 alarmId 集合 vs ES 检索副本的 alarmId 集合，差集即为 ES 缺失文档，逐条补写。
 * 结果留存内存可查（/api/maintenance/reconcile-status），并可手动触发（/api/maintenance/reconcile）。
 */
@Service
@EnableScheduling
public class AlarmReconcileService {

    private static final Logger log = LoggerFactory.getLogger(AlarmReconcileService.class);

    /** 对账窗口：最近 6 小时（窗口内文档数 << 10k，规避深分页限制；历史差异由窗口滚动自然覆盖）。 */
    private static final long WINDOW_SECONDS = 6 * 3600L;

    private final MongoTemplate mongo;
    private final AlarmSearchService searchService;

    /** 最近一次对账结果（内存态，供状态查询）。 */
    public record ReconcileResult(Instant ranAt, long mongoCount, long esCount, int fixed, long elapsedMs) {
    }

    private volatile ReconcileResult last;

    public AlarmReconcileService(MongoTemplate mongo, AlarmSearchService searchService) {
        this.mongo = mongo;
        this.searchService = searchService;
    }

    @Scheduled(fixedDelay = 300_000)
    public void scheduled() {
        run(WINDOW_SECONDS);
    }

    public synchronized ReconcileResult run() {
        return run(WINDOW_SECONDS);
    }

    /** S74：全量对账（30 天窗口 = 全部有效数据，用于修复历史存量缺口；窗口内文档数 << 10k 上限）。 */
    public synchronized ReconcileResult runFull() {
        return run(30L * 24 * 3600);
    }

    public synchronized ReconcileResult run(long windowSeconds) {
        long start = System.currentTimeMillis();
        Instant from = Instant.now().minusSeconds(windowSeconds);

        // 1) Mongo 权威：窗口内全部告警（近 6 小时量级，直接取回）
        List<AlarmDoc> docs = mongo.find(
                Query.query(Criteria.where("occurredTime").gte(from)), AlarmDoc.class);
        Set<String> mongoIds = docs.stream().map(AlarmDoc::getAlarmId).collect(Collectors.toSet());

        // 2) ES 副本：窗口内全部 alarmId（分页取回，窗口远小于 10k 上限）
        Set<String> esIds = new HashSet<>();
        int page = 0;
        while (true) {
            var resp = searchService.search(new AlarmSearchService.AlarmQuery(
                    null, null, null, null, null,
                    String.valueOf(from.toEpochMilli()), "now", null, null, null, page, 1000));
            if (resp.records().isEmpty()) {
                break;
            }
            esIds.addAll(resp.records().stream().map(AlarmEsDoc::getAlarmId).toList());
            if ((long) (page + 1) * 1000 >= resp.total()) {
                break;
            }
            page++;
        }

        // 3) 差集补写
        int fixed = 0;
        for (AlarmDoc d : docs) {
            if (!esIds.contains(d.getAlarmId())) {
                try {
                    searchService.index(AlarmEsDoc.from(d));
                    fixed++;
                } catch (Exception e) {
                    log.error("对账补写失败 alarmId={}: {}", d.getAlarmId(), e.getMessage());
                }
            }
        }

        ReconcileResult result = new ReconcileResult(Instant.now(), mongoIds.size(), esIds.size(),
                fixed, System.currentTimeMillis() - start);
        last = result;
        log.info("对账完成 mongo={} es={} fixed={} elapsedMs={}", result.mongoCount(), result.esCount(),
                result.fixed(), result.elapsedMs());
        return result;
    }

    public ReconcileResult lastResult() {
        return last;
    }
}

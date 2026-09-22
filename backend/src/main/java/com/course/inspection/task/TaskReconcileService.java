package com.course.inspection.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * S105 任务悬挂对账（用户反馈：重启系统后上次未完成的任务永久显示"执行中"）。
 *
 * 根因：任务状态机是"回执驱动"的（{@link TaskService#applyReceipt}，MongoDB 持久），
 * 而真实执行态（当前任务 currentTaskId / 优先级队列 taskQueue）只存在于仿真器 JVM 内存。
 * 整机或仿真器容器重启后：Kafka 消费组 offset 已提交、task.command 旧指令不重放，
 * 仿真器回到默认园区巡逻（TelemetryGenerator 初始 mode=PATROL），
 * 遗留的 DISPATCHED/RUNNING 任务永远等不到下一条回执 → 状态永久悬挂。
 *
 * 两层兜底（均为幂等条件更新，双实例并发安全）：
 * 1. 启动对账 {@link #reconcileOnBoot()}：后端启动时，把派发时间早于宽限期
 *    （task.reconcile-grace-seconds，默认 60 秒）的未终结任务置 FAILED——
 *    整机重启（含仿真器）场景立即清算，不留幽灵任务；
 * 2. 超时兜底 {@link #sweepStale()}：每 60 秒扫描执行超过 task.stale-minutes
 *    （默认 30 分钟）仍未终结的任务置 FAILED——覆盖启动宽限窗口内的残余、
 *    单实例滚动重启、回执丢失等边角场景。
 *
 * 取舍说明：单后端滚动重启时，正在执行且已过宽限期的个别任务也会被如实标记为
 * 失败（设备后续的 DONE 回执因状态已终态而不生效，与 TC022 取消受理同口径）——
 * 宁可如实标失败，不留永久"执行中"的悬挂。
 */
@Component
public class TaskReconcileService {

    private static final Logger log = LoggerFactory.getLogger(TaskReconcileService.class);

    private static final Set<String> OPEN_STATUSES = Set.of("DISPATCHED", "RUNNING");

    /** 启动对账宽限期（秒）：启动前该时长内派发的任务不动（防误杀刚下发的任务）。 */
    @Value("${task.reconcile-grace-seconds:60}")
    private long reconcileGraceSeconds;

    /** 超时兜底阈值（分钟）：执行超过该时长仍未终结的任务自动置 FAILED。 */
    @Value("${task.stale-minutes:30}")
    private long staleMinutes;

    private final MongoTemplate mongo;

    public TaskReconcileService(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    /** 启动对账（应用就绪即执行一次）：清算上次运行遗留的未终结任务。 */
    @EventListener(ApplicationReadyEvent.class)
    public void reconcileOnBoot() {
        Instant cutoff = Instant.now().minusSeconds(reconcileGraceSeconds);
        int aborted = abortOpenTasks(cutoff, "系统重启中断");
        if (aborted > 0) {
            log.warn("启动对账：清算遗留未终结任务 {} 个（宽限期 {} 秒内派发的不动）",
                    aborted, reconcileGraceSeconds);
        }
    }

    /** 超时兜底（每 60 秒）：终结执行过久的任务，防任何来源的回执断链导致永久悬挂。 */
    @Scheduled(fixedDelay = 60_000)
    public void sweepStale() {
        Instant cutoff = Instant.now().minus(staleMinutes, ChronoUnit.MINUTES);
        int aborted = abortOpenTasks(cutoff, "执行超时自动终结");
        if (aborted > 0) {
            log.warn("超时兜底：终结执行超过 {} 分钟仍未完成的任务 {} 个", staleMinutes, aborted);
        }
    }

    /**
     * 把派发时间早于 cutoff 的 DISPATCHED/RUNNING 任务置 FAILED。
     * 条件更新（status 仍在未终结集合）才生效 → 双实例同时执行对账也不会重复/脏写；
     * 每个被终结的任务补一条 FAILED 回执日志，任务详情时间线可见终结时刻。
     */
    private int abortOpenTasks(Instant cutoff, String reason) {
        List<TaskDoc> stale = mongo.find(
                Query.query(Criteria.where("status").in(OPEN_STATUSES)
                        .and("createTime").lt(cutoff)), TaskDoc.class);
        int aborted = 0;
        for (TaskDoc t : stale) {
            var result = mongo.updateFirst(
                    Query.query(Criteria.where("taskId").is(t.getTaskId())
                            .and("status").in(OPEN_STATUSES)),
                    new Update().set("status", "FAILED").set("finishTime", Instant.now()),
                    TaskDoc.class);
            if (result.getModifiedCount() > 0) {
                TaskLogDoc logDoc = new TaskLogDoc();
                logDoc.setTaskId(t.getTaskId());
                logDoc.setDeviceId(t.getDeviceId());
                logDoc.setAction("FAILED");
                logDoc.setTs(Instant.now());
                mongo.insert(logDoc);
                aborted++;
                log.info("任务对账终结 taskId={} deviceId={} {} -> FAILED（{}）",
                        t.getTaskId(), t.getDeviceId(), t.getStatus(), reason);
            }
        }
        return aborted;
    }
}

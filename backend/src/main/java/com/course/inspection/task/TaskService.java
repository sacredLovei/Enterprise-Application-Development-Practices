package com.course.inspection.task;

import com.course.inspection.common.Json;
import com.course.inspection.common.TaskCommandMsg;
import com.course.inspection.common.TopicConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 任务服务（设计报告 4.4.3 / FR-2.2）：
 * 创建任务 → 落库 → 经 task.command 下发（key=deviceId 保证同设备有序）→ 状态流转。
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private static final DateTimeFormatter SEQ = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> CANCELABLE = Set.of("DISPATCHED", "CREATED");

    private final MongoTemplate mongo;
    private final KafkaTemplate<String, String> kafka;
    private final AtomicInteger seq = new AtomicInteger(0);

    public TaskService(MongoTemplate mongo, KafkaTemplate<String, String> kafka) {
        this.mongo = mongo;
        this.kafka = kafka;
    }

    public record CreateRequest(String taskType, String deviceId, int priority, String remark) {
    }

    /** 创建并下发任务。设备离线/不存在时拒绝（S40 用户反馈：离线设备不应可派单）。 */
    public TaskDoc create(CreateRequest req) {
        var device = mongo.findById(req.deviceId(),
                com.course.inspection.device.DeviceDoc.class);
        if (device == null || !"ONLINE".equals(device.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "设备离线或不存在，请先检查/维修/重启设备: " + req.deviceId());
        }

        Instant now = Instant.now();
        String taskId = "TASK-" + SEQ.format(now.atZone(ZoneId.of("Asia/Shanghai")))
                + String.format("%03d", seq.incrementAndGet() % 1000);

        TaskDoc doc = new TaskDoc();
        doc.setTaskId(taskId);
        doc.setTaskType(req.taskType());
        doc.setDeviceId(req.deviceId());
        doc.setPriority(req.priority());
        doc.setStatus("DISPATCHED");
        doc.setRemark(req.remark());
        doc.setCreateTime(now);
        doc.setDispatchTime(now);
        mongo.insert(doc);

        TaskCommandMsg cmd = new TaskCommandMsg(taskId, req.taskType(), req.deviceId(),
                req.priority(), now.toEpochMilli());
        kafka.send(TopicConst.TASK_COMMAND, req.deviceId(), Json.toJson(cmd));
        log.info("任务创建并下发 taskId={} deviceId={} type={}", taskId, req.deviceId(), req.taskType());
        return doc;
    }

    public List<TaskDoc> list() {
        return mongo.findAll(TaskDoc.class);
    }

    /**
     * 回执驱动状态流转 + 日志归档（S32，设计报告 4.4.3 / FR-1.6）：
     * COMMAND_RECEIVED → RUNNING；DONE → DONE + finishTime；EXECUTING 仅归档。
     */
    public void applyReceipt(com.course.inspection.common.TaskLogMsg m) {
        TaskLogDoc log = new TaskLogDoc();
        log.setTaskId(m.taskId());
        log.setDeviceId(m.deviceId());
        log.setAction(m.action());
        log.setTs(Instant.ofEpochMilli(m.ts()));
        mongo.insert(log);

        switch (m.action()) {
            case "COMMAND_RECEIVED" -> transit(m.taskId(), Set.of("DISPATCHED"), "RUNNING");
            case "DONE" -> transit(m.taskId(), Set.of("DISPATCHED", "RUNNING"), "DONE");
            default -> { /* EXECUTING 等动作仅归档，不改状态 */ }
        }
    }

    /** 条件更新状态流转（防并发脏写，设计报告 5.2.4(5)）。 */
    private void transit(String taskId, Set<String> from, String to) {
        Query query = Query.query(Criteria.where("taskId").is(taskId).and("status").in(from));
        Update update = new Update().set("status", to);
        if ("DONE".equals(to)) {
            update.set("finishTime", Instant.now());
        }
        var r = mongo.updateFirst(query, update, TaskDoc.class);
        log.info("任务状态流转 taskId={} -> {} 生效={}", taskId, to, r.getModifiedCount() > 0);
    }

    /** 取消任务（仅 DISPATCHED/CREATED 可取消；条件更新防并发脏写，设计报告 5.2.4(5)）。 */
    public boolean cancel(String taskId) {
        Query query = Query.query(Criteria.where("taskId").is(taskId).and("status").in(CANCELABLE));
        Update update = new Update().set("status", "CANCELLED").set("finishTime", Instant.now());
        var r = mongo.updateFirst(query, update, TaskDoc.class);
        log.info("取消任务 taskId={} 生效={}", taskId, r.getModifiedCount() > 0);
        return r.getModifiedCount() > 0;
    }
}

package com.course.inspection.task;

import com.course.inspection.alarm.AlarmDoc;
import com.course.inspection.alarm.AlarmEsDoc;
import com.course.inspection.alarm.AlarmSearchService;
import com.course.inspection.alarm.AlarmStore;
import com.course.inspection.common.Json;
import com.course.inspection.common.TaskCommandMsg;
import com.course.inspection.common.TopicConst;
import com.course.inspection.device.DeviceDoc;
import com.course.inspection.storage.EvidenceImageGenerator;
import com.course.inspection.storage.HdfsClient;
import com.course.inspection.storage.PathBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.NearQuery;
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
import java.util.stream.Collectors;

/**
 * 任务服务（设计报告 4.4.3 / FR-2.2）：
 * 创建任务 → 落库 → 经 task.command 下发（key=deviceId 保证同设备有序）→ 状态流转。
 * S61：复核派单闭环——新告警触发就近派机器狗 POINT_REVIEW，DONE 回执回填告警 review。
 */
@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private static final DateTimeFormatter SEQ = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Set<String> CANCELABLE = Set.of("DISPATCHED", "CREATED", "RUNNING");

    private final MongoTemplate mongo;
    private final KafkaTemplate<String, String> kafka;
    private final HdfsClient hdfs;
    private final PathBuilder paths;
    private final EvidenceImageGenerator evidence;
    private final AlarmStore alarmStore;
    private final AlarmSearchService searchService;
    private final AtomicInteger seq = new AtomicInteger(0);

    public TaskService(MongoTemplate mongo, KafkaTemplate<String, String> kafka,
                       HdfsClient hdfs, PathBuilder paths, EvidenceImageGenerator evidence,
                       AlarmStore alarmStore, AlarmSearchService searchService) {
        this.mongo = mongo;
        this.kafka = kafka;
        this.hdfs = hdfs;
        this.paths = paths;
        this.evidence = evidence;
        this.alarmStore = alarmStore;
        this.searchService = searchService;
    }

    public record CreateRequest(
            @jakarta.validation.constraints.NotBlank(message = "taskType 不能为空") String taskType,
            @jakarta.validation.constraints.NotBlank(message = "deviceId 不能为空") String deviceId,
            int priority, String remark,
            Double targetLng, Double targetLat) {
    }

    /** 创建并下发任务。设备离线/不存在时拒绝（S40 用户反馈：离线设备不应可派单）。 */
    public TaskDoc create(CreateRequest req) {
        return createInternal(req.taskType(), req.deviceId(), req.priority(), req.remark(),
                req.targetLng(), req.targetLat(), null);
    }

    /**
     * S61 复核派单：2dsphere 就近查询在线机器狗（O-6 / TC021），空闲优先、否则就近排队，
     * 创建 POINT_REVIEW 任务并关联告警编号。
     */
    public TaskDoc dispatchReview(String alarmId, double lng, double lat) {
        NearQuery nq = NearQuery.near(new GeoJsonPoint(lng, lat))
                .spherical(true)
                // 口径：机器狗 deviceType = ROBOT_DOG（STATE 注册表，仿真配置 sim.device-type）
                .query(Query.query(Criteria.where("deviceType").is("ROBOT_DOG").and("status").is("ONLINE")))
                .limit(4);
        GeoResults<DeviceDoc> results = mongo.geoNear(nq, DeviceDoc.class);
        List<DeviceDoc> robots = results.getContent().stream()
                .map(GeoResult::getContent)
                .toList();
        if (robots.isEmpty()) {
            log.warn("复核派单失败：无在线机器狗 alarmId={}", alarmId);
            return null;
        }
        // 空闲优先：剔除有进行中任务的机器狗；全部忙碌时派给最近一台（其优先级队列兜底，risk #30 语义）
        Set<String> busy = mongo.find(Query.query(Criteria.where("deviceId")
                                .in(robots.stream().map(DeviceDoc::getDeviceId).collect(Collectors.toSet()))
                                .and("status").in("DISPATCHED", "RUNNING")), TaskDoc.class)
                .stream().map(TaskDoc::getDeviceId).collect(Collectors.toSet());
        DeviceDoc picked = robots.stream().filter(r -> !busy.contains(r.getDeviceId()))
                .findFirst().orElse(robots.get(0));

        // D-22 节流：选中机器人待办复核任务 ≥ 2 时跳过自动派单（告警留待人工复核 IT009），
        // 防止告警高峰把机器狗队列打满导致复核无限积压
        long pendingReviews = mongo.count(Query.query(Criteria.where("deviceId").is(picked.getDeviceId())
                        .and("taskType").is("POINT_REVIEW")
                        .and("status").in("DISPATCHED", "RUNNING")), TaskDoc.class);
        if (pendingReviews >= 2) {
            log.warn("复核节流：robot={} 待办复核 {} 个，跳过自动派单 alarmId={}（转人工复核 IT009）",
                    picked.getDeviceId(), pendingReviews, alarmId);
            return null;
        }

        TaskDoc task = createInternal("POINT_REVIEW", picked.getDeviceId(), 1,
                "复核告警 " + alarmId, lng, lat, alarmId);
        log.info("复核派单 alarmId={} -> robot={} taskId={} 就近顺序={}",
                alarmId, picked.getDeviceId(), task.getTaskId(),
                robots.stream().map(DeviceDoc::getDeviceId).toList());
        return task;
    }

    private TaskDoc createInternal(String taskType, String deviceId, int priority, String remark,
                                   Double targetLng, Double targetLat, String alarmId) {
        var device = mongo.findById(deviceId, DeviceDoc.class);
        if (device == null || !"ONLINE".equals(device.getStatus())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT,
                    "设备离线或不存在，请先检查/维修/重启设备: " + deviceId);
        }

        // S104（用户需求）：派任务的地点限定在园区内，不可外派——
        // 选点型任务（定点复核/区域覆盖）的目标坐标越界即拒绝
        if (("POINT_REVIEW".equals(taskType) || "AREA_COVER".equals(taskType))
                && !com.course.inspection.common.CampusBounds.contains(targetLng, targetLat)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "目标位置必须在园区范围内，不可外派");
        }

        Instant now = Instant.now();
        // 任务编号：时间戳 + UUID 后缀——双实例各自自增计数器会同秒撞号（风险 #28）
        String taskId = "TASK-" + SEQ.format(now.atZone(ZoneId.of("Asia/Shanghai")))
                + "-" + java.util.UUID.randomUUID().toString().substring(0, 8);

        TaskDoc doc = new TaskDoc();
        doc.setTaskId(taskId);
        doc.setTaskType(taskType);
        doc.setDeviceId(deviceId);
        doc.setPriority(priority);
        doc.setStatus("DISPATCHED");
        doc.setRemark(remark);
        doc.setTargetLng(targetLng);
        doc.setTargetLat(targetLat);
        doc.setAlarmId(alarmId);
        doc.setCreateTime(now);
        doc.setDispatchTime(now);
        mongo.insert(doc);

        TaskCommandMsg cmd = new TaskCommandMsg(taskId, taskType, deviceId,
                priority, now.toEpochMilli(), targetLng, targetLat);
        kafka.send(TopicConst.TASK_COMMAND, deviceId, Json.toJson(cmd));
        log.info("任务创建并下发 taskId={} deviceId={} type={}", taskId, deviceId, taskType);
        return doc;
    }

    /** 分页结果（S40 修正：任务列表分页 + 最新在前）。 */
    public record TaskPage(long total, List<TaskDoc> records) {
    }

    /** 任务列表：按创建时间倒序分页（最新的排最前）。 */
    public TaskPage list(int page, int size) {
        Query query = new Query()
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "createTime"))
                .skip((long) page * size)
                .limit(size);
        long total = mongo.count(new Query(), TaskDoc.class);
        return new TaskPage(total, mongo.find(query, TaskDoc.class));
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
            case "DONE" -> {
                // S61：复核任务完成 → 生成红外复核图入 HDFS → 回填告警 review
                if (transit(m.taskId(), Set.of("DISPATCHED", "RUNNING"), "DONE")) {
                    completeReview(m.taskId());
                }
            }
            case "CANCELLED" -> transit(m.taskId(), Set.of("DISPATCHED", "RUNNING"), "CANCELLED");
            case "FAILED" -> transit(m.taskId(), Set.of("DISPATCHED", "RUNNING"), "FAILED");
            default -> { /* EXECUTING 等动作仅归档，不改状态 */ }
        }
    }

    /** S85（课程任务项 2）：任务执行日志查询——按时间升序返回归档的全部回执。 */
    public List<TaskLogDoc> logs(String taskId) {
        return mongo.find(
                Query.query(Criteria.where("taskId").is(taskId))
                        .with(org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.ASC, "ts")),
                TaskLogDoc.class);
    }

    /**
     * S61：POINT_REVIEW 任务完成 → 复核结论回填。
     * 结论规则（D-21，仿真口径）：alarmId 哈希 70% CONFIRMED / 30% FALSE_ALARM。
     */
    private void completeReview(String taskId) {
        TaskDoc task = mongo.findById(taskId, TaskDoc.class);
        if (task == null || !"POINT_REVIEW".equals(task.getTaskType()) || task.getAlarmId() == null) {
            return;
        }
        try {
            String conclusion = (Math.floorMod(task.getAlarmId().hashCode(), 10) < 7)
                    ? "CONFIRMED" : "FALSE_ALARM";
            EvidenceImageGenerator.Evidence ev = evidence.generateReview(
                    task.getAlarmId(), conclusion, task.getDeviceId());
            String path = paths.build("dog_infrared", task.getDeviceId(), Instant.now(), ev.ext());
            String imagePath = hdfs.upload(path, ev.bytes());
            alarmStore.applyReview(task.getAlarmId(), task.getDeviceId(), conclusion,
                    "机器狗红外复核完成（仿真结论规则 D-21）", imagePath, Instant.now());
            AlarmDoc alarm = alarmStore.findById(task.getAlarmId());
            if (alarm != null) {
                searchService.index(AlarmEsDoc.from(alarm));   // ES 副本同步 status
            }
            log.info("复核回填完成 alarmId={} conclusion={} image={}",
                    task.getAlarmId(), conclusion, imagePath);
        } catch (Exception e) {
            log.error("复核回填失败 taskId={} alarmId={}", taskId, task.getAlarmId(), e);
        }
    }

    /** 条件更新状态流转（防并发脏写，设计报告 5.2.4(5)）。返回是否真正流转。 */
    private boolean transit(String taskId, Set<String> from, String to) {
        Query query = Query.query(Criteria.where("taskId").is(taskId).and("status").in(from));
        Update update = new Update().set("status", to);
        if ("DONE".equals(to) || "CANCELLED".equals(to) || "FAILED".equals(to)) {
            update.set("finishTime", Instant.now());
        }
        var r = mongo.updateFirst(query, update, TaskDoc.class);
        log.info("任务状态流转 taskId={} -> {} 生效={}", taskId, to, r.getModifiedCount() > 0);
        return r.getModifiedCount() > 0;
    }

    /**
     * 取消任务（S33 增强：DISPATCHED/CREATED/RUNNING 均可取消）。
     * 取消指令经 task.command 下发（CANCEL_TASK），由设备回执 CANCELLED 驱动最终状态；
     * 设备离线/不存在时直接置 CANCELLED（指令无法送达）。
     */
    public boolean cancel(String taskId) {
        TaskDoc task = mongo.findById(taskId, TaskDoc.class);
        if (task == null || !CANCELABLE.contains(task.getStatus())) {
            return false;
        }
        var device = mongo.findById(task.getDeviceId(),
                com.course.inspection.device.DeviceDoc.class);
        if (device == null || !"ONLINE".equals(device.getStatus())) {
            // 设备离线：指令无法送达，直接置为已取消
            mongo.updateFirst(Query.query(Criteria.where("taskId").is(taskId)),
                    new Update().set("status", "CANCELLED").set("finishTime", Instant.now()),
                    TaskDoc.class);
            log.info("设备离线，直接取消任务 taskId={}", taskId);
            return true;
        }
        // 下发取消指令：设备中止执行（或在队列中移除）并回执 CANCELLED，随后返航。
        // 同时同步置 CANCELLED 作为受理标记（TC022）：并发二次取消因状态已 CANCELLED 被拒；
        // 设备回执 CANCELLED 时状态已终态，transit 无操作不产生非法流转。
        TaskCommandMsg cmd = new TaskCommandMsg(taskId, "CANCEL_TASK", task.getDeviceId(),
                1, System.currentTimeMillis(), null, null);
        kafka.send(TopicConst.TASK_COMMAND, task.getDeviceId(), Json.toJson(cmd));
        mongo.updateFirst(Query.query(Criteria.where("taskId").is(taskId)),
                new Update().set("status", "CANCELLED").set("finishTime", Instant.now()),
                TaskDoc.class);
        log.info("取消指令已下发并同步置 CANCELLED taskId={} deviceId={}", taskId, task.getDeviceId());
        return true;
    }
}

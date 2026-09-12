package com.course.inspection.sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 仿真设备基类（设计报告 5.2.1）：
 * - 心跳 5s（FR-1.2）；电量单调下降，归零触发 BATTERY_LOW 告警并回充（S31 电量循环）；
 * - 故障注入：BATTERY_DROP（电量骤降）/ COMM_OFFLINE（停止心跳，验证离线检测）。
 * 子类实现设备类型专属的遥测与告警逻辑。
 */
public abstract class DeviceSimulator {

    protected static final Logger log = LoggerFactory.getLogger(DeviceSimulator.class);

    private static final long RECHARGE_MILLIS = 30_000;   // 回充耗时 30 秒（演示用）

    protected final KafkaTemplate<String, String> kafka;
    protected final ObjectMapper om = new ObjectMapper();
    protected final TelemetryGenerator track = new TelemetryGenerator(116.3974, 39.9092);

    @Value("${sim.device-id:UAV-001}")
    protected String deviceId;

    @Value("${sim.device-type:UAV}")
    protected String deviceType;

    protected volatile int battery = 100;
    private volatile boolean recharging = false;
    private volatile boolean batteryLowFired = false;
    private volatile long rechargeDeadline = 0;

    private final AtomicBoolean commEnabled = new AtomicBoolean(true);

    protected DeviceSimulator(KafkaTemplate<String, String> kafka) {
        this.kafka = kafka;
    }

    /** 通信是否正常（COMM_OFFLINE 注入后为 false，心跳与遥测全部停发）。 */
    protected boolean isCommUp() {
        return commEnabled.get();
    }

    /** 心跳：5 秒一次（FR-1.2）；COMM_OFFLINE 注入后停止。 */
    @Scheduled(fixedRate = 5_000)
    public final void heartbeat() {
        if (!commEnabled.get()) {
            return;
        }
        tickBattery();
        HeartbeatMsg msg = new HeartbeatMsg(deviceId, deviceType, battery, null, System.currentTimeMillis());
        send("device.heartbeat", msg);
    }

    /** 电量循环：消耗 → 归零触发告警 → 回充（S31 新增，解决 S21 遗留项 #20①）。 */
    private void tickBattery() {
        long now = System.currentTimeMillis();
        if (recharging) {
            if (now >= rechargeDeadline) {
                battery = 100;
                recharging = false;
                batteryLowFired = false;
                log.info("回充完成 deviceId={} battery=100", deviceId);
            }
            return;
        }
        if (battery > 0) {
            battery = Math.max(0, battery - 1);
        }
        if (battery == 0 && !batteryLowFired) {
            batteryLowFired = true;
            recharging = true;
            rechargeDeadline = now + RECHARGE_MILLIS;
            emitAlarm("BATTERY_LOW", "WARN", "电量耗尽，自动返航回充");
        }
    }

    /** 故障注入（FR-1.4）。COMM_OFFLINE 与 BATTERY_DROP 通用；OVERHEAT 由机器狗实现。 */
    public void injectFault(String type) {
        switch (type) {
            case "COMM_OFFLINE" -> {
                commEnabled.set(false);
                log.info("故障注入 COMM_OFFLINE：心跳与遥测全部停止 deviceId={}", deviceId);
            }
            case "COMM_RESTORE" -> {
                commEnabled.set(true);
                log.info("通信恢复 COMM_RESTORE deviceId={}", deviceId);
            }
            case "BATTERY_DROP" -> {
                battery = Math.min(battery, 10);
                log.info("故障注入 BATTERY_DROP：电量骤降至 {} deviceId={}", battery, deviceId);
            }
            default -> log.warn("未知故障类型: {}", type);
        }
    }

    protected void emitAlarm(String alarmType, String level, String description) {
        AlarmMsg msg = new AlarmMsg(
                "ALM-" + UUID.randomUUID().toString().substring(0, 8),
                deviceId, deviceType, alarmType, level, description,
                track.currentLng(), track.currentLat(),
                System.currentTimeMillis());
        send("inspection.alarm", msg);
        log.info("告警产生 {} {} {} deviceId={}", alarmType, level, description, deviceId);
    }

    // ---------- S33 任务真实执行 ----------

    private volatile String currentTaskId;
    private volatile String currentTaskType;
    private volatile boolean taskDoneSent = false;

    /**
     * 接收指令（S33 修复：优先级任务队列，风险 #30）。
     * 指令一律入队（按优先级降序、同优先级按下发先后）；设备空闲时立即取队首执行。
     * 排队中的任务不产生回执——后端保持 DISPATCHED，直到真正开始执行才 RECEIVED/RUNNING。
     */
    public void onCommand(TaskCommandMsg cmd) {
        if (!commEnabled.get()) {
            log.warn("指令被拒（通信中断） deviceId={} taskId={}", deviceId, cmd.taskId());
            return;
        }
        taskQueue.offer(cmd);
        log.info("指令入队 deviceId={} taskId={} priority={} 队列长度={}",
                deviceId, cmd.taskId(), cmd.priority(), taskQueue.size());
        maybeStartNext();
    }

    private final java.util.concurrent.PriorityBlockingQueue<TaskCommandMsg> taskQueue =
            new java.util.concurrent.PriorityBlockingQueue<>(16, (a, b) -> {
                int p = Integer.compare(a.priority(), b.priority());
                if (p != 0) {
                    return p;    // 数字小 = 优先级高，排前
                }
                return Long.compare(a.ts(), b.ts());
            });

    /** 汇聚窗口标记：空闲设备收到指令后延迟 3 秒开工，让"同时下发"的指令先按优先级排好队。 */
    private volatile boolean startScheduled = false;

    /** 空闲则经 3 秒汇聚窗口后，从队首取优先级最高的任务执行。 */
    private void maybeStartNext() {
        synchronized (this) {
            if (currentTaskId != null || startScheduled) {
                return;
            }
            startScheduled = true;
        }
        EXECUTOR.schedule(() -> {
            startScheduled = false;
            synchronized (this) {
                if (currentTaskId == null) {
                    TaskCommandMsg next = taskQueue.poll();
                    if (next != null) {
                        startTask(next);
                    }
                }
            }
        }, 3, java.util.concurrent.TimeUnit.SECONDS);
    }

    private void startTask(TaskCommandMsg cmd) {
        currentTaskId = cmd.taskId();
        currentTaskType = cmd.taskType();
        taskDoneSent = false;

        switch (cmd.taskType()) {
            case "POINT_REVIEW" -> beginReview(cmd);
            case "AREA_COVER" -> beginSweep(cmd);
            case "RETURN_HOME" -> beginHome();
            case "PERIMETER_PATROL" -> track.startPatrolLoop();
            default -> log.warn("未知任务类型: {}", cmd.taskType());
        }
        receipt(cmd.taskId(), "COMMAND_RECEIVED");
        EXECUTOR.schedule(() -> receipt(cmd.taskId(), "EXECUTING"), 3, java.util.concurrent.TimeUnit.SECONDS);
        log.info("任务开始执行 deviceId={} taskId={} type={} priority={}",
                deviceId, cmd.taskId(), cmd.taskType(), cmd.priority());
    }

    private static final java.util.concurrent.ScheduledExecutorService EXECUTOR =
            java.util.concurrent.Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r, "sim-receipt");
                t.setDaemon(true);
                return t;
            });

    /** 每遥测 tick 检查任务是否完成，完成即回执 DONE 并取下一个优先级最高的任务。 */
    protected void checkTaskCompletion() {
        if (currentTaskId == null || taskDoneSent) {
            return;
        }
        boolean done = switch (currentTaskType) {
            case "POINT_REVIEW", "RETURN_HOME" -> track.pointArrived();
            case "AREA_COVER" -> track.sweepFinished();
            case "PERIMETER_PATROL" -> track.patrolLoopDone();
            default -> false;
        };
        if (done) {
            receipt(currentTaskId, "DONE");
            taskDoneSent = true;
            log.info("任务完成 deviceId={} taskId={} type={}", deviceId, currentTaskId, currentTaskType);
            currentTaskId = null;
            currentTaskType = null;
            track.startPatrolLoop();   // 恢复默认巡逻
            maybeStartNext();          // 立即取队列中优先级最高的下一个任务
        }
    }

    private void receipt(String taskId, String action) {
        TaskLogMsg msg = new TaskLogMsg(taskId, deviceId, action, System.currentTimeMillis());
        send("task.log", msg);
    }

    // 子类实现任务行为
    protected abstract void beginReview(TaskCommandMsg cmd);

    protected abstract void beginSweep(TaskCommandMsg cmd);

    protected abstract void beginHome();

    protected void send(String topic, Object payload) {
        try {
            kafka.send(topic, deviceId, om.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("仿真消息发送失败 topic={}", topic, e);
        }
    }

    protected double[] advance(double speed, double intervalSec) {
        return track.next(speed, intervalSec);
    }
}

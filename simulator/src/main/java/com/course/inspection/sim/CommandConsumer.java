package com.course.inspection.sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * 指令消费与执行回执（设计报告 FR-1.6 / FR-2.2）：
 * 订阅 task.command，仅处理发给本机的指令，依次回执 COMMAND_RECEIVED → EXECUTING → DONE。
 */
@Component
public class CommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(CommandConsumer.class);

    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper om = new ObjectMapper();
    private final TaskScheduler scheduler;

    @Value("${sim.device-id:UAV-001}")
    private String deviceId;

    public CommandConsumer(KafkaTemplate<String, String> kafka, TaskScheduler scheduler) {
        this.kafka = kafka;
        this.scheduler = scheduler;
    }

    @KafkaListener(topics = "task.command", groupId = "${sim.consumer-group:sim-uav}")
    public void onCommand(ConsumerRecord<String, String> record) {
        try {
            TaskCommandMsg cmd = om.readValue(record.value(), TaskCommandMsg.class);
            if (!deviceId.equals(cmd.deviceId())) {
                return;   // 非本机指令，直接忽略（其他分区/实例会处理）
            }
            log.info("收到任务指令 taskId={} type={}", cmd.taskId(), cmd.taskType());
            receipt(cmd.taskId(), "COMMAND_RECEIVED");
            scheduler.schedule(() -> receipt(cmd.taskId(), "EXECUTING"),
                    Instant.now().plus(Duration.ofSeconds(3)));
            scheduler.schedule(() -> receipt(cmd.taskId(), "DONE"),
                    Instant.now().plus(Duration.ofSeconds(8)));
        } catch (Exception e) {
            log.error("指令解析失败 partition={} offset={}", record.partition(), record.offset(), e);
        }
    }

    private void receipt(String taskId, String action) {
        TaskLogMsg msg = new TaskLogMsg(taskId, deviceId, action, System.currentTimeMillis());
        try {
            kafka.send("task.log", deviceId, om.writeValueAsString(msg));
        } catch (Exception e) {
            log.error("回执发送失败 taskId={} action={}", taskId, action, e);
        }
    }
}

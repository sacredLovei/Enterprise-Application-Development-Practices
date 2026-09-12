package com.course.inspection.sim;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * 指令消费（设计报告 FR-1.6 / FR-2.2；S33 起行为执行交由 DeviceSimulator）：
 * 每设备独立消费组（风险 #27），解析指令后交给仿真设备真实执行。
 */
@Component
public class CommandConsumer {

    private static final Logger log = LoggerFactory.getLogger(CommandConsumer.class);

    private final DeviceSimulator simulator;
    private final ObjectMapper om = new ObjectMapper();

    @Value("${sim.device-id:UAV-001}")
    private String deviceId;

    public CommandConsumer(DeviceSimulator simulator) {
        this.simulator = simulator;
    }

    /**
     * 每设备独立消费组（S32 修复，风险 #27）：组内独占全部分区、按 deviceId 过滤，
     * 避免"指令经 key 哈希落错分区 → 被错误设备丢弃 → 目标设备永远收不到"。
     */
    @KafkaListener(topics = "task.command", groupId = "${sim.command-group:sim-cmd-default}")
    public void onCommand(ConsumerRecord<String, String> record) {
        try {
            TaskCommandMsg cmd = om.readValue(record.value(), TaskCommandMsg.class);
            if (!deviceId.equals(cmd.deviceId())) {
                return;   // 非本机指令，忽略
            }
            log.info("收到任务指令 taskId={} type={}", cmd.taskId(), cmd.taskType());
            simulator.onCommand(cmd);
        } catch (Exception e) {
            log.error("指令解析失败 partition={} offset={}", record.partition(), record.offset(), e);
        }
    }
}

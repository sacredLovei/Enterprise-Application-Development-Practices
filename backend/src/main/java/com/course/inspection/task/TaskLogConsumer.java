package com.course.inspection.task;

import com.course.inspection.common.Json;
import com.course.inspection.common.TaskLogMsg;
import com.course.inspection.common.TopicConst;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 任务回执消费者（S32，消费组 biz-task-consumer）：
 * 消费设备执行回执 → 任务状态流转 + 日志归档。手动提交：业务成功才确认。
 */
@Component
public class TaskLogConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskLogConsumer.class);

    private final TaskService taskService;

    public TaskLogConsumer(TaskService taskService) {
        this.taskService = taskService;
    }

    @KafkaListener(topics = TopicConst.TASK_LOG, groupId = TopicConst.GROUP_TASK)
    public void onLog(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            TaskLogMsg msg = Json.fromJson(record.value(), TaskLogMsg.class);
            taskService.applyReceipt(msg);
            log.info("任务回执归档 taskId={} action={}", msg.taskId(), msg.action());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("任务回执消费失败，不提交 offset 等待重投 partition={} offset={}",
                    record.partition(), record.offset(), e);
        }
    }
}

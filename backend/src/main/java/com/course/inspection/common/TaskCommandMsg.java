package com.course.inspection.common;

/** 任务指令消息契约（task.command 主题）。 */
public record TaskCommandMsg(
        String taskId,
        String taskType,
        String deviceId,
        int priority,
        long ts
) {
}

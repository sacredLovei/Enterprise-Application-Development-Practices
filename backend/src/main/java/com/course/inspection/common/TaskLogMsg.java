package com.course.inspection.common;

/** 任务执行回执消息契约（task.log 主题）。action ∈ COMMAND_RECEIVED / EXECUTING / DONE。 */
public record TaskLogMsg(
        String taskId,
        String deviceId,
        String action,
        long ts
) {
}

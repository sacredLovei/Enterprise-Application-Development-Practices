package com.course.inspection.sim;

/** 任务执行回执（task.log 主题，设计报告 FR-1.6）。action ∈ COMMAND_RECEIVED / EXECUTING / DONE。 */
public record TaskLogMsg(
        String taskId,
        String deviceId,
        String action,
        long ts
) {
}

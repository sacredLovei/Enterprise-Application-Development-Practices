package com.course.inspection.sim;

/** 巡检任务指令（task.command 主题，设计报告 FR-2.2）。 */
public record TaskCommandMsg(
        String taskId,
        String taskType,       // PERIMETER_PATROL / AREA_COVER / POINT_REVIEW / RETURN_HOME
        String deviceId,
        int priority,
        long ts
) {
}

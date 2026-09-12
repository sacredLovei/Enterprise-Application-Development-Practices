package com.course.inspection.common;

/** 主题与消费组常量（STATE 术语注册表唯一口径，设计报告 4.4.2）。 */
public final class TopicConst {

    public static final String UAV_TELEMETRY    = "uav.telemetry";
    public static final String ROBOT_TELEMETRY  = "robot.telemetry";
    public static final String DEVICE_HEARTBEAT = "device.heartbeat";
    public static final String INSPECTION_ALARM = "inspection.alarm";
    public static final String TASK_COMMAND     = "task.command";
    public static final String TASK_LOG         = "task.log";
    public static final String DLQ              = "inspection.dlq";

    public static final String GROUP_STORAGE = "biz-storage-consumer";
    public static final String GROUP_ALARM   = "biz-alarm-consumer";

    private TopicConst() {
    }
}

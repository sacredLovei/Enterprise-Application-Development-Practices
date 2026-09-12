package com.course.inspection.task;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** 任务执行日志（MongoDB `task_log` 集合，TTL 90 天，设计报告 4.5.1(6)）。 */
@Document("task_log")
public class TaskLogDoc {

    @Id
    private String id;

    @Indexed
    private String taskId;

    private String deviceId;
    private String action;

    /** TTL 索引：90 天自动过期（设计报告 4.5.1(6)）。 */
    @Indexed(expireAfterSeconds = 7776000)
    private Instant ts;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
    }
}

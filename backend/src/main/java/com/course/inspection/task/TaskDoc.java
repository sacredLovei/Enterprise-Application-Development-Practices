package com.course.inspection.task;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** 巡检任务（MongoDB `task` 集合，设计报告 4.5.1(3)）。 */
@Document("task")
public class TaskDoc {

    @Id
    private String taskId;

    private String taskType;     // PERIMETER_PATROL / AREA_COVER / POINT_REVIEW / RETURN_HOME
    private String deviceId;
    private int priority;
    private String status;       // CREATED / DISPATCHED / RUNNING / DONE / FAILED / CANCELLED
    private String remark;
    private Double targetLng;    // S33：POINT_REVIEW / AREA_COVER 目标坐标（前端地图展示任务目标点）
    private Double targetLat;
    private Instant createTime;
    private Instant dispatchTime;
    private Instant finishTime;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskType() {
        return taskType;
    }

    public void setTaskType(String taskType) {
        this.taskType = taskType;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Double getTargetLng() {
        return targetLng;
    }

    public void setTargetLng(Double targetLng) {
        this.targetLng = targetLng;
    }

    public Double getTargetLat() {
        return targetLat;
    }

    public void setTargetLat(Double targetLat) {
        this.targetLat = targetLat;
    }

    public Instant getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Instant createTime) {
        this.createTime = createTime;
    }

    public Instant getDispatchTime() {
        return dispatchTime;
    }

    public void setDispatchTime(Instant dispatchTime) {
        this.dispatchTime = dispatchTime;
    }

    public Instant getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Instant finishTime) {
        this.finishTime = finishTime;
    }
}

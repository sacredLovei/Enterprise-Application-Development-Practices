package com.course.inspection.alarm;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** 告警权威记录（MongoDB `alarm` 集合，设计报告 4.5.1(4)）。 */
@Document("alarm")
public class AlarmDoc {

    @Id
    private String alarmId;

    @Indexed
    private String deviceId;

    private String deviceType;

    @Indexed
    private String alarmType;

    private String level;
    private String description;
    private double lng;
    private double lat;
    private Instant occurredTime;
    private String snapshotPath;
    private String status;    // PENDING / CONFIRMED / FALSE_ALARM / RESOLVED

    public String getAlarmId() {
        return alarmId;
    }

    public void setAlarmId(String alarmId) {
        this.alarmId = alarmId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getAlarmType() {
        return alarmType;
    }

    public void setAlarmType(String alarmType) {
        this.alarmType = alarmType;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public Instant getOccurredTime() {
        return occurredTime;
    }

    public void setOccurredTime(Instant occurredTime) {
        this.occurredTime = occurredTime;
    }

    public String getSnapshotPath() {
        return snapshotPath;
    }

    public void setSnapshotPath(String snapshotPath) {
        this.snapshotPath = snapshotPath;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}

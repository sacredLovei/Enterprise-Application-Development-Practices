package com.course.inspection.alarm;

/** ES 检索副本文档（索引 inspection_alarm_v1，设计报告 4.5.2）。 */
public class AlarmEsDoc {

    private String alarmId;
    private String deviceId;
    private String deviceType;
    private String alarmType;
    private String level;
    private String description;
    private String status;
    /** GeoJSON 顺序：[经度, 纬度]（风险：顺序写反会落点错误，设计报告 4.5.2 要点 3）。 */
    private double[] location;
    private String occurredTime;   // ISO-8601
    private String snapshotPath;

    public static AlarmEsDoc from(AlarmDoc d) {
        AlarmEsDoc es = new AlarmEsDoc();
        es.alarmId = d.getAlarmId();
        es.deviceId = d.getDeviceId();
        es.deviceType = d.getDeviceType();
        es.alarmType = d.getAlarmType();
        es.level = d.getLevel();
        es.description = d.getDescription();
        es.status = d.getStatus();
        es.location = new double[]{d.getLng(), d.getLat()};
        es.occurredTime = d.getOccurredTime().toString();
        es.snapshotPath = d.getSnapshotPath();
        return es;
    }

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double[] getLocation() {
        return location;
    }

    public void setLocation(double[] location) {
        this.location = location;
    }

    public String getOccurredTime() {
        return occurredTime;
    }

    public void setOccurredTime(String occurredTime) {
        this.occurredTime = occurredTime;
    }

    public String getSnapshotPath() {
        return snapshotPath;
    }

    public void setSnapshotPath(String snapshotPath) {
        this.snapshotPath = snapshotPath;
    }
}

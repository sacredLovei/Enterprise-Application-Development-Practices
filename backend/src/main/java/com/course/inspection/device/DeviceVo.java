package com.course.inspection.device;

import java.time.Instant;

/** 设备台账视图对象：台账字段 + 最近遥测位置（S40 地图用）。 */
public class DeviceVo {

    private String deviceId;
    private String deviceType;
    private String status;
    private int battery;
    private String currentTaskId;
    private Instant registerTime;
    private Instant lastHeartbeat;
    private Double lng;
    private Double lat;

    public static DeviceVo from(DeviceDoc d, DeviceStatusDoc last) {
        DeviceVo vo = new DeviceVo();
        vo.deviceId = d.getDeviceId();
        vo.deviceType = d.getDeviceType();
        vo.status = d.getStatus();
        vo.battery = d.getBattery();
        vo.currentTaskId = d.getCurrentTaskId();
        vo.registerTime = d.getRegisterTime();
        vo.lastHeartbeat = d.getLastHeartbeat();
        if (last != null) {
            vo.lng = last.getLng();
            vo.lat = last.getLat();
        }
        return vo;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getStatus() {
        return status;
    }

    public int getBattery() {
        return battery;
    }

    public String getCurrentTaskId() {
        return currentTaskId;
    }

    public Instant getRegisterTime() {
        return registerTime;
    }

    public Instant getLastHeartbeat() {
        return lastHeartbeat;
    }

    public Double getLng() {
        return lng;
    }

    public Double getLat() {
        return lat;
    }

    /** S50 优化：聚合查询路径填充坐标。 */
    public void setLng(Double lng) {
        this.lng = lng;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }
}

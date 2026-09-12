package com.course.inspection.device;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/** 遥测时序（MongoDB `device_status` 集合，TTL 30 天，设计报告 4.5.1）。 */
@Document("device_status")
public class DeviceStatusDoc {

    @Id
    private String id;

    private String deviceId;

    /** TTL 索引：30 天自动过期（设计报告 2.2.3 / 4.5.1）。 */
    @Indexed(expireAfterSeconds = 2592000)
    private Instant ts;

    private double lng;
    private double lat;
    private double altitude;
    private double speed;
    private int battery;

    // 机器狗专属传感字段（无人机记录为 null，文档型数据库异构载荷，设计报告 2.2.1 / 4.5.1）
    private Double irMaxTemp;
    private Double ambientTemp;
    private Double humidity;
    private Double smoke;
    private Double gas;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Instant getTs() {
        return ts;
    }

    public void setTs(Instant ts) {
        this.ts = ts;
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

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public Double getIrMaxTemp() {
        return irMaxTemp;
    }

    public void setIrMaxTemp(Double irMaxTemp) {
        this.irMaxTemp = irMaxTemp;
    }

    public Double getAmbientTemp() {
        return ambientTemp;
    }

    public void setAmbientTemp(Double ambientTemp) {
        this.ambientTemp = ambientTemp;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public Double getSmoke() {
        return smoke;
    }

    public void setSmoke(Double smoke) {
        this.smoke = smoke;
    }

    public Double getGas() {
        return gas;
    }

    public void setGas(Double gas) {
        this.gas = gas;
    }
}

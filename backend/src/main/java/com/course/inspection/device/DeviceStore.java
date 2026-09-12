package com.course.inspection.device;

import com.course.inspection.common.HeartbeatMsg;
import com.course.inspection.common.UavTelemetryMsg;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * 设备数据读写。
 * 心跳用 upsert 单次往返（设计报告 5.2.4(1)），遥测直接插入时序集合（S30 升级批量写）。
 */
@Service
public class DeviceStore {

    private final MongoTemplate mongo;

    public DeviceStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void upsertHeartbeat(HeartbeatMsg m) {
        Query query = Query.query(Criteria.where("deviceId").is(m.deviceId()));
        Update update = new Update()
                .set("deviceType", m.deviceType())
                .set("status", "ONLINE")
                .set("battery", m.battery())
                .set("lastHeartbeat", Instant.ofEpochMilli(m.ts()))
                .setOnInsert("deviceId", m.deviceId())
                .setOnInsert("registerTime", Instant.ofEpochMilli(m.ts()));
        mongo.upsert(query, update, DeviceDoc.class);
    }

    public void saveTelemetry(UavTelemetryMsg m) {
        DeviceStatusDoc doc = new DeviceStatusDoc();
        doc.setDeviceId(m.deviceId());
        doc.setTs(Instant.ofEpochMilli(m.ts()));
        doc.setLng(m.lng());
        doc.setLat(m.lat());
        doc.setAltitude(m.altitude());
        doc.setSpeed(m.speed());
        doc.setBattery(m.battery());
        mongo.insert(doc);
    }

    public List<DeviceDoc> list() {
        return mongo.findAll(DeviceDoc.class);
    }

    public DeviceStatusDoc lastStatus(String deviceId) {
        return mongo.findOne(
                Query.query(Criteria.where("deviceId").is(deviceId))
                        .with(org.springframework.data.domain.Sort.by(
                                org.springframework.data.domain.Sort.Direction.DESC, "ts")),
                DeviceStatusDoc.class);
    }

    public DeviceDoc get(String deviceId) {
        return mongo.findById(deviceId, DeviceDoc.class);
    }
}

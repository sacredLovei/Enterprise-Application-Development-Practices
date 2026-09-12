package com.course.inspection.device;

import com.course.inspection.common.HeartbeatMsg;
import com.course.inspection.common.RobotTelemetryMsg;
import com.course.inspection.common.UavTelemetryMsg;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    public void saveRobotTelemetry(RobotTelemetryMsg m) {
        DeviceStatusDoc doc = new DeviceStatusDoc();
        doc.setDeviceId(m.deviceId());
        doc.setTs(Instant.ofEpochMilli(m.ts()));
        doc.setLng(m.lng());
        doc.setLat(m.lat());
        doc.setSpeed(m.speed());
        doc.setBattery(m.battery());
        doc.setIrMaxTemp(m.irMaxTemp());
        doc.setAmbientTemp(m.ambientTemp());
        doc.setHumidity(m.humidity());
        doc.setSmoke(m.smoke());
        doc.setGas(m.gas());
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

    /**
     * 全部设备最近位置（S50 优化：单次聚合替代 N+1 lastStatus 查询，
     * PT004 实测 /api/devices 100 线程平均 888ms 定位到此瓶颈）。
     */
    public Map<String, double[]> lastLocationsAll() {
        org.springframework.data.mongodb.core.aggregation.Aggregation agg =
                org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation(
                        org.springframework.data.mongodb.core.aggregation.Aggregation.sort(
                                org.springframework.data.domain.Sort.by(
                                        org.springframework.data.domain.Sort.Direction.DESC, "ts")),
                        org.springframework.data.mongodb.core.aggregation.Aggregation.group("deviceId")
                                .first("ts").as("ts")
                                .first("lng").as("lng")
                                .first("lat").as("lat"),
                        org.springframework.data.mongodb.core.aggregation.Aggregation.limit(200));
        var results = mongo.aggregate(agg, "device_status", org.bson.Document.class).getMappedResults();
        Map<String, double[]> map = new java.util.HashMap<>();
        for (org.bson.Document doc : results) {
            String id = doc.getString("_id");
            Double lng = doc.getDouble("lng");
            Double lat = doc.getDouble("lat");
            if (id != null && lng != null && lat != null) {
                map.put(id, new double[]{lng, lat});
            }
        }
        return map;
    }

    public DeviceDoc get(String deviceId) {
        return mongo.findById(deviceId, DeviceDoc.class);
    }
}

package com.course.inspection.alarm;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/** 告警权威存储（幂等 upsert，设计报告 5.2.2(4)）。 */
@Service
public class AlarmStore {

    private final MongoTemplate mongo;

    public AlarmStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void upsert(AlarmDoc doc) {
        Query query = Query.query(Criteria.where("alarmId").is(doc.getAlarmId()));
        Update update = new Update()
                .set("deviceId", doc.getDeviceId())
                .set("deviceType", doc.getDeviceType())
                .set("alarmType", doc.getAlarmType())
                .set("level", doc.getLevel())
                .set("description", doc.getDescription())
                .set("lng", doc.getLng())
                .set("lat", doc.getLat())
                .set("occurredTime", doc.getOccurredTime())
                .set("snapshotPath", doc.getSnapshotPath())
                .setOnInsert("alarmId", doc.getAlarmId())
                .setOnInsert("status", "PENDING");
        mongo.upsert(query, update, AlarmDoc.class);
    }

    public boolean exists(String alarmId) {
        return mongo.exists(Query.query(Criteria.where("alarmId").is(alarmId)), AlarmDoc.class);
    }

    public long count() {
        return mongo.count(new Query(), AlarmDoc.class);
    }
}

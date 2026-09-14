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

    public AlarmDoc findById(String alarmId) {
        return mongo.findById(alarmId, AlarmDoc.class);
    }

    /** S61：复核结论回填（自动派单回执 / IT009 手动复核共用）。 */
    public void applyReview(String alarmId, String reviewerDeviceId, String conclusion,
                            String note, String imagePath, java.time.Instant reviewedAt) {
        applyReview(alarmId, reviewerDeviceId, conclusion, note, imagePath, null, reviewedAt);
    }

    /** S75：带现场照片路径的复核回填（manualPhotoPath 为 null 时不清除已有值）。 */
    public void applyReview(String alarmId, String reviewerDeviceId, String conclusion,
                            String note, String imagePath, String manualPhotoPath,
                            java.time.Instant reviewedAt) {
        Query query = Query.query(Criteria.where("alarmId").is(alarmId));
        Update update = new Update()
                .set("status", conclusion)
                .set("review.reviewerDeviceId", reviewerDeviceId)
                .set("review.conclusion", conclusion)
                .set("review.note", note)
                .set("review.imagePath", imagePath)
                .set("review.reviewedAt", reviewedAt);
        if (manualPhotoPath != null) {
            update.set("review.manualPhotoPath", manualPhotoPath);
        }
        mongo.updateFirst(query, update, AlarmDoc.class);
    }
}

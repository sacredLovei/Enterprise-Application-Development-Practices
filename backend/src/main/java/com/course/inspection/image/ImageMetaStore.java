package com.course.inspection.image;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

/** 影像元数据读写（S72，幂等 upsert）。 */
@Service
public class ImageMetaStore {

    private final MongoTemplate mongo;

    public ImageMetaStore(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    public void upsert(ImageMetaDoc doc) {
        Query query = Query.query(Criteria.where("imageId").is(doc.getImageId()));
        Update update = new Update()
                .set("deviceId", doc.getDeviceId())
                .set("deviceType", doc.getDeviceType())
                .set("imageType", doc.getImageType())
                .set("lng", doc.getLng())
                .set("lat", doc.getLat())
                .set("ts", doc.getTs())
                .set("alarmId", doc.getAlarmId())
                .setOnInsert("imageId", doc.getImageId());
        mongo.upsert(query, update, ImageMetaDoc.class);
    }

    public List<ImageMetaDoc> list(String deviceId, int page, int size) {
        Query query = new Query()
                .with(org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "ts"))
                .skip((long) page * size)
                .limit(size);
        if (deviceId != null && !deviceId.isBlank()) {
            query.addCriteria(Criteria.where("deviceId").is(deviceId));
        }
        return mongo.find(query, ImageMetaDoc.class);
    }

    public long count(String deviceId) {
        Query query = new Query();
        if (deviceId != null && !deviceId.isBlank()) {
            query.addCriteria(Criteria.where("deviceId").is(deviceId));
        }
        return mongo.count(query, ImageMetaDoc.class);
    }

    public ImageMetaDoc get(String imageId) {
        return mongo.findById(imageId, ImageMetaDoc.class);
    }
}

package com.course.inspection.image;

import com.course.inspection.common.ImageMetaMsg;
import com.course.inspection.common.Json;
import com.course.inspection.common.TopicConst;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * S72：影像元数据消费者（消费组 biz-image-consumer，BUG-008 设计差异兑现）。
 * 幂等：按 imageId upsert；手动提交——业务成功才确认。
 */
@Component
public class ImageMetaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ImageMetaConsumer.class);

    private final ImageMetaStore store;

    public ImageMetaConsumer(ImageMetaStore store) {
        this.store = store;
    }

    @KafkaListener(topics = TopicConst.IMAGE_META,
            groupId = TopicConst.GROUP_IMAGE,
            containerFactory = "manualAckFactory")
    public void onMeta(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            ImageMetaMsg m = Json.fromJson(record.value(), ImageMetaMsg.class);
            ImageMetaDoc doc = new ImageMetaDoc();
            doc.setImageId(m.imageId());
            doc.setDeviceId(m.deviceId());
            doc.setDeviceType(m.deviceType());
            doc.setImageType(m.imageType());
            doc.setLng(m.lng());
            doc.setLat(m.lat());
            doc.setTs(Instant.ofEpochMilli(m.ts()));
            doc.setAlarmId(m.alarmId());
            store.upsert(doc);
            log.debug("影像元数据入库 imageId={} type={} alarmId={}", m.imageId(), m.imageType(), m.alarmId());
            ack.acknowledge();
        } catch (Exception e) {
            log.error("影像元数据消费失败，不提交 offset 等待重投 partition={} offset={}",
                    record.partition(), record.offset(), e);
        }
    }
}

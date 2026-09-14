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
    private Review review;    // S61：机器狗复核结论（复核派单闭环回填）

    /** 复核子文档（S61）：复核设备、结论、备注、红外复核图路径、复核时间；S75 增现场照片路径。 */
    public static class Review {
        private String reviewerDeviceId;
        private String conclusion;   // CONFIRMED / FALSE_ALARM
        private String note;
        private String imagePath;        // 机器狗红外复核图（自动复核）
        private String manualPhotoPath;  // S75：人工复核上传的现场照片
        private Instant reviewedAt;

        public String getReviewerDeviceId() {
            return reviewerDeviceId;
        }

        public void setReviewerDeviceId(String reviewerDeviceId) {
            this.reviewerDeviceId = reviewerDeviceId;
        }

        public String getConclusion() {
            return conclusion;
        }

        public void setConclusion(String conclusion) {
            this.conclusion = conclusion;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

        public String getImagePath() {
            return imagePath;
        }

        public void setImagePath(String imagePath) {
            this.imagePath = imagePath;
        }

        public String getManualPhotoPath() {
            return manualPhotoPath;
        }

        public void setManualPhotoPath(String manualPhotoPath) {
            this.manualPhotoPath = manualPhotoPath;
        }

        public Instant getReviewedAt() {
            return reviewedAt;
        }

        public void setReviewedAt(Instant reviewedAt) {
            this.reviewedAt = reviewedAt;
        }
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

    public Review getReview() {
        return review;
    }

    public void setReview(Review review) {
        this.review = review;
    }
}

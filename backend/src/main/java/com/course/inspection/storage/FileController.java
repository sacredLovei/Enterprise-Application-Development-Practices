package com.course.inspection.storage;

import com.course.inspection.alarm.AlarmDoc;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** 证据图下载接口（S50 补契约 IT013：按告警编号下载 HDFS 证据图）。 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final MongoTemplate mongo;
    private final HdfsClient hdfs;

    public FileController(MongoTemplate mongo, HdfsClient hdfs) {
        this.mongo = mongo;
        this.hdfs = hdfs;
    }

    @GetMapping("/{alarmId}")
    public ResponseEntity<byte[]> download(@PathVariable String alarmId) {
        AlarmDoc alarm = mongo.findOne(
                Query.query(Criteria.where("alarmId").is(alarmId)), AlarmDoc.class);
        if (alarm == null || alarm.getSnapshotPath() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "alarm snapshot not found: " + alarmId);
        }
        byte[] data = hdfs.download(alarm.getSnapshotPath());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                alarm.getSnapshotPath().substring(alarm.getSnapshotPath().lastIndexOf('/') + 1));
        return new ResponseEntity<>(data, headers, HttpStatus.OK);
    }
}

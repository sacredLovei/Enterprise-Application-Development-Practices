package com.course.inspection.alarm;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import com.course.inspection.storage.HdfsClient;
import com.course.inspection.storage.PathBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/** 检索与统计接口（设计报告 5.2.5，FR-4.1~FR-4.7）。 */
@RestController
public class AlarmSearchController {

    private final AlarmSearchService service;
    private final AlarmStore store;
    private final HdfsClient hdfs;
    private final PathBuilder paths;

    public AlarmSearchController(AlarmSearchService service, AlarmStore store,
                                 HdfsClient hdfs, PathBuilder paths) {
        this.service = service;
        this.store = store;
        this.hdfs = hdfs;
        this.paths = paths;
    }

    /** POST /api/search/alarms：组合条件检索（类型/设备/等级/状态/时间/地理半径/关键词/分类）。 */
    @PostMapping("/api/search/alarms")
    public AlarmSearchService.SearchResult search(@RequestBody(required = false) AlarmSearchService.AlarmQuery query) {
        AlarmSearchService.AlarmQuery q = query == null
                ? new AlarmSearchService.AlarmQuery(null, null, null, null, null, null, null, null, null, null, null, 0, 20)
                : query;
        // S71 深分页防护：ES from+size 累计上限 10,000（与 BUG-006 同类陷阱），显式 400 + 明确提示
        if ((long) q.page() * q.size() + q.size() > 10_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "分页深度超过 10,000 条上限（page*size），请缩小时间范围或增加筛选条件");
        }
        try {
            return service.search(q);
        } catch (ElasticsearchException e) {
            // 非法时间格式等查询参数错误 → 400 而非 500（契约 IT011）
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "查询参数错误: " + e.getMessage());
        }
    }

    /** GET /api/alarms：最近告警分页（契约 IT008，倒序）。 */
    @GetMapping("/api/alarms")
    public AlarmSearchService.SearchResult list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size) {
        return search(new AlarmSearchService.AlarmQuery(null, null, null, null, null, "now-24h", "now", null, null, null, null, page, size));
    }

    /** GET /api/search/stats：24 小时统计聚合。 */
    @GetMapping("/api/search/stats")
    public Map<String, Object> stats() {
        return service.stats();
    }

    /** S61/S62：告警详情（Mongo 权威文档，含复核子文档——ES 副本不含 review，TC032 证据链展示用）。 */
    @GetMapping("/api/alarms/{alarmId}")
    public AlarmDoc detail(@PathVariable String alarmId) {
        AlarmDoc alarm = store.findById(alarmId);
        if (alarm == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "alarm not found: " + alarmId);
        }
        return alarm;
    }

    /** S61/IT009 手动复核：结论 + 备注 → 更新告警 status 与 review 字段，ES 副本同步。 */
    public record ReviewRequest(String conclusion, String note) {
    }

    @PostMapping("/api/alarms/{alarmId}/review")
    public Map<String, String> review(@PathVariable String alarmId,
                                      @RequestBody(required = false) ReviewRequest req) {
        if (req == null || req.conclusion() == null
                || !Set.of("CONFIRMED", "FALSE_ALARM").contains(req.conclusion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "conclusion 必须为 CONFIRMED 或 FALSE_ALARM");
        }
        AlarmDoc alarm = store.findById(alarmId);
        if (alarm == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "alarm not found: " + alarmId);
        }
        if ("RESOLVED".equals(alarm.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已处置告警不可再复核: " + alarmId);
        }
        // S75 修正：人工复核不伪造红外复核图——imagePath 仅保留已有自动复核图（若有）
        String imagePath = (alarm.getReview() != null) ? alarm.getReview().getImagePath() : null;
        store.applyReview(alarmId, "MANUAL", req.conclusion(),
                req.note() == null ? "人工复核" : req.note(), imagePath, Instant.now());
        AlarmDoc updated = store.findById(alarmId);
        service.index(AlarmEsDoc.from(updated));   // ES 副本同步 status
        return Map.of("alarmId", alarmId, "status", req.conclusion());
    }

    /** S75：人工复核完善——结论 + 备注 + 现场照片（multipart，可选）一并提交；照片入 HDFS manual_review 目录。 */
    @PostMapping(value = "/api/alarms/{alarmId}/review-photo",
            consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> reviewWithPhoto(@PathVariable String alarmId,
                                               @RequestParam("conclusion") String conclusion,
                                               @RequestParam(value = "note", required = false) String note,
                                               @RequestParam(value = "file", required = false)
                                               org.springframework.web.multipart.MultipartFile file) {
        if (conclusion == null || !Set.of("CONFIRMED", "FALSE_ALARM").contains(conclusion)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "conclusion 必须为 CONFIRMED 或 FALSE_ALARM");
        }
        AlarmDoc alarm = store.findById(alarmId);
        if (alarm == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "alarm not found: " + alarmId);
        }
        if ("RESOLVED".equals(alarm.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "已处置告警不可再复核: " + alarmId);
        }
        String manualPhotoPath = null;
        if (file != null && !file.isEmpty()) {
            if (file.getSize() > 5 * 1024 * 1024) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "现场照片不能超过 5MB");
            }
            String ct = file.getContentType() == null ? "" : file.getContentType();
            String ext = "jpg";
            if (ct.contains("png")) {
                ext = "png";
            } else if (ct.contains("gif")) {
                ext = "gif";
            } else if (ct.contains("webp")) {
                ext = "webp";
            }
            try {
                manualPhotoPath = hdfs.upload(
                        paths.build("manual_review", alarm.getDeviceId(), Instant.now(), ext),
                        file.getBytes());
            } catch (java.io.IOException e) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "照片上传失败: " + e.getMessage());
            }
        }
        String imagePath = (alarm.getReview() != null) ? alarm.getReview().getImagePath() : null;
        store.applyReview(alarmId, "MANUAL", conclusion,
                note == null || note.isBlank() ? "人工复核" : note,
                imagePath, manualPhotoPath, Instant.now());
        AlarmDoc updated = store.findById(alarmId);
        service.index(AlarmEsDoc.from(updated));   // ES 副本同步 status
        return Map.of("alarmId", alarmId, "status", conclusion,
                "manualPhotoPath", manualPhotoPath == null ? "" : manualPhotoPath);
    }
}

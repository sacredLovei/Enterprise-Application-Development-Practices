package com.course.inspection.alarm;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
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

    public AlarmSearchController(AlarmSearchService service, AlarmStore store) {
        this.service = service;
        this.store = store;
    }

    /** POST /api/search/alarms：组合条件检索（类型/设备/等级/时间/地理半径/关键词）。 */
    @PostMapping("/api/search/alarms")
    public AlarmSearchService.SearchResult search(@RequestBody(required = false) AlarmSearchService.AlarmQuery query) {
        AlarmSearchService.AlarmQuery q = query == null
                ? new AlarmSearchService.AlarmQuery(null, null, null, null, null, null, null, null, null, null, 0, 20)
                : query;
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
        return search(new AlarmSearchService.AlarmQuery(null, null, null, null, null, "now-24h", "now", null, null, null, page, size));
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
        String imagePath = (alarm.getReview() != null && alarm.getReview().getImagePath() != null)
                ? alarm.getReview().getImagePath()
                : (alarm.getSnapshotPath() != null ? alarm.getSnapshotPath() : null);
        store.applyReview(alarmId, "MANUAL", req.conclusion(),
                req.note() == null ? "人工复核" : req.note(), imagePath, Instant.now());
        AlarmDoc updated = store.findById(alarmId);
        service.index(AlarmEsDoc.from(updated));   // ES 副本同步 status
        return Map.of("alarmId", alarmId, "status", req.conclusion());
    }
}

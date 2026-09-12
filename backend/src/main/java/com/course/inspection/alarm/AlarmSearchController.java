package com.course.inspection.alarm;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** 检索与统计接口（设计报告 5.2.5，FR-4.1~FR-4.7）。 */
@RestController
public class AlarmSearchController {

    private final AlarmSearchService service;

    public AlarmSearchController(AlarmSearchService service) {
        this.service = service;
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
}

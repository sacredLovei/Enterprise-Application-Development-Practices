package com.course.inspection.alarm;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 检索与统计接口（设计报告 5.2.5，FR-4.1~FR-4.7）。 */
@RestController
@RequestMapping("/api/search")
public class AlarmSearchController {

    private final AlarmSearchService service;

    public AlarmSearchController(AlarmSearchService service) {
        this.service = service;
    }

    /** POST /api/search/alarms：组合条件检索（类型/设备/等级/时间/地理半径/关键词）。 */
    @PostMapping("/alarms")
    public AlarmSearchService.SearchResult search(@RequestBody AlarmSearchService.AlarmQuery query) {
        AlarmSearchService.AlarmQuery q = query == null
                ? new AlarmSearchService.AlarmQuery(null, null, null, null, null, null, null, null, null, null, 0, 20)
                : query;
        return service.search(q);
    }

    /** GET /api/search/stats：24 小时统计聚合。 */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return service.stats();
    }
}

package com.course.inspection.stats;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/** 总览统计接口（S50 补契约 IT014：在线率、任务完成率、24h 告警数）。 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final MongoTemplate mongo;

    public StatsController(MongoTemplate mongo) {
        this.mongo = mongo;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        long total = mongo.count(new Query(), "device");
        long online = mongo.count(Query.query(Criteria.where("status").is("ONLINE")), "device");
        long alarms24h = mongo.count(Query.query(Criteria.where("occurredTime")
                .gte(Instant.now().minus(24, ChronoUnit.HOURS))), "alarm");
        long tasksDone = mongo.count(Query.query(Criteria.where("status").is("DONE")), "task");
        long tasksTotal = mongo.count(new Query(), "task");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("deviceTotal", total);
        result.put("deviceOnline", online);
        result.put("deviceOnlineRate", total == 0 ? 0 : Math.round(online * 1000.0 / total) / 10.0);
        result.put("alarms24h", alarms24h);
        result.put("taskDone", tasksDone);
        result.put("taskTotal", tasksTotal);
        result.put("taskDoneRate", tasksTotal == 0 ? 0 : Math.round(tasksDone * 1000.0 / tasksTotal) / 10.0);
        return result;
    }
}

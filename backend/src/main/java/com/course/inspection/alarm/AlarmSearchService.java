package com.course.inspection.alarm;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.json.JsonData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ES 检索副本读写（设计报告 5.2.5(2)~5.2.5(4)）：
 * 精确条件一律 filter 上下文（不评分、可缓存）；geo_distance 半径检索；聚合统计。
 */
@Service
public class AlarmSearchService {

    private static final Logger log = LoggerFactory.getLogger(AlarmSearchService.class);

    private final ElasticsearchClient client;

    public AlarmSearchService(ElasticsearchClient client) {
        this.client = client;
    }

    /** 批量写入（指定 _id=alarmId，天然幂等覆盖，设计报告 5.2.5(4)）。 */
    public void bulkIndex(List<AlarmEsDoc> docs) {
        if (docs.isEmpty()) {
            return;
        }
        try {
            BulkRequest.Builder br = new BulkRequest.Builder();
            for (AlarmEsDoc d : docs) {
                br.operations(op -> op.index(idx -> idx
                        .index(AlarmIndexInitializer.INDEX)
                        .id(d.getAlarmId())
                        .document(d)));
            }
            BulkResponse resp = client.bulk(br.build());
            if (resp.errors()) {
                log.error("ES 批量写入存在失败项，待对账补偿");
            }
        } catch (IOException e) {
            log.error("ES 批量写入失败，等待对账补偿", e);
        }
    }

    public void index(AlarmEsDoc doc) {
        bulkIndex(List.of(doc));
    }

    public record AlarmQuery(String alarmType, String deviceId, String level, String status,
                             String keyword, String from, String to,
                             Double lng, Double lat, String distance,
                             int page, int size) {
    }

    public record SearchResult(long total, List<AlarmEsDoc> records) {
    }

    /** 组合条件检索（FR-4.1~FR-4.6）：类型/设备/等级/状态/时间范围/地理半径/关键词。 */
    public SearchResult search(AlarmQuery q) {
        try {
            List<co.elastic.clients.elasticsearch._types.query_dsl.Query> filters = new ArrayList<>();
            if (StringUtils.hasText(q.alarmType())) {
                filters.add(term("alarmType", q.alarmType()));
            }
            if (StringUtils.hasText(q.deviceId())) {
                filters.add(term("deviceId", q.deviceId()));
            }
            if (StringUtils.hasText(q.level())) {
                filters.add(term("level", q.level()));
            }
            if (StringUtils.hasText(q.status())) {
                filters.add(term("status", q.status()));
            }

            String from = StringUtils.hasText(q.from()) ? q.from() : "now-24h";
            String to = StringUtils.hasText(q.to()) ? q.to() : "now";
            filters.add(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(b -> b
                    .range(r -> r.field("occurredTime").gte(JsonData.of(from)).lte(JsonData.of(to)))));

            if (q.lng() != null && q.lat() != null && StringUtils.hasText(q.distance())) {
                filters.add(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(b -> b
                        .geoDistance(g -> g.field("location").distance(q.distance())
                                .location(l -> l.latlon(a -> a.lat(q.lat()).lon(q.lng()))))));
            }

            List<co.elastic.clients.elasticsearch._types.query_dsl.Query> musts = new ArrayList<>();
            if (StringUtils.hasText(q.keyword())) {
                musts.add(co.elastic.clients.elasticsearch._types.query_dsl.Query.of(b -> b
                        .match(m -> m.field("description").query(q.keyword()))));
            }

            co.elastic.clients.elasticsearch._types.query_dsl.Query bool =
                    co.elastic.clients.elasticsearch._types.query_dsl.Query.of(b -> b
                            .bool(bb -> bb.filter(filters).must(musts)));

            SearchResponse<AlarmEsDoc> resp = client.search(s -> s
                            .index(AlarmIndexInitializer.INDEX)
                            .query(bool)
                            // S50 定位：ES 默认 hits.total 截断于 10,000（TC026 在 10k 规模实测暴露），
                            // 分页总数必须精确，显式开启 track_total_hits
                            .trackTotalHits(t -> t.enabled(true))
                            .sort(so -> so.field(f -> f.field("occurredTime").order(SortOrder.Desc)))
                            .from(q.page() * q.size())
                            .size(q.size()),
                    AlarmEsDoc.class);

            long total = resp.hits().total() == null ? 0 : resp.hits().total().value();
            List<AlarmEsDoc> records = resp.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
            return new SearchResult(total, records);
        } catch (IOException e) {
            log.error("ES 检索失败", e);
            return new SearchResult(0, List.of());
        }
    }

    /** 统计聚合（FR-4.7）：告警类型分布 + 等级分布 + 按小时趋势。桶手动提取（Aggregation._get() 序列化不可靠）。 */
    public Map<String, Object> stats() {
        try {
            SearchResponse<Void> resp = client.search(s -> s
                            .index(AlarmIndexInitializer.INDEX)
                            .size(0)
                            // 同 search：统计总数不受 10,000 截断（S50 TC026 定位）
                            .trackTotalHits(t -> t.enabled(true))
                            .query(q -> q.range(r -> r.field("occurredTime")
                                    .gte(JsonData.of("now-24h")).lte(JsonData.of("now"))))
                            .aggregations("by_type", a -> a.terms(t -> t.field("alarmType").size(10)))
                            .aggregations("by_level", a -> a.terms(t -> t.field("level")))
                            .aggregations("trend", a -> a.dateHistogram(h -> h
                                    .field("occurredTime").calendarInterval(
                                            co.elastic.clients.elasticsearch._types.aggregations.CalendarInterval.Hour))),
                    Void.class);

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            var byType = resp.aggregations().get("by_type");
            if (byType != null && byType.isSterms()) {
                List<Map<String, Object>> buckets = byType.sterms().buckets().array().stream()
                        .map(b -> Map.<String, Object>of(
                                "key", b.key().stringValue(),
                                "count", b.docCount()))
                        .toList();
                result.put("by_type", buckets);
            }
            var byLevel = resp.aggregations().get("by_level");
            if (byLevel != null && byLevel.isSterms()) {
                List<Map<String, Object>> buckets = byLevel.sterms().buckets().array().stream()
                        .map(b -> Map.<String, Object>of(
                                "key", b.key().stringValue(),
                                "count", b.docCount()))
                        .toList();
                result.put("by_level", buckets);
            }
            var trend = resp.aggregations().get("trend");
            if (trend != null && trend.isDateHistogram()) {
                List<Map<String, Object>> buckets = trend.dateHistogram().buckets().array().stream()
                        .map(b -> Map.<String, Object>of(
                                "time", b.keyAsString(),
                                "count", b.docCount()))
                        .toList();
                result.put("trend", buckets);
            }
            result.put("total_24h", resp.hits().total() == null ? 0 : resp.hits().total().value());
            return result;
        } catch (IOException e) {
            log.error("ES 统计聚合失败", e);
            return Map.of("error", "stats unavailable");
        }
    }

    private static co.elastic.clients.elasticsearch._types.query_dsl.Query term(String field, String value) {
        return co.elastic.clients.elasticsearch._types.query_dsl.Query.of(b -> b
                .term(t -> t.field(field).value(value)));
    }
}

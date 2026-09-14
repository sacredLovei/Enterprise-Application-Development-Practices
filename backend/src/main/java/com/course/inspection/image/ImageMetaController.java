package com.course.inspection.image;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** 影像元数据查询接口（S72，BUG-008 兑现：设计"图片元数据进 MongoDB"链路可查）。 */
@RestController
public class ImageMetaController {

    private final ImageMetaStore store;

    public ImageMetaController(ImageMetaStore store) {
        this.store = store;
    }

    public record ImageMetaPage(long total, List<ImageMetaDoc> records) {
    }

    @GetMapping("/api/images/meta")
    public ImageMetaPage list(@RequestParam(required = false) String deviceId,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "20") int size) {
        return new ImageMetaPage(store.count(deviceId), store.list(deviceId, page, size));
    }

    @GetMapping("/api/images/meta/{imageId}")
    public ImageMetaDoc get(@PathVariable String imageId) {
        ImageMetaDoc doc = store.get(imageId);
        if (doc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "image meta not found: " + imageId);
        }
        return doc;
    }

    /** 简版统计：总量与关联告警量（前端/验收用）。 */
    @GetMapping("/api/images/meta-stats")
    public Map<String, Object> stats() {
        return Map.of("total", store.count(null),
                "alarmLinked", store.list(null, 0, 10000).stream()
                        .filter(d -> d.getAlarmId() != null).count());
    }
}

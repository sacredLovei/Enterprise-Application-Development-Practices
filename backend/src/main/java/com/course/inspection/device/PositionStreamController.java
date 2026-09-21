package com.course.inspection.device;

import com.course.inspection.common.Json;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * S98：设备实时位置流（SSE）。
 *
 * 背景：总览页原为 3 秒轮询快照，地图标记位置跳变。本端点以 1 秒粒度推送
 * "台账 + 最近遥测位置"（与 GET /api/devices 完全同一套 DeviceVo 拼装逻辑），
 * 前端收到后对标记做插值动画，实现肉眼连续移动。
 *
 * 设计取舍（与用户确认的方案二）：
 * - 数据源用 Mongo 快照而非 Kafka 直推：双实例负载均衡会把遥测分区拆到两个后端，
 *   直推会导致 SSE 连接所在实例丢失另一实例消费的设备；Mongo 快照天然聚合全量，
 *   无需引入 Redis 等跨实例组件。4 台设备 + {deviceId,ts} 复合索引（风险 #34），
 *   每秒一次查询成本可忽略。
 * - 载荷有变化才推送；连续 15 个周期无变化发一次 SSE 注释心跳，
 *   防止 nginx/浏览器因空闲超时断连（配合网关 proxy_read_timeout 3600s）。
 * - 鉴权沿用 /api/** 拦截器：前端用 fetch 流式读取（可带 Authorization 头），
 *   不用原生 EventSource（无法自定义头），也不把令牌放 URL（与 S96 口径一致）。
 */
@RestController
@RequestMapping("/api/stream")
public class PositionStreamController {

    /** 推送载荷：与 DeviceVo 同源，仅保留地图与卡片需要的字段。 */
    public record PositionItem(String deviceId, Double lng, Double lat, String status, int battery) {
    }

    public record PositionsMsg(long ts, List<PositionItem> devices) {
    }

    private final DeviceStore store;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final AtomicLong tick = new AtomicLong();
    private volatile String lastPayload = "";

    private static final long PERIOD_MS = 1000;          // 1 秒粒度（遥测 2s 产生，足够）
    private static final int HEARTBEAT_TICKS = 15;       // 15 个无变化周期发一次注释心跳

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "position-stream");
        t.setDaemon(true);
        return t;
    });

    public PositionStreamController(DeviceStore store) {
        this.store = store;
        scheduler.scheduleAtFixedRate(this::tick, PERIOD_MS, PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    /** SSE 订阅端点。timeout=0 表示不由容器超时，断连由 onError/onCompletion 回收。 */
    @GetMapping(value = "/positions", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter positions() {
        SseEmitter em = new SseEmitter(0L);
        em.onTimeout(() -> remove(em));
        em.onError(e -> remove(em));
        em.onCompletion(() -> remove(em));
        emitters.add(em);
        // 订阅即推一份当前快照，前端无需等下一个周期
        try {
            em.send(SseEmitter.event().name("positions").data(snapshot()));
        } catch (Exception e) {
            remove(em);
        }
        return em;
    }

    private void tick() {
        long n = tick.incrementAndGet();
        String payload = snapshot();
        boolean changed = !payload.equals(lastPayload);
        if (!changed && n % HEARTBEAT_TICKS != 0) {
            return;   // 无变化且未到心跳周期，静默
        }
        for (SseEmitter em : emitters) {
            try {
                if (changed) {
                    em.send(SseEmitter.event().name("positions").data(payload));
                } else {
                    em.send(SseEmitter.event().comment("keep-alive"));
                }
            } catch (Exception e) {
                remove(em);   // 客户端断开/网络异常：回收连接，避免 emitter 泄漏
            }
        }
        if (changed) {
            lastPayload = payload;
        }
    }

    /** 台账 + 最近遥测位置快照（与 /api/devices 同一套拼装口径）。 */
    private String snapshot() {
        List<PositionItem> items = store.list().stream()
                .map(d -> {
                    DeviceStatusDoc last = store.lastStatus(d.getDeviceId());
                    return new PositionItem(
                            d.getDeviceId(),
                            last != null ? last.getLng() : null,
                            last != null ? last.getLat() : null,
                            d.getStatus(),
                            d.getBattery());
                })
                .toList();
        return Json.toJson(new PositionsMsg(System.currentTimeMillis(), items));
    }

    private void remove(SseEmitter em) {
        emitters.remove(em);
    }
}

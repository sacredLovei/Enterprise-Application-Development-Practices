package com.course.inspection.device;

import com.course.inspection.common.Json;
import com.course.inspection.common.TaskCommandMsg;
import com.course.inspection.common.TopicConst;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** 设备查询与控制接口（经 Nginx 网关对外）。 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceStore store;
    private final KafkaTemplate<String, String> kafka;

    public DeviceController(DeviceStore store, KafkaTemplate<String, String> kafka) {
        this.store = store;
        this.kafka = kafka;
    }

    @GetMapping
    public List<DeviceVo> list() {
        // 台账 + 最近遥测位置（S40 地图展示）
        return store.list().stream()
                .map(d -> DeviceVo.from(d, store.lastStatus(d.getDeviceId())))
                .toList();
    }

    @GetMapping("/{deviceId}")
    public DeviceDoc get(@PathVariable String deviceId) {
        DeviceDoc doc = store.get(deviceId);
        if (doc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "device not found: " + deviceId);
        }
        return doc;
    }

    /** 手动下线（S34）：控制指令经 Kafka 下发，仿真停发心跳/遥测，平台 15s 内判定 OFFLINE。 */
    @PostMapping("/{deviceId}/offline")
    public String offline(@PathVariable String deviceId) {
        return control(deviceId, "COMM_OFFLINE");
    }

    /** 手动上线（S34）：仿真恢复上报，平台随心跳置回 ONLINE。 */
    @PostMapping("/{deviceId}/online")
    public String online(@PathVariable String deviceId) {
        return control(deviceId, "COMM_RESTORE");
    }

    private String control(String deviceId, String type) {
        DeviceDoc doc = store.get(deviceId);
        if (doc == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "device not found: " + deviceId);
        }
        TaskCommandMsg cmd = new TaskCommandMsg(
                "CTRL-" + UUID.randomUUID().toString().substring(0, 8),
                type, deviceId, 1, System.currentTimeMillis(), null, null);
        kafka.send(TopicConst.TASK_COMMAND, deviceId, Json.toJson(cmd));
        return type + " sent to " + deviceId;
    }
}

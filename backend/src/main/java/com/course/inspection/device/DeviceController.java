package com.course.inspection.device;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/** 设备查询接口（经 Nginx 网关对外）。 */
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceStore store;

    public DeviceController(DeviceStore store) {
        this.store = store;
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
}

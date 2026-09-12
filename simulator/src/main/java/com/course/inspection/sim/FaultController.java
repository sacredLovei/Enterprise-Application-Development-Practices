package com.course.inspection.sim;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 故障注入端点（设计报告 5.2.1(3)，FR-1.4）：
 * POST /sim/fault?type=BATTERY_DROP|COMM_OFFLINE|OVERHEAT
 * 注意：此为仿真运维通道（宿主机映射端口），业务入口仍只有 Nginx 8080。
 */
@RestController
@RequestMapping("/sim/fault")
public class FaultController {

    private final DeviceSimulator simulator;

    public FaultController(DeviceSimulator simulator) {
        this.simulator = simulator;
    }

    @PostMapping
    public String inject(@RequestParam String type) {
        simulator.injectFault(type);
        return "fault injected: " + type;
    }
}

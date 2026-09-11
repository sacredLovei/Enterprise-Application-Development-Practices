package com.course.inspection.sim;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 无人机仿真模块（S21 最小版）。
 * 职责：周期产生心跳（5s）与遥测（2s）消息，作为生产者写入 Kafka。
 * 多实例启动方式（设计报告 5.2.1(2)）：
 *   java -jar inspection-simulator.jar --sim.device-id=UAV-001
 */
@SpringBootApplication
@EnableScheduling
public class SimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulatorApplication.class, args);
    }
}

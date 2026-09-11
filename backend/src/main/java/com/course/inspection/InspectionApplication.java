package com.course.inspection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 巡检平台业务服务（S21 最小版）。
 * 职责：消费 Kafka 心跳/遥测 → 落 MongoDB → 提供设备查询接口 → 响应头标注实例号。
 */
@SpringBootApplication
public class InspectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(InspectionApplication.class, args);
    }
}

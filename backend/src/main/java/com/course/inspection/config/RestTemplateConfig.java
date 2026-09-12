package com.course.inspection.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** WebHDFS 用的 RestTemplate（设计报告 5.2.3：以 REST 替代 hadoop-client，规避依赖冲突 D-7）。 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

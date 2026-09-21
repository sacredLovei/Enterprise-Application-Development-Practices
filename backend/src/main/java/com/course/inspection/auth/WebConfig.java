package com.course.inspection.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * S88：注册鉴权拦截器。
 *
 * 白名单（无需令牌）：
 * - {@code /api/auth/login}：登录入口本身；
 * - 静态资源与前端路由不在 {@code /api/**} 内，天然不受影响；
 * - {@code /actuator/health}（Spring Boot 健康检查，容器健康探针使用）、{@code /swagger-ui/**}、
 *   {@code /v3/api-docs/**}（S76 接口文档）同样不在 {@code /api/**} 内，故无需显式放行。
 *
 * 其余 {@code /api/**} 全部要求 Bearer 令牌。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    public WebConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }
}

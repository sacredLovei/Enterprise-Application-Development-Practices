package com.course.inspection.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 每个响应带上本实例标识（X-Backend-Instance）。
 * 用于现场演示 Nginx 负载均衡：多次刷新即见实例号轮换（设计报告 5.2.6）。
 */
@Component
public class InstanceHeaderFilter extends OncePerRequestFilter {

    @Value("${app.instance-id:backend-1}")
    private String instanceId;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("X-Backend-Instance", instanceId);
        filterChain.doFilter(request, response);
    }
}

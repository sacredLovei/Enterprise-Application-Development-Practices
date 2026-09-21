package com.course.inspection.common;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * S89：全局异常处理的契约测试（不依赖 Spring 上下文，standalone MockMvc）。
 * 三条判定：① 业务异常保留状态码与 message；② 校验失败 400 且带字段明细（不是 500）；③ 兜底 500 不吐栈。
 */
class GlobalExceptionHandlerTest {

    @RestController
    @RequestMapping("/api/test-errors")
    static class ProbeController {

        record Payload(@NotBlank(message = "deviceId 不能为空") String deviceId) {
        }

        @PostMapping("/validated")
        public String validated(@Valid @RequestBody Payload payload) {
            return payload.deviceId();
        }

        @PostMapping("/body")
        public String body(@RequestBody Payload payload) {
            return payload.deviceId();
        }

        @org.springframework.web.bind.annotation.GetMapping("/not-found")
        public String notFound() {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "alarm not found: ALM-x");
        }

        @org.springframework.web.bind.annotation.GetMapping("/conflict")
        public String conflict() {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "task not cancelable: T-1");
        }

        @org.springframework.web.bind.annotation.GetMapping("/boom")
        public String boom() {
            throw new IllegalStateException("内部实现细节：connection pool exhausted");
        }
    }

    private MockMvc mvc() {
        return MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    /** MockMvc 响应默认按 ISO-8859-1 解码，中文断言必须显式指定 UTF-8（否则永远是 mojibake）。 */
    private static String body(MvcResult r) throws Exception {
        return r.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    @Test
    void statusExceptionKeepsCodeAndMessage() throws Exception {
        MvcResult r = mvc().perform(get("/api/test-errors/not-found")).andReturn();
        assertEquals(404, r.getResponse().getStatus());
        String text = body(r);
        assertTrue(text.contains("\"code\":404"), text);
        assertTrue(text.contains("alarm not found: ALM-x"), text);

        MvcResult c = mvc().perform(get("/api/test-errors/conflict")).andReturn();
        assertEquals(409, c.getResponse().getStatus());
        assertTrue(body(c).contains("task not cancelable: T-1"));
    }

    @Test
    void validationFailureReturns400WithFieldDetails() throws Exception {
        MvcResult r = mvc().perform(post("/api/test-errors/validated")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"deviceId\":\"\"}")).andReturn();
        assertEquals(400, r.getResponse().getStatus());
        String text = body(r);
        assertTrue(text.contains("\"fields\""), text);
        assertTrue(text.contains("deviceId"), text);
        assertTrue(text.contains("deviceId 不能为空"), text);
    }

    @Test
    void unreadableBodyReturns400() throws Exception {
        MvcResult r = mvc().perform(post("/api/test-errors/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{not-json")).andReturn();
        assertEquals(400, r.getResponse().getStatus());
        assertTrue(body(r).contains("请求体缺失或 JSON 格式错误"));
    }

    @Test
    void fallbackReturns500WithoutStackTrace() throws Exception {
        MvcResult r = mvc().perform(get("/api/test-errors/boom")).andReturn();
        assertEquals(500, r.getResponse().getStatus());
        String text = body(r);
        assertTrue(text.contains("\"code\":500"), text);
        assertTrue(text.contains("服务内部错误"), text);
        assertFalse(text.contains("connection pool exhausted"), "500 响应不得泄露内部异常细节");
        assertFalse(text.contains("IllegalStateException"), "500 响应不得泄露异常类名");
    }

    /** 与拦截器 401 响应体结构保持一致的字段约定（结构对齐，便于前端统一处理）。 */
    @Test
    void errorBodyShapeIsStable() {
        GlobalExceptionHandler h = new GlobalExceptionHandler();
        ResponseEntity<java.util.Map<String, Object>> res = h.handleOther(
                new RuntimeException("x"), new org.springframework.mock.web.MockHttpServletRequest());
        assertEquals(500, res.getStatusCode().value());
        assertTrue(res.getBody().keySet().containsAll(java.util.List.of(
                "timestamp", "status", "code", "error", "message", "path")));
    }
}

package com.course.inspection.common;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * S89：全局异常处理（对应设计报告 6.4.2"接口测试判定原则"所述的统一错误语义）。
 *
 * 统一所有 {@code /api/**} 的错误响应体为
 * {@code {timestamp, status, code, error, message, path[, fields]}}，并保证：
 * <ul>
 *   <li>业务异常（{@link ResponseStatusException}）保留原状态码与业务 message（404 / 400 / 409 / 500 语义不变）；</li>
 *   <li>参数校验失败（{@code @Valid}）返回 <b>400</b> 并附逐字段明细（<b>不得是 500</b>）；</li>
 *   <li>请求体不可解析、缺少必填参数、参数类型不匹配 → <b>400</b>；</li>
 *   <li>兜底异常统一 <b>500</b>，只回中性文案、<b>不向客户端吐异常栈</b>，细节仅落服务端日志。</li>
 * </ul>
 * 鉴权失败（401）由 {@code AuthInterceptor} 直接写出，不经本处理器，响应体结构保持一致。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 业务异常：控制器/service 主动抛出的状态语义异常。 */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : (status != null ? status.getReasonPhrase() : "请求失败");
        Map<String, Object> body = base(status != null ? status : HttpStatus.INTERNAL_SERVER_ERROR, message, req);
        if (status != null && status.is5xxServerError()) {
            log.error("业务异常 {} {} -> {}", req.getMethod(), req.getRequestURI(), message, ex);
        } else {
            log.warn("业务异常 {} {} -> {} {}", req.getMethod(), req.getRequestURI(),
                    ex.getStatusCode().value(), message);
        }
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }

    /** 参数校验失败（@Valid）：400 + 字段明细。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex,
                                                               HttpServletRequest req) {
        List<Map<String, Object>> fields = new ArrayList<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("field", fe.getField());
            f.put("message", fe.getDefaultMessage());
            f.put("rejectedValue", fe.getRejectedValue() == null ? null : String.valueOf(fe.getRejectedValue()));
            fields.add(f);
        }
        Map<String, Object> body = base(HttpStatus.BAD_REQUEST, "参数校验失败", req);
        body.put("fields", fields);
        log.warn("参数校验失败 {} {} -> {}", req.getMethod(), req.getRequestURI(), fields);
        return ResponseEntity.badRequest().body(body);
    }

    /** 请求体缺失/格式错误。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex,
                                                                HttpServletRequest req) {
        String message = "请求体缺失或 JSON 格式错误";
        log.warn("请求体解析失败 {} {} -> {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest().body(base(HttpStatus.BAD_REQUEST, message, req));
    }

    /** 缺少必填查询参数。 */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex,
                                                                  HttpServletRequest req) {
        String message = "缺少必填参数: " + ex.getParameterName();
        log.warn("缺少参数 {} {} -> {}", req.getMethod(), req.getRequestURI(), ex.getParameterName());
        return ResponseEntity.badRequest().body(base(HttpStatus.BAD_REQUEST, message, req));
    }

    /** 参数类型不匹配（如分页参数传字符串）。 */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                                  HttpServletRequest req) {
        String message = "参数类型错误: " + ex.getName();
        log.warn("参数类型错误 {} {} -> {}", req.getMethod(), req.getRequestURI(), message);
        return ResponseEntity.badRequest().body(base(HttpStatus.BAD_REQUEST, message, req));
    }

    /** 兜底：不向客户端泄露栈信息。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleOther(Exception ex, HttpServletRequest req) {
        log.error("未处理异常 {} {}", req.getMethod(), req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(base(HttpStatus.INTERNAL_SERVER_ERROR, "服务内部错误，请稍后重试", req));
    }

    private Map<String, Object> base(HttpStatus status, String message, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("code", status.value());
        body.put("error", status.name());
        body.put("message", message);
        body.put("path", req.getRequestURI());
        return body;
    }
}

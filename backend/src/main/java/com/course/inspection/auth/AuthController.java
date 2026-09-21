package com.course.inspection.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S88：认证接口（课程任务项 1「简单权限认证」）。
 *
 * - {@code POST /api/auth/login}：用户名 + 口令 → Bearer 令牌（唯一免鉴权接口）；
 * - {@code GET  /api/auth/me}：返回当前令牌对应用户（前端用于刷新后恢复登录态）；
 * - {@code POST /api/auth/logout}：把当前令牌 jti 写入吊销表，令牌立即失效。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthProperties props;
    private final TokenService tokens;
    private final MongoTemplate mongo;

    public AuthController(AuthProperties props, TokenService tokens, MongoTemplate mongo) {
        this.props = props;
        this.tokens = tokens;
        this.mongo = mongo;
    }

    /** 登录请求体。 */
    public record LoginRequest(String username, String password) {
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody(required = false) LoginRequest req) {
        if (req == null || isBlank(req.username()) || isBlank(req.password())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名与口令不能为空");
        }
        AuthProperties.User user = props.find(req.username().trim());
        if (user == null || !req.password().equals(user.getPassword())) {
            // 不区分"用户不存在"与"口令错误"，避免用户名枚举
            log.warn("登录失败 username={}", req.username());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或口令错误");
        }

        String token = tokens.issue(user.getUsername(), user.getDisplayName(), user.getRole());
        log.info("登录成功 username={} role={}", user.getUsername(), user.getRole());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("tokenType", "Bearer");
        out.put("expiresAt", Instant.ofEpochMilli(tokens.expiryOf(token)).toString());
        out.put("username", user.getUsername());
        out.put("displayName", user.getDisplayName());
        out.put("role", user.getRole());
        return out;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("username", request.getAttribute(AuthInterceptor.ATTR_USERNAME));
        out.put("displayName", request.getAttribute(AuthInterceptor.ATTR_DISPLAY_NAME));
        out.put("role", request.getAttribute(AuthInterceptor.ATTR_ROLE));
        out.put("expiresAt", Instant.ofEpochMilli(
                (Long) request.getAttribute(AuthInterceptor.ATTR_EXP)).toString());
        return out;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(HttpServletRequest request) {
        String jti = (String) request.getAttribute(AuthInterceptor.ATTR_JTI);
        String username = (String) request.getAttribute(AuthInterceptor.ATTR_USERNAME);
        Object exp = request.getAttribute(AuthInterceptor.ATTR_EXP);
        if (jti != null) {
            Instant expAt = Instant.ofEpochMilli(exp instanceof Long l ? l : Instant.now().toEpochMilli());
            // 幂等：重复登出不会产生重复文档（_id = jti）
            if (!mongo.exists(org.springframework.data.mongodb.core.query.Query
                    .query(org.springframework.data.mongodb.core.query.Criteria.where("_id").is(jti)),
                    RevokedToken.class)) {
                mongo.insert(new RevokedToken(jti, username, expAt));
            }
            log.info("登出 username={} jti={}", username, jti);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("message", "已退出登录");
        return out;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}

package com.course.inspection.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * S88：统一鉴权拦截器。
 *
 * 保护范围：{@code /api/**}（白名单除外，见 {@link WebConfig}）。
 * 校验顺序：① 是否有 Bearer 令牌 → ② 签名与有效期 → ③ 是否已登出（MongoDB 吊销表）。
 * 任一不通过统一返回 <b>401</b> 与结构化 JSON，语义明确（缺令牌 / 令牌无效或过期 / 令牌已登出）。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USERNAME = "auth.username";
    public static final String ATTR_DISPLAY_NAME = "auth.displayName";
    public static final String ATTR_ROLE = "auth.role";
    public static final String ATTR_JTI = "auth.jti";
    public static final String ATTR_EXP = "auth.exp";

    private static final String BEARER = "Bearer ";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final TokenService tokens;
    private final MongoTemplate mongo;

    public AuthInterceptor(TokenService tokens, MongoTemplate mongo) {
        this.tokens = tokens;
        this.mongo = mongo;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // 跨域预检请求不带业务凭据，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, BEARER, 0, BEARER.length())
                || header.substring(BEARER.length()).trim().isEmpty()) {
            return deny(response, "未认证：缺少访问令牌（请在登录页登录后重试）");
        }

        String token = header.substring(BEARER.length()).trim();
        TokenService.Claims claims = tokens.verify(token);
        if (claims == null) {
            return deny(response, "未认证：访问令牌无效或已过期，请重新登录");
        }

        boolean revoked = mongo.exists(
                Query.query(Criteria.where("_id").is(claims.jti())), RevokedToken.class);
        if (revoked) {
            return deny(response, "未认证：该访问令牌已退出登录，请重新登录");
        }

        request.setAttribute(ATTR_USERNAME, claims.username());
        request.setAttribute(ATTR_DISPLAY_NAME, claims.displayName());
        request.setAttribute(ATTR_ROLE, claims.role());
        request.setAttribute(ATTR_JTI, claims.jti());
        request.setAttribute(ATTR_EXP, claims.expEpochMilli());
        return true;
    }

    /** 统一 401 响应体（与业务异常 JSON 结构保持一致：code / error / message）。 */
    private boolean deny(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", 401);
        body.put("error", "UNAUTHORIZED");
        body.put("message", message);
        response.getWriter().write(MAPPER.writeValueAsString(body));
        return false;
    }
}

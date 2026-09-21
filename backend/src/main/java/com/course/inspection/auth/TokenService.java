package com.course.inspection.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * S88：无状态令牌服务。
 *
 * 令牌格式：base64url(payloadJson) + "." + base64url(HMAC-SHA256(payloadJson))
 * payload = {sub, name, role, jti, exp(epoch millis)}
 *
 * 设计取舍（课程范围内的"简单"实现）：
 * - 只用 JDK 的 HmacSHA256，不引入 JWT 依赖（离线/代理环境下构建更稳）；
 * - 自包含令牌：后端任一实例用同一密钥即可校验，天然适配 nginx 双实例负载均衡（D-4）；
 * - 签名比对使用 {@link MessageDigest#isEqual} 常量时间比较，避免时序侧信道；
 * - 无状态令牌无法"主动作废"，故登出经 MongoDB 吊销表实现（见 {@link RevokedToken}）。
 */
@Service
public class TokenService {

    private static final Base64.Encoder B64E = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String HMAC_ALG = "HmacSHA256";

    private final ObjectMapper mapper = new ObjectMapper();
    private final AuthProperties props;

    public TokenService(AuthProperties props) {
        this.props = props;
    }

    /** 令牌声明（校验通过后的可信内容）。 */
    public record Claims(String username, String displayName, String role, String jti, long expEpochMilli) {
    }

    /** 签发令牌。 */
    public String issue(String username, String displayName, String role) {
        long exp = Instant.now().plusSeconds(props.getTtlHours() * 3600L).toEpochMilli();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", username);
        payload.put("name", displayName);
        payload.put("role", role);
        payload.put("jti", UUID.randomUUID().toString());
        payload.put("exp", exp);
        try {
            String body = B64E.encodeToString(mapper.writeValueAsBytes(payload));
            return body + "." + B64E.encodeToString(sign(body));
        } catch (Exception e) {
            throw new IllegalStateException("令牌签发失败: " + e.getMessage(), e);
        }
    }

    /** 令牌有效期（毫秒时间戳），供登录响应返回。 */
    public long expiryOf(String token) {
        Claims c = verify(token);
        return c == null ? 0L : c.expEpochMilli();
    }

    /** 校验令牌；格式/签名/有效期任一不通过返回 null。 */
    public Claims verify(String token) {
        if (token == null) {
            return null;
        }
        int dot = token.lastIndexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return null;
        }
        String body = token.substring(0, dot);
        String sig = token.substring(dot + 1);
        try {
            if (!MessageDigest.isEqual(sign(body), B64D.decode(sig))) {
                return null;
            }
            Map<?, ?> p = mapper.readValue(B64D.decode(body), Map.class);
            long exp = ((Number) p.get("exp")).longValue();
            if (exp <= Instant.now().toEpochMilli()) {
                return null;
            }
            return new Claims(
                    String.valueOf(p.get("sub")),
                    String.valueOf(p.get("name")),
                    String.valueOf(p.get("role")),
                    String.valueOf(p.get("jti")),
                    exp);
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] sign(String body) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(new SecretKeySpec(props.getSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALG));
            return mac.doFinal(body.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("令牌签名失败: " + e.getMessage(), e);
        }
    }
}

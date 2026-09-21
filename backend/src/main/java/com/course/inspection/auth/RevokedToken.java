package com.course.inspection.auth;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

/**
 * S88：已登出令牌的吊销表（集合 {@code auth_revoked}）。
 *
 * 无状态令牌本身不可撤回，登出时把该令牌的 {@code jti} 记入本集合；
 * 拦截器每次请求按 {@code _id} 主键查一次（索引命中，代价极小）。
 *
 * 存 MongoDB 而非单实例内存的原因：后端为双实例负载均衡（D-4），
 * 内存吊销表会导致"在 A 实例登出、请求落到 B 实例仍然有效"。
 * {@code exp} 上建 TTL 索引，过期令牌由 MongoDB 自动清理（90 天 TTL 同族做法见 task_log）。
 */
@Document(collection = "auth_revoked")
public class RevokedToken {

    /** 令牌唯一编号 jti，直接作为 _id（避免重复吊销写入重复文档）。 */
    @MongoId
    private String jti;

    @Field("username")
    private String username;

    /** 令牌原始到期时间；TTL 索引到点即删除（过期令牌本就无法通过签名+有效期校验）。 */
    @Indexed(expireAfter = "0s")
    @Field("exp")
    private Instant exp;

    @Field("revokedAt")
    private Instant revokedAt = Instant.now();

    public RevokedToken() {
    }

    public RevokedToken(String jti, String username, Instant exp) {
        this.jti = jti;
        this.username = username;
        this.exp = exp;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Instant getExp() {
        return exp;
    }

    public void setExp(Instant exp) {
        this.exp = exp;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
    }
}

package com.course.inspection.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * S88：认证配置（课程任务项 1「简单权限认证」）。
 * 演示账号以配置方式提供（不引入用户表/注册流程，课程范围为"简单"认证）；
 * 生产场景应改为用户表 + 加盐哈希口令，见 STATE 决策记录与本步验收记录中的边界说明。
 */
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    /** HMAC-SHA256 签名密钥（生产应经环境变量注入；多实例必须一致，否则跨实例令牌校验失败）。 */
    private String secret = "inspection-course-demo-secret";

    /** 令牌有效期（小时）。 */
    private int ttlHours = 12;

    private List<User> users = new ArrayList<>();

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public int getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(int ttlHours) {
        this.ttlHours = ttlHours;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    /** 按用户名查找（用户名不区分大小写）。 */
    public User find(String username) {
        if (username == null) {
            return null;
        }
        return users.stream()
                .filter(u -> username.equalsIgnoreCase(u.getUsername()))
                .findFirst()
                .orElse(null);
    }

    public static class User {
        private String username;
        private String password;
        private String displayName;
        private String role = "OPERATOR";

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }
    }
}

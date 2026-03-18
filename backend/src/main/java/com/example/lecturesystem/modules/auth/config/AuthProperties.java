package com.example.lecturesystem.modules.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {
    private DemoUser demoUser = new DemoUser();

    public DemoUser getDemoUser() {
        return demoUser;
    }

    public void setDemoUser(DemoUser demoUser) {
        this.demoUser = demoUser;
    }

    public static class DemoUser {
        private Long userId = 1L;
        private String username = "admin";
        private String password = "admin123";
        private String realName = "超级管理员";
        private boolean superAdmin = true;

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

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

        public String getRealName() {
            return realName;
        }

        public void setRealName(String realName) {
            this.realName = realName;
        }

        public boolean isSuperAdmin() {
            return superAdmin;
        }

        public void setSuperAdmin(boolean superAdmin) {
            this.superAdmin = superAdmin;
        }
    }
}

package com.example.lecturesystem.modules.auth.security;

public class LoginUser {
    private static final String ROLE_ADMIN = "ADMIN";

    private final Long userId;
    private final String username;
    private final String realName;
    private final String role;

    public LoginUser(Long userId, String username, String realName, boolean superAdmin) {
        this(userId, username, realName, superAdmin ? ROLE_ADMIN : "USER");
    }

    public LoginUser(Long userId, String username, String realName, String role) {
        this.userId = userId;
        this.username = username;
        this.realName = realName;
        this.role = role == null || role.isBlank() ? "USER" : role.trim().toUpperCase();
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRealName() {
        return realName;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }

    public boolean isSuperAdmin() {
        return isAdmin();
    }
}

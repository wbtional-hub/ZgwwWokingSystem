package com.example.lecturesystem.modules.auth.vo;

import com.example.lecturesystem.modules.auth.security.LoginUser;

public class LoginVO {
    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String role;
    private Boolean superAdmin;

    public static LoginVO fromLoginUser(LoginUser loginUser) {
        LoginVO vo = new LoginVO();
        vo.setUserId(loginUser.getUserId());
        vo.setUsername(loginUser.getUsername());
        vo.setRealName(loginUser.getRealName());
        vo.setRole(loginUser.getRole());
        vo.setSuperAdmin(loginUser.isAdmin());
        return vo;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Boolean getSuperAdmin() { return superAdmin; }
    public void setSuperAdmin(Boolean superAdmin) { this.superAdmin = superAdmin; }
}

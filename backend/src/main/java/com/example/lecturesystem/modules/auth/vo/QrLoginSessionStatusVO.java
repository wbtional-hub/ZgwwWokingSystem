package com.example.lecturesystem.modules.auth.vo;

public class QrLoginSessionStatusVO {
    private String status;
    private LoginVO login;

    public QrLoginSessionStatusVO() {
    }

    public QrLoginSessionStatusVO(String status) {
        this.status = status;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LoginVO getLogin() { return login; }
    public void setLogin(LoginVO login) { this.login = login; }
}

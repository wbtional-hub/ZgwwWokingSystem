package com.example.lecturesystem.modules.user.vo;

import java.util.ArrayList;
import java.util.List;

public class UserPageVO {
    private Integer pageNo;
    private Integer pageSize;
    private Long total;
    private Long scopeUserCount;
    private String scopeType;
    private String scopeDescription;
    private List<UserListItemVO> list = new ArrayList<>();

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Long getScopeUserCount() {
        return scopeUserCount;
    }

    public void setScopeUserCount(Long scopeUserCount) {
        this.scopeUserCount = scopeUserCount;
    }

    public String getScopeType() {
        return scopeType;
    }

    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }

    public String getScopeDescription() {
        return scopeDescription;
    }

    public void setScopeDescription(String scopeDescription) {
        this.scopeDescription = scopeDescription;
    }

    public List<UserListItemVO> getList() {
        return list;
    }

    public void setList(List<UserListItemVO> list) {
        this.list = list;
    }
}

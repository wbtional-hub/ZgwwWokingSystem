package com.example.lecturesystem.modules.weeklywork.entity;

import java.time.LocalDateTime;

public class WeeklyWorkApprovalLogEntity {
    private Long id;
    private Long weeklyWorkId;
    private String action;
    private String fromNode;
    private String toNode;
    private Long reviewerUserId;
    private String reviewerName;
    private String comment;
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWeeklyWorkId() {
        return weeklyWorkId;
    }

    public void setWeeklyWorkId(Long weeklyWorkId) {
        this.weeklyWorkId = weeklyWorkId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFromNode() {
        return fromNode;
    }

    public void setFromNode(String fromNode) {
        this.fromNode = fromNode;
    }

    public String getToNode() {
        return toNode;
    }

    public void setToNode(String toNode) {
        this.toNode = toNode;
    }

    public Long getReviewerUserId() {
        return reviewerUserId;
    }

    public void setReviewerUserId(Long reviewerUserId) {
        this.reviewerUserId = reviewerUserId;
    }

    public String getReviewerName() {
        return reviewerName;
    }

    public void setReviewerName(String reviewerName) {
        this.reviewerName = reviewerName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}

package com.example.lecturesystem.modules.weeklywork.vo;

import java.time.LocalDateTime;
import java.util.List;

public class WeeklyWorkDetailVO {
    private Long id;
    private Long unitId;
    private Long userId;
    private String weekNo;
    private String status;
    private String workPlan;
    private String workContent;
    private String remark;
    private String currentApprovalNode;
    private Long currentHandlerUserId;
    private String currentHandlerUserName;
    private Integer currentFlowOrder;
    private Long finalApproverUserId;
    private String lastReturnTarget;
    private String lastReturnComment;
    private Long lastReviewBy;
    private String lastReviewByName;
    private LocalDateTime lastReviewTime;
    private LocalDateTime submitTime;
    private LocalDateTime approvedTime;
    private LocalDateTime createTime;
    private List<WeeklyWorkFlowNodeVO> flowNodes;
    private List<String> availableReturnTargets;
    private List<WeeklyWorkApprovalLogVO> approvalLogs;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getWeekNo() {
        return weekNo;
    }

    public void setWeekNo(String weekNo) {
        this.weekNo = weekNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getWorkPlan() {
        return workPlan;
    }

    public void setWorkPlan(String workPlan) {
        this.workPlan = workPlan;
    }

    public String getWorkContent() {
        return workContent;
    }

    public void setWorkContent(String workContent) {
        this.workContent = workContent;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getCurrentApprovalNode() {
        return currentApprovalNode;
    }

    public void setCurrentApprovalNode(String currentApprovalNode) {
        this.currentApprovalNode = currentApprovalNode;
    }

    public Long getCurrentHandlerUserId() {
        return currentHandlerUserId;
    }

    public void setCurrentHandlerUserId(Long currentHandlerUserId) {
        this.currentHandlerUserId = currentHandlerUserId;
    }

    public String getCurrentHandlerUserName() {
        return currentHandlerUserName;
    }

    public void setCurrentHandlerUserName(String currentHandlerUserName) {
        this.currentHandlerUserName = currentHandlerUserName;
    }

    public Integer getCurrentFlowOrder() {
        return currentFlowOrder;
    }

    public void setCurrentFlowOrder(Integer currentFlowOrder) {
        this.currentFlowOrder = currentFlowOrder;
    }

    public Long getFinalApproverUserId() {
        return finalApproverUserId;
    }

    public void setFinalApproverUserId(Long finalApproverUserId) {
        this.finalApproverUserId = finalApproverUserId;
    }

    public String getLastReturnTarget() {
        return lastReturnTarget;
    }

    public void setLastReturnTarget(String lastReturnTarget) {
        this.lastReturnTarget = lastReturnTarget;
    }

    public String getLastReturnComment() {
        return lastReturnComment;
    }

    public void setLastReturnComment(String lastReturnComment) {
        this.lastReturnComment = lastReturnComment;
    }

    public Long getLastReviewBy() {
        return lastReviewBy;
    }

    public void setLastReviewBy(Long lastReviewBy) {
        this.lastReviewBy = lastReviewBy;
    }

    public String getLastReviewByName() {
        return lastReviewByName;
    }

    public void setLastReviewByName(String lastReviewByName) {
        this.lastReviewByName = lastReviewByName;
    }

    public LocalDateTime getLastReviewTime() {
        return lastReviewTime;
    }

    public void setLastReviewTime(LocalDateTime lastReviewTime) {
        this.lastReviewTime = lastReviewTime;
    }

    public LocalDateTime getSubmitTime() {
        return submitTime;
    }

    public void setSubmitTime(LocalDateTime submitTime) {
        this.submitTime = submitTime;
    }

    public LocalDateTime getApprovedTime() {
        return approvedTime;
    }

    public void setApprovedTime(LocalDateTime approvedTime) {
        this.approvedTime = approvedTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<WeeklyWorkFlowNodeVO> getFlowNodes() {
        return flowNodes;
    }

    public void setFlowNodes(List<WeeklyWorkFlowNodeVO> flowNodes) {
        this.flowNodes = flowNodes;
    }

    public List<String> getAvailableReturnTargets() {
        return availableReturnTargets;
    }

    public void setAvailableReturnTargets(List<String> availableReturnTargets) {
        this.availableReturnTargets = availableReturnTargets;
    }

    public List<WeeklyWorkApprovalLogVO> getApprovalLogs() {
        return approvalLogs;
    }

    public void setApprovalLogs(List<WeeklyWorkApprovalLogVO> approvalLogs) {
        this.approvalLogs = approvalLogs;
    }
}

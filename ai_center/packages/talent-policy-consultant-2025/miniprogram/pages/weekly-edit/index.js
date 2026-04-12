const api = require('../../services/api');
const { ensureAuthenticated } = require('../../utils/auth');
const { getCurrentWeekNo, formatDateTime, resolveWeeklyStatusText, resolveWeeklyNodeText } = require('../../utils/format');

function isEditableStatus(status) {
  return !status || status === 'DRAFT' || status === 'RETURNED';
}

Page({
  data: {
    loading: false,
    saving: false,
    submitting: false,
    id: null,
    weekNo: '',
    workPlan: '',
    workContent: '',
    remark: '',
    status: '',
    statusText: '',
    editable: true,
    currentApprovalNodeText: '-',
    flowNodes: [],
    approvalLogs: []
  },

  onLoad(options) {
    const weekNo = options && options.weekNo ? decodeURIComponent(options.weekNo) : getCurrentWeekNo(new Date());
    this.setData({
      weekNo
    });
    if (options && options.id) {
      this.loadDetail(options.id);
    }
  },

  onShow() {
    ensureAuthenticated();
  },

  async loadDetail(id) {
    this.setData({ loading: true });
    try {
      const detail = await api.queryWeeklyWorkDetail(id);
      this.applyDetail(detail);
    } catch (error) {
      wx.showModal({
        title: '加载失败',
        content: error.message || '周报详情加载失败',
        showCancel: false
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  applyDetail(detail) {
    const status = detail && detail.status;
    this.setData({
      id: detail && detail.id,
      weekNo: (detail && detail.weekNo) || this.data.weekNo,
      workPlan: (detail && detail.workPlan) || '',
      workContent: (detail && detail.workContent) || '',
      remark: (detail && detail.remark) || '',
      status,
      statusText: resolveWeeklyStatusText(status),
      editable: isEditableStatus(status),
      currentApprovalNodeText: resolveWeeklyNodeText(detail && detail.currentApprovalNode),
      flowNodes: Array.isArray(detail && detail.flowNodes) ? detail.flowNodes : [],
      approvalLogs: Array.isArray(detail && detail.approvalLogs) ? detail.approvalLogs.map((item) => ({
        reviewerName: item.reviewerName,
        action: item.action,
        fromNode: resolveWeeklyNodeText(item.fromNode),
        toNode: resolveWeeklyNodeText(item.toNode),
        comment: item.comment || '',
        createTimeText: formatDateTime(item.createTime)
      })) : []
    });
  },

  handleWeekNoInput(event) {
    this.setData({ weekNo: event.detail.value });
  },

  handleWorkPlanInput(event) {
    this.setData({ workPlan: event.detail.value });
  },

  handleWorkContentInput(event) {
    this.setData({ workContent: event.detail.value });
  },

  handleRemarkInput(event) {
    this.setData({ remark: event.detail.value });
  },

  validatePayload() {
    if (!this.data.weekNo.trim()) {
      throw new Error('周次不能为空');
    }
  },

  async handleSaveDraft() {
    if (this.data.saving || !this.data.editable) {
      return;
    }
    this.setData({ saving: true });
    try {
      this.validatePayload();
      const id = await api.saveWeeklyWorkDraft({
        weekNo: this.data.weekNo.trim(),
        workPlan: this.data.workPlan,
        workContent: this.data.workContent,
        remark: this.data.remark
      });
      wx.showToast({
        title: '草稿已保存',
        icon: 'success'
      });
      this.setData({ id });
      await this.loadDetail(id);
    } catch (error) {
      wx.showModal({
        title: '保存失败',
        content: error.message || '保存草稿失败',
        showCancel: false
      });
    } finally {
      this.setData({ saving: false });
    }
  },

  async handleSubmit() {
    if (this.data.submitting || !this.data.editable) {
      return;
    }
    this.setData({ submitting: true });
    try {
      this.validatePayload();
      let id = this.data.id;
      if (!id) {
        id = await api.saveWeeklyWorkDraft({
          weekNo: this.data.weekNo.trim(),
          workPlan: this.data.workPlan,
          workContent: this.data.workContent,
          remark: this.data.remark
        });
      }
      await api.submitWeeklyWork({ id });
      wx.showToast({
        title: '已提交审核',
        icon: 'success'
      });
      this.setData({ id });
      await this.loadDetail(id);
    } catch (error) {
      wx.showModal({
        title: '提交失败',
        content: error.message || '周报提交失败',
        showCancel: false
      });
    } finally {
      this.setData({ submitting: false });
    }
  }
});

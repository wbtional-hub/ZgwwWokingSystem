const api = require('../../services/api');
const { ensureAuthenticated, getUserInfo } = require('../../utils/auth');
const { getCurrentWeekNo, formatDateTime, truncate, resolveWeeklyStatusText } = require('../../utils/format');

Page({
  data: {
    loading: false,
    currentWeekNo: '',
    list: []
  },

  onLoad() {
    this.setData({
      currentWeekNo: getCurrentWeekNo(new Date())
    });
  },

  onShow() {
    if (!ensureAuthenticated()) {
      return;
    }
    this.fetchList();
  },

  async onPullDownRefresh() {
    if (!ensureAuthenticated()) {
      wx.stopPullDownRefresh();
      return;
    }
    await this.fetchList();
    wx.stopPullDownRefresh();
  },

  async fetchList() {
    this.setData({ loading: true });
    try {
      const currentUser = getUserInfo();
      const records = await api.queryWeeklyWorkList({
        userId: currentUser && currentUser.userId
      });
      const normalizedList = (Array.isArray(records) ? records : []).map((item) => ({
        id: item.id,
        weekNo: item.weekNo,
        status: item.status,
        statusText: resolveWeeklyStatusText(item.status),
        workPlanPreview: truncate(item.workPlan, 60),
        workContentPreview: truncate(item.workContent, 80),
        submitTimeText: formatDateTime(item.submitTime),
        createTimeText: formatDateTime(item.createTime)
      }));
      this.setData({ list: normalizedList });
    } catch (error) {
      wx.showModal({
        title: '加载失败',
        content: error.message || '周报列表加载失败',
        showCancel: false
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  handleCreate() {
    wx.navigateTo({
      url: `/pages/weekly-edit/index?weekNo=${encodeURIComponent(this.data.currentWeekNo)}`
    });
  },

  handleOpen(event) {
    const id = event.currentTarget.dataset.id;
    if (!id) {
      return;
    }
    wx.navigateTo({
      url: `/pages/weekly-edit/index?id=${id}`
    });
  }
});

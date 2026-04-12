const api = require('../../services/api');
const env = require('../../config/env');
const { ensureAuthenticated, loadSession, saveSession, clearSession } = require('../../utils/auth');

Page({
  data: {
    loading: false,
    session: null,
    apiBaseUrl: env.apiBaseUrl
  },

  onShow() {
    if (!ensureAuthenticated()) {
      return;
    }
    this.loadProfile();
  },

  async onPullDownRefresh() {
    if (!ensureAuthenticated()) {
      wx.stopPullDownRefresh();
      return;
    }
    await this.loadProfile();
    wx.stopPullDownRefresh();
  },

  async loadProfile() {
    this.setData({ loading: true });
    try {
      const currentSession = loadSession();
      const [userInfo, modulePermissionData] = await Promise.all([
        api.queryCurrentUser(),
        api.queryCurrentUserModulePermissions()
      ]);
      const session = saveSession({
        token: currentSession.token,
        userInfo,
        moduleCodes: Array.isArray(modulePermissionData && modulePermissionData.moduleCodes)
          ? modulePermissionData.moduleCodes
          : []
      });
      getApp().globalData.session = session;
      this.setData({
        session
      });
    } catch (error) {
      wx.showModal({
        title: '加载失败',
        content: error.message || '个人中心加载失败',
        showCancel: false
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  handleOpenWorkspace() {
    wx.reLaunch({
      url: '/pages/workspace/index'
    });
  },

  handleLogout() {
    wx.showModal({
      title: '退出登录',
      content: '确认清空当前小程序登录态并返回登录页吗？',
      success: (res) => {
        if (!res.confirm) {
          return;
        }
        clearSession();
        getApp().globalData.session = null;
        wx.reLaunch({
          url: '/pages/login/index'
        });
      }
    });
  }
});

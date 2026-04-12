const api = require('../../services/api');
const { ensureAuthenticated, saveSession, loadSession } = require('../../utils/auth');
const { buildAccessContext, resolveWorkspaceItems } = require('../../config/modules');

Page({
  data: {
    loading: false,
    userInfo: null,
    accessContext: {
      isAdmin: false,
      moduleCodes: []
    },
    items: []
  },

  onShow() {
    if (!ensureAuthenticated()) {
      return;
    }
    this.loadWorkspace();
  },

  async onPullDownRefresh() {
    if (!ensureAuthenticated()) {
      wx.stopPullDownRefresh();
      return;
    }
    await this.loadWorkspace();
    wx.stopPullDownRefresh();
  },

  async loadWorkspace() {
    this.setData({ loading: true });
    try {
      const [userInfo, modulePermissionData] = await Promise.all([
        api.queryCurrentUser(),
        api.queryCurrentUserModulePermissions()
      ]);
      const session = saveSession({
        token: loadSession().token,
        userInfo,
        moduleCodes: Array.isArray(modulePermissionData && modulePermissionData.moduleCodes)
          ? modulePermissionData.moduleCodes
          : []
      });
      getApp().globalData.session = session;
      const accessContext = buildAccessContext(session.userInfo, session.moduleCodes);
      this.setData({
        userInfo: session.userInfo,
        accessContext,
        items: resolveWorkspaceItems(accessContext)
      });
    } catch (error) {
      wx.showModal({
        title: '加载失败',
        content: error.message || '移动工作台加载失败',
        showCancel: false
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  handleOpen(event) {
    const path = event.currentTarget.dataset.path;
    if (!path) {
      return;
    }
    wx.navigateTo({ url: path });
  }
});

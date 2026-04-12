const api = require('../../services/api');
const { saveSession, buildRouteWithQuery, getToken } = require('../../utils/auth');

Page({
  data: {
    loading: false,
    redirect: '/pages/workspace/index'
  },

  onLoad(options) {
    this.setData({
      redirect: (options && options.redirect) || '/pages/workspace/index'
    });
  },

  onShow() {
    if (getToken()) {
      wx.reLaunch({
        url: buildRouteWithQuery(this.data.redirect || '/pages/workspace/index')
      });
    }
  },

  async handleWechatLogin() {
    if (this.data.loading) {
      return;
    }
    this.setData({ loading: true });
    try {
      const loginResult = await new Promise((resolve, reject) => {
        wx.login({
          success: resolve,
          fail: reject
        });
      });
      if (!loginResult || !loginResult.code) {
        throw new Error('微信登录失败，未获取到授权 code');
      }
      const loginInfo = await api.wechatMiniLogin({
        code: loginResult.code
      });
      const [userInfo, modulePermissionData] = await Promise.all([
        api.queryCurrentUser(),
        api.queryCurrentUserModulePermissions()
      ]);
      const session = saveSession({
        token: loginInfo.token,
        userInfo,
        moduleCodes: Array.isArray(modulePermissionData && modulePermissionData.moduleCodes)
          ? modulePermissionData.moduleCodes
          : []
      });
      getApp().globalData.session = session;
      wx.showToast({
        title: '登录成功',
        icon: 'success'
      });
      setTimeout(() => {
        wx.reLaunch({
          url: buildRouteWithQuery(this.data.redirect || '/pages/workspace/index')
        });
      }, 200);
    } catch (error) {
      wx.showModal({
        title: '登录失败',
        content: error.message || '微信授权登录失败，请确认账号已绑定并检查后端配置。',
        showCancel: false
      });
    } finally {
      this.setData({ loading: false });
    }
  }
});

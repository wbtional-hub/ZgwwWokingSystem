const api = require('../../services/api');
const { ensureAuthenticated, getUserInfo } = require('../../utils/auth');
const { formatDate, formatDateTime, resolveAttendanceStatusText } = require('../../utils/format');

function buildLocationPayload(location) {
  return {
    address: `微信定位(${location.latitude.toFixed(6)}, ${location.longitude.toFixed(6)})`,
    latitude: location.latitude,
    longitude: location.longitude,
    accuracyMeters: Math.round(location.accuracy || 0),
    locationSource: 'WECHAT_MINIPROGRAM',
    locationProvider: 'wx.getLocation'
  };
}

Page({
  data: {
    loading: false,
    checkingIn: false,
    locationInfo: null,
    currentPositionText: '尚未获取当前位置',
    lastResult: null,
    records: []
  },

  onShow() {
    if (!ensureAuthenticated()) {
      return;
    }
    this.loadPage();
  },

  async onPullDownRefresh() {
    if (!ensureAuthenticated()) {
      wx.stopPullDownRefresh();
      return;
    }
    await this.loadPage();
    wx.stopPullDownRefresh();
  },

  async loadPage() {
    this.setData({ loading: true });
    try {
      const currentUser = getUserInfo();
      const today = formatDate(new Date());
      const [locationInfo, attendancePage] = await Promise.all([
        api.queryCurrentAttendanceLocation(),
        api.queryAttendanceList({
          userId: currentUser && currentUser.userId,
          dateFrom: today,
          dateTo: today,
          pageNo: 1,
          pageSize: 6
        })
      ]);
      this.setData({
        locationInfo,
        records: Array.isArray(attendancePage && attendancePage.list) ? attendancePage.list : []
      });
    } catch (error) {
      wx.showModal({
        title: '加载失败',
        content: error.message || '考勤信息加载失败',
        showCancel: false
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  async handleCheckIn() {
    if (this.data.checkingIn) {
      return;
    }
    this.setData({ checkingIn: true });
    try {
      const location = await new Promise((resolve, reject) => {
        wx.getLocation({
          type: 'gcj02',
          isHighAccuracy: true,
          highAccuracyExpireTime: 5000,
          success: resolve,
          fail: reject
        });
      });
      this.setData({
        currentPositionText: `纬度 ${location.latitude.toFixed(6)} / 经度 ${location.longitude.toFixed(6)} / 精度约 ${Math.round(location.accuracy || 0)} 米`
      });
      const result = await api.checkIn(buildLocationPayload(location));
      this.setData({
        lastResult: Object.assign({}, result, {
          statusText: resolveAttendanceStatusText(result && result.status),
          actionText: result && result.action === 'CHECK_OUT' ? '签退完成' : '签到完成',
          checkInTimeText: formatDateTime(result && result.checkInTime),
          checkOutTimeText: formatDateTime(result && result.checkOutTime)
        })
      });
      wx.showToast({
        title: result && result.success ? '打卡成功' : '结果已返回',
        icon: result && result.success ? 'success' : 'none'
      });
      await this.loadPage();
    } catch (error) {
      const message = error.message || '';
      if (message.includes('auth deny') || message.includes('auth denied')) {
        wx.showModal({
          title: '需要定位权限',
          content: '请先允许小程序获取定位，然后再尝试签到。',
          showCancel: true,
          confirmText: '去设置',
          success: (res) => {
            if (res.confirm) {
              wx.openSetting();
            }
          }
        });
      } else {
        wx.showModal({
          title: '打卡失败',
          content: message || '签到失败',
          showCancel: false
        });
      }
    } finally {
      this.setData({ checkingIn: false });
    }
  }
});

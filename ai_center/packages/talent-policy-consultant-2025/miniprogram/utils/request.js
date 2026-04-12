const env = require('../config/env');
const { getToken, clearSession, resolveCurrentRoute, redirectToLogin } = require('./auth');

let authRedirecting = false;

function normalizeUrl(url) {
  if (!url) {
    return env.apiBaseUrl;
  }
  if (/^https?:\/\//.test(url)) {
    return url;
  }
  const baseUrl = (env.apiBaseUrl || '').replace(/\/+$/, '');
  const path = String(url).startsWith('/') ? url : `/${url}`;
  return `${baseUrl}${path}`;
}

function notify(message) {
  wx.showToast({
    title: message || '请求失败',
    icon: 'none',
    duration: 2200
  });
}

function handleAuthExpired() {
  if (authRedirecting) {
    return;
  }
  authRedirecting = true;
  clearSession();
  notify('登录状态已失效，请重新登录');
  setTimeout(() => {
    redirectToLogin(resolveCurrentRoute());
    authRedirecting = false;
  }, 250);
}

function request(options) {
  const settings = options || {};
  const headers = Object.assign({
    'content-type': 'application/json'
  }, settings.header);
  const token = getToken();
  if (settings.auth !== false && token) {
    headers.Authorization = `Bearer ${token}`;
  }
  return new Promise((resolve, reject) => {
    wx.request({
      url: normalizeUrl(settings.url),
      method: settings.method || 'GET',
      timeout: settings.timeout || env.requestTimeout,
      data: settings.data,
      header: headers,
      success(res) {
        const payload = res.data || {};
        if ((res.statusCode === 401 || res.statusCode === 403) && settings.auth !== false) {
          handleAuthExpired();
          reject(new Error(payload.message || '登录已失效'));
          return;
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          const message = payload.message || `请求失败 (${res.statusCode})`;
          if (settings.showError !== false) {
            notify(message);
          }
          reject(new Error(message));
          return;
        }
        if (payload.code !== 0) {
          const message = payload.message || '请求失败';
          if (settings.showError !== false) {
            notify(message);
          }
          reject(new Error(message));
          return;
        }
        resolve(payload.data);
      },
      fail(error) {
        const message = error && error.errMsg ? error.errMsg : '网络请求失败';
        if (settings.showError !== false) {
          notify(message);
        }
        reject(new Error(message));
      }
    });
  });
}

module.exports = {
  request
};

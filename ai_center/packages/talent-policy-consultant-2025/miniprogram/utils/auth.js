const TOKEN_KEY = 'mini_token';
const USER_INFO_KEY = 'mini_user_info';
const MODULE_CODES_KEY = 'mini_module_codes';

function safeParse(value, fallback) {
  if (!value) {
    return fallback;
  }
  try {
    return JSON.parse(value);
  } catch (error) {
    return fallback;
  }
}

function normalizeUserInfo(userInfo, moduleCodes) {
  const role = userInfo && (userInfo.role || (userInfo.superAdmin ? 'ADMIN' : 'USER'));
  return {
    userId: userInfo && userInfo.userId,
    username: userInfo && userInfo.username,
    realName: userInfo && userInfo.realName,
    role,
    superAdmin: Boolean(userInfo && userInfo.superAdmin),
    isAdmin: role === 'ADMIN',
    moduleCodes: Array.isArray(moduleCodes) ? moduleCodes.slice() : []
  };
}

function loadSession() {
  const token = wx.getStorageSync(TOKEN_KEY) || '';
  const userInfo = safeParse(wx.getStorageSync(USER_INFO_KEY), null);
  const moduleCodes = safeParse(wx.getStorageSync(MODULE_CODES_KEY), []);
  return {
    token,
    userInfo: userInfo ? normalizeUserInfo(userInfo, moduleCodes) : null,
    moduleCodes: Array.isArray(moduleCodes) ? moduleCodes : []
  };
}

function saveSession(session) {
  const token = session && session.token ? session.token : '';
  const moduleCodes = Array.isArray(session && session.moduleCodes) ? session.moduleCodes : [];
  const userInfo = normalizeUserInfo(session && session.userInfo, moduleCodes);
  wx.setStorageSync(TOKEN_KEY, token);
  wx.setStorageSync(USER_INFO_KEY, JSON.stringify(userInfo));
  wx.setStorageSync(MODULE_CODES_KEY, JSON.stringify(moduleCodes));
  return {
    token,
    userInfo,
    moduleCodes
  };
}

function clearSession() {
  wx.removeStorageSync(TOKEN_KEY);
  wx.removeStorageSync(USER_INFO_KEY);
  wx.removeStorageSync(MODULE_CODES_KEY);
}

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || '';
}

function getUserInfo() {
  return loadSession().userInfo;
}

function buildRouteWithQuery(route, options) {
  if (!options || !Object.keys(options).length) {
    return route;
  }
  const query = Object.keys(options)
    .filter((key) => options[key] !== undefined && options[key] !== null && options[key] !== '')
    .map((key) => `${encodeURIComponent(key)}=${encodeURIComponent(options[key])}`)
    .join('&');
  return query ? `${route}?${query}` : route;
}

function resolveCurrentRoute() {
  const pages = getCurrentPages();
  if (!pages.length) {
    return '/pages/workspace/index';
  }
  const currentPage = pages[pages.length - 1];
  const route = `/${currentPage.route}`;
  return buildRouteWithQuery(route, currentPage.options);
}

function redirectToLogin(redirect) {
  wx.reLaunch({
    url: buildRouteWithQuery('/pages/login/index', {
      redirect: redirect || '/pages/workspace/index'
    })
  });
}

function ensureAuthenticated() {
  if (getToken()) {
    return true;
  }
  redirectToLogin(resolveCurrentRoute());
  return false;
}

module.exports = {
  loadSession,
  saveSession,
  clearSession,
  getToken,
  getUserInfo,
  normalizeUserInfo,
  buildRouteWithQuery,
  resolveCurrentRoute,
  redirectToLogin,
  ensureAuthenticated
};

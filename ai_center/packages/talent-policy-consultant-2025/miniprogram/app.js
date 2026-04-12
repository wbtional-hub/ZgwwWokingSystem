const { loadSession } = require('./utils/auth');
const env = require('./config/env');

App({
  globalData: {
    apiBaseUrl: env.apiBaseUrl,
    session: loadSession()
  },
  syncSession() {
    this.globalData.session = loadSession();
    return this.globalData.session;
  }
});

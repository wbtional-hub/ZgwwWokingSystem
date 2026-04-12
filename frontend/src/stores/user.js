import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null'),
    accessReady: localStorage.getItem('userAccessReady') === '1',
    forcePasswordChange: localStorage.getItem('forcePasswordChange') === '1'
  }),
  actions: {
    setLogin(loginInfo) {
      this.token = loginInfo.token
      this.forcePasswordChange = Boolean(loginInfo.forcePasswordChange)
      this.userInfo = {
        userId: loginInfo.userId,
        username: loginInfo.username,
        realName: loginInfo.realName,
        role: loginInfo.role || (loginInfo.superAdmin ? 'ADMIN' : 'USER'),
        superAdmin: Boolean(loginInfo.superAdmin),
        isAdmin: (loginInfo.role || (loginInfo.superAdmin ? 'ADMIN' : 'USER')) === 'ADMIN',
        moduleCodes: Array.isArray(loginInfo.moduleCodes) ? [...loginInfo.moduleCodes] : [],
        forcePasswordChange: this.forcePasswordChange
      }
      this.accessReady = Array.isArray(loginInfo.moduleCodes)
      localStorage.setItem('token', loginInfo.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      localStorage.setItem('userAccessReady', this.accessReady ? '1' : '0')
      localStorage.setItem('forcePasswordChange', this.forcePasswordChange ? '1' : '0')
    },
    setAccessContext({ userInfo, moduleCodes }) {
      const currentUserInfo = this.userInfo || {}
      const forcePasswordChange = Boolean(userInfo?.forcePasswordChange ?? currentUserInfo.forcePasswordChange ?? this.forcePasswordChange)
      this.forcePasswordChange = forcePasswordChange
      this.userInfo = {
        ...currentUserInfo,
        ...(userInfo || {}),
        role: (userInfo?.role || currentUserInfo.role || (userInfo?.superAdmin || currentUserInfo.superAdmin ? 'ADMIN' : 'USER')),
        superAdmin: Boolean(userInfo?.superAdmin ?? currentUserInfo.superAdmin),
        isAdmin: (userInfo?.role || currentUserInfo.role || (userInfo?.superAdmin || currentUserInfo.superAdmin ? 'ADMIN' : 'USER')) === 'ADMIN',
        moduleCodes: Array.isArray(moduleCodes) ? [...moduleCodes] : Array.isArray(currentUserInfo.moduleCodes) ? [...currentUserInfo.moduleCodes] : [],
        forcePasswordChange
      }
      this.accessReady = true
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
      localStorage.setItem('userAccessReady', '1')
      localStorage.setItem('forcePasswordChange', forcePasswordChange ? '1' : '0')
    },
    clearLogin() {
      this.token = ''
      this.userInfo = null
      this.accessReady = false
      this.forcePasswordChange = false
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      localStorage.removeItem('userAccessReady')
      localStorage.removeItem('forcePasswordChange')
    }
  }
})

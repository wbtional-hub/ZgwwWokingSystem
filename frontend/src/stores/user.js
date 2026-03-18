import { defineStore } from 'pinia'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    userInfo: JSON.parse(localStorage.getItem('userInfo') || 'null')
  }),
  actions: {
    setLogin(loginInfo) {
      this.token = loginInfo.token
      this.userInfo = {
        userId: loginInfo.userId,
        username: loginInfo.username,
        realName: loginInfo.realName,
        role: loginInfo.role || (loginInfo.superAdmin ? 'ADMIN' : 'USER'),
        superAdmin: Boolean(loginInfo.superAdmin),
        isAdmin: (loginInfo.role || (loginInfo.superAdmin ? 'ADMIN' : 'USER')) === 'ADMIN'
      }
      localStorage.setItem('token', loginInfo.token)
      localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
    },
    clearLogin() {
      this.token = ''
      this.userInfo = null
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
    }
  }
})

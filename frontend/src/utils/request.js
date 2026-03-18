import axios from 'axios'
import { showToast } from 'vant'
import router from '@/router'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
let authRedirecting = false

const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000
})

request.interceptors.request.use(config => {
  const isLoginRequest = typeof config.url === 'string' && config.url.includes('/auth/login')
  const token = localStorage.getItem('token')
  if (token && !isLoginRequest) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => response.data,
  error => {
    const status = error.response?.status
    const backendCode = error.response?.data?.code
    const backendMessage = error.response?.data?.message
    const isLoginRequest = typeof error.config?.url === 'string' && error.config.url.includes('/auth/login')
    if ((status === 401 || status === 403) && !isLoginRequest && !authRedirecting) {
      authRedirecting = true
      localStorage.removeItem('token')
      localStorage.removeItem('userInfo')
      showToast('登录状态已失效，请重新登录')
      const redirect = router.currentRoute.value.fullPath || '/'
      router.replace({
        path: '/login',
        query: redirect && redirect !== '/login' ? { redirect } : undefined
      }).finally(() => {
        authRedirecting = false
      })
      return Promise.reject(error)
    }
    console.error('[request error]', {
      url: error.config?.url,
      method: error.config?.method,
      status,
      code: backendCode,
      message: backendMessage,
      data: error.response?.data
    })
    const message = backendMessage || '请求失败，请稍后重试'
    showToast(message)
    return Promise.reject(error)
  }
)

export default request

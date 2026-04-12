import axios from 'axios'
import { showToast } from 'vant'
import router from '@/router'
import { LOG_TYPES, reportLog, resolveModuleFromUrl, resolveSubModuleFromUrl } from '@/utils/log-center'
import { TRACE_ID_HEADER, createTraceId } from '@/utils/trace'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
// FIX: wechat login recovery protection
const WECHAT_LOGIN_RECOVERING_KEY = 'wechat_login_recovering'
let authRedirecting = false

const request = axios.create({
  baseURL: apiBaseUrl,
  timeout: 15000
})

function isLogCenterReportRequest(config) {
  return typeof config?.url === 'string' && config.url.includes('/log-center/report')
}

function normalizeText(value) {
  if (value == null) {
    return ''
  }
  return String(value).trim()
}

function buildRequestUrl(config) {
  const base = normalizeText(config?.baseURL)
  const url = normalizeText(config?.url)
  if (!url) {
    return ''
  }
  if (/^https?:\/\//i.test(url)) {
    return url
  }
  return `${base}${url}`
}

function parseRequestBody(config) {
  if (config?.data == null || config.data === '') {
    return null
  }
  if (typeof config.data === 'string') {
    try {
      return JSON.parse(config.data)
    } catch (error) {
      return config.data
    }
  }
  return config.data
}

function parseResponseData(data) {
  if (typeof data === 'string') {
    try {
      return JSON.parse(data)
    } catch (error) {
      return data
    }
  }
  return data
}

function buildApiDiagnosis({ responseStatus, hasResponse, code, traceId }) {
  if (!hasResponse) {
    return '接口请求未收到后端响应，请优先检查网络、跨域、超时或网关连通性。'
  }
  if (responseStatus >= 500) {
    return `后端接口返回 ${responseStatus}，通常表示服务端异常；可使用 traceId=${traceId || '-'} 继续排查。`
  }
  if (responseStatus === 404) {
    return '接口返回 404，请检查请求地址、路由映射或网关转发配置。'
  }
  if (responseStatus === 401 || responseStatus === 403) {
    return '接口鉴权失败，请检查登录态、权限或 token 是否失效。'
  }
  if (code && Number(code) !== 0) {
    return `接口已返回业务失败，请结合后端 message 和 traceId=${traceId || '-'} 排查业务校验。`
  }
  return '接口请求失败，请结合请求参数、响应状态和 traceId 排查。'
}

// FIX: wechat login recovery protection
function hasWechatLoginHash() {
  if (typeof window === 'undefined') {
    return false
  }
  const hash = window.location?.hash || ''
  return hash.includes('wechatLogin=')
}

// FIX: wechat login recovery protection
function isWechatLoginRecovering() {
  if (hasWechatLoginHash()) {
    return true
  }
  if (typeof window === 'undefined') {
    return false
  }
  try {
    return window.sessionStorage.getItem(WECHAT_LOGIN_RECOVERING_KEY) === '1'
  } catch (error) {
    return false
  }
}

request.interceptors.request.use(config => {
  config.headers = config.headers || {}
  const traceId = config.headers?.[TRACE_ID_HEADER] || createTraceId()
  const isLoginRequest = typeof config.url === 'string' && (
    config.url.includes('/auth/login')
      || config.url.includes('/auth/mobile-login-options')
      || config.url.includes('/auth/wechat-mini-login')
      || config.url.includes('/auth/wechat-mp-login')
      || config.url.includes('/auth/wechat-mp-authorize-url')
  )
  const token = localStorage.getItem('token')
  if (token && !isLoginRequest) {
    config.headers.Authorization = `Bearer ${token}`
  }
  config.headers[TRACE_ID_HEADER] = traceId
  config.headers['X-Page-Url'] = typeof window === 'undefined' ? '' : window.location.href
  config.metadata = {
    ...(config.metadata || {}),
    traceId
  }
  return config
})

request.interceptors.response.use(
  response => {
    const traceId = response.headers?.[TRACE_ID_HEADER.toLowerCase()] || response.config?.metadata?.traceId || ''
    if (response.data && typeof response.data === 'object' && traceId && !response.data.traceId) {
      response.data.traceId = traceId
    }
    const businessCode = response.data?.code
    if (
      Number(businessCode) !== 0
      && !response.config?.skipBusinessErrorReport
      && !isLogCenterReportRequest(response.config)
    ) {
      reportLog(LOG_TYPES.FRONTEND_API_ERROR, {
        traceId,
        module: resolveModuleFromUrl(buildRequestUrl(response.config)),
        subModule: resolveSubModuleFromUrl(buildRequestUrl(response.config)),
        title: '前端接口业务失败',
        summary: normalizeText(response.data?.message) || '接口返回业务失败',
        diagnosis: buildApiDiagnosis({
          responseStatus: response.status,
          hasResponse: true,
          code: businessCode,
          traceId
        }),
        errorCode: normalizeText(businessCode) || 'API_BUSINESS_FAIL',
        message: normalizeText(response.data?.message),
        requestUrl: buildRequestUrl(response.config),
        requestMethod: normalizeText(response.config?.method).toUpperCase(),
        requestParams: response.config?.params || parseRequestBody(response.config),
        responseStatus: response.status,
        rawData: {
          responseData: parseResponseData(response.data),
          responseHeaders: response.headers || {}
        }
      })
    }
    return response.data
  },
  error => {
    const status = error.response?.status
    const backendCode = error.response?.data?.code
    const backendMessage = error.response?.data?.message
    const responseTraceId = error.response?.headers?.[TRACE_ID_HEADER.toLowerCase()] || error.config?.metadata?.traceId || ''
    const isLoginRequest = typeof error.config?.url === 'string' && (
      error.config.url.includes('/auth/login')
        || error.config.url.includes('/auth/mobile-login-options')
        || error.config.url.includes('/auth/wechat-mini-login')
        || error.config.url.includes('/auth/wechat-mp-login')
        || error.config.url.includes('/auth/wechat-mp-authorize-url')
    )
    if ((status === 401 || status === 403) && !isLoginRequest && !authRedirecting) {
      // FIX: wechat login recovery protection
      if (isWechatLoginRecovering()) {
        return Promise.reject(error)
      }
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
    if (!error.config?.skipErrorReport && !isLogCenterReportRequest(error.config)) {
      reportLog(LOG_TYPES.FRONTEND_API_ERROR, {
        traceId: responseTraceId,
        module: resolveModuleFromUrl(buildRequestUrl(error.config)),
        subModule: resolveSubModuleFromUrl(buildRequestUrl(error.config)),
        title: '前端接口请求异常',
        summary: normalizeText(backendMessage) || normalizeText(error.message) || '接口请求异常',
        diagnosis: buildApiDiagnosis({
          responseStatus: status,
          hasResponse: Boolean(error.response),
          code: backendCode,
          traceId: responseTraceId
        }),
        errorCode: normalizeText(backendCode) || (error.code === 'ECONNABORTED' ? 'API_TIMEOUT' : 'API_REQUEST_ERROR'),
        message: normalizeText(backendMessage) || normalizeText(error.message),
        requestUrl: buildRequestUrl(error.config),
        requestMethod: normalizeText(error.config?.method).toUpperCase(),
        requestParams: error.config?.params || parseRequestBody(error.config),
        responseStatus: status ?? null,
        rawData: {
          responseData: parseResponseData(error.response?.data),
          responseHeaders: error.response?.headers || {},
          axiosCode: error.code || '',
          stack: error.stack || ''
        }
      })
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
    if (!error.config?.skipErrorToast) {
      showToast(message)
    }
    return Promise.reject(error)
  }
)

export default request

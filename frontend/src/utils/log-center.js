import axios from 'axios'
import { TRACE_ID_HEADER, createTraceId, normalizeTraceId } from '@/utils/trace'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
const reporter = axios.create({
  baseURL: apiBaseUrl,
  timeout: 8000
})
const reportWindowMap = new Map()
const REPORT_WINDOW_MS = 5000
const REPORT_LIMIT = 4

export const LOG_TYPES = Object.freeze({
  FRONTEND_JS_ERROR: 'FRONTEND_JS_ERROR',
  FRONTEND_API_ERROR: 'FRONTEND_API_ERROR',
  FRONTEND_LOCATION_ERROR: 'FRONTEND_LOCATION_ERROR',
  WECHAT_JSAPI_ERROR: 'WECHAT_JSAPI_ERROR'
})

function isWechatBrowser() {
  if (typeof navigator === 'undefined') {
    return false
  }
  return /micromessenger/i.test(navigator.userAgent || '')
}

function normalizeText(value) {
  if (value == null) {
    return ''
  }
  return String(value).trim()
}

function resolveUserInfo() {
  if (typeof window === 'undefined') {
    return {}
  }
  try {
    const raw = window.localStorage.getItem('userInfo')
    return raw ? JSON.parse(raw) || {} : {}
  } catch (error) {
    return {}
  }
}

function inferDeviceType(userAgent = '') {
  const normalized = String(userAgent || '').toLowerCase()
  if (normalized.includes('micromessenger')) {
    return 'WECHAT'
  }
  if (normalized.includes('iphone') || normalized.includes('android') || normalized.includes('mobile') || normalized.includes('harmony')) {
    return 'MOBILE'
  }
  if (!normalized) {
    return 'UNKNOWN'
  }
  return 'PC'
}

function inferPlatform(userAgent = '') {
  const normalized = String(userAgent || '').toLowerCase()
  if (normalized.includes('windows')) {
    return 'WINDOWS'
  }
  if (normalized.includes('android')) {
    return 'ANDROID'
  }
  if (normalized.includes('iphone') || normalized.includes('ipad') || normalized.includes('ios')) {
    return 'IOS'
  }
  if (normalized.includes('mac os')) {
    return 'MAC'
  }
  if (normalized.includes('linux')) {
    return 'LINUX'
  }
  return 'UNKNOWN'
}

export function resolveModuleFromUrl(url = '') {
  const normalized = String(url || '').toLowerCase()
  if (normalized.includes('attendance')) {
    return 'ATTENDANCE'
  }
  if (normalized.includes('weekly-work')) {
    return 'WEEKLY_WORK'
  }
  if (normalized.includes('/auth') || normalized.includes('login')) {
    return 'AUTH'
  }
  if (normalized.includes('wechat')) {
    return 'WECHAT'
  }
  if (normalized.includes('operation-log')) {
    return 'OPERATION_LOG'
  }
  if (normalized.includes('log-center')) {
    return 'LOG_CENTER'
  }
  return 'FRONTEND'
}

export function resolveSubModuleFromUrl(url = '') {
  const normalized = String(url || '').toLowerCase()
  if (normalized.includes('check-in')) {
    return 'CHECK_IN'
  }
  if (normalized.includes('patch-apply')) {
    return 'PATCH_APPLY'
  }
  if (normalized.includes('/auth/login')) {
    return 'LOGIN'
  }
  if (normalized.includes('jsapi-config')) {
    return 'JSAPI_CONFIG'
  }
  if (normalized.includes('approve')) {
    return 'APPROVE'
  }
  if (normalized.includes('reject')) {
    return 'REJECT'
  }
  return ''
}

function buildCurrentPageContext() {
  if (typeof window === 'undefined') {
    return {
      pageUrl: '',
      referer: '',
      userAgent: '',
      env: 'BROWSER',
      clientIp: ''
    }
  }
  return {
    pageUrl: window.location?.href || '',
    referer: document?.referrer || '',
    userAgent: navigator?.userAgent || '',
    env: isWechatBrowser() ? 'WECHAT' : 'BROWSER',
    clientIp: ''
  }
}

function shouldSkipReport(payload) {
  const fingerprint = [
    normalizeText(payload.logType),
    normalizeText(payload.module),
    normalizeText(payload.subModule),
    normalizeText(payload.level),
    normalizeText(payload.errorCode),
    normalizeText(payload.title),
    normalizeText(payload.summary),
    normalizeText(payload.requestUrl),
    normalizeText(payload.pageUrl)
  ].join('|')
  const now = Date.now()
  const state = reportWindowMap.get(fingerprint)
  if (!state || now - state.windowStart > REPORT_WINDOW_MS) {
    reportWindowMap.set(fingerprint, { windowStart: now, count: 1 })
    return false
  }
  state.count += 1
  return state.count > REPORT_LIMIT
}

function cleanupWindowMap() {
  if (reportWindowMap.size < 256) {
    return
  }
  const expireBefore = Date.now() - REPORT_WINDOW_MS * 2
  for (const [key, value] of reportWindowMap.entries()) {
    if (value.windowStart < expireBefore) {
      reportWindowMap.delete(key)
    }
  }
}

function buildHeaders(traceId) {
  const headers = {
    [TRACE_ID_HEADER]: traceId
  }
  if (typeof window !== 'undefined') {
    const token = window.localStorage.getItem('token')
    if (token) {
      headers.Authorization = `Bearer ${token}`
    }
  }
  return headers
}

export function buildGlobalErrorDiagnosis(title) {
  return `${title}，请结合页面地址、浏览器环境、traceId 和原始堆栈定位具体触发点。`
}

export async function reportLog(logType, payload = {}) {
  cleanupWindowMap()
  const pageContext = buildCurrentPageContext()
  const userInfo = resolveUserInfo()
  const traceId = normalizeTraceId(payload.traceId) || createTraceId()
  const requestUrl = normalizeText(payload.requestUrl)
  const finalPayload = {
    traceId,
    logType,
    module: normalizeText(payload.module) || resolveModuleFromUrl(requestUrl || payload.pageUrl || pageContext.pageUrl),
    subModule: normalizeText(payload.subModule) || resolveSubModuleFromUrl(requestUrl || payload.pageUrl || pageContext.pageUrl),
    level: normalizeText(payload.level) || 'ERROR',
    title: normalizeText(payload.title) || '前端异常上报',
    summary: normalizeText(payload.summary) || normalizeText(payload.message) || '前端异常上报',
    diagnosis: normalizeText(payload.diagnosis) || buildGlobalErrorDiagnosis(normalizeText(payload.title) || '前端异常'),
    errorCode: normalizeText(payload.errorCode) || '',
    message: normalizeText(payload.message) || '',
    rawData: payload.rawData ?? null,
    requestUrl: requestUrl || '',
    requestMethod: normalizeText(payload.requestMethod) || '',
    requestParams: payload.requestParams ?? null,
    responseStatus: payload.responseStatus ?? null,
    userId: payload.userId ?? userInfo.userId ?? null,
    userName: normalizeText(payload.userName) || normalizeText(userInfo.realName) || normalizeText(userInfo.username) || '',
    orgId: payload.orgId ?? userInfo.unitId ?? null,
    orgName: normalizeText(payload.orgName) || normalizeText(userInfo.unitName) || '',
    clientIp: normalizeText(payload.clientIp) || pageContext.clientIp,
    userAgent: normalizeText(payload.userAgent) || pageContext.userAgent,
    deviceType: normalizeText(payload.deviceType) || inferDeviceType(normalizeText(payload.userAgent) || pageContext.userAgent),
    platform: normalizeText(payload.platform) || inferPlatform(normalizeText(payload.userAgent) || pageContext.userAgent),
    pageUrl: normalizeText(payload.pageUrl) || normalizeText(payload.currentUrl) || pageContext.pageUrl,
    referer: normalizeText(payload.referer) || pageContext.referer,
    env: normalizeText(payload.env) || pageContext.env
  }
  if (shouldSkipReport(finalPayload)) {
    return
  }
  try {
    await reporter.post('/log-center/report', finalPayload, {
      headers: buildHeaders(traceId)
    })
  } catch (error) {
    console.warn('[log center report failed]', error)
  }
}

export function setupGlobalErrorMonitoring(app) {
  if (typeof window === 'undefined' || window.__logCenterMonitoringInstalled) {
    return
  }
  window.__logCenterMonitoringInstalled = true

  window.addEventListener('error', (event) => {
    reportLog(LOG_TYPES.FRONTEND_JS_ERROR, {
      title: '前端 JS 异常',
      summary: normalizeText(event.message) || 'window.onerror 捕获到异常',
      diagnosis: buildGlobalErrorDiagnosis('前端 JS 异常'),
      errorCode: 'WINDOW_ONERROR',
      message: normalizeText(event.message),
      rawData: {
        source: event.filename || '',
        lineno: event.lineno ?? null,
        colno: event.colno ?? null,
        stack: event.error?.stack || ''
      }
    })
  })

  window.addEventListener('unhandledrejection', (event) => {
    const reason = event.reason
    reportLog(LOG_TYPES.FRONTEND_JS_ERROR, {
      title: '前端 Promise 未处理异常',
      summary: normalizeText(reason?.message) || normalizeText(reason) || 'unhandledrejection 捕获到异常',
      diagnosis: buildGlobalErrorDiagnosis('前端 Promise 未处理异常'),
      errorCode: 'UNHANDLED_REJECTION',
      message: normalizeText(reason?.message) || normalizeText(reason),
      rawData: {
        stack: reason?.stack || '',
        reason: reason ?? null
      }
    })
  })

  if (app?.config) {
    app.config.errorHandler = (error, instance, info) => {
      reportLog(LOG_TYPES.FRONTEND_JS_ERROR, {
        title: 'Vue 运行时异常',
        summary: normalizeText(error?.message) || 'Vue 组件运行失败',
        diagnosis: buildGlobalErrorDiagnosis('Vue 运行时异常'),
        errorCode: 'VUE_RUNTIME_ERROR',
        message: normalizeText(error?.message),
        rawData: {
          info,
          component: instance?.type?.name || '',
          stack: error?.stack || ''
        }
      })
      console.error(error)
    }
  }
}

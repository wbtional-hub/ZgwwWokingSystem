import { buildLocationEnvironmentDiagnostics } from '@/utils/location'

const DEFAULT_GOOD_THRESHOLD = 100
const DEFAULT_MAX_THRESHOLD = 1000

function normalizeNumber(value) {
  if (value == null || value === '') {
    return null
  }
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : null
}

function normalizeText(value) {
  if (value == null) {
    return ''
  }
  return String(value).trim()
}

function isPcPlatform(userAgent = '') {
  return /Windows|Macintosh|Linux x86_64|X11/i.test(userAgent || '')
}

function isServerLikeUserAgent(userAgent = '') {
  return /TencentCloud|HeadlessChrome|curl|Wget|PostmanRuntime|Linux x86_64/i.test(userAgent || '')
}

export function resolveLocationAccuracyPolicy(policy = {}) {
  const goodThreshold = normalizeNumber(policy.goodThreshold) ?? DEFAULT_GOOD_THRESHOLD
  const maxThreshold = normalizeNumber(policy.maxThreshold) ?? DEFAULT_MAX_THRESHOLD
  return {
    goodThreshold,
    maxThreshold: Math.max(maxThreshold, goodThreshold)
  }
}

export function classifyLocationAccuracy(accuracyMeters, policy = {}) {
  const accuracy = normalizeNumber(accuracyMeters)
  const normalizedPolicy = resolveLocationAccuracyPolicy(policy)
  if (accuracy == null) {
    return {
      category: 'UNKNOWN',
      accuracyMeters: null,
      goodThreshold: normalizedPolicy.goodThreshold,
      maxThreshold: normalizedPolicy.maxThreshold
    }
  }
  if (accuracy > normalizedPolicy.maxThreshold) {
    return {
      category: 'INVALID',
      accuracyMeters: accuracy,
      goodThreshold: normalizedPolicy.goodThreshold,
      maxThreshold: normalizedPolicy.maxThreshold
    }
  }
  if (accuracy > normalizedPolicy.goodThreshold) {
    return {
      category: 'WEAK',
      accuracyMeters: accuracy,
      goodThreshold: normalizedPolicy.goodThreshold,
      maxThreshold: normalizedPolicy.maxThreshold
    }
  }
  return {
    category: 'NORMAL',
    accuracyMeters: accuracy,
    goodThreshold: normalizedPolicy.goodThreshold,
    maxThreshold: normalizedPolicy.maxThreshold
  }
}

export function resolveLocationFailureMessage({ errorCode = '', accuracyMeters = null, policy = {} } = {}) {
  const code = normalizeText(errorCode).toUpperCase()
  const accuracyState = classifyLocationAccuracy(accuracyMeters, policy)
  if (code.includes('TIMEOUT')) {
    return '定位超时，请确认网络与定位开关后重试'
  }
  if (code.includes('PERMISSION_DENIED') || code.includes('CANCEL')) {
    return '定位失败，请先允许定位权限后重试'
  }
  if (accuracyState.category === 'INVALID') {
    return `定位精度过低（约 ${accuracyState.accuracyMeters} 米），不适合做 ${accuracyState.goodThreshold} 米围栏判断`
  }
  if (accuracyState.category === 'WEAK') {
    return `定位精度不足（约 ${accuracyState.accuracyMeters} 米），请确认后重试`
  }
  if (code.includes('POSITION_UNAVAILABLE') || code.includes('UNAVAILABLE')) {
    return '定位失败，当前设备暂时无法获取有效坐标'
  }
  return '定位失败，请检查定位权限、网络和系统定位服务后重试'
}

export function buildLocationSuggestion({
  errorCode = '',
  accuracyMeters = null,
  policy = {},
  jsapiConfigFailed = false,
  environment = buildLocationEnvironmentDiagnostics()
} = {}) {
  const code = normalizeText(errorCode).toUpperCase()
  const suggestions = []
  if (environment.protocol === 'http:' && !environment.isLocalhost && !environment.isIntranet) {
    suggestions.push('当前为 HTTP，浏览器安全策略可能禁止定位')
  }
  if (isPcPlatform(environment.userAgent || '')) {
    suggestions.push('当前为 PC 浏览器，定位精度可能较差，建议手机端重试')
  }
  if (isServerLikeUserAgent(environment.userAgent || '')) {
    suggestions.push('当前为服务器或云主机浏览器，返回的可能是机房位置，不是用户真实位置')
  }
  if (jsapiConfigFailed || code.includes('JSAPI_CONFIG') || code.includes('INVALID_SIGNATURE')) {
    suggestions.push('当前为微信环境但 jsapi-config 获取失败或签名异常')
  }
  const accuracyState = classifyLocationAccuracy(accuracyMeters, policy)
  if (accuracyState.category === 'WEAK' || accuracyState.category === 'INVALID') {
    suggestions.push(`当前定位精度约 ${accuracyState.accuracyMeters} 米，不适合做 ${accuracyState.goodThreshold} 米围栏判断`)
  }
  return suggestions.join('；') || '请结合定位环境、权限状态和原始日志继续排查'
}

export function buildLocationDiagnosticPayload({
  env = '',
  provider = '',
  stage = '',
  errorCode = '',
  rawMessage = '',
  diagnostics = null,
  policy = {},
  jsapiConfigFailed = false,
  extra = {}
} = {}) {
  const environment = buildLocationEnvironmentDiagnostics()
  return {
    env: normalizeText(env) || (/micromessenger/i.test(environment.userAgent || '') ? 'WECHAT' : 'BROWSER'),
    provider: normalizeText(provider) || 'BROWSER_GEO',
    stage: normalizeText(stage) || 'LOCATION',
    errorCode: normalizeText(errorCode) || 'LOCATION_FAILED',
    rawMessage: normalizeText(rawMessage) || '',
    latitude: diagnostics?.submitLatitude ?? diagnostics?.rawLatitude ?? null,
    longitude: diagnostics?.submitLongitude ?? diagnostics?.rawLongitude ?? null,
    accuracy: diagnostics?.accuracyMeters ?? null,
    distanceMeters: diagnostics?.localDistanceMeters ?? null,
    currentUrl: environment.url || '',
    userAgent: environment.userAgent || '',
    suggestion: buildLocationSuggestion({
      errorCode,
      accuracyMeters: diagnostics?.accuracyMeters ?? null,
      policy,
      jsapiConfigFailed,
      environment
    }),
    rawData: {
      environment,
      diagnostics,
      ...extra
    }
  }
}

export function buildDesktopLocationHint() {
  return '当前设备定位精度可能不足，建议使用手机端或微信中打开后打卡。'
}

export function isDesktopLocationEnvironment() {
  const environment = buildLocationEnvironmentDiagnostics()
  return isPcPlatform(environment.userAgent || '') || isServerLikeUserAgent(environment.userAgent || '')
}

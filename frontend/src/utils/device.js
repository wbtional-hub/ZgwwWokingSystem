const MOBILE_DEVICE_PATTERN = /Android|iPhone|iPad|iPod|Mobile|HarmonyOS/i

export function resolveUserAgent() {
  if (typeof navigator === 'undefined') {
    return ''
  }
  return navigator.userAgent || ''
}

export function isMobileClient() {
  return MOBILE_DEVICE_PATTERN.test(resolveUserAgent())
}

export function isMiniProgramClient() {
  return typeof window !== 'undefined' && window.__wxjs_environment === 'miniprogram'
}

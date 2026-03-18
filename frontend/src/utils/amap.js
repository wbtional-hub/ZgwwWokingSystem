import request from '@/utils/request'

const AMAP_CALLBACK_NAME = '__unitAttendanceLocationAmapInit__'

let amapLoadPromise
let amapConfigPromise
let amapConfigCache

export function isAmapSdkConfigured() {
  return Boolean(amapConfigCache?.key && amapConfigCache?.securityJsCode)
}

async function loadAmapConfig() {
  if (amapConfigCache) {
    return amapConfigCache
  }
  if (amapConfigPromise) {
    return amapConfigPromise
  }
  amapConfigPromise = request
    .get('/config/amap')
    .then((response) => {
      const key = response?.data?.key?.trim?.() || ''
      const securityJsCode = response?.data?.securityJsCode?.trim?.() || ''
      if (!response || response.code !== 0 || !key || !securityJsCode) {
        throw new Error(response?.message || '地图配置读取失败')
      }
      amapConfigCache = { key, securityJsCode }
      console.info('[AMap] config loaded', {
        hasKey: Boolean(key),
        hasSecurityJsCode: Boolean(securityJsCode)
      })
      return amapConfigCache
    })
    .catch((error) => {
      amapConfigPromise = null
      throw error
    })
  return amapConfigPromise
}

function applyAmapSecurityConfig(config) {
  window._AMapSecurityConfig = {
    securityJsCode: config.securityJsCode
  }
  console.info('[AMap] security injected', {
    hasKey: Boolean(config.key),
    hasSecurityJsCode: Boolean(config.securityJsCode)
  })
}

export async function loadAmapSdk() {
  if (typeof window === 'undefined') {
    throw new Error('当前环境不支持地图加载')
  }
  const config = await loadAmapConfig()
  applyAmapSecurityConfig(config)
  if (window.AMap) {
    console.info('[AMap] SDK loaded', {
      version: window.AMap?.v || window.AMap?.version || 'unknown',
      fromCache: true
    })
    return window.AMap
  }
  if (amapLoadPromise) {
    return amapLoadPromise
  }

  amapLoadPromise = new Promise((resolve, reject) => {
    window[AMAP_CALLBACK_NAME] = () => {
      console.info('[AMap] SDK loaded', {
        version: window.AMap?.v || window.AMap?.version || 'unknown'
      })
      resolve(window.AMap)
      delete window[AMAP_CALLBACK_NAME]
    }

    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(config.key)}&plugin=AMap.Geocoder,AMap.Geolocation&callback=${AMAP_CALLBACK_NAME}`
    script.async = true
    script.onerror = () => {
      console.error('[AMap] SDK script load failed')
      amapLoadPromise = null
      reject(new Error('地图 SDK 加载失败，请检查网络或 Key 配置'))
      delete window[AMAP_CALLBACK_NAME]
    }
    document.head.appendChild(script)
  })

  return amapLoadPromise
}

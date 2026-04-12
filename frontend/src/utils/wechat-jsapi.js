const WECHAT_JS_SDK_URL = 'https://res.wx.qq.com/open/js/jweixin-1.6.0.js'
const WECHAT_JS_SDK_SCRIPT_ID = 'wechat-js-sdk-script'

let sdkLoadPromise
let configuredSignatureKey = ''
let configuredReadyPromise

export function isWechatBrowser() {
  if (typeof navigator === 'undefined') {
    return false
  }
  return /micromessenger/i.test(navigator.userAgent || '')
}

export async function loadWechatJsSdk() {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    throw new Error('当前环境不支持微信 JS-SDK')
  }
  if (window.wx) {
    return window.wx
  }
  if (sdkLoadPromise) {
    return sdkLoadPromise
  }

  sdkLoadPromise = new Promise((resolve, reject) => {
    const resolveWithSdk = () => {
      if (!window.wx) {
        sdkLoadPromise = null
        reject(new Error('微信 JS-SDK 加载完成，但 wx 对象不可用'))
        return
      }
      resolve(window.wx)
    }

    const existingScript = document.getElementById(WECHAT_JS_SDK_SCRIPT_ID)
    if (existingScript) {
      existingScript.addEventListener('load', resolveWithSdk, { once: true })
      existingScript.addEventListener('error', () => {
        sdkLoadPromise = null
        reject(new Error('微信 JS-SDK 加载失败'))
      }, { once: true })
      return
    }

    const script = document.createElement('script')
    script.id = WECHAT_JS_SDK_SCRIPT_ID
    script.src = WECHAT_JS_SDK_URL
    script.async = true
    script.onload = resolveWithSdk
    script.onerror = () => {
      sdkLoadPromise = null
      reject(new Error('微信 JS-SDK 加载失败'))
    }
    document.head.appendChild(script)
  })

  return sdkLoadPromise
}

export async function ensureWechatJsapiReady(config) {
  if (!config?.enabled) {
    throw new Error('当前页面未启用微信 JS-SDK')
  }
  const wx = await loadWechatJsSdk()
  const jsApiList = Array.isArray(config.jsApiList) ? config.jsApiList : []
  const signatureKey = [
    config.appId || '',
    config.timestamp || '',
    config.nonceStr || '',
    config.signature || '',
    jsApiList.join(',')
  ].join('|')

  if (configuredReadyPromise && configuredSignatureKey === signatureKey) {
    return configuredReadyPromise
  }

  configuredSignatureKey = signatureKey
  configuredReadyPromise = new Promise((resolve, reject) => {
    let settled = false

    const resolveOnce = () => {
      if (settled) {
        return
      }
      settled = true
      resolve(wx)
    }

    const rejectOnce = (error) => {
      if (settled) {
        return
      }
      settled = true
      configuredReadyPromise = null
      configuredSignatureKey = ''
      reject(error instanceof Error ? error : new Error(String(error || '微信 JS-SDK 初始化失败')))
    }

    wx.ready(() => {
      if (!jsApiList.length) {
        resolveOnce()
        return
      }
      if (typeof wx.checkJsApi !== 'function') {
        resolveOnce()
        return
      }
      wx.checkJsApi({
        jsApiList,
        success: () => resolveOnce(),
        fail: () => resolveOnce()
      })
    })

    wx.error((error) => {
      console.error('[wechat jsapi config failed]', error)
      rejectOnce(new Error(error?.errMsg || '微信 JS-SDK 初始化失败'))
    })

    wx.config({
      debug: Boolean(config.debug),
      appId: config.appId,
      timestamp: config.timestamp,
      nonceStr: config.nonceStr,
      signature: config.signature,
      jsApiList
    })
  })

  return configuredReadyPromise
}

export async function getWechatLocationByJsapi({
  config,
  timeoutMs = 8000,
  locationType = 'gcj02'
} = {}) {
  const wx = await ensureWechatJsapiReady(config)
  if (typeof wx.getLocation !== 'function') {
    throw new Error('当前微信 JS-SDK 不支持定位接口')
  }

  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      reject(Object.assign(new Error('微信定位超时'), { code: 'TIMEOUT' }))
    }, Math.max(Number(timeoutMs) || 8000, 1000))

    const clearTimer = () => {
      clearTimeout(timer)
    }

    wx.getLocation({
      type: locationType || config?.locationType || 'gcj02',
      success: (result) => {
        clearTimer()
        resolve({
          latitude: result?.latitude,
          longitude: result?.longitude,
          accuracy: result?.accuracy ?? null,
          speed: result?.speed ?? null
        })
      },
      fail: (error) => {
        clearTimer()
        reject(Object.assign(new Error(error?.errMsg || '微信定位失败'), { code: 'FAIL', detail: error }))
      },
      cancel: () => {
        clearTimer()
        reject(Object.assign(new Error('用户取消微信定位'), { code: 'CANCEL' }))
      }
    })
  })
}

import request from '@/utils/request'

export function loginApi(data) {
  return request.post('/auth/login', data)
}

export function queryMobileLoginOptionsApi() {
  return request.get('/auth/mobile-login-options')
}

export function changePasswordApi(data) {
  return request.post('/auth/change-password', data)
}

export function wechatMiniLoginApi(data) {
  return request.post('/auth/wechat-mini-login', data)
}

export function wechatMpLoginApi(data) {
  return request.post('/auth/wechat-mp-login', data)
}

export function queryWechatMpAuthorizeUrlApi(params) {
  return request.get('/auth/wechat-mp-authorize-url', { params })
}

export function queryCurrentUserApi() {
  return request.get('/auth/me')
}

export function createQrSession() {
  return request.post('/auth/qr-login/session')
}

export function getQrStatus(qrToken) {
  return request.get('/auth/qr-login/status', {
    params: { qrToken }
  })
}

export function getQrMobileSession(qrToken) {
  return request.get('/auth/qr-login/mobile-session', {
    params: { qrToken }
  })
}

export function confirmQrLogin(data) {
  return request.post('/auth/qr-login/confirm', data)
}

import request from '@/utils/request'

export function loginApi(data) {
  return request.post('/auth/login', data)
}

export function queryCurrentUserApi() {
  return request.get('/auth/me')
}

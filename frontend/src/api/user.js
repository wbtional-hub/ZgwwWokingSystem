import request from '@/utils/request'

export function queryUserPageApi(params) {
  return request.get('/users', { params })
}

export function createUserApi(data) {
  return request.post('/users', data)
}

export function queryUserDetailApi(userId) {
  return request.get(`/users/${userId}`)
}

export function updateUserApi(userId, data) {
  return request.put(`/users/${userId}`, data)
}

export function deleteUserApi(userId) {
  return request.delete(`/users/${userId}`)
}

export function resetUserPasswordApi(userId) {
  return request.post(`/user/reset-password/${userId}`)
}

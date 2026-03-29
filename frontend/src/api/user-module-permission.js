import request from '@/utils/request'

export function queryModuleDefinitionListApi() {
  return request.get('/user-module-permissions/modules')
}

export function queryUserModulePermissionsApi(userId) {
  return request.get(`/user-module-permissions/users/${userId}`)
}

export function saveUserModulePermissionsApi(userId, data) {
  return request.put(`/user-module-permissions/users/${userId}`, data)
}

export function queryCurrentUserModulePermissionsApi() {
  return request.get('/user-module-permissions/current')
}

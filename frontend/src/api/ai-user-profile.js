import request from '@/utils/request'

export function getCurrentUserProfile() {
  return request.get('/agent/user-profile/current')
}

export function deleteUserProfileItem(itemId) {
  return request.delete(`/agent/user-profile/item/${itemId}`)
}

export function clearCurrentUserProfile() {
  return request.delete('/agent/user-profile/clear')
}
import request from '@/utils/request'

export function queryExpertList(data) {
  return request.post('/expert/list', data)
}

export function saveSkillOwner(data) {
  return request.post('/expert/save', data)
}
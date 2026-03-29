import request from '@/utils/request'

export function querySkillList(data) {
  return request.post('/skill/list', data)
}

export function saveSkill(data) {
  return request.post('/skill/save', data)
}

export function saveSkillVersion(data) {
  return request.post('/skill/version/save', data)
}

export function publishSkillVersion(data) {
  return request.post('/skill/version/publish', data)
}

export function getPublishedSkillVersion(skillId) {
  return request.get(`/skill/${skillId}/published-version`)
}

export function saveSkillBinding(data) {
  return request.post('/skill/binding/save', data)
}

export function querySkillTestCaseList(data) {
  return request.post('/skill/test-case/list', data)
}

export function saveSkillTestCase(data) {
  return request.post('/skill/test-case/save', data)
}

export function runSkillValidation(data) {
  return request.post('/skill/validation/run', data)
}

export function getSkillValidationDetail(runId) {
  return request.get(`/skill/validation/${runId}`)
}
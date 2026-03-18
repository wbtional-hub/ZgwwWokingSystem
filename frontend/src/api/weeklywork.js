import request from '@/utils/request'

export function queryWeeklyWorkList(data) {
  return request.post('/weekly-work/query', data)
}

export function saveWeeklyWorkDraft(data) {
  return request.post('/weekly-work/save-draft', data)
}

export function submitWeeklyWork(data) {
  return request.post('/weekly-work/submit', data)
}

export function queryWeeklyWorkDetail(id) {
  return request.get(`/weekly-work/detail/${id}`)
}

export function reviewWeeklyWork(data) {
  return request.post('/weekly-work/review', data)
}

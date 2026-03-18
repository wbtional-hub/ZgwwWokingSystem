import request from '@/utils/request'

export function queryWorkScoreList(data) {
  return request.post('/work-score/query', data)
}

export function calculateWorkScore(data) {
  return request.post('/work-score/calculate', data)
}

export function queryWorkScoreDetail(id) {
  return request.get(`/work-score/detail/${id}`)
}

export function exportWorkScoreReport(weekNo) {
  return request.get('/score/report/export', {
    params: { weekNo },
    responseType: 'blob'
  })
}

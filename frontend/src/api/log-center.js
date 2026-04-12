import request from '@/utils/request'

export function reportLogCenterApi(data) {
  return request.post('/log-center/report', data, {
    skipErrorReport: true,
    skipBusinessErrorReport: true,
    skipErrorToast: true
  })
}

export function queryLogCenterPageApi(data) {
  return request.post('/log-center/query', data)
}

export function queryLogCenterDetailApi(id) {
  return request.get(`/log-center/${id}`)
}

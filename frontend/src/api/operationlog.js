import request from '@/utils/request'

export function queryOperationLogListApi(data) {
  return request.post('/operation-log/query', data)
}

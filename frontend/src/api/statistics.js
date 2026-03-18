import request from '@/utils/request'

export function queryStatisticsOverview(params) {
  return request.get('/statistics/overview', { params })
}

export function queryStatisticsOrgRank(params) {
  return request.get('/statistics/orgRank', { params })
}

export function queryStatisticsRedList(params) {
  return request.get('/statistics/redList', { params })
}

export function queryStatisticsYellowList(params) {
  return request.get('/statistics/yellowList', { params })
}

export function queryStatisticsTrend(params) {
  return request.get('/statistics/trend', { params })
}

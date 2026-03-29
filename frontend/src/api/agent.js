import request from '@/utils/request'

export function createAgentSession(data) {
  return request.post('/agent/session/create', data)
}

export function queryAgentSessions(data) {
  return request.post('/agent/session/list', data)
}

export function queryAgentSessionStats(data) {
  return request.post('/agent/session/stats', data)
}

export function queryAgentSessionTrend(data) {
  return request.post('/agent/session/trend', data)
}

export function queryAgentMonthlyReport(data) {
  return request.post('/agent/session/monthly-report', data)
}

export function exportAgentMonthlyReportExcel(params) {
  return request.get('/agent/session/monthly-report/export/excel', {
    params,
    responseType: 'blob'
  })
}

export function exportAgentSessions(params) {
  return request.get('/agent/session/export', {
    params,
    responseType: 'blob'
  })
}

export function exportAgentSessionsExcel(params) {
  return request.get('/agent/session/export/excel', {
    params,
    responseType: 'blob'
  })
}

export function updateAgentSessionStatus(data) {
  return request.post('/agent/session/status', data)
}

export function sendAgentQuestion(data) {
  return request.post('/agent/chat', data)
}

export function queryAgentMessages(sessionId) {
  return request.get(`/agent/session/${sessionId}/messages`)
}
import request from '@/utils/request'

const AI_SESSION_TIMEOUT = 60000
const AI_CHAT_TIMEOUT = 90000
const AI_EXPORT_TIMEOUT = 90000

export function createAgentSession(data) {
  return request.post('/agent/session/create', data, {
    timeout: AI_SESSION_TIMEOUT
  })
}

export function queryAgentSessions(data) {
  return request.post('/agent/session/list', data, {
    timeout: AI_SESSION_TIMEOUT
  })
}

export function queryAgentSessionStats(data) {
  return request.post('/agent/session/stats', data, {
    timeout: AI_SESSION_TIMEOUT
  })
}

export function queryAgentSessionTrend(data) {
  return request.post('/agent/session/trend', data, {
    timeout: AI_SESSION_TIMEOUT
  })
}

export function queryAgentMonthlyReport(data) {
  return request.post('/agent/session/monthly-report', data, {
    timeout: AI_SESSION_TIMEOUT
  })
}

export function exportAgentMonthlyReportExcel(params) {
  return request.get('/agent/session/monthly-report/export/excel', {
    params,
    responseType: 'blob',
    timeout: AI_EXPORT_TIMEOUT
  })
}

export function exportAgentSessions(params) {
  return request.get('/agent/session/export', {
    params,
    responseType: 'blob',
    timeout: AI_EXPORT_TIMEOUT
  })
}

export function exportAgentSessionsExcel(params) {
  return request.get('/agent/session/export/excel', {
    params,
    responseType: 'blob',
    timeout: AI_EXPORT_TIMEOUT
  })
}

export function updateAgentSessionStatus(data) {
  return request.post('/agent/session/status', data)
}

export function sendAgentQuestion(data) {
  return request.post('/agent/chat', data, {
    timeout: AI_CHAT_TIMEOUT
  })
}

export function queryAgentMessages(sessionId) {
  return request.get(`/agent/session/${sessionId}/messages`, {
    timeout: AI_SESSION_TIMEOUT
  })
}

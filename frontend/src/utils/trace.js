export const TRACE_ID_HEADER = 'X-Trace-Id'

export function createTraceId() {
  return `${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`
}

export function normalizeTraceId(traceId) {
  if (traceId == null) {
    return ''
  }
  return String(traceId).trim()
}

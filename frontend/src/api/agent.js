import request from '@/utils/request'
import { TRACE_ID_HEADER, createTraceId } from '@/utils/trace'

const AI_SESSION_TIMEOUT = 60000
const AI_CHAT_TIMEOUT = 90000
const AI_STREAM_TIMEOUT = 120000
const AI_EXPORT_TIMEOUT = 90000
const AI_API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api'

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

export async function streamAgentQuestion(data, handlers = {}) {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(new Error('AI_STREAM_TIMEOUT')), AI_STREAM_TIMEOUT)
  if (handlers.signal) {
    if (handlers.signal.aborted) {
      controller.abort(handlers.signal.reason)
    } else {
      handlers.signal.addEventListener('abort', () => controller.abort(handlers.signal.reason), { once: true })
    }
  }
  const headers = {
    'Content-Type': 'application/json',
    Accept: 'text/event-stream',
    [TRACE_ID_HEADER]: createTraceId(),
    'X-Page-Url': typeof window === 'undefined' ? '' : window.location.href
  }
  const token = localStorage.getItem('token')
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }

  const streamState = {
    started: false,
    receivedDelta: false,
    done: false,
    doneMeta: null
  }

  try {
    console.debug('STREAM_CLIENT_START', {
      sessionId: data?.sessionId,
      sourceScene: data?.sourceScene || '',
      hasSkillHint: Boolean(data?.skillHint)
    })
    const response = await fetch(`${AI_API_BASE_URL}/agent/chat-stream`, {
      method: 'POST',
      headers,
      body: JSON.stringify(data),
      signal: controller.signal
    })
    if (!response.ok) {
      throw new Error(await resolveStreamErrorMessage(response))
    }
    if (!response.body) {
      throw new Error('流式响应为空')
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''
    while (true) {
      const { done, value } = await reader.read()
      buffer += decoder.decode(value || new Uint8Array(), { stream: !done })
      buffer = await consumeSseBuffer(buffer, streamState, handlers)
      if (done) {
        break
      }
    }
    if (!streamState.done) {
      throw new Error('流式响应中断')
    }
    console.debug('STREAM_CLIENT_DONE', {
      sessionId: data?.sessionId,
      started: streamState.started,
      receivedDelta: streamState.receivedDelta,
      hasDoneMeta: Boolean(streamState.doneMeta)
    })
    return streamState
  } catch (error) {
    const normalizedError = normalizeStreamClientError(error, controller.signal, handlers.signal, streamState)
    if (isStreamAbortLikeError(normalizedError)) {
      console.debug(normalizedError.intentionalAbort ? 'STREAM_CLIENT_ABORT_INTENTIONAL' : 'STREAM_CLIENT_ABORT_UNEXPECTED', {
        sessionId: data?.sessionId,
        started: streamState.started,
        receivedDelta: streamState.receivedDelta,
        done: streamState.done,
        message: normalizedError.message || ''
      })
    }
    throw normalizedError
  } finally {
    window.clearTimeout(timer)
  }
}

export function queryAgentMessages(sessionId) {
  return request.get(`/agent/session/${sessionId}/messages`, {
    timeout: AI_SESSION_TIMEOUT
  })
}

async function consumeSseBuffer(buffer, streamState, handlers) {
  let working = String(buffer || '').replace(/\r\n/g, '\n')
  let boundary = working.indexOf('\n\n')
  while (boundary >= 0) {
    const block = working.slice(0, boundary)
    working = working.slice(boundary + 2)
    await dispatchSseEvent(block, streamState, handlers)
    boundary = working.indexOf('\n\n')
  }
  return working
}

async function dispatchSseEvent(block, streamState, handlers) {
  const lines = String(block || '').split('\n').filter(Boolean)
  if (!lines.length) {
    return
  }
  let eventName = 'message'
  const dataLines = []
  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      eventName = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  })
  const payload = parseSsePayload(dataLines.join('\n'))
  if (eventName === 'start') {
    streamState.started = true
    handlers.onStart?.(payload)
    return
  }
  if (eventName === 'delta') {
    streamState.receivedDelta = true
    handlers.onDelta?.(payload)
    return
  }
  if (eventName === 'done_meta') {
    streamState.doneMeta = payload
    handlers.onDoneMeta?.(payload)
    return
  }
  if (eventName === 'done') {
    streamState.done = true
    handlers.onDone?.(payload)
    return
  }
  if (eventName === 'error') {
    handlers.onError?.(payload)
    throw new Error(payload?.message || '流式请求失败')
  }
}

function parseSsePayload(text) {
  const raw = String(text || '').trim()
  if (!raw) {
    return {}
  }
  try {
    return JSON.parse(raw)
  } catch (error) {
    return { text: raw }
  }
}

async function resolveStreamErrorMessage(response) {
  const text = await response.text()
  try {
    const parsed = JSON.parse(text)
    return parsed?.message || parsed?.data?.message || '流式请求失败'
  } catch (error) {
    return text || '流式请求失败'
  }
}

function normalizeStreamClientError(error, ...context) {
  const normalizedError = error instanceof Error ? error : new Error(String(error || '流式请求失败'))
  const signals = context.filter((item) => item && typeof item === 'object' && 'aborted' in item)
  const streamState = context.find((item) => item && typeof item === 'object' && 'started' in item)
  const abortReason = signals.map((signal) => signal.reason).find(Boolean)
  if (abortReason?.intentionalAbort) {
    normalizedError.intentionalAbort = true
  }
  if (streamState) {
    normalizedError.streamStarted = Boolean(streamState.started)
    normalizedError.receivedDelta = Boolean(streamState.receivedDelta)
    normalizedError.receivedDone = Boolean(streamState.done)
  }
  return normalizedError
}

function isStreamAbortLikeError(error) {
  const message = String(error?.message || '')
  return error?.name === 'AbortError'
    || message.includes('BodyStreamBuffer was aborted')
    || message.includes('The operation was aborted')
    || error?.intentionalAbort === true
}

<template>
  <div class="policy-page" :class="{ 'policy-page--with-tabbar': showMobileTabbar }">
    <div class="policy-shell">
      <AIPageGuideCard guide-key="policyConsultant" />

      <section v-if="!hasToken" class="panel panel--empty">
        <h1>手机端政策咨询</h1>
        <p>需要先登录后再进入问答链路。</p>
        <button type="button" class="primary-button" @click="goToLogin">去登录</button>
      </section>

      <template v-else>
        <section class="panel panel--summary">
          <div class="summary-title">政策咨询最小闭环</div>
          <div class="summary-row">
            <span class="summary-chip">{{ permissionFlags.canUseAi ? 'AI 优先' : '知识库兜底' }}</span>
            <span class="summary-chip">{{ currentExpertLabel }}</span>
            <span class="summary-chip">{{ currentRouteLabel }}</span>
            <span class="summary-chip">{{ state.sessionInfo?.id ? `会话 #${state.sessionInfo.id}` : '未创建会话' }}</span>
          </div>
          <div class="summary-text">{{ helperText }}</div>
          <div v-if="usageSummaryText" class="usage-strip">{{ usageSummaryText }}</div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <div class="panel-title">对话区</div>
              <div class="panel-subtitle">查询到什么内容，就即时展示什么内容。优先走流式返回，失败后再回退原有接口。</div>
            </div>
            <button type="button" class="ghost-button" :disabled="state.asking" @click="handleResetSession">新会话</button>
          </div>

          <div ref="messageListRef" class="message-list">
            <div v-if="state.loading" class="empty-state">正在准备会话...</div>
            <div v-else-if="state.errorMessage" class="empty-state empty-state--warning">{{ state.errorMessage }}</div>
            <template v-else-if="state.messageList.length">
              <article
                v-for="item in state.messageList"
                :key="item.id"
                class="message-bubble"
                :class="item.messageRole === 'user' ? 'message-bubble--user' : 'message-bubble--assistant'"
              >
                <div class="message-bubble__role">{{ item.messageRole === 'user' ? '我' : '政策咨询助手' }}</div>
                <div class="message-bubble__text">
                  <span>{{ item.messageText }}</span>
                  <span v-if="item.isStreaming" class="stream-cursor"></span>
                </div>
                <div v-if="item.messageRole !== 'user' && formatCitations(item).length" class="message-bubble__meta">
                  引用：{{ formatCitations(item).join(' / ') }}
                </div>
              </article>
            </template>
            <div v-else class="empty-state">
              先直接问一个具体问题，例如“厦门市高层次人才住房补贴怎么申请？”
            </div>
            <article v-if="state.asking && !hasStreamingMessage" class="message-bubble message-bubble--assistant">
              <div class="message-bubble__role">政策咨询助手</div>
              <div class="message-bubble__text">正在整理政策依据，请稍候...</div>
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">提问区</div>
          <div class="active-skill-bar">
            <div class="active-skill-text">
              <span class="active-skill-label">当前专家</span>
              <span class="active-skill-name">{{ currentExpertName }}</span>
            </div>
          </div>
          <textarea
            v-model="state.question"
            class="question-input"
            rows="4"
            maxlength="500"
            placeholder="直接输入政策问题，例如“双百计划怎么申请？”"
            :disabled="state.asking || !permissionFlags.canUseAgent"
            @input="handleQuestionInput"
            @keydown.enter.exact.prevent="handleSend"
          ></textarea>
          <div v-if="policySuggestions.length" class="intent-suggest-list">
            <button
              v-for="item in policySuggestions"
              :key="item.intentId"
              type="button"
              class="intent-suggest-item"
              @click="applyPolicySuggestion(item)"
            >
              <span class="intent-suggest-item__title">{{ item.intentName }}</span>
              <small class="intent-suggest-item__question">{{ item.standardQuestion }}</small>
            </button>
          </div>
          <div class="composer-footer">
            <div class="composer-hint">
              当前页面已默认绑定人才政策咨询专家，直接提问即可。
            </div>
            <button type="button" class="primary-button" :disabled="!canSend" @click="handleSend">
              {{ state.asking ? '发送中...' : '发送' }}
            </button>
          </div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <div class="panel-title">会话入口</div>
              <div class="panel-subtitle">点击已有会话继续追问；不点击则默认按新问题重新路由。</div>
            </div>
            <button type="button" class="ghost-button" :disabled="state.loading" @click="loadSessions">刷新</button>
          </div>
          <div v-if="state.sessionList.length" class="session-list">
            <button
              v-for="item in state.sessionList"
              :key="item.id"
              type="button"
              class="session-item"
              :disabled="state.asking"
              :class="{ 'session-item--active': state.sessionInfo?.id === item.id && state.sessionPinned }"
              @click="selectSession(item)"
            >
              <div class="session-item__title">{{ item.sessionTitle || `会话 #${item.id}` }}</div>
              <div class="session-item__meta">{{ item.skillName || '-' }} / {{ formatDateTime(item.lastMessageTime || item.createTime) }}</div>
            </button>
          </div>
          <div v-else class="empty-state">当前还没有手机端会话，首次发送问题时会自动创建。</div>
        </section>
      </template>
    </div>

    <MobileTabBar v-if="showMobileTabbar" :items="mobileNavItems" />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { showToast } from 'vant'
import { useRouter } from 'vue-router'
import AIPageGuideCard from '@/components/ai/AIPageGuideCard.vue'
import MobileTabBar from '@/components/mobile/MobileTabBar.vue'
import {
  createAgentSession,
  queryAgentMessages,
  queryAgentSessions,
  selectPolicyIntentSuggestion,
  sendAgentQuestion,
  suggestPolicyIntents,
  streamAgentQuestion
} from '@/api/agent'
import { queryCurrentAiPermission } from '@/api/ai'
import { querySkillList } from '@/api/skill'
import { useUserStore } from '@/stores/user'
import { getAgentFunctionBinding } from '@/constants/agent-function-bindings'
import { buildAccessContext } from '@/constants/modules'
import { resolveMobileWorkspaceItems } from '@/constants/mobile-workspace'
import { isMobileClient } from '@/utils/device'

const router = useRouter()
const userStore = useUserStore()
const messageListRef = ref(null)
const activeStreamContext = ref(null)
const sendRunId = ref(0)
const isComponentUnmounted = ref(false)
const FUNCTION_BINDING = getAgentFunctionBinding('talent_policy_consult')
const SOURCE_SCENE = 'MOBILE_POLICY_CONSULTANT'
const POLICY_BASE_ID = FUNCTION_BINDING?.defaultBaseId ?? 1
const POLICY_SUGGEST_MIN_LENGTH = 2
const POLICY_SUGGEST_MAX_LENGTH = 6
let policySuggestTimer = null
let policySuggestSeq = 0

const state = reactive({
  loading: false,
  asking: false,
  errorMessage: '',
  question: '',
  activeSkillId: null,
  activeSkillCode: '',
  activeSkillName: '',
  permissionInfo: null,
  sessionInfo: null,
  sessionPinned: false,
  sessionList: [],
  messageList: [],
  skillOptions: [],
  mentionKeyword: '',
  lastChatMeta: null,
  policySuggestLoading: false,
  policySuggestLogId: null,
  policySuggestItems: []
})

const hasToken = computed(() => Boolean(userStore.token || localStorage.getItem('token')))
const showMobileTabbar = computed(() => hasToken.value && isMobileClient())
const mobileNavItems = computed(() => resolveMobileWorkspaceItems(buildAccessContext(userStore.userInfo)))
const permissionFlags = computed(() => {
  const aiPermissions = state.permissionInfo?.aiPermissions || []
  return {
    canUseAi: Boolean(state.permissionInfo?.admin || aiPermissions.some((item) => item.canUseAi)),
    canUseAgent: Boolean(state.permissionInfo?.admin || aiPermissions.some((item) => item.canUseAgent))
  }
})
const availableSkills = computed(() => {
  if (state.permissionInfo?.admin) {
    return state.skillOptions
  }
  const allowedIds = new Set(
    (state.permissionInfo?.skillPermissions || [])
      .filter((item) => item.canUse && Number(item.status) === 1)
      .map((item) => Number(item.skillId))
  )
  return state.skillOptions.filter((item) => allowedIds.has(Number(item.id)))
})
const policySuggestions = computed(() => {
  return state.policySuggestItems.slice(0, 6)
})
const currentExpertName = computed(() => state.activeSkillName || FUNCTION_BINDING?.expertName || '人才政策咨询专家')
const currentExpertLabel = computed(() => `当前专家：${currentExpertName.value}`)
const currentRouteLabel = computed(() => formatMatchMode(state.lastChatMeta?.skillMatchMode || state.sessionInfo?.skillMatchMode))
const usageSummaryText = computed(() => {
  const meta = state.lastChatMeta || {}
  const hasUsage = ['promptTokens', 'completionTokens', 'totalTokens', 'monthTotalTokens', 'durationMs', 'modelCode']
    .some((key) => meta[key] !== undefined && meta[key] !== null && meta[key] !== '')
  if (!hasUsage) {
    return ''
  }
  return [
    `本次 P ${Number(meta.promptTokens || 0)}`,
    `C ${Number(meta.completionTokens || 0)}`,
    `T ${Number(meta.totalTokens || 0)}`,
    `本月 ${Number(meta.monthTotalTokens || 0)}`,
    `模型 ${meta.modelCode || '-'}`,
    `${Number(meta.durationMs || 0)} ms`
  ].join(' / ')
})
const helperText = computed(() => {
  if (state.errorMessage) {
    return state.errorMessage
  }
  if (!permissionFlags.value.canUseAgent) {
    return '当前账号还没有 AI 主链权限，请先到权限配置中开通。'
  }
  if (!permissionFlags.value.canUseAi) {
    return '当前链路会优先走知识库兜底，AI Provider 恢复后会自动切回 AI。'
  }
  if (state.lastChatMeta?.answerSource === 'FAST_PATH_STRUCTURED') {
    return '本次回答已命中快路径，页面会按真实流式逐段展示。'
  }
  if (state.lastChatMeta?.skillName) {
    return `本次问题命中 ${state.lastChatMeta.skillName}，路由方式：${formatMatchMode(state.lastChatMeta.skillMatchMode)}。`
  }
  return '当前链路已准备好，页面会默认使用人才政策咨询专家和政策知识库 baseId=1。'
})
const canSend = computed(() => permissionFlags.value.canUseAgent && Boolean(buildQuestionContent(state.question)) && !state.asking)

function createIntentionalAbortReason(code) {
  const error = new Error(code || 'STREAM_ABORTED')
  error.name = 'AbortError'
  error.intentionalAbort = true
  return error
}

function isExpectedStreamAbort(error) {
  const message = String(error?.message || '')
  return error?.intentionalAbort === true
    || error?.name === 'AbortError'
    || message.includes('BodyStreamBuffer was aborted')
    || message.includes('The operation was aborted')
}

function createStreamContext(runId) {
  return {
    runId,
    controller: new AbortController(),
    receivedStart: false,
    receivedDelta: false,
    doneReceived: false,
    intentionalAbort: false,
    watchdogTimer: null,
    watchdogTriggered: false
  }
}

function clearActiveStream(streamContext) {
  if (!streamContext || activeStreamContext.value !== streamContext) {
    return
  }
  clearStreamWatchdog(streamContext)
  activeStreamContext.value = null
}

function abortActiveStream(reasonCode = 'STREAM_REPLACED') {
  const streamContext = activeStreamContext.value
  if (!streamContext || streamContext.doneReceived || streamContext.controller.signal.aborted) {
    return false
  }
  streamContext.intentionalAbort = true
  streamContext.controller.abort(createIntentionalAbortReason(reasonCode))
  return true
}

function clearStreamWatchdog(streamContext) {
  if (!streamContext?.watchdogTimer) {
    return
  }
  window.clearTimeout(streamContext.watchdogTimer)
  streamContext.watchdogTimer = null
}

function startStreamWatchdog(streamContext, assistantId) {
  clearStreamWatchdog(streamContext)
  streamContext.watchdogTimer = window.setTimeout(() => {
    if (!streamContext || activeStreamContext.value !== streamContext || streamContext.receivedDelta || streamContext.doneReceived) {
      return
    }
    streamContext.watchdogTriggered = true
    const message = findMessageById(assistantId)
    if (message) {
      message.messageText = '当前未获取到可展示内容，请稍后重试或补充更具体条件'
      message.isStreaming = false
    }
    abortActiveStream('NO_DELTA_TIMEOUT')
  }, 7000)
}
const hasStreamingMessage = computed(() => state.messageList.some((item) => item.isStreaming))

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function goToLogin() {
  router.push({ path: '/login', query: { redirect: '/policy-consultant' } })
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ') : '-'
}

function formatMatchMode(value) {
  if (value === 'EXPLICIT_HINT') return '@Skill 指定'
  if (value === 'EXPLICIT_ID') return '指定 Skill'
  if (value === 'AUTO_MATCH') return '自动匹配'
  if (value === 'DEFAULT_POLICY_SKILL') return '移动端默认 Skill'
  if (value === 'FALLBACK_FIRST_PUBLISHED') return '兜底 Skill'
  if (value === 'SESSION_REUSE') return '继续当前会话'
  return '待自动路由'
}

function formatCitations(message) {
  if (Array.isArray(message?.citedTitles) && message.citedTitles.length) {
    return message.citedTitles
  }
  if (Array.isArray(message?.citedChunkIdList) && message.citedChunkIdList.length) {
    return message.citedChunkIdList.map((item) => `Chunk ${item}`)
  }
  return []
}

function stripSkillMentions(question) {
  return String(question || '').replace(/(?:^|\s)@[^\s@]*/g, ' ').replace(/\s+/g, ' ').trim()
}

function normalizeQuestion(question) {
  return stripSkillMentions(question)
}

function buildQuestionContent(question) {
  return stripSkillMentions(question)
}

function buildSuggestQuery(question) {
  return stripSkillMentions(question)
}

function resolveSkillByHint(skillHint) {
  if (!skillHint) {
    return null
  }
  const hint = String(skillHint).trim().toLowerCase()
  return availableSkills.value.find((item) => {
    return String(item.skillCode || '').toLowerCase() === hint
      || String(item.skillName || '').toLowerCase() === hint
  }) || null
}

function resolveFunctionBoundSkill() {
  const boundSkillCode = String(FUNCTION_BINDING?.defaultSkillCode || '').trim().toLowerCase()
  if (!boundSkillCode) {
    return null
  }
  return availableSkills.value.find((item) => String(item.skillCode || '').trim().toLowerCase() === boundSkillCode) || null
}

function setActiveSkill(skill) {
  state.activeSkillId = skill?.id ? Number(skill.id) : null
  state.activeSkillCode = String(skill?.skillCode || '').trim()
  state.activeSkillName = String(skill?.skillName || '').trim()
}

function syncActiveSkillFromSession(session, fallbackSkill = null) {
  if (session?.skillId || session?.skillCode || session?.skillName) {
    setActiveSkill({
      id: session?.skillId ?? fallbackSkill?.id,
      skillCode: session?.skillCode || fallbackSkill?.skillCode || '',
      skillName: session?.skillName || fallbackSkill?.skillName || ''
    })
    return
  }
  if (fallbackSkill) {
    setActiveSkill(fallbackSkill)
    return
  }
  state.activeSkillId = null
  state.activeSkillCode = ''
  state.activeSkillName = ''
}

function clearPolicySuggestions() {
  if (policySuggestTimer) {
    window.clearTimeout(policySuggestTimer)
    policySuggestTimer = null
  }
  state.policySuggestLoading = false
  state.policySuggestLogId = null
  state.policySuggestItems = []
}

function currentSuggestUserId() {
  const rawUserId = userStore.userInfo?.id ?? userStore.userInfo?.userId ?? null
  if (rawUserId === null || rawUserId === undefined || rawUserId === '') {
    return undefined
  }
  const numericUserId = Number(rawUserId)
  return Number.isFinite(numericUserId) ? numericUserId : undefined
}

function shouldRequestPolicySuggest(input) {
  if (!hasToken.value || state.asking || !permissionFlags.value.canUseAgent) {
    return false
  }
  const compact = String(buildSuggestQuery(input) || '').replace(/\s+/g, '')
  return compact.length >= POLICY_SUGGEST_MIN_LENGTH && compact.length <= POLICY_SUGGEST_MAX_LENGTH
}

function schedulePolicySuggest() {
  const rawInput = String(state.question || '').trim()
  const suggestQuery = buildSuggestQuery(rawInput)
  if (!shouldRequestPolicySuggest(suggestQuery)) {
    clearPolicySuggestions()
    return
  }
  if (policySuggestTimer) {
    window.clearTimeout(policySuggestTimer)
  }
  const requestSeq = ++policySuggestSeq
  policySuggestTimer = window.setTimeout(() => {
    fetchPolicySuggestions(suggestQuery, requestSeq)
  }, 180)
}

async function fetchPolicySuggestions(input, requestSeq) {
  if (!shouldRequestPolicySuggest(input)) {
    clearPolicySuggestions()
    return
  }
  state.policySuggestLoading = true
  try {
    const data = ensureSuccess(await suggestPolicyIntents({
      baseId: POLICY_BASE_ID,
      q: input,
      userId: currentSuggestUserId()
    }), '获取联想推荐失败')
    if (requestSeq !== policySuggestSeq || buildSuggestQuery(state.question) !== input.trim()) {
      return
    }
    state.policySuggestLogId = data?.suggestLogId ?? null
    state.policySuggestItems = Array.isArray(data?.items) ? data.items : []
  } catch (error) {
    if (requestSeq !== policySuggestSeq) {
      return
    }
    state.policySuggestLogId = null
    state.policySuggestItems = []
  } finally {
    if (requestSeq === policySuggestSeq) {
      state.policySuggestLoading = false
    }
  }
}

function handleQuestionInput() {
  schedulePolicySuggest()
}

async function applyPolicySuggestion(item) {
  state.question = String(item?.standardQuestion || '').trim()
  state.mentionKeyword = ''
  const suggestLogId = state.policySuggestLogId
  const selectedIntentId = item?.intentId
  clearPolicySuggestions()
  if (!suggestLogId || !selectedIntentId) {
    return
  }
  try {
    await selectPolicyIntentSuggestion({
      suggestLogId,
      selectedIntentId
    })
  } catch (error) {
    console.warn('policy suggest select failed', error)
  }
}

function createLocalMessage({ id, messageRole, messageText, isStreaming = false, citedTitles = [] }) {
  return {
    id,
    messageRole,
    messageText,
    isStreaming,
    citedTitles
  }
}

function appendLocalConversation(question) {
  const seed = Date.now()
  const userId = `local-user-${seed}`
  const assistantId = `local-assistant-${seed}`
  state.messageList.push(createLocalMessage({
    id: userId,
    messageRole: 'user',
    messageText: question
  }))
  state.messageList.push(createLocalMessage({
    id: assistantId,
    messageRole: 'assistant',
    messageText: '',
    isStreaming: true
  }))
  return { userId, assistantId }
}

function findMessageById(id) {
  return state.messageList.find((item) => item.id === id)
}

function removeLocalConversation(localIds) {
  if (!localIds) {
    return
  }
  const blocked = new Set([localIds.userId, localIds.assistantId])
  state.messageList = state.messageList.filter((item) => !blocked.has(item.id))
}

function appendAssistantDelta(assistantId, text) {
  const message = findMessageById(assistantId)
  if (!message) {
    return
  }
  message.messageText = `${message.messageText || ''}${text || ''}`
}

function applyDoneMeta(assistantId, payload) {
  const message = findMessageById(assistantId)
  if (message && Array.isArray(payload?.citations)) {
    message.citedTitles = payload.citations
  }
  state.lastChatMeta = {
    ...(state.lastChatMeta || {}),
    answerSource: payload?.answerSource || '',
    promptTokens: Number(payload?.promptTokens ?? 0),
    completionTokens: Number(payload?.completionTokens ?? 0),
    totalTokens: Number(payload?.totalTokens ?? 0),
    monthTotalTokens: Number(payload?.monthTotalTokens ?? 0),
    durationMs: Number(payload?.durationMs ?? 0),
    modelCode: payload?.modelCode || ''
  }
}

function finishStreamingAssistant(assistantId) {
  const message = findMessageById(assistantId)
  if (message) {
    message.isStreaming = false
  }
}

function splitTypingSegments(text) {
  const normalized = String(text || '').replace(/\r\n/g, '\n')
  const segments = []
  normalized.split('\n').forEach((line) => {
    const trimmed = line.trim()
    if (!trimmed) {
      return
    }
    if (/^\d+[.、]/.test(trimmed) || trimmed.startsWith('- ')) {
      segments.push(`${trimmed}\n`)
      return
    }
    trimmed.split(/(?<=[。！？!?；;])/).forEach((part) => {
      if (part.trim()) {
        segments.push(part)
      }
    })
  })
  return segments.length ? segments : [normalized]
}

function sleep(ms) {
  return new Promise((resolve) => window.setTimeout(resolve, ms))
}

async function playFallbackTyping(assistantId, answer) {
  const message = findMessageById(assistantId)
  if (!message) {
    return
  }
  message.messageText = ''
  message.isStreaming = true
  for (const segment of splitTypingSegments(answer)) {
    appendAssistantDelta(assistantId, segment)
    await scrollToBottom()
    await sleep(48)
  }
  message.isStreaming = false
}

async function scrollToBottom() {
  await nextTick()
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

async function fetchMessages() {
  if (!state.sessionInfo?.id) {
    state.messageList = []
    return
  }
  const data = ensureSuccess(await queryAgentMessages(state.sessionInfo.id), '加载会话消息失败')
  state.messageList = Array.isArray(data) ? data : []
  await scrollToBottom()
}

async function loadSessions() {
  const data = ensureSuccess(await queryAgentSessions({ sourceScene: SOURCE_SCENE, status: 'ACTIVE' }), '加载会话列表失败')
  const sessionList = Array.isArray(data) ? data : []
  const boundSkillId = state.activeSkillId ? Number(state.activeSkillId) : null
  state.sessionList = boundSkillId
    ? sessionList.filter((item) => Number(item?.skillId) === boundSkillId)
    : sessionList
  if (!state.sessionPinned && !state.sessionInfo?.id && state.sessionList.length) {
    state.sessionInfo = state.sessionList[0]
    syncActiveSkillFromSession(state.sessionInfo, resolveFunctionBoundSkill())
    state.messageList = []
  }
}

async function createOrReuseSession(payload) {
  const explicitSkill = resolveFunctionBoundSkill()

  if (!explicitSkill?.id) {
    throw new Error(`当前功能缺少默认专家绑定，请确认 ${FUNCTION_BINDING?.defaultSkillCode || 'talent_policy_consultant'} 已发布且当前账号可用。`)
  }

  if (state.sessionPinned && state.sessionInfo?.id && (!explicitSkill || Number(state.sessionInfo.skillId) === Number(explicitSkill.id))) {
    syncActiveSkillFromSession(state.sessionInfo, explicitSkill)
    return state.sessionInfo
  }

  if (explicitSkill) {
    const existing = state.sessionList.find((item) => Number(item.skillId) === Number(explicitSkill.id))
    if (existing?.id && state.sessionPinned) {
      state.sessionInfo = existing
      syncActiveSkillFromSession(existing, explicitSkill)
      return existing
    }
  }

  const session = ensureSuccess(await createAgentSession({
    skillId: Number(explicitSkill.id),
    baseId: POLICY_BASE_ID,
    skillHint: explicitSkill?.skillCode || undefined,
    question: payload.question,
    sourceScene: SOURCE_SCENE
  }), '创建会话失败')
  state.sessionInfo = session
  state.sessionPinned = true
  syncActiveSkillFromSession(session, explicitSkill)
  state.lastChatMeta = {
    skillName: session?.skillName || '',
    skillMatchMode: session?.skillMatchMode || ''
  }
  await loadSessions()
  return session
}

async function selectSession(item) {
  if (state.asking) {
    return
  }
  state.sessionInfo = item
  state.sessionPinned = true
  syncActiveSkillFromSession(item)
  state.lastChatMeta = {
    skillName: item?.skillName || '',
    skillMatchMode: 'SESSION_REUSE'
  }
  await fetchMessages()
}

async function handleResetSession() {
  abortActiveStream('RESET_SESSION')
  state.sessionInfo = null
  state.sessionPinned = false
  state.lastChatMeta = null
  state.messageList = []
  state.question = ''
  syncActiveSkillFromSession(null, resolveFunctionBoundSkill())
  clearPolicySuggestions()
}

async function handleSend() {
  if (activeStreamContext.value) {
    abortActiveStream('USER_NEXT_QUESTION')
  }
  if (state.asking) {
    return
  }
  const rawQuestion = state.question.trim()
  if (!rawQuestion) {
    showToast('请输入问题')
    return
  }
  if (!permissionFlags.value.canUseAgent) {
    showToast(state.errorMessage || '当前账号暂时无法发起问答')
    return
  }

  const skillHint = state.activeSkillCode
  const cleanQuestion = buildQuestionContent(rawQuestion)
  const runId = ++sendRunId.value
  const streamContext = createStreamContext(runId)
  state.question = ''
  clearPolicySuggestions()

  state.asking = true
  state.errorMessage = ''
  let localIds = null
  activeStreamContext.value = streamContext

  try {
    await createOrReuseSession({ question: cleanQuestion, skillHint })
    localIds = appendLocalConversation(cleanQuestion)
    await scrollToBottom()
    await streamAgentQuestion({
      sessionId: state.sessionInfo.id,
      question: cleanQuestion,
      sourceScene: SOURCE_SCENE,
      skillHint: skillHint || undefined
    }, {
      signal: streamContext.controller.signal,
      onStart(payload) {
        streamContext.receivedStart = true
        startStreamWatchdog(streamContext, localIds.assistantId)
        state.lastChatMeta = {
          ...(state.lastChatMeta || {}),
          answerSource: payload?.answerSource || ''
        }
      },
      onDelta(payload) {
        streamContext.receivedDelta = true
        clearStreamWatchdog(streamContext)
        appendAssistantDelta(localIds.assistantId, payload?.text || '')
        scrollToBottom()
      },
      onDoneMeta(payload) {
        applyDoneMeta(localIds.assistantId, payload)
      },
      onDone() {
        streamContext.doneReceived = true
        clearStreamWatchdog(streamContext)
        finishStreamingAssistant(localIds.assistantId)
      }
    })
    clearActiveStream(streamContext)
    if (isComponentUnmounted.value) {
      return
    }
    state.question = ''
    clearPolicySuggestions()
    await Promise.all([fetchMessages(), loadSessions()])
  } catch (error) {
    clearActiveStream(streamContext)
    if (isExpectedStreamAbort(error)) {
      clearStreamWatchdog(streamContext)
      finishStreamingAssistant(localIds?.assistantId)
      if (!streamContext.receivedStart && !streamContext.receivedDelta && !streamContext.watchdogTriggered) {
        removeLocalConversation(localIds)
        return
      }
      if (!isComponentUnmounted.value && state.sessionInfo?.id) {
        await fetchMessages()
      }
      return
    }
    if (!streamContext.receivedStart && !streamContext.receivedDelta && localIds) {
      try {
        const result = ensureSuccess(await sendAgentQuestion({
          sessionId: state.sessionInfo.id,
          question: cleanQuestion,
          sourceScene: SOURCE_SCENE,
          skillHint: skillHint || undefined
        }), '发送问题失败')
        state.lastChatMeta = {
          skillName: result?.skillName || state.sessionInfo?.skillName || '',
          skillMatchMode: result?.skillMatchMode || state.sessionInfo?.skillMatchMode || '',
          promptTokens: result?.promptTokens ?? 0,
          completionTokens: result?.completionTokens ?? 0,
          totalTokens: result?.totalTokens ?? 0,
          monthTotalTokens: result?.monthTotalTokens ?? 0,
          durationMs: result?.durationMs ?? 0,
          modelCode: result?.modelCode || state.sessionInfo?.modelCode || '',
          answerSource: 'CHAT_FALLBACK'
        }
        await playFallbackTyping(localIds.assistantId, result?.answer || '')
        const assistant = findMessageById(localIds.assistantId)
        if (assistant) {
          assistant.citedTitles = Array.isArray(result?.citedTitles) ? result.citedTitles : []
        }
        state.question = ''
        clearPolicySuggestions()
        await Promise.all([fetchMessages(), loadSessions()])
        return
      } catch (fallbackError) {
        removeLocalConversation(localIds)
        if (isExpectedStreamAbort(fallbackError) || isComponentUnmounted.value) {
          return
        }
        showToast(fallbackError.message || '发送问题失败')
        state.errorMessage = fallbackError.message || '发送问题失败'
        return
      }
    }
    finishStreamingAssistant(localIds?.assistantId)
    if (isComponentUnmounted.value) {
      return
    }
    showToast(error.message || '发送问题失败')
    state.errorMessage = error.message || '发送问题失败'
    await fetchMessages()
  } finally {
    clearStreamWatchdog(streamContext)
    clearActiveStream(streamContext)
    if (sendRunId.value === runId) {
      state.asking = false
    }
  }
}

onBeforeUnmount(() => {
  isComponentUnmounted.value = true
  abortActiveStream('COMPONENT_UNMOUNT')
  clearPolicySuggestions()
})

onMounted(async () => {
  if (!hasToken.value) {
    return
  }
  state.loading = true
  try {
    const [permissionResponse, skillResponse] = await Promise.all([
      queryCurrentAiPermission(),
      querySkillList({ publishStatus: 'PUBLISHED', status: 1 })
    ])
    state.permissionInfo = ensureSuccess(permissionResponse, '加载 AI 权限失败') || {}
    state.skillOptions = ensureSuccess(skillResponse, '加载 Skill 列表失败') || []
    if (!permissionFlags.value.canUseAgent) {
      state.errorMessage = '当前账号还没有 AI 主链权限，请先到 AI 权限配置中开通。'
      return
    }
    const boundSkill = resolveFunctionBoundSkill()
    if (!boundSkill?.id) {
      state.errorMessage = `当前功能缺少默认专家绑定，请确认 ${FUNCTION_BINDING?.defaultSkillCode || 'talent_policy_consultant'} 已发布且当前账号可用。`
      return
    }
    setActiveSkill(boundSkill)
    await loadSessions()
  } catch (error) {
    state.errorMessage = error.message || '初始化手机端政策咨询失败'
  } finally {
    state.loading = false
  }
})
</script>

<style scoped>
.policy-page {
  min-height: 100vh;
  padding: 16px;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
}

.policy-page--with-tabbar {
  padding-bottom: 0;
}

.policy-shell {
  max-width: 920px;
  margin: 0 auto;
}

.panel {
  padding: 18px;
  margin-bottom: 16px;
  border: 1px solid #e2e8f0;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.06);
}

.panel--empty {
  text-align: center;
}

.panel-head,
.composer-footer,
.summary-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.panel-title,
.summary-title {
  color: #111827;
  font-size: 18px;
  font-weight: 700;
}

.panel-subtitle,
.summary-text,
.composer-hint,
.message-bubble__meta,
.session-item__meta,
.empty-state {
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

.summary-row {
  justify-content: flex-start;
  flex-wrap: wrap;
  margin-top: 12px;
}

.summary-chip {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.summary-text {
  margin-top: 12px;
}

.usage-strip {
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe4f0;
  border-radius: 12px;
  background: #f8fafc;
  color: #334155;
  font-size: 13px;
  line-height: 1.6;
}

.message-list {
  max-height: 52vh;
  margin-top: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow-y: auto;
}

.message-bubble {
  max-width: 88%;
  padding: 14px 16px;
  border-radius: 18px;
}

.message-bubble--user {
  align-self: flex-end;
  background: #2563eb;
  color: #fff;
}

.message-bubble--assistant {
  align-self: flex-start;
  background: #f8fafc;
  border: 1px solid #dbe4f0;
}

.message-bubble__role {
  margin-bottom: 8px;
  font-size: 12px;
  font-weight: 700;
}

.message-bubble__text {
  white-space: pre-wrap;
  line-height: 1.7;
}

.stream-cursor {
  display: inline-block;
  width: 8px;
  height: 1.1em;
  margin-left: 2px;
  vertical-align: text-bottom;
  border-radius: 999px;
  background: #2563eb;
  animation: blink-cursor 1s steps(1) infinite;
}

.question-input {
  width: 100%;
  padding: 14px 16px;
  margin-top: 12px;
  border: 1px solid #cbd5e1;
  border-radius: 14px;
  resize: none;
  font: inherit;
  line-height: 1.7;
  background: #fff;
}

.active-skill-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  padding: 10px 12px;
  border: 1px solid #dbe4f0;
  border-radius: 12px;
  background: #eff6ff;
}

.active-skill-text {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.active-skill-label {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
}

.active-skill-name {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
  line-height: 1.5;
}

.active-skill-action {
  padding: 0;
  border: none;
  background: transparent;
  color: #2563eb;
  font: inherit;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.mention-list,
.intent-suggest-list,
.session-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.mention-item,
.intent-suggest-item,
.session-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #dbe4f0;
  border-radius: 12px;
  background: #f8fafc;
  color: #0f172a;
}

.intent-suggest-item {
  align-items: flex-start;
  flex-direction: column;
  text-align: left;
}

.intent-suggest-item__title {
  font-weight: 700;
}

.intent-suggest-item__question {
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.session-item--active {
  border-color: #2563eb;
  background: #eff6ff;
}

.session-item__title {
  font-weight: 700;
}

.primary-button,
.ghost-button {
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  font: inherit;
  cursor: pointer;
}

.primary-button {
  border: none;
  background: linear-gradient(90deg, #2563eb, #1d4ed8);
  color: #fff;
}

.ghost-button {
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #0f172a;
}

.primary-button:disabled,
.ghost-button:disabled,
.session-item:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.empty-state {
  padding: 24px 12px;
  text-align: center;
}

.empty-state--warning {
  color: #b45309;
}

@keyframes blink-cursor {
  0%,
  49% {
    opacity: 1;
  }
  50%,
  100% {
    opacity: 0;
  }
}

@media (max-width: 768px) {
  .policy-page {
    padding: 12px;
  }

  .active-skill-bar,
  .panel-head,
  .composer-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

<template>
  <div
  class="policy-page"
  :class="{
    'policy-page--with-tabbar': showBottomTabbar,
    'policy-page--keyboard': state.composerFocused
  }"
>
    <div class="policy-shell">
      <section v-if="!hasToken" class="panel panel--empty">
        <h1>手机端政策咨询</h1>
        <p>需要先登录后再进入问答链路。</p>
        <button type="button" class="primary-button" @click="goToLogin">去登录</button>
      </section>

      <template v-else>
  <section class="mobile-policy-header">
    <div>
      <div class="mobile-policy-title">政策咨询</div>
      <div class="mobile-policy-subtitle">人才政策智能问答</div>
    </div>
    <div class="mobile-policy-actions">
      <button type="button" class="mobile-icon-button" @click="openHistoryDrawer">历史</button>
      <button type="button" class="mobile-icon-button mobile-icon-button--primary" :disabled="state.asking" @click="handleResetSession">
        新会话
      </button>
    </div>
  </section>

  <section class="mobile-token-card">
    <div class="mobile-token-item">
      <span>本次</span>
      <strong>{{ currentTotalTokensText }}</strong>
      <small>Token</small>
    </div>
    <div class="mobile-token-divider"></div>
    <div class="mobile-token-item">
      <span>本月</span>
      <strong>{{ monthTotalTokensText }}</strong>
      <small>Token</small>
    </div>
    <div class="mobile-token-divider"></div>
    <div class="mobile-token-item mobile-token-item--model">
      <span>模型</span>
      <strong>{{ currentModelText }}</strong>
    </div>
  </section>

  <section class="mobile-chat-card">
    <div ref="messageListRef" class="mobile-message-list">
      <div v-if="state.loading" class="mobile-empty-state">
        正在准备会话...
      </div>

      <div v-else-if="state.errorMessage" class="mobile-empty-state mobile-empty-state--warning">
        {{ state.errorMessage }}
      </div>

      <template v-else-if="state.messageList.length">
        <article
          v-for="item in state.messageList"
          :key="item.id"
          class="mobile-message"
          :class="item.messageRole === 'user' ? 'mobile-message--user' : 'mobile-message--assistant'"
        >
          <div class="mobile-message__role">
            {{ item.messageRole === 'user' ? '我' : '政策咨询助手' }}
          </div>

          <div class="mobile-message__bubble">
            <div class="mobile-message__text">
              <span>{{ item.messageText }}</span>
              <span v-if="item.isStreaming" class="stream-cursor"></span>
            </div>

            <div v-if="item.messageRole !== 'user' && formatCitations(item).length" class="mobile-message__meta">
              依据：{{ formatCitations(item).join(' / ') }}
            </div>
          </div>
        </article>
      </template>

      <div v-else class="mobile-welcome">
        <div class="mobile-welcome__title">您好，我可以帮您查询人才政策</div>
        <div class="mobile-welcome__text">
          可以直接咨询补助标准、申报条件、办理流程、材料清单和政策依据。
        </div>
        <div class="quick-question-list">
          <button
            v-for="item in quickQuestions"
            :key="item"
            type="button"
            class="quick-question"
            @click="handleQuickQuestion(item)"
          >
            {{ item }}
          </button>
        </div>
      </div>

      <article v-if="state.asking && !hasStreamingMessage" class="mobile-message mobile-message--assistant">
        <div class="mobile-message__role">政策咨询助手</div>
        <div class="mobile-message__bubble">
          <div class="mobile-message__text">正在查询政策依据...</div>
        </div>
      </article>
    </div>
  </section>

  <div class="mobile-composer-wrap">
    <div v-if="policySuggestions.length" class="mobile-suggest-list">
      <button
        v-for="item in policySuggestions"
        :key="item.intentId"
        type="button"
        class="mobile-suggest-chip"
        @click="applyPolicySuggestion(item)"
      >
        {{ item.standardQuestion || item.intentName }}
      </button>
    </div>

    <div class="mobile-composer">
      <textarea
  v-model="state.question"
  class="mobile-question-input"
  rows="1"
  maxlength="500"
  placeholder="请输入人才政策问题"
  :disabled="state.asking || !permissionFlags.canUseAgent"
  @focus="handleComposerFocus"
  @blur="handleComposerBlur"
  @input="handleQuestionInput"
  @keydown.enter.exact.prevent="handleSend"
></textarea>

      <button type="button" class="mobile-send-button" :disabled="!canSend" @click="handleSend">
        {{ state.asking ? '中...' : '发送' }}
      </button>
    </div>
  </div>

  <div v-if="state.showHistoryDrawer" class="mobile-history-mask" @click.self="closeHistoryDrawer">
    <section class="mobile-history-panel">
      <div class="mobile-history-head">
        <div>
          <div class="mobile-history-title">历史主题</div>
          <div class="mobile-history-subtitle">点击后继续查看之前的问答</div>
        </div>
        <button type="button" class="mobile-history-close" @click="closeHistoryDrawer">关闭</button>
      </div>

      <div v-if="state.sessionList.length" class="mobile-history-list">
        <button
          v-for="item in state.sessionList"
          :key="item.id"
          type="button"
          class="mobile-history-item"
          :disabled="state.asking"
          :class="{ 'mobile-history-item--active': state.sessionInfo?.id === item.id && state.sessionPinned }"
          @click="selectSession(item)"
        >
          <span>{{ item.sessionTitle || `会话 #${item.id}` }}</span>
          <small>{{ formatDateTime(item.lastMessageTime || item.createTime) }}</small>
        </button>
      </div>

      <div v-else class="mobile-empty-state">
        暂无历史主题
      </div>
    </section>
    </div>

      </template>
    </div>

    <MobileTabBar
  v-if="showBottomTabbar"
  :items="mobileNavItems"
/>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { showToast } from 'vant'
import { useRouter } from 'vue-router'

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
const questionInputRef = ref(null)
const activeStreamContext = ref(null)
const sendRunId = ref(0)
const isComponentUnmounted = ref(false)
const FUNCTION_BINDING = getAgentFunctionBinding('talent_policy_consult')
const SOURCE_SCENE = 'MOBILE_POLICY_CONSULTANT'
const POLICY_BASE_ID = FUNCTION_BINDING?.defaultBaseId ?? 1
const POLICY_SUGGEST_MIN_LENGTH = 1
const POLICY_SUGGEST_MAX_LENGTH = 30
let policySuggestTimer = null
let policySuggestSeq = 0

const state = reactive({
  loading: false,
  asking: false,
  errorMessage: '',
  question: '',
  showHistoryDrawer: false,
  composerFocused: false,
  selectedPolicySuggestion: null,
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
const showBottomTabbar = computed(() => showMobileTabbar.value && !state.composerFocused)
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
const currentTotalTokensText = computed(() => {
  const meta = state.lastChatMeta || {}
  const total = Number(meta.totalTokens || 0)
  return total > 0 ? `${total}` : '0'
})

const monthTotalTokensText = computed(() => {
  const meta = state.lastChatMeta || {}
  const total = Number(meta.monthTotalTokens || 0)
  return total > 0 ? `${total}` : '0'
})

const currentModelText = computed(() => {
  const meta = state.lastChatMeta || {}
  return meta.modelCode || state.sessionInfo?.modelCode || '未配置'
})

const quickQuestions = computed(() => [
  '双百计划补助标准是什么',
  '住房补贴怎么申请',
  '福建省百人计划补助多少',
  '人工智能人才有什么支持',
  '人才服务保障包括什么'
])
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
  return stripSkillMentions(question).trim()
}

function compactQuestionText(value) {
  return String(value || '')
    .replace(/\s+/g, '')
    .replace(/[？?。！!，,、；;]/g, '')
    .trim()
}

function clearSelectedPolicySuggestion() {
  state.selectedPolicySuggestion = null
}

function getSuggestionStandardQuestion(item) {
  return String(
    item?.standardQuestion ||
    item?.standard_question ||
    item?.intentName ||
    item?.question ||
    ''
  ).trim()
}

function isSelectedPolicySuggestionStillValid(question) {
  if (!state.selectedPolicySuggestion) {
    return false
  }
  const currentQuestion = compactQuestionText(question)
  const selectedQuestion = compactQuestionText(getSuggestionStandardQuestion(state.selectedPolicySuggestion))
  return Boolean(currentQuestion && selectedQuestion && currentQuestion === selectedQuestion)
}

function buildPolicyIntentPayload(question) {
  if (!isSelectedPolicySuggestionStillValid(question)) {
    clearSelectedPolicySuggestion()
    return {}
  }

  const item = state.selectedPolicySuggestion
  return {
    intentId: item?.intentId || item?.intent_id || item?.id || undefined,
    policyKey: item?.policyKey || item?.policy_key || undefined,
    topicType: item?.topicType || item?.topic_type || undefined,
    regionScope: item?.regionScope || item?.region_scope || undefined
  }
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
async function openHistoryDrawer() {
  state.showHistoryDrawer = true
  try {
    await loadSessions()
  } catch (error) {
    console.warn('load history failed', error)
  }
}

function closeHistoryDrawer() {
  state.showHistoryDrawer = false
}

async function handleQuickQuestion(text) {
  state.question = text
  clearSelectedPolicySuggestion()
  clearPolicySuggestions()
  schedulePolicySuggest()
  await nextTick()
  questionInputRef.value?.focus?.()
}
function handleComposerFocus() {
  state.composerFocused = true
  schedulePolicySuggest()
  window.setTimeout(() => {
    scrollToBottom()
  }, 120)
}

function handleComposerBlur() {
  window.setTimeout(() => {
    state.composerFocused = false
  }, 180)
}
function handleQuestionInput() {
  if (state.selectedPolicySuggestion && !isSelectedPolicySuggestionStillValid(state.question)) {
    clearSelectedPolicySuggestion()
  }
  schedulePolicySuggest()
}

async function applyPolicySuggestion(item) {
  const standardQuestion = getSuggestionStandardQuestion(item)
  state.question = standardQuestion
  state.mentionKeyword = ''
  state.selectedPolicySuggestion = {
    ...item,
    standardQuestion
  }

  const suggestLogId = state.policySuggestLogId
  const selectedIntentId = item?.intentId || item?.intent_id || item?.id
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

// 注意：新问题自动创建的会话不要默认 pin。
// 只有用户点击“会话入口”里的已有会话，才表示继续追问。
// 否则下一次提问应按新问题重新路由，避免复用上一轮政策意图。
state.sessionPinned = false

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
  state.showHistoryDrawer = false
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
  clearSelectedPolicySuggestion()
  
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
const policyIntentPayload = buildPolicyIntentPayload(cleanQuestion)
const runId = ++sendRunId.value
  const streamContext = createStreamContext(runId)
  state.question = ''
  clearPolicySuggestions()

  state.asking = true
  state.errorMessage = ''
  let localIds = null
  activeStreamContext.value = streamContext

  try {
    await createOrReuseSession({ question: cleanQuestion, skillHint, ...policyIntentPayload })
    localIds = appendLocalConversation(cleanQuestion)
    await scrollToBottom()
    await streamAgentQuestion({
  sessionId: state.sessionInfo.id,
  question: cleanQuestion,
  sourceScene: SOURCE_SCENE,
  skillHint: skillHint || undefined,
  ...policyIntentPayload
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
clearSelectedPolicySuggestion()
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
  skillHint: skillHint || undefined,
  ...policyIntentPayload
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
        clearSelectedPolicySuggestion()
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
  --tabbar-space: 0px;
  --composer-bottom: env(safe-area-inset-bottom);
  height: 100vh;
  height: 100dvh;
  min-height: 100vh;
  padding: 10px 12px 0;
  overflow: hidden;
  box-sizing: border-box;
  background: linear-gradient(180deg, #f8fafc 0%, #eef4ff 100%);
}

.policy-page--with-tabbar {
  --tabbar-space: 84px;
  --composer-bottom: calc(84px + env(safe-area-inset-bottom));
}

.policy-page--keyboard {
  --tabbar-space: 0px;
  --composer-bottom: env(safe-area-inset-bottom);
}


.policy-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  max-width: 920px;
  margin: 0 auto;
  overflow: hidden;
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
.mobile-policy-header {
  position: sticky;
  top: 0;
  z-index: 20;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 4px 10px;
  background: linear-gradient(180deg, #f8fafc 0%, rgba(248, 250, 252, 0.92) 100%);
  backdrop-filter: blur(12px);
}

.mobile-policy-title {
  color: #0f172a;
  font-size: 20px;
  font-weight: 800;
  line-height: 1.2;
}

.mobile-policy-subtitle {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.mobile-policy-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mobile-icon-button {
  height: 32px;
  padding: 0 10px;
  border: 1px solid #dbe4f0;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.mobile-icon-button--primary {
  border-color: #2563eb;
  background: #2563eb;
  color: #fff;
}

.mobile-token-card {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto 1.2fr;
  align-items: center;
  gap: 10px;
  margin: 4px 0 12px;
  padding: 10px 12px;
  border: 1px solid #dbeafe;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: 0 10px 28px rgba(37, 99, 235, 0.08);
}

.mobile-token-item {
  min-width: 0;
  text-align: center;
}

.mobile-token-item span,
.mobile-token-item small {
  display: block;
  color: #64748b;
  font-size: 11px;
  line-height: 1.2;
}

.mobile-token-item strong {
  display: block;
  margin: 3px 0;
  color: #1d4ed8;
  font-size: 15px;
  font-weight: 800;
  word-break: break-all;
}

.mobile-token-item--model strong {
  font-size: 12px;
}

.mobile-token-divider {
  width: 1px;
  height: 28px;
  background: #e2e8f0;
}

.mobile-chat-card {
  flex: 1;
  min-height: 0;
  overflow: hidden;
  padding-bottom: calc(92px + var(--tabbar-space));
}

.mobile-message-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  height: 100%;
  min-height: 0;
  max-height: none;
  overflow-y: auto;
  padding: 4px 0 20px;
  scroll-behavior: smooth;
  -webkit-overflow-scrolling: touch;
}

.mobile-message {
  display: flex;
  flex-direction: column;
  max-width: 86%;
}

.mobile-message--user {
  align-self: flex-end;
  align-items: flex-end;
}

.mobile-message--assistant {
  align-self: flex-start;
  align-items: flex-start;
}

.mobile-message__role {
  margin: 0 8px 5px;
  color: #94a3b8;
  font-size: 11px;
  font-weight: 700;
}

.mobile-message__bubble {
  padding: 12px 14px;
  border-radius: 18px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.06);
}

.mobile-message--user .mobile-message__bubble {
  border-bottom-right-radius: 6px;
  background: linear-gradient(135deg, #2563eb, #1d4ed8);
  color: #fff;
}

.mobile-message--assistant .mobile-message__bubble {
  border: 1px solid #e2e8f0;
  border-bottom-left-radius: 6px;
  background: rgba(255, 255, 255, 0.96);
  color: #0f172a;
}

.mobile-message__text {
  white-space: pre-wrap;
  font-size: 15px;
  line-height: 1.75;
}

.mobile-message__meta {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #dbe4f0;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.mobile-empty-state,
.mobile-welcome {
  margin: auto 0;
  padding: 24px 18px;
  border: 1px solid #dbeafe;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.9);
  color: #64748b;
  text-align: center;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.05);
}

.mobile-empty-state--warning {
  border-color: #fecaca;
  background: #fff7f7;
  color: #b91c1c;
}

.mobile-welcome__title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

.mobile-welcome__text {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
}

.quick-question-list {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 8px;
  margin-top: 16px;
}

.quick-question {
  padding: 8px 11px;
  border: 1px solid #bfdbfe;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 700;
}

.mobile-composer-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: var(--composer-bottom);
  z-index: 40;
  padding: 8px 12px calc(10px + env(safe-area-inset-bottom));
  background: rgba(248, 250, 252, 0.96);
  border-top: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 -8px 28px rgba(15, 23, 42, 0.08);
  backdrop-filter: blur(16px);
}

.mobile-suggest-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: 32vh;
  margin-bottom: 8px;
  padding: 10px;
  overflow-y: auto;
  border: 1px solid #dbeafe;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.98);
  box-shadow: 0 -10px 30px rgba(15, 23, 42, 0.08);
  -webkit-overflow-scrolling: touch;
}

.mobile-suggest-chip {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0ecff;
  border-radius: 14px;
  background: #f8fbff;
  color: #1d4ed8;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.45;
  text-align: left;
}

.mobile-composer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  max-width: 920px;
  margin: 0 auto;
}

.mobile-question-input {
  flex: 1;
  min-height: 46px;
  max-height: 92px;
  padding: 12px 14px;
  border: 1px solid #cbd5e1;
  border-radius: 20px;
  background: #fff;
  color: #0f172a;
  font-size: 16px;
  line-height: 1.45;
  resize: none;
  outline: none;
  box-sizing: border-box;
}

.mobile-question-input:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.14);
}

.mobile-send-button {
  width: 66px;
  min-width: 66px;
  height: 46px;
  border: none;
  border-radius: 20px;
  background: #2563eb;
  color: #fff;
  font-size: 15px;
  font-weight: 800;
}

.mobile-send-button:disabled {
  background: #cbd5e1;
  color: #f8fafc;
}


.mobile-history-mask {
  position: fixed;
  inset: 0;
  z-index: 50;
  display: flex;
  align-items: flex-end;
  background: rgba(15, 23, 42, 0.38);
}

.mobile-history-panel {
  width: 100%;
  max-height: 72vh;
  padding: 16px 16px calc(18px + env(safe-area-inset-bottom));
  border-radius: 24px 24px 0 0;
  background: #fff;
  box-shadow: 0 -18px 48px rgba(15, 23, 42, 0.16);
  overflow-y: auto;
}

.mobile-history-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.mobile-history-title {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

.mobile-history-subtitle {
  margin-top: 3px;
  color: #64748b;
  font-size: 12px;
}

.mobile-history-close {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #e2e8f0;
  border-radius: 999px;
  background: #f8fafc;
  color: #334155;
  font-size: 12px;
  font-weight: 700;
}

.mobile-history-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.mobile-history-item {
  width: 100%;
  padding: 12px 13px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #f8fafc;
  text-align: left;
}

.mobile-history-item span {
  display: block;
  color: #0f172a;
  font-size: 14px;
  font-weight: 800;
}

.mobile-history-item small {
  display: block;
  margin-top: 4px;
  color: #64748b;
  font-size: 12px;
}

.mobile-history-item--active {
  border-color: #2563eb;
  background: #eff6ff;
}

@media (min-width: 768px) {
  .mobile-composer-wrap {
    left: 50%;
    max-width: 920px;
    transform: translateX(-50%);
    border-left: 1px solid rgba(226, 232, 240, 0.9);
    border-right: 1px solid rgba(226, 232, 240, 0.9);
    border-radius: 18px 18px 0 0;
  }

  .mobile-message-list {
    max-height: 62vh;
  }
}
</style>

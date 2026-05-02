<template>
  <div
  class="policy-page"
  :class="{
    'policy-page--with-tabbar': showBottomTabbar,
    'policy-page--keyboard': state.composerFocused,
    'policy-page--chat': state.messageList.length
  }"
>
    <div class="policy-shell">
      <section v-if="!hasToken" class="panel panel--empty">
        <h1>政策智能体</h1>
        <p>需要先登录后再进入智能咨询链路。</p>
        <button type="button" class="primary-button" @click="goToLogin">去登录</button>
      </section>

      <template v-else>
  <section class="mobile-policy-header" :class="{ 'mobile-policy-header--compact': state.messageList.length }">
    <div class="mobile-policy-hero">
      <div v-if="!state.messageList.length" class="mobile-policy-eyebrow">AI Policy Agent · 政策匹配 / 申报指引 / 依据追溯 / 智能纠错</div>
      <div class="mobile-policy-title">政策智能体</div>
      <div v-if="!state.messageList.length" class="mobile-policy-subtitle">面向人才政策、产业政策与申报服务的智能咨询助手</div>
      <div v-if="!state.messageList.length" class="mobile-policy-status-row">
        <span>政策知识库已连接</span>
        <span>专题卡优先</span>
        <span>依据可追溯</span>
        <span>人审纠错闭环</span>
      </div>
    </div>
    <div class="mobile-policy-actions">
      <div class="mobile-policy-action-row">
        <button type="button" class="mobile-icon-button" @click="openHistoryDrawer">历史</button>
        <button type="button" class="mobile-icon-button mobile-icon-button--primary" :disabled="state.asking" @click="handleResetSession">
          新会话
        </button>
      </div>
      <div v-if="state.messageList.length" class="mobile-policy-usage-line">
        本次 {{ currentTotalTokensText }} Token · 本月 {{ monthTotalTokensText }} Token · {{ currentModelText }}
      </div>
    </div>
  </section>

  <section v-if="!state.messageList.length" class="mobile-token-card">
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
          <div v-if="item.messageRole !== 'user' && !item.isStreaming" class="mobile-feedback">
            <div v-if="feedbackSubmitted(item)" class="mobile-feedback__done">
              已收到反馈，我们会用于优化政策智能体。
            </div>
            <template v-else>
              <button
                v-for="option in feedbackOptions"
                :key="option.type"
                type="button"
                class="mobile-feedback__chip"
                :disabled="isFeedbackSubmitting(item)"
                @click="handleFeedbackClick(item, option)"
              >
                {{ option.label }}
              </button>
            </template>
          </div>

          <div v-if="state.feedbackDraft.messageId === item.id && !feedbackSubmitted(item)" class="mobile-feedback-editor">
            <div class="mobile-feedback-editor__title">{{ state.feedbackDraft.label }}，请补充说明</div>
            <textarea
              v-model="state.feedbackDraft.content"
              class="mobile-feedback-editor__input"
              rows="3"
              maxlength="300"
              placeholder="例如：依据不够明确、材料清单缺项、政策可能已更新..."
            ></textarea>
            <div class="mobile-feedback-editor__actions">
              <button type="button" class="mobile-feedback-editor__cancel" :disabled="state.feedbackSubmittingMessageId" @click="cancelFeedbackDraft">
                取消
              </button>
              <button type="button" class="mobile-feedback-editor__submit" :disabled="state.feedbackSubmittingMessageId" @click="submitFeedbackDraft">
                {{ state.feedbackSubmittingMessageId ? '提交中...' : '提交反馈' }}
              </button>
            </div>
          </div>
        </article>
      </template>

      <div v-else class="mobile-welcome">
        <div class="mobile-welcome__title">您好，我是政策智能体</div>
        <div class="mobile-welcome__text">
          可以直接咨询政策匹配、申报流程、材料清单、省市边界和政策依据；涉及动态事项会提示以正式通知为准。
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
  placeholder="请输入政策问题，例如：双百计划怎么申请、金融服务产业人才项目需要什么材料"
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
  submitPolicyFeedback,
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
const forceCreateNextSession = ref(false)
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
  policySuggestItems: [],
  feedbackSubmitted: {},
  feedbackSubmittingMessageId: null,
  feedbackDraft: {
    messageId: null,
    type: '',
    label: '',
    content: ''
  }
})

const feedbackOptions = [
  { type: 'HELPFUL', label: '有帮助', direct: true },
  { type: 'INACCURATE', label: '不准确' },
  { type: 'MISSING_EVIDENCE', label: '缺少依据' },
  { type: 'INCOMPLETE_MATERIAL', label: '材料不完整' },
  { type: 'POLICY_UPDATED', label: '政策已更新' },
  { type: 'OTHER', label: '其他' }
]

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
  '我可以申请哪些政策？',
  '双百计划怎么申请？',
  '金融服务产业人才项目需要什么材料？',
  '博士后工作站怎么申请？',
  '厦门政策和福建政策有什么区别？'
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

function feedbackKey(message) {
  return String(message?.id || '')
}

function feedbackSubmitted(message) {
  const key = feedbackKey(message)
  return Boolean(key && state.feedbackSubmitted[key])
}

function isFeedbackSubmitting(message) {
  return state.feedbackSubmittingMessageId === feedbackKey(message)
}

function handleFeedbackClick(message, option) {
  if (!message || !option || feedbackSubmitted(message) || isFeedbackSubmitting(message)) {
    return
  }
  if (option.direct) {
    submitMessageFeedback(message, option.type, '')
    return
  }
  state.feedbackDraft = {
    messageId: message.id,
    type: option.type,
    label: option.label,
    content: ''
  }
}

function cancelFeedbackDraft() {
  state.feedbackDraft = {
    messageId: null,
    type: '',
    label: '',
    content: ''
  }
}

function clearFeedbackState() {
  state.feedbackSubmitted = {}
  state.feedbackSubmittingMessageId = null
  cancelFeedbackDraft()
}

function submitFeedbackDraft() {
  const message = findMessageById(state.feedbackDraft.messageId)
  if (!message) {
    cancelFeedbackDraft()
    return
  }
  submitMessageFeedback(message, state.feedbackDraft.type, state.feedbackDraft.content)
}

async function submitMessageFeedback(message, feedbackType, feedbackContent) {
  const key = feedbackKey(message)
  if (!key || state.feedbackSubmitted[key]) {
    return
  }
  state.feedbackSubmittingMessageId = key
  try {
    ensureSuccess(await submitPolicyFeedback(buildFeedbackPayload(message, feedbackType, feedbackContent)), '提交反馈失败')
    state.feedbackSubmitted[key] = true
    cancelFeedbackDraft()
    showToast('反馈已提交，我们会用于优化政策智能体')
  } catch (error) {
    showToast(error.message || '反馈提交失败，请稍后重试')
  } finally {
    state.feedbackSubmittingMessageId = null
  }
}

function buildFeedbackPayload(message, feedbackType, feedbackContent) {
  return {
    baseId: POLICY_BASE_ID,
    userId: userStore.userInfo?.userId || userStore.userInfo?.id || null,
    sessionId: state.sessionInfo?.id || null,
    messageId: typeof message?.id === 'number' ? message.id : null,
    traceId: message?.traceId || state.lastChatMeta?.traceId || '',
    question: findPreviousUserQuestion(message),
    answer: message?.messageText || '',
    feedbackType,
    feedbackContent: feedbackContent || '',
    submitContext: true,
    policyKey: message?.policyKey || state.lastChatMeta?.policyKey || '',
    topicType: message?.topicType || state.lastChatMeta?.topicType || '',
    questionType: message?.questionType || state.lastChatMeta?.questionType || '',
    evidenceIds: formatEvidenceIds(message),
    evidenceSource: formatCitations(message).join(' / '),
    intentId: message?.intentId || state.lastChatMeta?.intentId || null
  }
}

function findPreviousUserQuestion(message) {
  const index = state.messageList.findIndex((item) => item.id === message?.id)
  for (let i = index - 1; i >= 0; i -= 1) {
    if (state.messageList[i]?.messageRole === 'user') {
      return state.messageList[i].messageText || ''
    }
  }
  return state.question || ''
}

function formatEvidenceIds(message) {
  if (Array.isArray(message?.citedChunkIdList) && message.citedChunkIdList.length) {
    return message.citedChunkIdList.join(',')
  }
  if (Array.isArray(message?.evidenceIds) && message.evidenceIds.length) {
    return message.evidenceIds.join(',')
  }
  return ''
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
  if (message) {
    message.traceId = payload?.traceId || message.traceId || ''
    message.policyKey = payload?.policyKey || message.policyKey || ''
    message.topicType = payload?.topicType || message.topicType || ''
    message.questionType = payload?.questionType || message.questionType || ''
    message.intentId = payload?.intentId || message.intentId || null
    message.evidenceIds = Array.isArray(payload?.evidenceIds) ? payload.evidenceIds : message.evidenceIds
  }
  state.lastChatMeta = {
    ...(state.lastChatMeta || {}),
    answerSource: payload?.answerSource || '',
    promptTokens: Number(payload?.promptTokens ?? 0),
    completionTokens: Number(payload?.completionTokens ?? 0),
    totalTokens: Number(payload?.totalTokens ?? 0),
    monthTotalTokens: Number(payload?.monthTotalTokens ?? 0),
    durationMs: Number(payload?.durationMs ?? 0),
    modelCode: payload?.modelCode || '',
    traceId: payload?.traceId || state.lastChatMeta?.traceId || '',
    policyKey: payload?.policyKey || state.lastChatMeta?.policyKey || '',
    topicType: payload?.topicType || state.lastChatMeta?.topicType || '',
    questionType: payload?.questionType || state.lastChatMeta?.questionType || '',
    intentId: payload?.intentId || state.lastChatMeta?.intentId || null
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
  if (!state.sessionInfo?.id && state.sessionList.length) {
  state.sessionInfo = state.sessionList[0]
  state.sessionPinned = true
  syncActiveSkillFromSession(state.sessionInfo, resolveFunctionBoundSkill())
  await fetchMessages()
}
}

async function createOrReuseSession(payload) {
  const explicitSkill = resolveFunctionBoundSkill()

  if (!explicitSkill?.id) {
    throw new Error(`当前功能缺少默认专家绑定，请确认 ${FUNCTION_BINDING?.defaultSkillCode || 'talent_policy_consultant'} 已发布且当前账号可用。`)
  }

  /*
   * 核心规则：
   * 只要用户没有主动点击“新会话”，就一直复用当前 session。
   * 这样同一个主题内的上下文才能连续，不能每问一句就新建主题。
   */
  if (!forceCreateNextSession.value && state.sessionInfo?.id) {
    syncActiveSkillFromSession(state.sessionInfo, explicitSkill)
    return state.sessionInfo
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
  forceCreateNextSession.value = false

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
  forceCreateNextSession.value = false
  state.showHistoryDrawer = false
  cancelFeedbackDraft()
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
  forceCreateNextSession.value = true
  state.lastChatMeta = null
  state.messageList = []
  state.question = ''
  clearFeedbackState()
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
  --agent-navy: #061a3d;
  --agent-blue: #1d4ed8;
  --agent-cyan: #22d3ee;
  --agent-violet: #7c3aed;
  height: 100vh;
  height: 100dvh;
  min-height: 100vh;
  padding: 10px 12px 0;
  overflow: hidden;
  box-sizing: border-box;
  background:
    radial-gradient(circle at 12% 0%, rgba(34, 211, 238, 0.34), transparent 28%),
    radial-gradient(circle at 88% 12%, rgba(124, 58, 237, 0.28), transparent 30%),
    linear-gradient(180deg, #061a3d 0%, #0f2f68 38%, #eff6ff 100%);
}

.policy-page--with-tabbar {
  --tabbar-space: 84px;
  --composer-bottom: calc(84px + env(safe-area-inset-bottom));
}

.policy-page--keyboard {
  --tabbar-space: 0px;
  --composer-bottom: env(safe-area-inset-bottom);
}

.policy-page--chat {
  padding-top: 8px;
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
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 10px;
  padding: 18px 16px 16px;
  overflow: hidden;
  border: 1px solid rgba(147, 197, 253, 0.28);
  border-radius: 26px;
  background:
    radial-gradient(circle at 85% 12%, rgba(34, 211, 238, 0.28), transparent 34%),
    linear-gradient(135deg, rgba(8, 26, 61, 0.96), rgba(30, 64, 175, 0.88) 55%, rgba(88, 28, 135, 0.86));
  box-shadow: 0 22px 54px rgba(6, 26, 61, 0.28);
  backdrop-filter: blur(18px);
  transition: padding 0.22s ease, border-radius 0.22s ease, min-height 0.22s ease, box-shadow 0.22s ease;
}

.mobile-policy-header::before {
  content: '';
  position: absolute;
  inset: -45% auto auto -18%;
  width: 220px;
  height: 220px;
  border-radius: 999px;
  background: rgba(34, 211, 238, 0.2);
  filter: blur(2px);
  animation: agent-glow 6s ease-in-out infinite alternate;
}

.mobile-policy-header--compact {
  min-height: 66px;
  align-items: center;
  margin-bottom: 8px;
  padding: 12px 12px;
  border-radius: 22px;
  box-shadow: 0 14px 36px rgba(6, 26, 61, 0.24);
}

.mobile-policy-header--compact::before {
  inset: -90px auto auto -90px;
  width: 170px;
  height: 170px;
  opacity: 0.72;
}

.mobile-policy-hero {
  position: relative;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.mobile-policy-eyebrow {
  display: inline-flex;
  max-width: 100%;
  color: rgba(224, 242, 254, 0.88);
  font-size: 11px;
  font-weight: 800;
  line-height: 1.4;
  letter-spacing: 0.02em;
}

.mobile-policy-title {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 9px;
  margin-top: 8px;
  color: #f8fbff;
  font-size: clamp(26px, 7vw, 38px);
  font-weight: 900;
  line-height: 1.05;
  letter-spacing: 0.02em;
}

.mobile-policy-title::before {
  content: '';
  width: 8px;
  height: 8px;
  flex: 0 0 auto;
  border-radius: 999px;
  background: #67e8f9;
  box-shadow: 0 0 0 4px rgba(34, 211, 238, 0.14), 0 0 18px rgba(103, 232, 249, 0.95);
  animation: agent-pulse 2.4s ease-in-out infinite;
}

.mobile-policy-header--compact .mobile-policy-title {
  margin-top: 0;
  font-size: clamp(24px, 6.4vw, 28px);
  font-weight: 800;
  letter-spacing: 0.01em;
  text-shadow: 0 0 18px rgba(147, 197, 253, 0.32);
}

.mobile-policy-header--compact .mobile-policy-title::before {
  width: 7px;
  height: 7px;
}

.mobile-policy-subtitle {
  margin-top: 8px;
  color: rgba(219, 234, 254, 0.92);
  font-size: 13px;
  line-height: 1.55;
}

.mobile-policy-status-row {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: 12px;
}

.mobile-policy-status-row span {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 9px;
  border: 1px solid rgba(186, 230, 253, 0.3);
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.18);
  color: #e0f2fe;
  font-size: 11px;
  font-weight: 800;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.12);
}

.mobile-policy-actions {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
  flex-shrink: 0;
}

.mobile-policy-action-row {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.mobile-icon-button {
  height: 32px;
  padding: 0 10px;
  border: 1px solid rgba(191, 219, 254, 0.32);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #eff6ff;
  font-size: 12px;
  font-weight: 700;
  backdrop-filter: blur(10px);
}

.mobile-policy-usage-line {
  max-width: 180px;
  color: rgba(219, 234, 254, 0.74);
  font-size: 10.5px;
  font-weight: 700;
  line-height: 1.25;
  text-align: right;
  white-space: nowrap;
  transform: translateY(-1px);
}

.mobile-icon-button--primary {
  border-color: rgba(34, 211, 238, 0.68);
  background: linear-gradient(135deg, #0891b2, #2563eb);
  color: #fff;
}

.mobile-token-card {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto 1.2fr;
  align-items: center;
  gap: 10px;
  margin: 0 0 12px;
  padding: 11px 12px;
  border: 1px solid rgba(147, 197, 253, 0.34);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: 0 16px 38px rgba(15, 23, 42, 0.12);
  backdrop-filter: blur(16px);
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
  color: #0f3f9f;
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

.policy-page--chat .mobile-chat-card {
  padding-bottom: calc(84px + var(--tabbar-space));
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

.policy-page--chat .mobile-message {
  max-width: 94%;
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

.mobile-feedback {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  max-width: 100%;
  margin: 7px 8px 0;
}

.mobile-feedback__chip,
.mobile-feedback-editor__cancel,
.mobile-feedback-editor__submit {
  border: 1px solid rgba(147, 197, 253, 0.42);
  border-radius: 999px;
  font: inherit;
  font-size: 11px;
  font-weight: 800;
}

.mobile-feedback__chip {
  min-height: 26px;
  padding: 0 9px;
  background: rgba(239, 246, 255, 0.86);
  color: #1d4ed8;
}

.mobile-feedback__chip:disabled {
  opacity: 0.58;
}

.mobile-feedback__done {
  padding: 5px 9px;
  border-radius: 999px;
  background: rgba(236, 254, 255, 0.86);
  color: #0e7490;
  font-size: 11px;
  font-weight: 800;
}

.mobile-feedback-editor {
  width: min(100%, 420px);
  margin: 8px 8px 0;
  padding: 10px;
  border: 1px solid rgba(147, 197, 253, 0.38);
  border-radius: 16px;
  background: rgba(248, 251, 255, 0.95);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
}

.mobile-feedback-editor__title {
  color: #334155;
  font-size: 12px;
  font-weight: 800;
}

.mobile-feedback-editor__input {
  width: 100%;
  margin-top: 8px;
  padding: 9px 10px;
  border: 1px solid #dbeafe;
  border-radius: 12px;
  background: #fff;
  color: #0f172a;
  font: inherit;
  font-size: 13px;
  line-height: 1.5;
  resize: vertical;
  box-sizing: border-box;
}

.mobile-feedback-editor__actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.mobile-feedback-editor__cancel,
.mobile-feedback-editor__submit {
  min-height: 28px;
  padding: 0 12px;
}

.mobile-feedback-editor__cancel {
  background: #fff;
  color: #475569;
}

.mobile-feedback-editor__submit {
  border-color: rgba(34, 211, 238, 0.55);
  background: linear-gradient(135deg, #0891b2, #2563eb);
  color: #fff;
}

.mobile-empty-state,
.mobile-welcome {
  margin: auto 0;
  padding: 24px 18px;
  border: 1px solid rgba(147, 197, 253, 0.5);
  border-radius: 22px;
  background:
    radial-gradient(circle at 20% 0%, rgba(34, 211, 238, 0.16), transparent 32%),
    rgba(255, 255, 255, 0.9);
  color: #64748b;
  text-align: center;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.09);
  backdrop-filter: blur(14px);
}

.mobile-empty-state--warning {
  border-color: #fecaca;
  background: #fff7f7;
  color: #b91c1c;
}

.mobile-welcome__title {
  color: #0f172a;
  font-size: 19px;
  font-weight: 900;
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
  gap: 9px;
  margin-top: 16px;
}

.quick-question {
  padding: 9px 12px;
  border: 1px solid rgba(59, 130, 246, 0.24);
  border-radius: 999px;
  background: linear-gradient(135deg, rgba(239, 246, 255, 0.96), rgba(236, 254, 255, 0.96));
  color: #0f3f9f;
  font-size: 12px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(37, 99, 235, 0.08);
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.quick-question:active {
  transform: scale(0.98);
}

.mobile-composer-wrap {
  position: fixed;
  left: 0;
  right: 0;
  bottom: var(--composer-bottom);
  z-index: 40;
  padding: 8px 12px calc(10px + env(safe-area-inset-bottom));
  background: rgba(241, 247, 255, 0.88);
  border-top: 1px solid rgba(147, 197, 253, 0.32);
  box-shadow: 0 -14px 36px rgba(15, 23, 42, 0.12);
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
  border: 1px solid rgba(37, 99, 235, 0.18);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.96);
  color: #0f172a;
  font-size: 16px;
  line-height: 1.45;
  resize: none;
  outline: none;
  box-sizing: border-box;
}

.mobile-question-input:focus {
  border-color: #0891b2;
  box-shadow: 0 0 0 3px rgba(34, 211, 238, 0.18), 0 10px 28px rgba(37, 99, 235, 0.08);
}

.mobile-send-button {
  width: 66px;
  min-width: 66px;
  height: 46px;
  border: none;
  border-radius: 20px;
  background: linear-gradient(135deg, #0891b2, #2563eb 58%, #4f46e5);
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

@keyframes agent-glow {
  from {
    transform: translate3d(0, 0, 0) scale(1);
    opacity: 0.76;
  }
  to {
    transform: translate3d(18px, 8px, 0) scale(1.08);
    opacity: 1;
  }
}

@keyframes agent-pulse {
  0%,
  100% {
    opacity: 0.72;
    transform: scale(0.92);
  }
  50% {
    opacity: 1;
    transform: scale(1.08);
  }
}
</style>

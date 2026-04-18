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
            <span class="summary-chip">{{ currentSkillLabel }}</span>
            <span class="summary-chip">{{ currentRouteLabel }}</span>
            <span class="summary-chip">{{ state.sessionInfo?.id ? `会话 #${state.sessionInfo.id}` : '未创建会话' }}</span>
          </div>
          <div class="summary-text">{{ helperText }}</div>
        </section>

        <section class="panel">
          <div class="panel-head">
            <div>
              <div class="panel-title">对话区</div>
              <div class="panel-subtitle">用户只管提问，后端统一决定 Skill、知识来源和 AI 调用。</div>
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
                <div class="message-bubble__text">{{ item.messageText }}</div>
                <div v-if="item.messageRole !== 'user' && formatCitations(item).length" class="message-bubble__meta">
                  引用：{{ formatCitations(item).join(' / ') }}
                </div>
              </article>
            </template>
            <div v-else class="empty-state">
              先直接问一个具体问题，例如“厦门市高层次人才住房补贴怎么申请？”
            </div>
            <article v-if="state.asking" class="message-bubble message-bubble--assistant">
              <div class="message-bubble__role">政策咨询助手</div>
              <div class="message-bubble__text">正在整理知识依据，请稍候...</div>
            </article>
          </div>
        </section>

        <section class="panel">
          <div class="panel-title">提问区</div>
          <textarea
            v-model="state.question"
            class="question-input"
            rows="4"
            maxlength="500"
            placeholder="输入问题，或用 @技能名 指定 Skill"
            :disabled="state.asking || !permissionFlags.canUseAgent"
            @input="handleQuestionInput"
            @keydown.enter.exact.prevent="handleSend"
          ></textarea>
          <div v-if="mentionSuggestions.length" class="mention-list">
            <button
              v-for="item in mentionSuggestions"
              :key="item.id"
              type="button"
              class="mention-item"
              @click="applyMention(item)"
            >
              <span>{{ item.skillName }}</span>
              <small>@{{ item.skillCode }}</small>
            </button>
          </div>
          <div class="composer-footer">
            <div class="composer-hint">
              输入 `@` 会提示可用 Skills；不写 `@` 时后端会自动匹配，匹配失败会走默认 Skill 或知识库兜底。
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
              <div class="panel-subtitle">点击已有会话继续追问；不点则默认按新问题重新路由。</div>
            </div>
            <button type="button" class="ghost-button" :disabled="state.loading" @click="loadSessions">刷新</button>
          </div>
          <div v-if="state.sessionList.length" class="session-list">
            <button
              v-for="item in state.sessionList"
              :key="item.id"
              type="button"
              class="session-item"
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
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { showToast } from 'vant'
import { useRouter } from 'vue-router'
import AIPageGuideCard from '@/components/ai/AIPageGuideCard.vue'
import MobileTabBar from '@/components/mobile/MobileTabBar.vue'
import { createAgentSession, queryAgentMessages, queryAgentSessions, sendAgentQuestion } from '@/api/agent'
import { queryCurrentAiPermission } from '@/api/ai'
import { querySkillList } from '@/api/skill'
import { useUserStore } from '@/stores/user'
import { buildAccessContext } from '@/constants/modules'
import { resolveMobileWorkspaceItems } from '@/constants/mobile-workspace'
import { isMobileClient } from '@/utils/device'

const router = useRouter()
const userStore = useUserStore()
const messageListRef = ref(null)
const SOURCE_SCENE = 'MOBILE_POLICY_CONSULTANT'

const state = reactive({
  loading: false,
  asking: false,
  errorMessage: '',
  question: '',
  permissionInfo: null,
  sessionInfo: null,
  sessionPinned: false,
  sessionList: [],
  messageList: [],
  skillOptions: [],
  mentionKeyword: '',
  lastChatMeta: null
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
const mentionSuggestions = computed(() => {
  if (!state.mentionKeyword.startsWith('@')) {
    return []
  }
  const keyword = state.mentionKeyword.slice(1).trim().toLowerCase()
  return availableSkills.value
    .filter((item) => {
      if (!keyword) {
        return true
      }
      return String(item.skillName || '').toLowerCase().includes(keyword)
        || String(item.skillCode || '').toLowerCase().includes(keyword)
    })
    .slice(0, 6)
})
const currentSkillLabel = computed(() => state.lastChatMeta?.skillName || state.sessionInfo?.skillName || '自动选择 Skill')
const currentRouteLabel = computed(() => formatMatchMode(state.lastChatMeta?.skillMatchMode || state.sessionInfo?.skillMatchMode))
const helperText = computed(() => {
  if (state.errorMessage) {
    return state.errorMessage
  }
  if (!permissionFlags.value.canUseAgent) {
    return '当前账号还没有 AI 主链问答权限，请先去 AI 权限配置开通。'
  }
  if (!permissionFlags.value.canUseAi) {
    return '当前链路会优先走知识库兜底回答，Provider 恢复后会自动切回 AI。'
  }
  if (state.lastChatMeta?.skillName) {
    return `本次问题命中 ${state.lastChatMeta.skillName}，路由方式：${formatMatchMode(state.lastChatMeta.skillMatchMode)}。`
  }
  return '当前链路已准备好，可直接发问；来源场景会自动标记为“手机端政策咨询”。'
})
const canSend = computed(() => permissionFlags.value.canUseAgent && Boolean(state.question.trim()) && !state.asking)

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

function extractSkillHint(question) {
  const match = String(question || '').match(/(?:^|\s)@([^\s@]+)/)
  return match ? match[1] : ''
}

function normalizeQuestion(question) {
  return String(question || '').replace(/(?:^|\s)@([^\s@]+)/, ' ').replace(/\s+/g, ' ').trim()
}

function handleQuestionInput() {
  const match = String(state.question || '').match(/(?:^|\s)(@[^\s@]*)$/)
  state.mentionKeyword = match ? match[1] : ''
}

function applyMention(skill) {
  const suffix = `@${skill.skillCode} `
  state.question = String(state.question || '').replace(/(?:^|\s)(@[^\s@]*)$/, ` ${suffix}`).trimStart()
  state.mentionKeyword = ''
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
  state.sessionList = Array.isArray(data) ? data : []
  if (!state.sessionPinned && !state.sessionInfo?.id && state.sessionList.length) {
    state.sessionInfo = state.sessionList[0]
    state.messageList = []
  }
}

async function createOrReuseSession(payload) {
  const explicitSkill = payload.skillHint
    ? availableSkills.value.find((item) => {
      const hint = payload.skillHint.toLowerCase()
      return String(item.skillCode || '').toLowerCase() === hint || String(item.skillName || '').toLowerCase() === hint
    })
    : null

  if (state.sessionPinned && state.sessionInfo?.id && (!explicitSkill || Number(state.sessionInfo.skillId) === Number(explicitSkill.id))) {
    return state.sessionInfo
  }

  if (explicitSkill) {
    const existing = state.sessionList.find((item) => Number(item.skillId) === Number(explicitSkill.id))
    if (existing?.id && state.sessionPinned) {
      state.sessionInfo = existing
      return existing
    }
  }

  const session = ensureSuccess(await createAgentSession({
    skillId: explicitSkill ? Number(explicitSkill.id) : undefined,
    skillHint: payload.skillHint || undefined,
    question: payload.question,
    sourceScene: SOURCE_SCENE
  }), '创建会话失败')
  state.sessionInfo = session
  state.sessionPinned = true
  state.lastChatMeta = {
    skillName: session?.skillName || '',
    skillMatchMode: session?.skillMatchMode || ''
  }
  await loadSessions()
  return session
}

async function selectSession(item) {
  state.sessionInfo = item
  state.sessionPinned = true
  state.lastChatMeta = {
    skillName: item?.skillName || '',
    skillMatchMode: 'SESSION_REUSE'
  }
  await fetchMessages()
}

async function handleResetSession() {
  state.sessionInfo = null
  state.sessionPinned = false
  state.lastChatMeta = null
  state.messageList = []
  state.question = ''
}

async function handleSend() {
  const rawQuestion = state.question.trim()
  if (!rawQuestion) {
    showToast('请输入问题')
    return
  }
  if (!permissionFlags.value.canUseAgent) {
    showToast(state.errorMessage || '当前账号还不能发起问答')
    return
  }

  const skillHint = extractSkillHint(rawQuestion)
  const cleanQuestion = normalizeQuestion(rawQuestion)

  state.asking = true
  try {
    await createOrReuseSession({ question: cleanQuestion, skillHint })
    const result = ensureSuccess(await sendAgentQuestion({
      sessionId: state.sessionInfo.id,
      question: cleanQuestion,
      sourceScene: SOURCE_SCENE,
      skillHint: skillHint || undefined
    }), '发送问题失败')
    state.lastChatMeta = {
      skillName: result?.skillName || state.sessionInfo?.skillName || '',
      skillMatchMode: result?.skillMatchMode || state.sessionInfo?.skillMatchMode || ''
    }
    if (state.sessionInfo) {
      state.sessionInfo.skillName = result?.skillName || state.sessionInfo.skillName
      state.sessionInfo.skillMatchMode = result?.skillMatchMode || state.sessionInfo.skillMatchMode
    }
    state.question = ''
    state.mentionKeyword = ''
    await Promise.all([fetchMessages(), loadSessions()])
  } catch (error) {
    showToast(error.message || '发送问题失败')
    state.errorMessage = error.message || '发送问题失败'
  } finally {
    state.asking = false
  }
}

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
      state.errorMessage = '当前账号还没有 AI 主链权限，请先去 AI 权限配置开通。'
      return
    }
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

.mention-list,
.session-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.mention-item,
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
.ghost-button:disabled {
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

@media (max-width: 768px) {
  .policy-page {
    padding: 12px;
  }

  .panel-head,
  .composer-footer {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

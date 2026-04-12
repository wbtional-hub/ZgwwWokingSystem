<template>
  <div class="policy-page" :class="{ 'policy-page--with-tabbar': showMobileTabbar }">
    <div class="policy-page__glow policy-page__glow--top"></div>
    <div class="policy-page__glow policy-page__glow--bottom"></div>

    <header class="hero">
      <div class="hero__topbar">
        <div class="hero__brand">
          <span class="hero__brand-mark">TP</span>
          <div>
            <div class="hero__brand-name">Talent Policy Desk</div>
            <div class="hero__brand-subtitle">人才政策咨询专家</div>
          </div>
        </div>
        <button
          v-if="hasToken"
          type="button"
          class="hero__top-action"
          @click="router.push(showMobileTabbar ? '/mobile-workspace' : '/ai-workbench')"
        >
          工作台
        </button>
      </div>

      <div class="hero__content">
        <p class="hero__eyebrow">Mobile Consultant</p>
        <h1>把复杂政策，翻译成客户能马上听懂的话。</h1>
        <p class="hero__summary">
          面向人才认定、补贴、住房、创业扶持等场景，提供简洁答复、适用条件与政策依据。
        </p>
      </div>

      <div class="hero__chips">
        <span class="hero__chip">{{ skillTitle }}</span>
        <span class="hero__chip">{{ connectionText }}</span>
        <span class="hero__chip">{{ sessionText }}</span>
      </div>

      <div class="quick-prompts">
        <button
          v-for="prompt in quickPrompts"
          :key="prompt"
          type="button"
          class="quick-prompts__item"
          :disabled="!canChat || state.asking"
          @click="handleQuickPrompt(prompt)"
        >
          {{ prompt }}
        </button>
      </div>
    </header>

    <main class="chat-shell">
      <section v-if="!hasToken" class="gate-panel">
        <div class="gate-panel__label">需要登录</div>
        <h2>先登录系统，再把这个页面发给客户演示。</h2>
        <p>
          当前手机端页面已经就绪，但后端问答仍依赖登录态与 AI 权限。登录后会自动接入已发布的政策 Skill。
        </p>
        <div class="gate-panel__actions">
          <button type="button" class="primary-button" @click="goToLogin">登录后进入咨询</button>
        </div>
      </section>

      <template v-else>
        <section class="conversation-card">
          <div class="conversation-card__header">
            <div>
              <div class="conversation-card__title">政策对话</div>
              <div class="conversation-card__meta">{{ helperText }}</div>
            </div>
            <button
              type="button"
              class="ghost-button"
              :disabled="state.loading || state.asking || !state.skill"
              @click="handleResetSession"
            >
              新会话
            </button>
          </div>

          <div ref="messageListRef" class="message-list">
            <div v-if="state.loading" class="state-card">
              <van-loading size="24px" vertical>正在准备政策顾问...</van-loading>
            </div>

            <div v-else-if="state.errorMessage" class="state-card state-card--warning">
              <div class="state-card__title">当前还不能开始咨询</div>
              <p>{{ state.errorMessage }}</p>
            </div>

            <div v-else-if="!state.messageList.length" class="empty-scene">
              <div class="empty-scene__headline">可以直接问政策问题</div>
              <p>建议一次只问一个主题，例如“厦门市高层次人才住房补贴怎么申请”。</p>
              <div class="empty-scene__grid">
                <div class="empty-scene__item">先说你的身份或企业类型</div>
                <div class="empty-scene__item">再说你关心的政策主题</div>
                <div class="empty-scene__item">最后让系统给出依据与提醒</div>
              </div>
            </div>

            <template v-else>
              <article
                v-for="item in state.messageList"
                :key="item.id"
                class="message-bubble"
                :class="item.messageRole === 'user' ? 'message-bubble--user' : 'message-bubble--assistant'"
              >
                <div class="message-bubble__role">
                  {{ item.messageRole === 'user' ? '咨询人' : '政策顾问' }}
                </div>
                <div class="message-bubble__text">{{ item.messageText }}</div>
                <div
                  v-if="item.messageRole !== 'user' && formatCitations(item).length"
                  class="message-bubble__citations"
                >
                  依据：{{ formatCitations(item).join(' / ') }}
                </div>
              </article>
            </template>

            <article v-if="state.asking" class="message-bubble message-bubble--assistant message-bubble--pending">
              <div class="message-bubble__role">政策顾问</div>
              <div class="message-bubble__text">正在整理政策依据，请稍候...</div>
            </article>
          </div>
        </section>

        <section class="composer-card">
          <label class="composer-card__label" for="policy-question">你的问题</label>
          <textarea
            id="policy-question"
            v-model.trim="state.question"
            class="composer-card__input"
            rows="4"
            maxlength="500"
            placeholder="例如：厦门市新引进人才有哪些补贴，申请时要注意什么？"
            :disabled="!canChat || state.asking"
            @keydown.enter.exact.prevent="handleSend"
          ></textarea>
          <div class="composer-card__footer">
            <div class="composer-card__hint">按 Enter 发送，Shift + Enter 换行</div>
            <button
              type="button"
              class="primary-button"
              :disabled="!canSend"
              @click="handleSend"
            >
              {{ state.asking ? '发送中...' : '发送咨询' }}
            </button>
          </div>
        </section>
      </template>
    </main>

    <MobileTabBar v-if="showMobileTabbar" :items="mobileNavItems" />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { showToast } from 'vant'
import { useRouter } from 'vue-router'
import { createAgentSession, queryAgentMessages, queryAgentSessions, sendAgentQuestion } from '@/api/agent'
import { queryCurrentAiPermission } from '@/api/ai'
import { querySkillList } from '@/api/skill'
import { useUserStore } from '@/stores/user'
import { buildAccessContext } from '@/constants/modules'
import { resolveMobileWorkspaceItems } from '@/constants/mobile-workspace'
import MobileTabBar from '@/components/mobile/MobileTabBar.vue'
import { isMobileClient } from '@/utils/device'

const router = useRouter()
const userStore = useUserStore()
const messageListRef = ref(null)

const TARGET_SKILL_CODE = 'talent_policy_consultant'

const quickPrompts = [
  '厦门市高层次人才可以申请哪些住房支持？',
  '企业想申请人才项目，通常先准备哪些材料？',
  '新引进人才常见补贴有哪些，适用条件怎么判断？'
]

const state = reactive({
  loading: false,
  asking: false,
  skill: null,
  sessionInfo: null,
  messageList: [],
  question: '',
  permissionInfo: null,
  errorMessage: ''
})

const hasToken = computed(() => Boolean(localStorage.getItem('token')))
const showMobileTabbar = computed(() => hasToken.value && isMobileClient())
const mobileNavItems = computed(() => resolveMobileWorkspaceItems(buildAccessContext(userStore.userInfo)))
const permissionFlags = computed(() => {
  const aiPermissions = state.permissionInfo?.aiPermissions || []
  return {
    canUseAi: Boolean(state.permissionInfo?.admin || aiPermissions.some((item) => item.canUseAi)),
    canUseAgent: Boolean(state.permissionInfo?.admin || aiPermissions.some((item) => item.canUseAgent))
  }
})
const canChat = computed(() => {
  return hasToken.value
    && Boolean(state.skill?.id)
    && Boolean(state.sessionInfo?.id)
    && permissionFlags.value.canUseAgent
    && !state.errorMessage
})
const canSend = computed(() => canChat.value && Boolean(state.question.trim()) && !state.asking)
const skillTitle = computed(() => state.skill?.skillName || '政策顾问准备中')
const connectionText = computed(() => {
  if (!hasToken.value) return '未登录'
  if (!permissionFlags.value.canUseAgent) return '未授权'
  if (permissionFlags.value.canUseAi) return 'AI 优先'
  return '知识库兜底'
})
const sessionText = computed(() => (state.sessionInfo?.id ? `会话 #${state.sessionInfo.id}` : '未创建会话'))
const helperText = computed(() => {
  if (state.errorMessage) return state.errorMessage
  if (!state.skill) return '正在定位政策知识与技能版本'
  if (!permissionFlags.value.canUseAgent) {
    return '页面已就绪，但当前账号尚未开通咨询权限'
  }
  if (!permissionFlags.value.canUseAi) {
    return `当前已连接 ${state.skill.baseName || '政策知识库'}，未开通 AI 时将自动使用知识库兜底答复`
  }
  return `当前已连接 ${state.skill.baseName || '政策知识库'}，可直接开始咨询`
})

function ensureSuccess(response, fallbackMessage) {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallbackMessage)
  }
  return response.data
}

function goToLogin() {
  router.push({
    path: '/login',
    query: {
      redirect: '/policy-consultant'
    }
  })
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

async function scrollToBottom() {
  await nextTick()
  if (!messageListRef.value) return
  messageListRef.value.scrollTop = messageListRef.value.scrollHeight
}

async function fetchMessages() {
  if (!state.sessionInfo?.id) return
  const data = ensureSuccess(await queryAgentMessages(state.sessionInfo.id), '会话记录加载失败')
  state.messageList = Array.isArray(data) ? data : []
  await scrollToBottom()
}

async function ensureSession() {
  const sessions = ensureSuccess(
    await queryAgentSessions({
      skillId: state.skill.id,
      status: 'ACTIVE'
    }),
    '会话列表加载失败'
  )
  const sessionList = Array.isArray(sessions) ? sessions : []
  const latestSession = sessionList[0]
  if (latestSession?.id) {
    state.sessionInfo = latestSession
    await fetchMessages()
    return
  }

  state.sessionInfo = ensureSuccess(
    await createAgentSession({ skillId: Number(state.skill.id) }),
    '会话创建失败'
  )
  state.messageList = []
}

async function bootstrap() {
  if (!hasToken.value) return

  state.loading = true
  state.errorMessage = ''
  try {
    const [skillData, permissionData] = await Promise.all([
      querySkillList({
        keywords: TARGET_SKILL_CODE,
        publishStatus: 'PUBLISHED',
        status: 1
      }),
      queryCurrentAiPermission()
    ])

    const skillList = ensureSuccess(skillData, '技能列表加载失败')
    state.permissionInfo = ensureSuccess(permissionData, 'AI 权限加载失败')

    const resolvedSkill = (Array.isArray(skillList) ? skillList : []).find((item) => item.skillCode === TARGET_SKILL_CODE)
      || (Array.isArray(skillList) ? skillList[0] : null)

    if (!resolvedSkill) {
      throw new Error('系统中还没有可用的政策咨询 Skill。')
    }

    state.skill = resolvedSkill

    if (!permissionFlags.value.canUseAgent) {
      state.errorMessage = '当前账号还没有咨询权限，请先在 AI 权限中心开通。'
      return
    }

    await ensureSession()
  } catch (error) {
    state.errorMessage = error.message || '政策咨询页面初始化失败'
  } finally {
    state.loading = false
  }
}

async function handleResetSession() {
  if (!state.skill?.id) return
  state.loading = true
  state.errorMessage = ''
  try {
    state.sessionInfo = ensureSuccess(
      await createAgentSession({ skillId: Number(state.skill.id) }),
      '新会话创建失败'
    )
    state.messageList = []
    state.question = ''
  } catch (error) {
    state.errorMessage = error.message || '新会话创建失败'
  } finally {
    state.loading = false
  }
}

async function sendQuestion(questionText) {
  state.asking = true
  try {
    await sendAgentQuestion({
      sessionId: state.sessionInfo.id,
      question: questionText
    }).then((response) => ensureSuccess(response, '政策咨询失败'))
    state.question = ''
    await fetchMessages()
  } catch (error) {
    const message = error.message || '政策咨询失败'
    if (message.includes('provider not found')) {
      state.errorMessage = '当前系统还没有可用的 AI Provider，系统将改为知识库兜底模式。'
      showToast('当前未接入 AI，已切换知识库兜底')
      return
    }
    showToast(message)
  } finally {
    state.asking = false
  }
}

async function handleSend() {
  const questionText = state.question.trim()
  if (!questionText) {
    showToast('请输入政策问题')
    return
  }
  if (!canChat.value) {
    showToast(state.errorMessage || '当前还不能开始咨询')
    return
  }
  await sendQuestion(questionText)
}

async function handleQuickPrompt(prompt) {
  state.question = prompt
  if (canChat.value && !state.asking) {
    await handleSend()
  }
}

onMounted(() => {
  bootstrap()
})
</script>

<style scoped>
.policy-page {
  --policy-bg: #f4efe6;
  --policy-surface: rgba(255, 251, 246, 0.84);
  --policy-surface-strong: rgba(255, 248, 241, 0.96);
  --policy-ink: #1d2a30;
  --policy-muted: #6a756e;
  --policy-line: rgba(43, 62, 69, 0.12);
  --policy-accent: #0f6c63;
  --policy-accent-deep: #0b4f49;
  --policy-gold: #c58b3a;
  min-height: 100vh;
  padding: 20px 16px 28px;
  background:
    radial-gradient(circle at top left, rgba(197, 139, 58, 0.18), transparent 28%),
    radial-gradient(circle at 80% 10%, rgba(15, 108, 99, 0.14), transparent 24%),
    linear-gradient(180deg, #f8f1e6 0%, #efe7dc 48%, #efece7 100%);
  color: var(--policy-ink);
  font-family: "Source Han Serif SC", "Noto Serif SC", "Songti SC", serif;
  position: relative;
  overflow: hidden;
}

.policy-page--with-tabbar {
  padding-bottom: 0;
}

.policy-page__glow {
  position: absolute;
  width: 240px;
  height: 240px;
  border-radius: 999px;
  filter: blur(72px);
  opacity: 0.42;
  pointer-events: none;
}

.policy-page__glow--top {
  top: -80px;
  right: -80px;
  background: rgba(197, 139, 58, 0.28);
}

.policy-page__glow--bottom {
  left: -90px;
  bottom: 160px;
  background: rgba(15, 108, 99, 0.18);
}

.hero,
.conversation-card,
.composer-card,
.gate-panel {
  position: relative;
  z-index: 1;
  max-width: 760px;
  margin: 0 auto;
}

.hero {
  padding: 22px 18px 20px;
  border: 1px solid rgba(255, 255, 255, 0.58);
  border-radius: 30px;
  background: linear-gradient(180deg, rgba(255, 252, 247, 0.9), rgba(245, 237, 226, 0.78));
  box-shadow: 0 28px 80px rgba(67, 45, 18, 0.1);
  backdrop-filter: blur(16px);
}

.hero__topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.hero__brand {
  display: inline-flex;
  align-items: center;
  gap: 12px;
}

.hero__brand-mark {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: linear-gradient(145deg, var(--policy-accent), var(--policy-accent-deep));
  color: #fffaf2;
  font-family: "Avenir Next", "PingFang SC", sans-serif;
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.14em;
}

.hero__brand-name,
.hero__top-action,
.hero__chip,
.quick-prompts__item,
.conversation-card__meta,
.ghost-button,
.primary-button,
.composer-card__hint,
.message-bubble__citations,
.gate-panel__label,
.empty-scene__item {
  font-family: "Avenir Next", "PingFang SC", "Microsoft YaHei", sans-serif;
}

.hero__brand-name {
  font-size: 13px;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: var(--policy-muted);
}

.hero__brand-subtitle {
  margin-top: 3px;
  font-size: 18px;
  font-weight: 700;
}

.hero__top-action,
.ghost-button,
.primary-button,
.quick-prompts__item {
  border: none;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, opacity 0.22s ease, background 0.22s ease;
}

.hero__top-action,
.ghost-button {
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(16, 38, 43, 0.06);
  color: var(--policy-ink);
}

.hero__content {
  margin-top: 26px;
}

.hero__eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.24em;
  text-transform: uppercase;
  color: var(--policy-accent);
}

.hero h1 {
  margin: 14px 0 12px;
  font-size: clamp(32px, 8vw, 54px);
  line-height: 1.02;
  letter-spacing: -0.04em;
}

.hero__summary {
  max-width: 32rem;
  margin: 0;
  font-size: 15px;
  line-height: 1.75;
  color: var(--policy-muted);
}

.hero__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero__chip {
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.62);
  border: 1px solid rgba(43, 62, 69, 0.08);
  color: #294348;
  font-size: 12px;
}

.quick-prompts {
  display: grid;
  gap: 10px;
  margin-top: 22px;
}

.quick-prompts__item {
  text-align: left;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(43, 62, 69, 0.08);
  color: #22383d;
  font-size: 14px;
}

.chat-shell {
  max-width: 760px;
  margin: 18px auto 0;
}

.conversation-card,
.composer-card,
.gate-panel {
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.55);
  background: var(--policy-surface);
  box-shadow: 0 22px 60px rgba(45, 35, 21, 0.08);
  backdrop-filter: blur(14px);
}

.conversation-card {
  padding: 18px;
}

.conversation-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.conversation-card__title {
  font-size: 18px;
  font-weight: 700;
}

.conversation-card__meta {
  margin-top: 5px;
  color: var(--policy-muted);
  font-size: 13px;
  line-height: 1.5;
}

.message-list {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  max-height: min(54vh, 620px);
  overflow-y: auto;
  padding-right: 4px;
}

.message-bubble {
  max-width: 88%;
  padding: 14px 16px 15px;
  border-radius: 22px;
  animation: bubble-in 0.28s ease;
}

.message-bubble--user {
  align-self: flex-end;
  background: linear-gradient(145deg, #1f7a70, #14564f);
  color: #f8f7f0;
  border-bottom-right-radius: 8px;
}

.message-bubble--assistant {
  align-self: flex-start;
  background: var(--policy-surface-strong);
  border: 1px solid var(--policy-line);
  border-bottom-left-radius: 8px;
}

.message-bubble--pending {
  opacity: 0.8;
}

.message-bubble__role {
  margin-bottom: 8px;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  opacity: 0.72;
}

.message-bubble__text {
  white-space: pre-wrap;
  line-height: 1.75;
  font-size: 15px;
}

.message-bubble__citations {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid rgba(43, 62, 69, 0.08);
  font-size: 12px;
  color: var(--policy-muted);
}

.state-card,
.empty-scene {
  padding: 22px 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.58);
  border: 1px dashed rgba(43, 62, 69, 0.12);
}

.state-card--warning {
  background: rgba(255, 244, 232, 0.8);
}

.state-card__title,
.empty-scene__headline {
  font-size: 18px;
  font-weight: 700;
}

.state-card p,
.empty-scene p,
.gate-panel p {
  margin: 10px 0 0;
  color: var(--policy-muted);
  line-height: 1.7;
}

.empty-scene__grid {
  display: grid;
  gap: 10px;
  margin-top: 14px;
}

.empty-scene__item {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.72);
  color: #294348;
  font-size: 13px;
}

.composer-card,
.gate-panel {
  margin-top: 14px;
  padding: 18px;
}

.composer-card__label {
  display: block;
  margin-bottom: 10px;
  font-family: "Avenir Next", "PingFang SC", sans-serif;
  font-size: 13px;
  color: var(--policy-muted);
}

.composer-card__input {
  width: 100%;
  resize: none;
  border: 1px solid rgba(43, 62, 69, 0.08);
  border-radius: 20px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.84);
  color: var(--policy-ink);
  font: inherit;
  line-height: 1.7;
  outline: none;
}

.composer-card__input:focus {
  border-color: rgba(15, 108, 99, 0.36);
  box-shadow: 0 0 0 4px rgba(15, 108, 99, 0.08);
}

.composer-card__footer,
.gate-panel__actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.composer-card__hint {
  color: var(--policy-muted);
  font-size: 12px;
}

.primary-button {
  min-width: 120px;
  padding: 12px 18px;
  border-radius: 999px;
  background: linear-gradient(135deg, var(--policy-gold), #9c6220);
  color: #fffefb;
  box-shadow: 0 14px 28px rgba(156, 98, 32, 0.22);
}

.ghost-button {
  min-width: 84px;
}

.primary-button:disabled,
.ghost-button:disabled,
.quick-prompts__item:disabled,
.hero__top-action:disabled {
  opacity: 0.45;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.primary-button:not(:disabled):hover,
.ghost-button:not(:disabled):hover,
.quick-prompts__item:not(:disabled):hover,
.hero__top-action:not(:disabled):hover {
  transform: translateY(-1px);
}

.gate-panel__label {
  display: inline-flex;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 108, 99, 0.1);
  color: var(--policy-accent-deep);
  font-size: 12px;
}

.gate-panel h2 {
  margin: 14px 0 0;
  font-size: 28px;
  line-height: 1.12;
}

@keyframes bubble-in {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (min-width: 768px) {
  .policy-page {
    padding: 28px 22px 36px;
  }

  .quick-prompts {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .hero h1 {
    max-width: 9em;
  }

  .conversation-card__header,
  .composer-card__footer,
  .gate-panel__actions {
    flex-direction: column;
    align-items: stretch;
  }

  .message-bubble {
    max-width: 94%;
  }

  .primary-button,
  .ghost-button {
    width: 100%;
  }
}
</style>

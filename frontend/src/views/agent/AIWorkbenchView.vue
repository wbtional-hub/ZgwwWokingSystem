<template>
  <AppPageShell
    title="AI 工作台"
    description="按技能调用 AI 分析知识库内容，并沉淀历史咨询记录与当前会话上下文。"
  >
    <section class="overview-grid" data-guide="ai-workbench-capability">
      <section class="workspace-card hero-card">
        <div class="card-head">
          <div>
            <div class="section-kicker">AI Workbench</div>
            <div class="card-title">我的AI能力</div>
          </div>
          <div class="brand-badge">Skills Ready</div>
        </div>

        <div class="status-list">
          <div class="status-item">
            <span>使用 AI</span>
            <strong>{{ boolText(permissionFlags.canUseAi) }}</strong>
          </div>
          <div class="status-item">
            <span>进入工作台</span>
            <strong>{{ boolText(permissionFlags.canUseAgent) }}</strong>
          </div>
          <div class="status-item">
            <span>训练技能</span>
            <strong>{{ boolText(permissionFlags.canTrainSkill) }}</strong>
          </div>
          <div class="status-item">
            <span>发布技能</span>
            <strong>{{ boolText(permissionFlags.canPublishSkill) }}</strong>
          </div>
        </div>
      </section>

      <section class="workspace-card" data-guide="ai-workbench-expert">
        <div class="card-head">
          <div>
            <div class="section-kicker">Expert Identity</div>
            <div class="card-title">我的专家身份</div>
          </div>
        </div>
        <van-empty v-if="!state.expertList.length" description="当前还没有绑定专家身份" />
        <div v-else class="expert-badges">
          <div v-for="item in state.expertList" :key="item.id" class="expert-badge">
            <div class="expert-badge-chip">Frontend Skill</div>
            <div class="expert-badge-title">{{ item.skillName }}</div>
            <div class="meta-line">等级：{{ item.expertLevel || '-' }}</div>
            <div class="meta-line">版本：{{ item.versionNo || '-' }}</div>
          </div>
        </div>
      </section>
    </section>

    <section class="workspace-main">
      <section class="workspace-card history-panel" data-guide="ai-workbench-session">
        <div class="card-head">
          <div>
            <div class="section-kicker">Session Ledger</div>
            <div class="card-title">历史会话 / 咨询记录</div>
          </div>
        </div>

        <div class="field-stack">
          <van-field
            v-model.trim="state.sessionQuery.keywords"
            label="关键词"
            placeholder="按标题、技能或知识库过滤"
          />
        </div>

        <div class="action-row history-actions">
          <van-button size="small" type="primary" :loading="state.loadingSessions" @click="fetchSessions">查询</van-button>
          <van-button size="small" plain @click="resetSessionQuery">重置</van-button>
        </div>

        <van-loading v-if="state.loadingSessions" class="state-block" size="24px" vertical>加载中...</van-loading>
        <van-empty v-else-if="!state.sessionList.length" description="还没有历史会话" />
        <div v-else class="session-list">
          <div
            v-for="item in state.sessionList"
            :key="item.id"
            class="session-item"
            :class="{ active: state.sessionInfo?.id === item.id }"
            @click="handleSelectSession(item)"
          >
            <div class="session-title">{{ item.sessionTitle || '未命名会话' }}</div>
            <div class="meta-line">技能：{{ item.skillName || '-' }}</div>
            <div class="meta-line">知识库：{{ item.baseName || '-' }}</div>
            <div class="meta-line">创建时间：{{ item.createTime || '-' }}</div>
          </div>
        </div>
      </section>

      <div class="workspace-side">
        <section class="workspace-card" data-guide="ai-workbench-config">
          <div class="card-head">
            <div>
              <div class="section-kicker">Workspace Setup</div>
              <div class="card-title">会话配置</div>
            </div>
          </div>

          <div class="select-field">
            <span class="select-label">技能</span>
            <select v-model="state.selectedSkillId" @change="handleSkillChange">
              <option value="">请选择技能</option>
              <option v-for="item in state.skillOptions" :key="item.id" :value="String(item.id)">
                {{ item.skillName }}（{{ item.baseName || '未绑定知识库' }}）
              </option>
            </select>
          </div>

          <div class="selected-card" v-if="selectedSkill">
            <div class="selected-chip">Skills Center</div>
            <div class="selected-title">{{ selectedSkill.skillName }}</div>
            <div class="meta-line">知识库：{{ selectedSkill.baseName || '-' }}</div>
            <div class="meta-line">已发布版本：{{ selectedSkill.publishedVersionNo || '未发布' }}</div>
            <div class="meta-line">专家身份：{{ selectedExpert ? `${selectedExpert.expertLevel || 'NORMAL'} 专家` : '未绑定' }}</div>
          </div>

          <div class="action-row">
            <van-button size="small" type="primary" :loading="state.creatingSession" :disabled="!state.selectedSkillId" @click="handleCreateSession">打开新会话</van-button>
            <van-button size="small" plain @click="reloadAll">刷新数据</van-button>
          </div>
          <div class="meta-line current-session-line">当前会话：{{ state.sessionInfo?.sessionTitle || '未创建' }}</div>
        </section>

        <section class="workspace-card ask-panel" data-guide="ai-workbench-ask">
          <div class="card-head">
            <div>
              <div class="section-kicker">Ask AI</div>
              <div class="card-title">提问</div>
            </div>
          </div>

          <van-field
            v-model="state.question"
            label="问题"
            type="textarea"
            rows="5"
            autosize
            placeholder="例如：A 类人才在上海可以申请哪些补贴？"
            :disabled="!state.sessionInfo?.id"
          />

          <div class="action-row">
            <van-button size="small" type="primary" :loading="state.asking" :disabled="!state.sessionInfo?.id" @click="handleAsk">发送问题</van-button>
          </div>

          <div v-if="state.lastAnswer" class="answer-box">
            <div class="answer-head">
              <div class="selected-chip">Latest Answer</div>
              <div class="answer-title">最新回答</div>
            </div>
            <div class="answer-text">{{ state.lastAnswer.answer }}</div>
            <div class="meta-line">引用文档：{{ (state.lastAnswer.citedTitles || []).join(' / ') || '-' }}</div>
            <div class="meta-line">引用 Chunk：{{ (state.lastAnswer.citedChunkIdList || []).join(', ') || '-' }}</div>
          </div>
        </section>
      </div>
    </section>

    <section class="workspace-card messages-panel" data-guide="ai-workbench-messages">
      <div class="card-head">
        <div>
          <div class="section-kicker">Conversation Trace</div>
          <div class="card-title">会话记录</div>
        </div>
      </div>

      <div class="message-list">
        <div
          v-for="item in state.messageList"
          :key="item.id"
          class="message-item"
          :class="`message-item--${item.messageRole}`"
        >
          <div class="message-role">{{ item.messageRole === 'user' ? '我' : 'AI' }}</div>
          <div class="message-text">{{ item.messageText }}</div>
          <div class="meta-line">引用 Chunk：{{ (item.citedChunkIdList || []).join(', ') || '-' }}</div>
        </div>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { createAgentSession, queryAgentMessages, queryAgentSessions, sendAgentQuestion } from '@/api/agent'
import { queryCurrentAiPermission } from '@/api/ai'
import { queryExpertList } from '@/api/expert'
import { querySkillList } from '@/api/skill'

const state = reactive({
  permissions: null,
  expertList: [],
  skillOptions: [],
  sessionList: [],
  sessionQuery: { keywords: '' },
  loadingSessions: false,
  selectedSkillId: '',
  creatingSession: false,
  asking: false,
  sessionInfo: null,
  question: '',
  lastAnswer: null,
  messageList: []
})

const permissionFlags = computed(() => {
  const aiPermissions = state.permissions?.aiPermissions || []
  return {
    canUseAi: Boolean(state.permissions?.admin || aiPermissions.some((item) => item.canUseAi)),
    canUseAgent: Boolean(state.permissions?.admin || aiPermissions.some((item) => item.canUseAgent)),
    canTrainSkill: Boolean(state.permissions?.admin || aiPermissions.some((item) => item.canTrainSkill)),
    canPublishSkill: Boolean(state.permissions?.admin || aiPermissions.some((item) => item.canPublishSkill))
  }
})
const selectedSkill = computed(() => state.skillOptions.find((item) => String(item.id) === state.selectedSkillId) || null)
const selectedExpert = computed(() => state.expertList.find((item) => String(item.skillId) === state.selectedSkillId) || null)

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) throw new Error(response?.message || fallback)
  return response.data
}

function boolText(value) {
  return value ? '已授权' : '未授权'
}

function resetSessionQuery() {
  state.sessionQuery.keywords = ''
  fetchSessions()
}

async function fetchPermissions() {
  state.permissions = ensureSuccess(await queryCurrentAiPermission(), '权限信息加载失败')
}

async function fetchExperts() {
  const data = ensureSuccess(await queryExpertList({ status: 1 }), '专家台账加载失败')
  state.expertList = Array.isArray(data) ? data : []
}

async function fetchSkills() {
  const data = ensureSuccess(await querySkillList({ publishStatus: 'PUBLISHED', status: 1 }), '技能列表加载失败')
  state.skillOptions = Array.isArray(data) ? data : []
}

async function fetchSessions() {
  state.loadingSessions = true
  try {
    const data = ensureSuccess(await queryAgentSessions({ keywords: state.sessionQuery.keywords || undefined, status: 'ACTIVE' }), '历史会话加载失败')
    state.sessionList = Array.isArray(data) ? data : []
  } catch (error) {
    showToast(error.message || '历史会话加载失败')
  } finally {
    state.loadingSessions = false
  }
}

function handleSkillChange() {
  state.sessionInfo = null
  state.question = ''
  state.lastAnswer = null
  state.messageList = []
}

async function handleCreateSession() {
  state.creatingSession = true
  try {
    const data = ensureSuccess(await createAgentSession({ skillId: Number(state.selectedSkillId) }), '创建会话失败')
    state.sessionInfo = data
    state.messageList = []
    state.lastAnswer = null
    state.question = ''
    await fetchSessions()
    showToast('会话已创建')
  } catch (error) {
    showToast(error.message || '创建会话失败')
  } finally {
    state.creatingSession = false
  }
}

async function handleSelectSession(item) {
  state.sessionInfo = item
  state.selectedSkillId = item.skillId ? String(item.skillId) : ''
  state.lastAnswer = null
  state.question = ''
  await fetchMessages()
}

async function handleAsk() {
  if (!state.question.trim()) {
    showToast('请输入问题')
    return
  }
  state.asking = true
  try {
    const data = ensureSuccess(await sendAgentQuestion({ sessionId: state.sessionInfo.id, question: state.question.trim() }), 'AI 分析失败')
    state.lastAnswer = data
    state.question = ''
    await fetchMessages()
    await fetchSessions()
    const matched = state.sessionList.find((item) => item.id === state.sessionInfo?.id)
    if (matched) {
      state.sessionInfo = matched
    }
  } catch (error) {
    showToast(error.message || 'AI 分析失败')
  } finally {
    state.asking = false
  }
}

async function fetchMessages() {
  if (!state.sessionInfo?.id) return
  try {
    const data = ensureSuccess(await queryAgentMessages(state.sessionInfo.id), '会话记录加载失败')
    state.messageList = Array.isArray(data) ? data : []
  } catch (error) {
    showToast(error.message || '会话记录加载失败')
  }
}

async function reloadAll() {
  try {
    await Promise.all([fetchPermissions(), fetchExperts(), fetchSkills(), fetchSessions()])
  } catch (error) {
    showToast(error.message || '工作台初始化失败')
  }
}

onMounted(() => {
  reloadAll()
})
</script>

<style scoped>
.overview-grid,
.workspace-main {
  display: grid;
  gap: 18px;
  margin-bottom: 18px;
}

.overview-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.workspace-main {
  grid-template-columns: minmax(300px, 340px) minmax(0, 1fr);
  align-items: start;
}

.workspace-side {
  display: grid;
  gap: 18px;
}

.workspace-card {
  padding: 24px;
  border-radius: 14px;
  background: #ffffff;
  border: 1px solid #eef2f7;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.hero-card {
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.1), transparent 22%),
    #ffffff;
}

.card-head,
.answer-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.section-kicker,
.brand-badge,
.expert-badge-chip,
.selected-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 11px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.section-kicker {
  background: #eef2ff;
  color: #3b82f6;
}

.brand-badge,
.selected-chip,
.expert-badge-chip {
  background: #eef2ff;
  color: #3b82f6;
}

.card-title,
.answer-title,
.selected-title,
.session-title,
.expert-badge-title,
.message-role {
  color: #111827;
  font-weight: 700;
}

.card-title {
  margin-top: 12px;
  font-size: 18px;
  letter-spacing: -0.02em;
}

.field-stack {
  display: grid;
  gap: 12px;
}

.status-list {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.status-item,
.expert-badge,
.selected-card,
.answer-box,
.message-item,
.session-item {
  border-radius: 14px;
  border: 1px solid #eef2f7;
  background: #ffffff;
}

.status-item {
  padding: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.status-item span {
  color: #6b7280;
  font-size: 13px;
}

.status-item strong {
  color: #111827;
  font-size: 14px;
}

.expert-badges,
.message-list,
.session-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.expert-badge,
.selected-card,
.answer-box,
.message-item,
.session-item {
  padding: 14px;
}

.expert-badge-title,
.selected-title,
.session-title {
  margin: 10px 0 4px;
  font-size: 16px;
}

.session-item {
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, box-shadow 0.18s ease;
}

.session-item:hover {
  transform: translateY(-1px);
  border-color: rgba(143, 168, 211, 0.88);
}

.session-item.active {
  border-color: #3b82f6;
  background: #eff6ff;
  box-shadow: 0 8px 18px rgba(59, 130, 246, 0.08);
}

.select-field {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0 14px;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.select-label {
  flex: 0 0 58px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 600;
}

.select-field select {
  flex: 1;
  border: 0;
  background: transparent;
  color: #374151;
  font-size: 14px;
  outline: none;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.history-actions {
  margin: 12px 0 14px;
}

.current-session-line {
  margin-top: 12px;
}

.answer-text,
.message-text,
.meta-line {
  color: #6b7280;
  font-size: 13px;
  line-height: 1.5;
}

.message-item--assistant {
  background: #eff6ff;
}

.state-block {
  padding: 24px 0;
}

:deep(.van-field) {
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
}

:deep(.van-cell) {
  background: transparent;
}

:deep(.van-field__label) {
  width: 58px;
  color: #6b7280;
  font-size: 13px;
  font-weight: 600;
}

:deep(.van-field__control) {
  color: #374151;
  font-size: 14px;
}

:deep(.van-field__control::placeholder) {
  color: #9ca3af;
}

:deep(.van-button) {
  min-height: 40px;
  border-radius: 10px;
  font-weight: 600;
}

:deep(.van-button--primary) {
  border-color: #2563eb;
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.18);
}

@media (max-width: 1200px) {
  .workspace-main {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .overview-grid {
    grid-template-columns: 1fr;
  }

  .status-list {
    grid-template-columns: 1fr;
  }
}
</style>

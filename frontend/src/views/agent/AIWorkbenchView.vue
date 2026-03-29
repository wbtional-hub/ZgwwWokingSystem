<template>
  <AppPageShell title="AI 工作台" description="按技能调用 AI 分析知识库内容，并沉淀历史咨询记录。">
    <section class="panel-grid header-grid" data-guide="ai-workbench-capability">
      <section class="panel">
        <div class="panel-title">我的 AI 能力</div>
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

      <section class="panel">
        <div class="panel-title">我的专家身份</div>
        <van-empty v-if="!state.expertList.length" description="当前还没有绑定专家身份" />
        <div v-else class="expert-badges">
          <div v-for="item in state.expertList" :key="item.id" class="expert-badge">
            <div class="expert-badge-title">{{ item.skillName }}</div>
            <div class="meta-line">等级：{{ item.expertLevel || '-' }}</div>
            <div class="meta-line">版本：{{ item.versionNo || '-' }}</div>
          </div>
        </div>
      </section>
    </section>

    <section class="panel-grid main-grid">
      <section class="panel history-panel" data-guide="ai-workbench-session">
        <div class="panel-title">历史会话 / 咨询记录</div>
        <van-field v-model.trim="state.sessionQuery.keywords" label="关键字" placeholder="按标题、技能或知识库过滤" />
        <div class="action-row history-actions">
          <van-button size="small" type="primary" :loading="state.loadingSessions" @click="fetchSessions">查询</van-button>
          <van-button size="small" plain @click="resetSessionQuery">重置</van-button>
        </div>
        <van-loading v-if="state.loadingSessions" class="state-block" size="24px" vertical>加载中...</van-loading>
        <van-empty v-else-if="!state.sessionList.length" description="还没有历史会话" />
        <div v-else class="session-list">
          <div v-for="item in state.sessionList" :key="item.id" class="session-item" :class="{ active: state.sessionInfo?.id === item.id }" @click="handleSelectSession(item)">
            <div class="session-title">{{ item.sessionTitle || '未命名会话' }}</div>
            <div class="meta-line">技能：{{ item.skillName || '-' }}</div>
            <div class="meta-line">知识库：{{ item.baseName || '-' }}</div>
            <div class="meta-line">创建时间：{{ item.createTime || '-' }}</div>
          </div>
        </div>
      </section>

      <section class="panel" data-guide="ai-workbench-config">
        <div class="panel-title">会话配置</div>
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
          <div class="selected-title">{{ selectedSkill.skillName }}</div>
          <div class="meta-line">知识库：{{ selectedSkill.baseName || '-' }}</div>
          <div class="meta-line">已发布版本：{{ selectedSkill.publishedVersionNo || '未发布' }}</div>
          <div class="meta-line">专家身份：{{ selectedExpert ? `${selectedExpert.expertLevel || 'NORMAL'} 专家` : '未绑定' }}</div>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.creatingSession" :disabled="!state.selectedSkillId" @click="handleCreateSession">打开新会话</van-button>
          <van-button size="small" plain @click="reloadAll">刷新数据</van-button>
        </div>
        <div class="meta-line">当前会话：{{ state.sessionInfo?.sessionTitle || '未创建' }}</div>
      </section>

      <section class="panel ask-panel" data-guide="ai-workbench-ask">
        <div class="panel-title">提问</div>
        <van-field v-model="state.question" label="问题" type="textarea" rows="5" autosize placeholder="例如：A 类人才在上海可以申请哪些补贴？" :disabled="!state.sessionInfo?.id" />
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.asking" :disabled="!state.sessionInfo?.id" @click="handleAsk">发送问题</van-button>
        </div>
        <div v-if="state.lastAnswer" class="answer-box">
          <div class="answer-title">最新回答</div>
          <div class="answer-text">{{ state.lastAnswer.answer }}</div>
          <div class="meta-line">引用文档：{{ (state.lastAnswer.citedTitles || []).join(' / ') || '-' }}</div>
          <div class="meta-line">引用 Chunk：{{ (state.lastAnswer.citedChunkIdList || []).join(', ') || '-' }}</div>
        </div>
      </section>
    </section>

    <section class="panel" data-guide="ai-workbench-messages">
      <div class="panel-title">会话记录</div>
      <div class="message-list">
        <div v-for="item in state.messageList" :key="item.id" class="message-item" :class="`message-item--${item.messageRole}`">
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
.panel {
  margin-bottom: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.main-grid {
  grid-template-columns: minmax(280px, 320px) minmax(0, 1fr) minmax(0, 1fr);
  align-items: start;
}

.panel-title,
.answer-title,
.message-role,
.selected-title,
.session-title,
.expert-badge-title {
  font-weight: 600;
  color: #1f2937;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-actions {
  margin-bottom: 12px;
}

.select-field {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 10px 0;
  padding: 10px 12px;
  border: 1px solid #ebedf0;
  border-radius: 8px;
}

.select-label {
  flex: 0 0 72px;
}

.select-field select {
  flex: 1;
  border: 0;
  background: transparent;
}

.status-list,
.expert-badges,
.message-list,
.session-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.status-item,
.expert-badge,
.selected-card,
.answer-box,
.message-item,
.session-item {
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.selected-card,
.expert-badge,
.session-item {
  border: 1px solid #e5e7eb;
}

.session-item {
  cursor: pointer;
}

.session-item.active {
  border-color: #1677ff;
  background: #eff6ff;
}

.message-item--assistant {
  background: #eff6ff;
}

.answer-text,
.message-text,
.meta-line {
  color: #4b5563;
  font-size: 13px;
  line-height: 1.8;
}

.state-block {
  padding: 20px 0;
}

@media (max-width: 1200px) {
  .main-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 960px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>

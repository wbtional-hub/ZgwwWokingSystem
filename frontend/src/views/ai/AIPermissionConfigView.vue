<template>
  <AppPageShell title="AI 权限配置" description="为指定用户分配 AI 接入、知识库和技能使用权限，未授权用户不能使用对应能力。">
    <template #actions>
      <div class="action-row">
        <van-button plain type="primary" :loading="state.loadingOptions" @click="reloadOptions">刷新基础数据</van-button>
      </div>
    </template>

    <section class="panel-grid intro-grid" data-guide="ai-permission-user">
      <section class="panel">
        <div class="panel-title">授权对象</div>
        <div class="select-field">
          <span class="select-label">用户</span>
          <select v-model="state.selectedUserId" @change="handleUserChange">
            <option value="">请选择用户</option>
            <option v-for="user in state.userOptions" :key="user.id" :value="String(user.id)">
              {{ user.realName || user.username }}（{{ user.username }}）
            </option>
          </select>
        </div>
        <div class="meta-line">选择用户后，会自动载入该用户已有的 AI、知识库和技能权限。</div>
      </section>

      <section class="panel">
        <div class="panel-title">说明</div>
        <div class="meta-line">AI 能力权限决定是否可以调用 AI、训练技能、发布技能和进入工作台。</div>
        <div class="meta-line">知识库权限决定是否可以查看、导入、训练和分析指定知识库。</div>
        <div class="meta-line">技能权限决定是否可以查看、使用、训练和发布指定技能。</div>
      </section>
    </section>

    <section class="panel-grid" data-guide="ai-permission-config">
      <section class="panel">
        <div class="panel-title">AI 能力授权</div>
        <div class="select-field">
          <span class="select-label">AI 接入</span>
          <select v-model="state.aiForm.providerConfigId" :disabled="!state.selectedUserId">
            <option value="">请选择 AI 接入</option>
            <option v-for="item in state.providerOptions" :key="item.id" :value="String(item.id)">
              {{ item.providerName }}（{{ item.providerCode }}）
            </option>
          </select>
        </div>
        <div class="switch-list">
          <div class="switch-row"><span>管理接入</span><van-switch v-model="state.aiForm.canManageProvider" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>使用 AI</span><van-switch v-model="state.aiForm.canUseAi" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>训练技能</span><van-switch v-model="state.aiForm.canTrainSkill" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>发布技能</span><van-switch v-model="state.aiForm.canPublishSkill" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>进入工作台</span><van-switch v-model="state.aiForm.canUseAgent" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>AI 分析</span><van-switch v-model="state.aiForm.canRunAnalysis" :disabled="!state.selectedUserId" /></div>
        </div>
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.aiForm.status" :disabled="!state.selectedUserId">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :disabled="!state.selectedUserId" :loading="state.savingAiPermission" @click="handleSaveAiPermission">保存 AI 权限</van-button>
          <van-button size="small" plain :disabled="!state.selectedUserId" @click="resetAiForm">清空</van-button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">知识库授权</div>
        <div class="select-field">
          <span class="select-label">知识库</span>
          <select v-model="state.knowledgeForm.baseId" :disabled="!state.selectedUserId">
            <option value="">请选择知识库</option>
            <option v-for="item in state.baseOptions" :key="item.id" :value="String(item.id)">
              {{ item.baseName }}（{{ item.baseCode }}）
            </option>
          </select>
        </div>
        <div class="switch-list">
          <div class="switch-row"><span>查看知识库</span><van-switch v-model="state.knowledgeForm.canView" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>上传文档</span><van-switch v-model="state.knowledgeForm.canUpload" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>基于知识库训练</span><van-switch v-model="state.knowledgeForm.canTrainSkill" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>基于知识库分析</span><van-switch v-model="state.knowledgeForm.canAnalyze" :disabled="!state.selectedUserId" /></div>
        </div>
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.knowledgeForm.status" :disabled="!state.selectedUserId">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :disabled="!state.selectedUserId" :loading="state.savingKnowledgePermission" @click="handleSaveKnowledgePermission">保存知识库权限</van-button>
          <van-button size="small" plain :disabled="!state.selectedUserId" @click="resetKnowledgeForm">清空</van-button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">技能授权</div>
        <div class="select-field">
          <span class="select-label">技能</span>
          <select v-model="state.skillForm.skillId" :disabled="!state.selectedUserId">
            <option value="">请选择技能</option>
            <option v-for="item in state.skillOptions" :key="item.id" :value="String(item.id)">
              {{ item.skillName }}（{{ item.skillCode }}）
            </option>
          </select>
        </div>
        <div class="switch-list">
          <div class="switch-row"><span>查看技能</span><van-switch v-model="state.skillForm.canView" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>使用技能</span><van-switch v-model="state.skillForm.canUse" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>训练技能</span><van-switch v-model="state.skillForm.canTrain" :disabled="!state.selectedUserId" /></div>
          <div class="switch-row"><span>发布技能</span><van-switch v-model="state.skillForm.canPublish" :disabled="!state.selectedUserId" /></div>
        </div>
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.skillForm.status" :disabled="!state.selectedUserId">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :disabled="!state.selectedUserId" :loading="state.savingSkillPermission" @click="handleSaveSkillPermission">保存技能权限</van-button>
          <van-button size="small" plain :disabled="!state.selectedUserId" @click="resetSkillForm">清空</van-button>
        </div>
      </section>
    </section>

    <section class="panel-grid" data-guide="ai-permission-result">
      <section class="panel">
        <div class="panel-title">已授权 AI 能力</div>
        <van-loading v-if="state.loadingAiPermissions" class="state-block" size="24px" vertical>加载中...</van-loading>
        <van-empty v-else-if="state.selectedUserId && !state.aiPermissionList.length" description="该用户暂无 AI 权限" />
        <div v-else class="permission-list">
          <div v-for="item in state.aiPermissionList" :key="item.id" class="permission-item" @click="fillAiPermission(item)">
            <div class="permission-title">{{ item.providerName }}</div>
            <div class="meta-line">AI：{{ boolText(item.canUseAi) }}，工作台：{{ boolText(item.canUseAgent) }}，分析：{{ boolText(item.canRunAnalysis) }}</div>
            <div class="meta-line">训练：{{ boolText(item.canTrainSkill) }}，发布：{{ boolText(item.canPublishSkill) }}</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">已授权知识库</div>
        <van-loading v-if="state.loadingKnowledgePermissions" class="state-block" size="24px" vertical>加载中...</van-loading>
        <van-empty v-else-if="state.selectedUserId && !state.knowledgePermissionList.length" description="该用户暂无知识库权限" />
        <div v-else class="permission-list">
          <div v-for="item in state.knowledgePermissionList" :key="item.id" class="permission-item" @click="fillKnowledgePermission(item)">
            <div class="permission-title">{{ item.baseName }}</div>
            <div class="meta-line">查看：{{ boolText(item.canView) }}，上传：{{ boolText(item.canUpload) }}</div>
            <div class="meta-line">训练：{{ boolText(item.canTrainSkill) }}，分析：{{ boolText(item.canAnalyze) }}</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">已授权技能</div>
        <van-loading v-if="state.loadingSkillPermissions" class="state-block" size="24px" vertical>加载中...</van-loading>
        <van-empty v-else-if="state.selectedUserId && !state.skillPermissionList.length" description="该用户暂无技能权限" />
        <div v-else class="permission-list">
          <div v-for="item in state.skillPermissionList" :key="item.id" class="permission-item" @click="fillSkillPermission(item)">
            <div class="permission-title">{{ item.skillName }}</div>
            <div class="meta-line">查看：{{ boolText(item.canView) }}，使用：{{ boolText(item.canUse) }}</div>
            <div class="meta-line">训练：{{ boolText(item.canTrain) }}，发布：{{ boolText(item.canPublish) }}</div>
          </div>
        </div>
      </section>
    </section>
  </AppPageShell>
</template>

<script setup>
import { onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import {
  queryAiProviderList,
  queryUserAiPermissionList,
  queryUserKnowledgePermissionList,
  queryUserSkillPermissionList,
  saveUserAiPermission,
  saveUserKnowledgePermission,
  saveUserSkillPermission
} from '@/api/ai'
import { queryKnowledgeBaseList } from '@/api/knowledge'
import { querySkillList } from '@/api/skill'
import { queryUserPageApi } from '@/api/user'

const state = reactive({
  loadingOptions: false,
  loadingAiPermissions: false,
  loadingKnowledgePermissions: false,
  loadingSkillPermissions: false,
  savingAiPermission: false,
  savingKnowledgePermission: false,
  savingSkillPermission: false,
  selectedUserId: '',
  userOptions: [],
  providerOptions: [],
  baseOptions: [],
  skillOptions: [],
  aiPermissionList: [],
  knowledgePermissionList: [],
  skillPermissionList: [],
  aiForm: createEmptyAiForm(),
  knowledgeForm: createEmptyKnowledgeForm(),
  skillForm: createEmptySkillForm()
})

function createEmptyAiForm() {
  return {
    providerConfigId: '',
    canManageProvider: false,
    canUseAi: true,
    canTrainSkill: false,
    canPublishSkill: false,
    canUseAgent: true,
    canRunAnalysis: true,
    status: 1
  }
}

function createEmptyKnowledgeForm() {
  return {
    baseId: '',
    canView: true,
    canUpload: false,
    canTrainSkill: false,
    canAnalyze: true,
    status: 1
  }
}

function createEmptySkillForm() {
  return {
    skillId: '',
    canView: true,
    canUse: true,
    canTrain: false,
    canPublish: false,
    status: 1
  }
}

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function boolText(value) {
  return value ? '是' : '否'
}

function resetAiForm() {
  state.aiForm = createEmptyAiForm()
}

function resetKnowledgeForm() {
  state.knowledgeForm = createEmptyKnowledgeForm()
}

function resetSkillForm() {
  state.skillForm = createEmptySkillForm()
}

async function reloadOptions() {
  state.loadingOptions = true
  try {
    const [users, providers, bases, skills] = await Promise.all([
      queryUserPageApi({ pageNo: 1, pageSize: 200 }),
      queryAiProviderList({ status: 1 }),
      queryKnowledgeBaseList({ status: 1 }),
      querySkillList({})
    ])
    const userData = ensureSuccess(users, '用户列表加载失败')
    state.userOptions = Array.isArray(userData?.list) ? userData.list : []
    state.providerOptions = ensureSuccess(providers, 'AI 接入加载失败') || []
    state.baseOptions = ensureSuccess(bases, '知识库加载失败') || []
    state.skillOptions = ensureSuccess(skills, '技能列表加载失败') || []
  } catch (error) {
    showToast(error.message || '基础数据加载失败')
  } finally {
    state.loadingOptions = false
  }
}

async function loadPermissionLists() {
  if (!state.selectedUserId) {
    state.aiPermissionList = []
    state.knowledgePermissionList = []
    state.skillPermissionList = []
    return
  }
  state.loadingAiPermissions = true
  state.loadingKnowledgePermissions = true
  state.loadingSkillPermissions = true
  try {
    const [aiRes, knowledgeRes, skillRes] = await Promise.all([
      queryUserAiPermissionList({ userId: Number(state.selectedUserId) }),
      queryUserKnowledgePermissionList({ userId: Number(state.selectedUserId) }),
      queryUserSkillPermissionList({ userId: Number(state.selectedUserId) })
    ])
    state.aiPermissionList = ensureSuccess(aiRes, 'AI 权限加载失败') || []
    state.knowledgePermissionList = ensureSuccess(knowledgeRes, '知识库权限加载失败') || []
    state.skillPermissionList = ensureSuccess(skillRes, '技能权限加载失败') || []
  } catch (error) {
    showToast(error.message || '权限列表加载失败')
  } finally {
    state.loadingAiPermissions = false
    state.loadingKnowledgePermissions = false
    state.loadingSkillPermissions = false
  }
}

function handleUserChange() {
  resetAiForm()
  resetKnowledgeForm()
  resetSkillForm()
  loadPermissionLists()
}

function fillAiPermission(item) {
  state.aiForm = {
    providerConfigId: String(item.providerConfigId),
    canManageProvider: Boolean(item.canManageProvider),
    canUseAi: Boolean(item.canUseAi),
    canTrainSkill: Boolean(item.canTrainSkill),
    canPublishSkill: Boolean(item.canPublishSkill),
    canUseAgent: Boolean(item.canUseAgent),
    canRunAnalysis: Boolean(item.canRunAnalysis),
    status: Number(item.status) === 1 ? 1 : 0
  }
}

function fillKnowledgePermission(item) {
  state.knowledgeForm = {
    baseId: String(item.baseId),
    canView: Boolean(item.canView),
    canUpload: Boolean(item.canUpload),
    canTrainSkill: Boolean(item.canTrainSkill),
    canAnalyze: Boolean(item.canAnalyze),
    status: Number(item.status) === 1 ? 1 : 0
  }
}

function fillSkillPermission(item) {
  state.skillForm = {
    skillId: String(item.skillId),
    canView: Boolean(item.canView),
    canUse: Boolean(item.canUse),
    canTrain: Boolean(item.canTrain),
    canPublish: Boolean(item.canPublish),
    status: Number(item.status) === 1 ? 1 : 0
  }
}

async function handleSaveAiPermission() {
  if (!state.selectedUserId) {
    showToast('请先选择用户')
    return
  }
  if (!state.aiForm.providerConfigId) {
    showToast('请选择 AI 接入')
    return
  }
  state.savingAiPermission = true
  try {
    ensureSuccess(await saveUserAiPermission({
      userId: Number(state.selectedUserId),
      providerConfigId: Number(state.aiForm.providerConfigId),
      canManageProvider: Boolean(state.aiForm.canManageProvider),
      canUseAi: Boolean(state.aiForm.canUseAi),
      canTrainSkill: Boolean(state.aiForm.canTrainSkill),
      canPublishSkill: Boolean(state.aiForm.canPublishSkill),
      canUseAgent: Boolean(state.aiForm.canUseAgent),
      canRunAnalysis: Boolean(state.aiForm.canRunAnalysis),
      status: Number(state.aiForm.status)
    }), 'AI 权限保存失败')
    showToast('AI 权限已保存')
    await loadPermissionLists()
  } catch (error) {
    showToast(error.message || 'AI 权限保存失败')
  } finally {
    state.savingAiPermission = false
  }
}

async function handleSaveKnowledgePermission() {
  if (!state.selectedUserId) {
    showToast('请先选择用户')
    return
  }
  if (!state.knowledgeForm.baseId) {
    showToast('请选择知识库')
    return
  }
  state.savingKnowledgePermission = true
  try {
    ensureSuccess(await saveUserKnowledgePermission({
      userId: Number(state.selectedUserId),
      baseId: Number(state.knowledgeForm.baseId),
      canView: Boolean(state.knowledgeForm.canView),
      canUpload: Boolean(state.knowledgeForm.canUpload),
      canTrainSkill: Boolean(state.knowledgeForm.canTrainSkill),
      canAnalyze: Boolean(state.knowledgeForm.canAnalyze),
      status: Number(state.knowledgeForm.status)
    }), '知识库权限保存失败')
    showToast('知识库权限已保存')
    await loadPermissionLists()
  } catch (error) {
    showToast(error.message || '知识库权限保存失败')
  } finally {
    state.savingKnowledgePermission = false
  }
}

async function handleSaveSkillPermission() {
  if (!state.selectedUserId) {
    showToast('请先选择用户')
    return
  }
  if (!state.skillForm.skillId) {
    showToast('请选择技能')
    return
  }
  state.savingSkillPermission = true
  try {
    ensureSuccess(await saveUserSkillPermission({
      userId: Number(state.selectedUserId),
      skillId: Number(state.skillForm.skillId),
      canView: Boolean(state.skillForm.canView),
      canUse: Boolean(state.skillForm.canUse),
      canTrain: Boolean(state.skillForm.canTrain),
      canPublish: Boolean(state.skillForm.canPublish),
      status: Number(state.skillForm.status)
    }), '技能权限保存失败')
    showToast('技能权限已保存')
    await loadPermissionLists()
  } catch (error) {
    showToast(error.message || '技能权限保存失败')
  } finally {
    state.savingSkillPermission = false
  }
}

onMounted(async () => {
  await reloadOptions()
})
</script>

<style scoped>
.panel-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.intro-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.panel {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.panel-title {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.select-field,
.switch-row {
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
  font-size: 14px;
  color: #323233;
}

.select-field select {
  flex: 1;
  border: 0;
  background: transparent;
  font-size: 14px;
}

.switch-list,
.permission-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.switch-row {
  justify-content: space-between;
}

.permission-item {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
  cursor: pointer;
}

.permission-title,
.meta-line {
  line-height: 1.7;
}

.permission-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 4px;
}

.meta-line {
  color: #4b5563;
  font-size: 13px;
}

.state-block {
  padding: 20px 0;
}

@media (max-width: 1200px) {
  .panel-grid,
  .intro-grid {
    grid-template-columns: 1fr;
  }
}
</style>

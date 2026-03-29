<template>
  <AppPageShell title="专家台账" description="查看技能归属、专家等级与授权结果。管理员可以在这里维护专家身份。">
    <template #actions>
      <div class="action-row">
        <van-button plain type="primary" :loading="state.loadingList" @click="fetchExperts">刷新</van-button>
      </div>
    </template>

    <section v-if="canManage" class="panel-grid" data-guide="expert-binding">
      <section class="panel">
        <div class="panel-title">专家绑定</div>
        <div class="select-field">
          <span class="select-label">用户</span>
          <select v-model="state.form.userId">
            <option value="">请选择用户</option>
            <option v-for="user in state.userOptions" :key="user.id" :value="String(user.id)">
              {{ user.realName || user.username }}（{{ user.username }}）
            </option>
          </select>
        </div>
        <div class="select-field">
          <span class="select-label">技能</span>
          <select v-model="state.form.skillId" @change="handleSkillChange">
            <option value="">请选择技能</option>
            <option v-for="item in state.skillOptions" :key="item.id" :value="String(item.id)">
              {{ item.skillName }}（{{ item.publishedVersionNo || '未发布' }}）
            </option>
          </select>
        </div>
        <div class="select-field">
          <span class="select-label">专家等级</span>
          <select v-model="state.form.expertLevel">
            <option value="NORMAL">NORMAL</option>
            <option value="SENIOR">SENIOR</option>
            <option value="LEAD">LEAD</option>
          </select>
        </div>
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.form.status">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="meta-line">当前选择版本：{{ selectedSkillVersionLabel }}</div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.saving" @click="handleSave">保存专家身份</van-button>
          <van-button size="small" plain @click="resetForm">清空</van-button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">管理说明</div>
        <div class="meta-line">管理员可将某个用户与某项技能绑定，形成“专家身份”。</div>
        <div class="meta-line">绑定时会记录对应技能版本，便于后续追溯专家依据。</div>
        <div class="meta-line">非管理员只能查看自己的专家台账，不能修改。</div>
      </section>
    </section>

    <section class="panel" data-guide="expert-list">
      <div class="panel-title">专家列表</div>
      <div class="panel-grid compact-grid">
        <div v-if="canManage" class="select-field filter-field">
          <span class="select-label">用户</span>
          <select v-model="state.query.userId">
            <option value="">全部</option>
            <option v-for="user in state.userOptions" :key="user.id" :value="String(user.id)">{{ user.realName || user.username }}</option>
          </select>
        </div>
        <div class="select-field filter-field">
          <span class="select-label">技能</span>
          <select v-model="state.query.skillId">
            <option value="">全部</option>
            <option v-for="item in state.skillOptions" :key="item.id" :value="String(item.id)">{{ item.skillName }}</option>
          </select>
        </div>
        <div class="select-field filter-field">
          <span class="select-label">状态</span>
          <select v-model="state.query.status">
            <option value="">全部</option>
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.loadingList" @click="fetchExperts">查询</van-button>
        <van-button size="small" plain @click="resetQuery">重置</van-button>
      </div>
      <div class="card-list">
        <div v-for="item in state.list" :key="item.id" class="card-item" @click="fillForm(item)">
          <div class="card-title">{{ item.skillName }}</div>
          <div class="meta-line">用户：{{ item.realName || item.username }}</div>
          <div class="meta-line">版本：{{ item.versionNo || '-' }}</div>
          <div class="meta-line">专家等级：{{ item.expertLevel || '-' }}</div>
          <div class="meta-line">状态：{{ Number(item.status) === 1 ? '启用' : '停用' }}</div>
          <div class="meta-line">授予时间：{{ item.grantTime || '-' }}</div>
        </div>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { queryCurrentAiPermission } from '@/api/ai'
import { queryExpertList, saveSkillOwner } from '@/api/expert'
import { querySkillList } from '@/api/skill'
import { queryUserPageApi } from '@/api/user'

const state = reactive({
  permissions: null,
  loadingList: false,
  saving: false,
  userOptions: [],
  skillOptions: [],
  list: [],
  query: { userId: '', skillId: '', status: '' },
  form: createEmptyForm()
})

const canManage = computed(() => Boolean(state.permissions?.admin))
const selectedSkill = computed(() => state.skillOptions.find((item) => String(item.id) === state.form.skillId) || null)
const selectedSkillVersionLabel = computed(() => selectedSkill.value?.publishedVersionNo || '未发布版本')

function createEmptyForm() {
  return {
    userId: '',
    skillId: '',
    skillVersionId: '',
    expertLevel: 'NORMAL',
    status: 1
  }
}

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function resetForm() {
  state.form = createEmptyForm()
}

function resetQuery() {
  state.query = { userId: '', skillId: '', status: '' }
  fetchExperts()
}

function handleSkillChange() {
  state.form.skillVersionId = selectedSkill.value?.publishedVersionId ? String(selectedSkill.value.publishedVersionId) : ''
}

function fillForm(item) {
  if (!canManage.value) {
    return
  }
  state.form = {
    userId: String(item.userId),
    skillId: String(item.skillId),
    skillVersionId: String(item.skillVersionId),
    expertLevel: item.expertLevel || 'NORMAL',
    status: Number(item.status) === 1 ? 1 : 0
  }
}

async function fetchPermissions() {
  state.permissions = ensureSuccess(await queryCurrentAiPermission(), '权限信息加载失败')
}

async function fetchSkillOptions() {
  const skills = await querySkillList({})
  state.skillOptions = ensureSuccess(skills, '技能列表加载失败') || []
}

async function fetchUserOptions() {
  const users = await queryUserPageApi({ pageNo: 1, pageSize: 200 })
  const userData = ensureSuccess(users, '用户列表加载失败')
  state.userOptions = Array.isArray(userData?.list) ? userData.list : []
}

async function fetchExperts() {
  state.loadingList = true
  try {
    const data = ensureSuccess(await queryExpertList({
      userId: state.query.userId ? Number(state.query.userId) : undefined,
      skillId: state.query.skillId ? Number(state.query.skillId) : undefined,
      status: state.query.status === '' ? undefined : Number(state.query.status)
    }), '专家列表加载失败')
    state.list = Array.isArray(data) ? data : []
  } catch (error) {
    showToast(error.message || '专家列表加载失败')
  } finally {
    state.loadingList = false
  }
}

async function handleSave() {
  if (!canManage.value) {
    showToast('当前用户无法维护专家台账')
    return
  }
  if (!state.form.userId || !state.form.skillId) {
    showToast('请选择用户和技能')
    return
  }
  if (!state.form.skillVersionId) {
    showToast('当前技能没有已发布版本，暂不能绑定专家身份')
    return
  }
  state.saving = true
  try {
    ensureSuccess(await saveSkillOwner({
      userId: Number(state.form.userId),
      skillId: Number(state.form.skillId),
      skillVersionId: Number(state.form.skillVersionId),
      expertLevel: state.form.expertLevel,
      status: Number(state.form.status)
    }), '专家绑定失败')
    showToast('专家身份已保存')
    resetForm()
    await fetchExperts()
  } catch (error) {
    showToast(error.message || '专家绑定失败')
  } finally {
    state.saving = false
  }
}

onMounted(async () => {
  try {
    await fetchPermissions()
    await fetchSkillOptions()
    if (canManage.value) {
      await fetchUserOptions()
    }
    await fetchExperts()
  } catch (error) {
    showToast(error.message || '页面初始化失败')
  }
})
</script>

<style scoped>
.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.compact-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.panel,
.card-item {
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.panel-title,
.card-title {
  font-weight: 600;
  color: #1f2937;
}

.action-row,
.card-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.card-list {
  flex-direction: column;
  margin-top: 12px;
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

.filter-field {
  margin: 0;
}

.select-label {
  flex: 0 0 72px;
}

.select-field select {
  flex: 1;
  border: 0;
  background: transparent;
}

.meta-line {
  color: #4b5563;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .panel-grid,
  .compact-grid {
    grid-template-columns: 1fr;
  }
}
</style>

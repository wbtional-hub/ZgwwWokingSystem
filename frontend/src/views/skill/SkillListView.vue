<template>
  <AppPageShell title="Skills 中心" description="在这里配置技能、训练版本、绑定知识库、录入验证题并执行验证。">
    <template #actions>
      <div class="action-row">
        <van-button plain type="primary" :loading="state.loadingSkills" @click="reloadAll">刷新</van-button>
      </div>
    </template>

    <section class="panel" data-guide="skills-list">
      <div class="panel-title">技能列表</div>
      <div class="panel-grid compact-grid">
        <van-field v-model.trim="state.query.keywords" label="关键字" placeholder="按技能编码或名称查询" />
        <van-field v-model.trim="state.query.domainType" label="领域" placeholder="例如：人才政策" />
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.loadingSkills" @click="fetchSkills">查询</van-button>
        <van-button size="small" plain @click="resetQuery">重置</van-button>
      </div>
      <div class="card-list">
        <div v-for="item in state.skillList" :key="item.id" class="card-item" :class="{ active: state.activeSkillId === item.id }" @click="selectSkill(item)">
          <div class="card-title">{{ item.skillName }}</div>
          <div class="meta-line">编码：{{ item.skillCode }}</div>
          <div class="meta-line">领域：{{ item.domainType || '-' }} / 类型：{{ item.skillType || '-' }}</div>
          <div class="meta-line">知识库：{{ item.baseName || '-' }}</div>
          <div class="meta-line">已发布版本：{{ item.publishedVersionNo || '未发布' }}</div>
        </div>
      </div>
    </section>

    <section v-if="showTrainPanels" class="panel-grid" data-guide="skills-version">
      <section class="panel">
        <div class="panel-title">技能维护</div>
        <div class="panel-hint">新建技能需要全局训练权限；已授权技能支持按技能训练。</div>
        <van-field v-model.trim="state.skillForm.skillCode" label="编码" placeholder="例如：talent_consultant" :readonly="Boolean(state.skillForm.id)" />
        <van-field v-model.trim="state.skillForm.skillName" label="名称" placeholder="例如：人才政策咨询专家" />
        <van-field v-model.trim="state.skillForm.domainType" label="领域" placeholder="例如：人才政策" />
        <van-field v-model.trim="state.skillForm.skillType" label="类型" placeholder="例如：CONSULTANT" />
        <van-field v-model="state.skillForm.description" label="说明" type="textarea" rows="3" autosize placeholder="技能说明" />
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.skillForm.status">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.savingSkill" @click="handleSaveSkill">保存技能</van-button>
          <van-button size="small" plain @click="resetSkillForm">清空</van-button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">技能版本配置</div>
        <div class="panel-hint">当前技能：{{ activeSkillName }}</div>
        <van-field v-model.trim="state.versionForm.versionNo" label="版本号" placeholder="例如：v1.0.0" :disabled="!state.activeSkillId || !canTrainCurrentSkill" />
        <div class="select-field">
          <span class="select-label">AI 接入</span>
          <select v-model="state.versionForm.providerConfigId" :disabled="!state.activeSkillId || !canTrainCurrentSkill">
            <option value="">请选择</option>
            <option v-for="item in state.providerOptions" :key="item.id" :value="String(item.id)">{{ item.providerName }}</option>
          </select>
        </div>
        <van-field v-model.trim="state.versionForm.modelCode" label="模型" placeholder="例如：gpt-5.4" :disabled="!state.activeSkillId || !canTrainCurrentSkill" />
        <van-field v-model="state.versionForm.systemPrompt" label="系统提示词" type="textarea" rows="4" autosize placeholder="角色设定和原则" :disabled="!state.activeSkillId || !canTrainCurrentSkill" />
        <van-field v-model="state.versionForm.taskPrompt" label="任务提示词" type="textarea" rows="3" autosize placeholder="任务范围说明" :disabled="!state.activeSkillId || !canTrainCurrentSkill" />
        <van-field v-model="state.versionForm.outputTemplate" label="输出模板" type="textarea" rows="3" autosize placeholder="输出格式模板" :disabled="!state.activeSkillId || !canTrainCurrentSkill" />
        <van-field v-model="state.versionForm.forbiddenRules" label="禁答规则" type="textarea" rows="2" autosize placeholder="禁止回答范围" :disabled="!state.activeSkillId || !canTrainCurrentSkill" />
        <van-field v-model="state.versionForm.citationRules" label="引用规则" type="textarea" rows="2" autosize placeholder="引用要求" :disabled="!state.activeSkillId || !canTrainCurrentSkill" />
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.savingVersion" :disabled="!state.activeSkillId || !canTrainCurrentSkill" @click="handleSaveVersion">保存版本</van-button>
          <van-button size="small" plain type="success" :disabled="!state.versionForm.id || !canPublishCurrentSkill" @click="handlePublishVersion">发布版本</van-button>
        </div>
      </section>
    </section>

    <section v-if="showTrainPanels && state.activeSkillId" class="panel-grid" data-guide="skills-binding">
      <section class="panel">
        <div class="panel-title">知识库绑定</div>
        <div class="select-field">
          <span class="select-label">知识库</span>
          <select v-model="state.bindingForm.baseId" :disabled="!state.versionForm.id || !canTrainCurrentSkill">
            <option value="">请选择</option>
            <option v-for="item in state.baseOptions" :key="item.id" :value="String(item.id)">{{ item.baseName }}</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :disabled="!state.versionForm.id || !state.bindingForm.baseId || !canTrainCurrentSkill" @click="handleSaveBinding">绑定知识库</van-button>
        </div>
        <div class="meta-line">当前绑定：{{ state.bindingBaseName || '-' }}</div>
      </section>

      <section class="panel">
        <div class="panel-title">验证题维护</div>
        <van-field v-model.trim="state.testCaseForm.caseType" label="类型" placeholder="例如：POLICY_QA" :disabled="!state.versionForm.id || !canTrainCurrentSkill" />
        <van-field v-model="state.testCaseForm.questionText" label="问题" type="textarea" rows="3" autosize placeholder="输入验证问题" :disabled="!state.versionForm.id || !canTrainCurrentSkill" />
        <van-field v-model="state.testCaseForm.expectedPoints" label="预期要点" type="textarea" rows="3" autosize placeholder="用换行或逗号分隔" :disabled="!state.versionForm.id || !canTrainCurrentSkill" />
        <van-field v-model="state.testCaseForm.expectedFormat" label="预期格式" placeholder="例如：列点说明+引用依据" :disabled="!state.versionForm.id || !canTrainCurrentSkill" />
        <van-field v-model="state.testCaseForm.standardAnswer" label="参考答案" type="textarea" rows="3" autosize placeholder="可选" :disabled="!state.versionForm.id || !canTrainCurrentSkill" />
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.savingTestCase" :disabled="!state.versionForm.id || !canTrainCurrentSkill" @click="handleSaveTestCase">保存验证题</van-button>
          <van-button size="small" plain :disabled="!state.versionForm.id || !canTrainCurrentSkill" @click="resetTestCaseForm">清空</van-button>
        </div>
        <div class="permission-list">
          <div v-for="item in state.testCaseList" :key="item.id" class="permission-item" @click="fillTestCaseForm(item)">
            <div class="permission-title">{{ item.caseType }} / {{ item.questionText }}</div>
            <div class="meta-line">要点：{{ item.expectedPoints || '-' }}</div>
          </div>
        </div>
      </section>
    </section>

    <section v-if="showTrainPanels && state.activeSkillId" class="panel" data-guide="skills-validation">
      <div class="panel-title">技能验证</div>
      <div class="panel-hint">当前版本：{{ state.versionForm.versionNo || '-' }}</div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.runningValidation" :disabled="!state.versionForm.id || !canTrainCurrentSkill" @click="handleRunValidation">执行验证</van-button>
      </div>
      <div v-if="state.validationDetail" class="validation-box">
        <div class="meta-line">状态：{{ state.validationDetail.runStatus }}</div>
        <div class="meta-line">通过率：{{ state.validationDetail.passRate }}%</div>
        <div class="meta-line">引用率：{{ state.validationDetail.citationRate }}%</div>
        <div class="meta-line">平均分：{{ state.validationDetail.avgScore }}</div>
        <div class="permission-list">
          <div v-for="item in state.validationDetail.results || []" :key="item.id" class="permission-item">
            <div class="permission-title">{{ item.questionText }}</div>
            <div class="meta-line">分数：{{ item.score }} / 通过：{{ item.isPass ? '是' : '否' }}</div>
            <div class="meta-line">失败原因：{{ item.failReason || '-' }}</div>
            <div class="meta-line">回答：{{ item.answerText || '-' }}</div>
          </div>
        </div>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { queryAiProviderList, queryCurrentAiPermission } from '@/api/ai'
import { queryKnowledgeBaseList } from '@/api/knowledge'
import {
  getPublishedSkillVersion,
  publishSkillVersion,
  querySkillList,
  querySkillTestCaseList,
  runSkillValidation,
  saveSkill,
  saveSkillBinding,
  saveSkillTestCase,
  saveSkillVersion,
  getSkillValidationDetail
} from '@/api/skill'

const state = reactive({
  permissions: null,
  loadingSkills: false,
  savingSkill: false,
  savingVersion: false,
  savingTestCase: false,
  runningValidation: false,
  skillList: [],
  providerOptions: [],
  baseOptions: [],
  activeSkillId: null,
  bindingBaseName: '',
  validationDetail: null,
  testCaseList: [],
  query: { keywords: '', domainType: '' },
  skillForm: createEmptySkillForm(),
  versionForm: createEmptyVersionForm(),
  bindingForm: { baseId: '' },
  testCaseForm: createEmptyTestCaseForm()
})

const activeSkillName = computed(() => state.skillList.find((item) => item.id === state.activeSkillId)?.skillName || '未选择')
const genericTrainPermission = computed(() => Boolean(state.permissions?.admin || (state.permissions?.aiPermissions || []).some((item) => item.canTrainSkill)))
const genericPublishPermission = computed(() => Boolean(state.permissions?.admin || (state.permissions?.aiPermissions || []).some((item) => item.canPublishSkill)))
const activeSkillPermission = computed(() => (state.permissions?.skillPermissions || []).find((item) => item.skillId === state.activeSkillId) || null)
const canTrainCurrentSkill = computed(() => Boolean(genericTrainPermission.value || activeSkillPermission.value?.canTrain || activeSkillPermission.value?.canPublish))
const canPublishCurrentSkill = computed(() => Boolean(genericPublishPermission.value || activeSkillPermission.value?.canPublish))
const showTrainPanels = computed(() => Boolean(genericTrainPermission.value || state.activeSkillId && activeSkillPermission.value?.canTrain || state.activeSkillId && activeSkillPermission.value?.canPublish))

function createEmptySkillForm() {
  return { id: null, skillCode: '', skillName: '', domainType: '', skillType: '', description: '', status: 1 }
}

function createEmptyVersionForm() {
  return { id: null, versionNo: 'v1.0.0', providerConfigId: '', modelCode: '', systemPrompt: '', taskPrompt: '', outputTemplate: '', forbiddenRules: '', citationRules: '' }
}

function createEmptyTestCaseForm() {
  return { id: null, caseType: 'POLICY_QA', questionText: '', expectedPoints: '', expectedFormat: '', standardAnswer: '' }
}

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) throw new Error(response?.message || fallback)
  return response.data
}

function resetQuery() {
  state.query.keywords = ''
  state.query.domainType = ''
  fetchSkills()
}

function resetSkillForm() {
  state.skillForm = createEmptySkillForm()
}

function resetVersionForm() {
  state.versionForm = createEmptyVersionForm()
  state.bindingForm.baseId = ''
  state.bindingBaseName = ''
}

function resetTestCaseForm() {
  state.testCaseForm = createEmptyTestCaseForm()
}

async function fetchPermissions() {
  state.permissions = ensureSuccess(await queryCurrentAiPermission(), '权限信息加载失败')
}

async function fetchProvidersAndBases() {
  state.providerOptions = ensureSuccess(await queryAiProviderList({ status: 1 }), 'AI 接入加载失败') || []
  state.baseOptions = ensureSuccess(await queryKnowledgeBaseList({ status: 1 }), '知识库加载失败') || []
}

async function fetchSkills() {
  state.loadingSkills = true
  try {
    const data = ensureSuccess(await querySkillList({
      keywords: state.query.keywords || undefined,
      domainType: state.query.domainType || undefined
    }), '技能列表加载失败')
    state.skillList = Array.isArray(data) ? data : []
  } catch (error) {
    showToast(error.message || '技能列表加载失败')
  } finally {
    state.loadingSkills = false
  }
}

async function selectSkill(item) {
  state.activeSkillId = item.id
  state.skillForm = {
    id: item.id,
    skillCode: item.skillCode || '',
    skillName: item.skillName || '',
    domainType: item.domainType || '',
    skillType: item.skillType || '',
    description: item.description || '',
    status: Number(item.status) === 1 ? 1 : 0
  }
  resetVersionForm()
  resetTestCaseForm()
  state.validationDetail = null
  try {
    const version = ensureSuccess(await getPublishedSkillVersion(item.id), '技能版本加载失败')
    if (version) {
      state.versionForm = {
        id: version.id,
        versionNo: version.versionNo || 'v1.0.0',
        providerConfigId: version.providerConfigId ? String(version.providerConfigId) : '',
        modelCode: version.modelCode || '',
        systemPrompt: version.systemPrompt || '',
        taskPrompt: version.taskPrompt || '',
        outputTemplate: version.outputTemplate || '',
        forbiddenRules: version.forbiddenRules || '',
        citationRules: version.citationRules || ''
      }
      state.bindingForm.baseId = version.baseId ? String(version.baseId) : ''
      state.bindingBaseName = version.baseName || ''
      if (canTrainCurrentSkill.value) {
        await fetchTestCases(version.id)
      } else {
        state.testCaseList = []
      }
    } else {
      state.testCaseList = []
    }
  } catch (error) {
    state.testCaseList = []
  }
}

async function handleSaveSkill() {
  if (!state.skillForm.id && !genericTrainPermission.value) {
    showToast('当前用户未获得新建技能权限')
    return
  }
  if (state.skillForm.id && !canTrainCurrentSkill.value) {
    showToast('当前用户未获得该技能训练权限')
    return
  }
  if (!state.skillForm.skillCode.trim() || !state.skillForm.skillName.trim()) {
    showToast('请填写技能编码和名称')
    return
  }
  state.savingSkill = true
  try {
    const skillId = ensureSuccess(await saveSkill({
      id: state.skillForm.id || undefined,
      skillCode: state.skillForm.skillCode.trim(),
      skillName: state.skillForm.skillName.trim(),
      domainType: state.skillForm.domainType.trim() || undefined,
      skillType: state.skillForm.skillType.trim() || undefined,
      description: state.skillForm.description || undefined,
      status: Number(state.skillForm.status)
    }), '技能保存失败')
    showToast('技能已保存')
    state.activeSkillId = skillId
    await fetchPermissions()
    await fetchSkills()
  } catch (error) {
    showToast(error.message || '技能保存失败')
  } finally {
    state.savingSkill = false
  }
}

async function handleSaveVersion() {
  if (!state.activeSkillId) {
    showToast('请先选择技能')
    return
  }
  if (!canTrainCurrentSkill.value) {
    showToast('当前用户未获得该技能训练权限')
    return
  }
  if (!state.versionForm.versionNo.trim() || !state.versionForm.systemPrompt.trim()) {
    showToast('请填写版本号和系统提示词')
    return
  }
  state.savingVersion = true
  try {
    const versionId = ensureSuccess(await saveSkillVersion({
      id: state.versionForm.id || undefined,
      skillId: state.activeSkillId,
      versionNo: state.versionForm.versionNo.trim(),
      providerConfigId: state.versionForm.providerConfigId ? Number(state.versionForm.providerConfigId) : undefined,
      modelCode: state.versionForm.modelCode.trim() || undefined,
      systemPrompt: state.versionForm.systemPrompt,
      taskPrompt: state.versionForm.taskPrompt || undefined,
      outputTemplate: state.versionForm.outputTemplate || undefined,
      forbiddenRules: state.versionForm.forbiddenRules || undefined,
      citationRules: state.versionForm.citationRules || undefined
    }), '版本保存失败')
    state.versionForm.id = versionId
    showToast('技能版本已保存')
    await fetchSkills()
  } catch (error) {
    showToast(error.message || '版本保存失败')
  } finally {
    state.savingVersion = false
  }
}

async function handlePublishVersion() {
  if (!canPublishCurrentSkill.value) {
    showToast('当前用户未获得该技能发布权限')
    return
  }
  try {
    ensureSuccess(await publishSkillVersion({ skillVersionId: state.versionForm.id }), '发布失败')
    showToast('技能版本已发布')
    await fetchSkills()
  } catch (error) {
    showToast(error.message || '发布失败')
  }
}

async function handleSaveBinding() {
  if (!canTrainCurrentSkill.value) {
    showToast('当前用户未获得该技能训练权限')
    return
  }
  try {
    ensureSuccess(await saveSkillBinding({
      skillId: state.activeSkillId,
      skillVersionId: state.versionForm.id,
      baseId: Number(state.bindingForm.baseId)
    }), '知识库绑定失败')
    const base = state.baseOptions.find((item) => String(item.id) === state.bindingForm.baseId)
    state.bindingBaseName = base?.baseName || ''
    showToast('知识库绑定成功')
    await fetchSkills()
  } catch (error) {
    showToast(error.message || '知识库绑定失败')
  }
}

async function fetchTestCases(skillVersionId) {
  const data = ensureSuccess(await querySkillTestCaseList({ skillVersionId }), '验证题加载失败')
  state.testCaseList = Array.isArray(data) ? data : []
}

function fillTestCaseForm(item) {
  state.testCaseForm = {
    id: item.id,
    caseType: item.caseType || 'POLICY_QA',
    questionText: item.questionText || '',
    expectedPoints: item.expectedPoints || '',
    expectedFormat: item.expectedFormat || '',
    standardAnswer: item.standardAnswer || ''
  }
}

async function handleSaveTestCase() {
  if (!state.versionForm.id) {
    showToast('请先保存技能版本')
    return
  }
  if (!canTrainCurrentSkill.value) {
    showToast('当前用户未获得该技能训练权限')
    return
  }
  state.savingTestCase = true
  try {
    ensureSuccess(await saveSkillTestCase({
      id: state.testCaseForm.id || undefined,
      skillId: state.activeSkillId,
      skillVersionId: state.versionForm.id,
      caseType: state.testCaseForm.caseType,
      questionText: state.testCaseForm.questionText,
      expectedPoints: state.testCaseForm.expectedPoints || undefined,
      expectedFormat: state.testCaseForm.expectedFormat || undefined,
      standardAnswer: state.testCaseForm.standardAnswer || undefined,
      status: 1
    }), '验证题保存失败')
    showToast('验证题已保存')
    resetTestCaseForm()
    await fetchTestCases(state.versionForm.id)
  } catch (error) {
    showToast(error.message || '验证题保存失败')
  } finally {
    state.savingTestCase = false
  }
}

async function handleRunValidation() {
  if (!canTrainCurrentSkill.value) {
    showToast('当前用户未获得该技能训练权限')
    return
  }
  state.runningValidation = true
  try {
    const run = ensureSuccess(await runSkillValidation({ skillVersionId: state.versionForm.id }), '执行验证失败')
    if (run?.runId) {
      state.validationDetail = ensureSuccess(await getSkillValidationDetail(run.runId), '验证详情加载失败')
    }
    showToast('技能验证完成')
  } catch (error) {
    showToast(error.message || '执行验证失败')
  } finally {
    state.runningValidation = false
  }
}

async function reloadAll() {
  await Promise.all([fetchPermissions(), fetchProvidersAndBases(), fetchSkills()])
}

onMounted(async () => {
  await reloadAll()
})
</script>

<style scoped>
.panel,
.card-item,
.permission-item {
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
}

.panel {
  margin-bottom: 16px;
  padding: 16px;
}

.panel-title {
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
}

.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.compact-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin-bottom: 8px;
}

.action-row,
.card-list,
.permission-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.card-list,
.permission-list {
  flex-direction: column;
  margin-top: 12px;
}

.card-item,
.permission-item {
  padding: 12px;
  cursor: pointer;
}

.card-item.active {
  border-color: #1677ff;
  background: #eff6ff;
}

.card-title,
.permission-title {
  font-weight: 600;
  color: #1f2937;
}

.meta-line,
.panel-hint {
  color: #4b5563;
  font-size: 13px;
  line-height: 1.7;
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

.validation-box {
  margin-top: 12px;
}

@media (max-width: 960px) {
  .panel-grid,
  .compact-grid {
    grid-template-columns: 1fr;
  }
}
</style>

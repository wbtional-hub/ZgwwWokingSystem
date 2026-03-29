<template>
  <AppPageShell title="知识库管理" description="第一期先把知识输入、文档导入、切片查看和检索链路跑通，后续再叠加技能训练和权限细分。">
    <template #actions>
      <div class="action-row">
        <van-button plain type="primary" :loading="state.baseLoading || state.documentLoading || state.searchLoading" @click="reloadAll">刷新数据</van-button>
      </div>
    </template>

    <section class="panel-grid" data-guide="knowledge-base">
      <section class="panel">
        <div class="panel-title">知识库查询</div>
        <van-field v-model.trim="state.baseQuery.keywords" label="关键字" placeholder="按编码或名称查询" />
        <van-field v-model.trim="state.baseQuery.domainType" label="领域" placeholder="例如：人才政策" />
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.baseQuery.status">
            <option value="">全部</option>
            <option value="1">启用</option>
            <option value="0">停用</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.baseLoading" @click="fetchBases">查询</van-button>
          <van-button size="small" plain @click="resetBaseQuery">重置</van-button>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">知识库维护</div>
        <van-field v-model.trim="state.baseForm.baseCode" label="编码" placeholder="例如：talent_policy" :readonly="Boolean(state.baseForm.id)" />
        <van-field v-model.trim="state.baseForm.baseName" label="名称" placeholder="例如：人才政策知识库" />
        <van-field v-model.trim="state.baseForm.domainType" label="领域" placeholder="例如：人才政策" />
        <van-field v-model="state.baseForm.description" label="说明" type="textarea" rows="3" autosize placeholder="知识库用途说明" />
        <div class="select-field">
          <span class="select-label">状态</span>
          <select v-model="state.baseForm.status">
            <option :value="1">启用</option>
            <option :value="0">停用</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.baseSubmitting" @click="handleSaveBase">保存</van-button>
          <van-button size="small" plain @click="resetBaseForm">清空</van-button>
        </div>
      </section>
    </section>

    <section class="panel">
      <div class="panel-title">知识库列表</div>
      <div class="panel-hint">当前共 {{ state.baseList.length }} 个知识库，点击“进入”即可切换当前工作知识库。</div>
      <van-empty v-if="!state.baseLoading && !state.baseList.length" description="暂无知识库数据" />
      <van-loading v-else-if="state.baseLoading" class="state-block" size="24px" vertical>加载中...</van-loading>
      <div v-else class="card-list">
        <van-card v-for="item in state.baseList" :key="item.id" class="knowledge-card">
          <template #title>
            <div class="card-title-row">
              <div class="title-with-badge">
                <span>{{ item.baseName }}</span>
                <van-tag :type="Number(item.status) === 1 ? 'success' : 'danger'">{{ Number(item.status) === 1 ? '启用' : '停用' }}</van-tag>
              </div>
              <van-tag :type="state.activeBaseId === item.id ? 'primary' : 'default'">{{ state.activeBaseId === item.id ? '当前' : '待选' }}</van-tag>
            </div>
          </template>
          <template #desc>
            <div class="meta-line">编码：{{ item.baseCode }}</div>
            <div class="meta-line">领域：{{ item.domainType || '-' }}</div>
            <div class="meta-line">文档数：{{ item.documentCount || 0 }}，Chunk 数：{{ item.chunkCount || 0 }}</div>
            <div class="meta-line">说明：{{ item.description || '-' }}</div>
            <div class="meta-line">更新时间：{{ formatDateTime(item.updateTime) }}</div>
          </template>
          <template #footer>
            <div class="action-row">
              <van-button size="small" plain type="primary" @click="activateBase(item)">进入</van-button>
              <van-button size="small" plain type="warning" @click="fillBaseForm(item)">编辑</van-button>
              <van-button size="small" plain :type="Number(item.status) === 1 ? 'danger' : 'success'" :loading="state.baseTogglingId === item.id" @click="handleToggleBaseStatus(item)">
                {{ Number(item.status) === 1 ? '停用' : '启用' }}
              </van-button>
            </div>
          </template>
        </van-card>
      </div>
    </section>

    <section class="panel" data-guide="knowledge-import">
      <div class="panel-title">文档导入</div>
      <div class="panel-hint">当前知识库：{{ activeBaseName }}</div>
      <div v-if="!state.activeBaseId" class="panel-hint">请先从上方选择一个知识库，再上传 Word 文档。</div>
      <template v-else>
        <van-field v-model.trim="state.importForm.policyRegion" label="适用地区" placeholder="例如：上海市" />
        <van-field v-model.trim="state.importForm.policyLevel" label="政策级别" placeholder="例如：市级" />
        <van-field v-model.trim="state.importForm.keywords" label="关键字" placeholder="多个关键字用逗号分隔" />
        <van-field v-model="state.importForm.summary" label="摘要" type="textarea" rows="3" autosize placeholder="可选，留空则自动抽取" />
        <van-field v-model="state.importForm.effectiveDate" label="生效日期" placeholder="YYYY-MM-DD" />
        <van-field v-model="state.importForm.expireDate" label="失效日期" placeholder="YYYY-MM-DD" />
        <div class="file-field">
          <span class="select-label">Word 文件</span>
          <input type="file" accept=".docx" @change="handleFileChange" />
        </div>
        <div class="panel-hint">已选择文件：{{ state.importFile ? state.importFile.name : '未选择' }}</div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.importing" @click="handleImport">上传并入库</van-button>
          <van-button size="small" plain @click="clearImportForm">清空</van-button>
        </div>
        <div v-if="state.lastImportResult" class="panel-hint import-result">
          最近一次导入：状态 {{ state.lastImportResult.jobStatus || '-' }}，文档ID {{ state.lastImportResult.documentId || '-' }}，切片 {{ state.lastImportResult.successChunks || 0 }}/{{ state.lastImportResult.totalChunks || 0 }}
        </div>
      </template>
    </section>

    <section class="panel-grid">
      <section class="panel">
        <div class="panel-title">文档列表</div>
        <div class="panel-hint">当前知识库：{{ activeBaseName }}</div>
        <van-field v-model.trim="state.documentQuery.keywords" label="关键字" placeholder="按标题或关键字搜索" :disabled="!state.activeBaseId" />
        <van-field v-model.trim="state.documentQuery.policyRegion" label="地区" placeholder="例如：上海" :disabled="!state.activeBaseId" />
        <div class="select-field">
          <span class="select-label">解析</span>
          <select v-model="state.documentQuery.parseStatus" :disabled="!state.activeBaseId">
            <option value="">全部</option>
            <option value="SUCCESS">成功</option>
            <option value="FAILED">失败</option>
            <option value="PENDING">待处理</option>
          </select>
        </div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.documentLoading" :disabled="!state.activeBaseId" @click="fetchDocuments">查询文档</van-button>
          <van-button size="small" plain :disabled="!state.activeBaseId" @click="resetDocumentQuery">重置</van-button>
        </div>

        <van-empty v-if="state.activeBaseId && !state.documentLoading && !state.documentList.length" description="暂无文档" />
        <van-loading v-else-if="state.documentLoading" class="state-block" size="24px" vertical>加载中...</van-loading>
        <div v-else class="doc-list">
          <div v-for="item in state.documentList" :key="item.id" class="doc-item" :class="{ 'doc-item--active': state.activeDocumentId === item.id }" @click="loadDocument(item)">
            <div class="doc-item-title">{{ item.docTitle }}</div>
            <div class="meta-line">地区：{{ item.policyRegion || '-' }}</div>
            <div class="meta-line">级别：{{ item.policyLevel || '-' }}</div>
            <div class="meta-line">解析：{{ item.parseStatus || '-' }}</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">文档详情与 Chunk</div>
        <div v-if="!state.documentDetail" class="panel-hint">从左侧选择一份文档后，可查看详情和切片内容。</div>
        <template v-else>
          <div class="meta-line">标题：{{ state.documentDetail.docTitle }}</div>
          <div class="meta-line">文件：{{ state.documentDetail.fileName || '-' }}</div>
          <div class="meta-line">地区：{{ state.documentDetail.policyRegion || '-' }}</div>
          <div class="meta-line">级别：{{ state.documentDetail.policyLevel || '-' }}</div>
          <div class="meta-line">摘要：{{ state.documentDetail.summary || '-' }}</div>
          <div class="meta-line">关键字：{{ state.documentDetail.keywords || '-' }}</div>
          <div class="meta-line">状态：{{ state.documentDetail.parseStatus || '-' }}</div>
          <div class="meta-line">更新时间：{{ formatDateTime(state.documentDetail.updateTime) }}</div>
          <div class="chunk-list">
            <div v-for="chunk in state.chunkList" :key="chunk.id" class="chunk-item">
              <div class="chunk-head">Chunk #{{ chunk.chunkNo }} · {{ chunk.headingPath || '未标记标题路径' }}</div>
              <div class="chunk-text">{{ chunk.contentText }}</div>
            </div>
          </div>
        </template>
      </section>
    </section>

    <section class="panel" data-guide="knowledge-search">
      <div class="panel-title">知识检索</div>
      <div class="panel-hint">这一步是后续技能训练和 AI 分析的基础召回能力。</div>
      <van-field v-model.trim="state.searchForm.keywords" label="问题/关键字" placeholder="例如：A类人才补贴标准" :disabled="!state.activeBaseId" />
      <van-field v-model.trim="state.searchForm.policyRegion" label="地区" placeholder="例如：上海" :disabled="!state.activeBaseId" />
      <van-field v-model="state.searchForm.topN" label="返回条数" type="number" placeholder="默认 10" :disabled="!state.activeBaseId" />
      <div class="switch-row">
        <span class="select-label">仅生效政策</span>
        <van-switch v-model="state.searchForm.effectiveOnly" :disabled="!state.activeBaseId" />
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.searchLoading" :disabled="!state.activeBaseId" @click="handleSearch">执行检索</van-button>
        <van-button size="small" plain :disabled="!state.activeBaseId" @click="resetSearchForm">重置</van-button>
      </div>
      <van-empty v-if="state.activeBaseId && !state.searchLoading && !state.searchResults.length" description="暂无检索结果" />
      <van-loading v-else-if="state.searchLoading" class="state-block" size="24px" vertical>检索中...</van-loading>
      <div v-else class="search-list">
        <div v-for="item in state.searchResults" :key="`${item.chunkId}-${item.documentId}`" class="search-item">
          <div class="search-item-title">{{ item.docTitle }}</div>
          <div class="meta-line">地区：{{ item.policyRegion || '-' }}</div>
          <div class="meta-line">定位：{{ item.headingPath || '未标记标题路径' }} / Chunk #{{ item.chunkNo }}</div>
          <div class="snippet-text">{{ item.snippet }}</div>
        </div>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import {
  getKnowledgeDocumentDetail,
  importKnowledgeDocx,
  queryKnowledgeBaseList,
  queryKnowledgeChunkList,
  queryKnowledgeDocumentList,
  saveKnowledgeBase,
  searchKnowledge,
  toggleKnowledgeBaseStatus
} from '@/api/knowledge'

const state = reactive({
  baseLoading: false,
  baseSubmitting: false,
  baseTogglingId: null,
  importing: false,
  documentLoading: false,
  searchLoading: false,
  baseList: [],
  documentList: [],
  chunkList: [],
  searchResults: [],
  documentDetail: null,
  activeBaseId: null,
  activeDocumentId: null,
  importFile: null,
  lastImportResult: null,
  baseQuery: {
    keywords: '',
    domainType: '',
    status: ''
  },
  baseForm: createEmptyBaseForm(),
  importForm: createEmptyImportForm(),
  documentQuery: {
    keywords: '',
    policyRegion: '',
    parseStatus: ''
  },
  searchForm: {
    keywords: '',
    policyRegion: '',
    effectiveOnly: true,
    topN: 10
  }
})

const activeBaseName = computed(() => {
  const matched = state.baseList.find((item) => item.id === state.activeBaseId)
  return matched ? matched.baseName : '未选择'
})

function createEmptyBaseForm() {
  return {
    id: null,
    baseCode: '',
    baseName: '',
    domainType: '',
    description: '',
    status: 1
  }
}

function createEmptyImportForm() {
  return {
    policyRegion: '',
    policyLevel: '',
    keywords: '',
    summary: '',
    effectiveDate: '',
    expireDate: ''
  }
}

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

function resetBaseQuery() {
  state.baseQuery.keywords = ''
  state.baseQuery.domainType = ''
  state.baseQuery.status = ''
  fetchBases()
}

function resetBaseForm() {
  state.baseForm = createEmptyBaseForm()
}

function fillBaseForm(item) {
  state.baseForm = {
    id: item.id,
    baseCode: item.baseCode || '',
    baseName: item.baseName || '',
    domainType: item.domainType || '',
    description: item.description || '',
    status: Number(item.status) === 1 ? 1 : 0
  }
}

async function fetchBases() {
  state.baseLoading = true
  try {
    const data = ensureSuccess(await queryKnowledgeBaseList({
      keywords: state.baseQuery.keywords || undefined,
      domainType: state.baseQuery.domainType || undefined,
      status: state.baseQuery.status === '' ? undefined : Number(state.baseQuery.status)
    }), '知识库列表加载失败')
    state.baseList = Array.isArray(data) ? data : []
    if (!state.activeBaseId && state.baseList.length) {
      activateBase(state.baseList[0])
      return
    }
    if (state.activeBaseId && !state.baseList.find((item) => item.id === state.activeBaseId)) {
      state.activeBaseId = null
      state.activeDocumentId = null
      state.documentList = []
      state.documentDetail = null
      state.chunkList = []
      state.searchResults = []
    }
  } catch (error) {
    showToast(error.message || '知识库列表加载失败')
  } finally {
    state.baseLoading = false
  }
}

async function handleSaveBase() {
  if (!state.baseForm.baseCode.trim()) {
    showToast('请输入知识库编码')
    return
  }
  if (!state.baseForm.baseName.trim()) {
    showToast('请输入知识库名称')
    return
  }
  state.baseSubmitting = true
  try {
    ensureSuccess(await saveKnowledgeBase({
      id: state.baseForm.id || undefined,
      baseCode: state.baseForm.baseCode.trim(),
      baseName: state.baseForm.baseName.trim(),
      domainType: state.baseForm.domainType.trim() || undefined,
      description: state.baseForm.description || undefined,
      status: Number(state.baseForm.status)
    }), '知识库保存失败')
    showToast(state.baseForm.id ? '知识库已更新' : '知识库已创建')
    resetBaseForm()
    await fetchBases()
  } catch (error) {
    showToast(error.message || '知识库保存失败')
  } finally {
    state.baseSubmitting = false
  }
}

async function handleToggleBaseStatus(item) {
  const nextStatus = Number(item.status) === 1 ? 0 : 1
  state.baseTogglingId = item.id
  try {
    await showConfirmDialog({
      title: nextStatus === 1 ? '确认启用' : '确认停用',
      message: `${nextStatus === 1 ? '启用' : '停用'}知识库 ${item.baseName} 吗？`
    })
    ensureSuccess(await toggleKnowledgeBaseStatus({
      id: item.id,
      status: nextStatus
    }), '知识库状态更新失败')
    showToast(nextStatus === 1 ? '知识库已启用' : '知识库已停用')
    await fetchBases()
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || '知识库状态更新失败')
    }
  } finally {
    state.baseTogglingId = null
  }
}

function clearImportForm() {
  state.importForm = createEmptyImportForm()
  state.importFile = null
}

function handleFileChange(event) {
  const files = event?.target?.files
  state.importFile = files && files.length ? files[0] : null
}

async function handleImport() {
  if (!state.activeBaseId) {
    showToast('请先选择知识库')
    return
  }
  if (!state.importFile) {
    showToast('请选择 docx 文件')
    return
  }
  const formData = new FormData()
  formData.append('baseId', state.activeBaseId)
  formData.append('policyRegion', state.importForm.policyRegion || '')
  formData.append('policyLevel', state.importForm.policyLevel || '')
  formData.append('keywords', state.importForm.keywords || '')
  formData.append('summary', state.importForm.summary || '')
  formData.append('effectiveDate', state.importForm.effectiveDate || '')
  formData.append('expireDate', state.importForm.expireDate || '')
  formData.append('file', state.importFile)
  state.importing = true
  try {
    const data = ensureSuccess(await importKnowledgeDocx(formData), '文档导入失败')
    state.lastImportResult = data
    showToast('Word 文档导入成功')
    clearImportForm()
    await fetchDocuments()
    await fetchBases()
  } catch (error) {
    showToast(error.message || '文档导入失败')
  } finally {
    state.importing = false
  }
}

async function fetchDocuments() {
  if (!state.activeBaseId) {
    state.documentList = []
    return
  }
  state.documentLoading = true
  try {
    const data = ensureSuccess(await queryKnowledgeDocumentList({
      baseId: state.activeBaseId,
      keywords: state.documentQuery.keywords || undefined,
      policyRegion: state.documentQuery.policyRegion || undefined,
      parseStatus: state.documentQuery.parseStatus || undefined
    }), '文档列表加载失败')
    state.documentList = Array.isArray(data) ? data : []
  } catch (error) {
    showToast(error.message || '文档列表加载失败')
  } finally {
    state.documentLoading = false
  }
}

function resetDocumentQuery() {
  state.documentQuery.keywords = ''
  state.documentQuery.policyRegion = ''
  state.documentQuery.parseStatus = ''
  fetchDocuments()
}

async function loadDocument(item) {
  state.activeDocumentId = item.id
  try {
    const [detailResponse, chunkResponse] = await Promise.all([
      getKnowledgeDocumentDetail(item.id),
      queryKnowledgeChunkList({ documentId: item.id })
    ])
    state.documentDetail = ensureSuccess(detailResponse, '文档详情加载失败')
    state.chunkList = ensureSuccess(chunkResponse, '文档切片加载失败') || []
  } catch (error) {
    showToast(error.message || '文档详情加载失败')
  }
}

async function handleSearch() {
  if (!state.activeBaseId) {
    showToast('请先选择知识库')
    return
  }
  state.searchLoading = true
  try {
    const data = ensureSuccess(await searchKnowledge({
      baseId: state.activeBaseId,
      keywords: state.searchForm.keywords || undefined,
      policyRegion: state.searchForm.policyRegion || undefined,
      effectiveOnly: Boolean(state.searchForm.effectiveOnly),
      topN: Number(state.searchForm.topN) || 10
    }), '知识检索失败')
    state.searchResults = Array.isArray(data) ? data : []
  } catch (error) {
    showToast(error.message || '知识检索失败')
  } finally {
    state.searchLoading = false
  }
}

function resetSearchForm() {
  state.searchForm.keywords = ''
  state.searchForm.policyRegion = ''
  state.searchForm.effectiveOnly = true
  state.searchForm.topN = 10
  state.searchResults = []
}

function activateBase(item) {
  state.activeBaseId = item.id
  state.activeDocumentId = null
  state.documentDetail = null
  state.chunkList = []
  state.searchResults = []
  fetchDocuments()
}

async function reloadAll() {
  await fetchBases()
  if (state.activeBaseId) {
    await fetchDocuments()
  }
}

onMounted(async () => {
  await fetchBases()
})
</script>

<style scoped>
.panel-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 16px;
}

.panel {
  margin-bottom: 16px;
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

.panel-hint,
.meta-line,
.snippet-text,
.chunk-text {
  color: #4b5563;
  font-size: 13px;
  line-height: 1.7;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.select-field,
.file-field,
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

.select-field select,
.file-field input {
  flex: 1;
  min-width: 0;
  border: 0;
  background: transparent;
  font-size: 14px;
}

.card-list,
.doc-list,
.chunk-list,
.search-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.knowledge-card {
  border-radius: 12px;
  overflow: hidden;
}

.card-title-row,
.title-with-badge {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.doc-item,
.chunk-item,
.search-item {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
}

.doc-item {
  cursor: pointer;
}

.doc-item--active {
  border-color: #1677ff;
  background: #eff6ff;
}

.doc-item-title,
.search-item-title,
.chunk-head {
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.import-result {
  margin-top: 10px;
}

.state-block {
  padding: 20px 0;
}

@media (max-width: 960px) {
  .panel-grid {
    grid-template-columns: 1fr;
  }
}
</style>

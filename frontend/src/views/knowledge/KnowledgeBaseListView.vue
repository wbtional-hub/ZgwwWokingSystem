<template>
  <AppPageShell
    title="知识库管理"
    description="优化知识库列表、编辑表单与权限授权的交互表现，让选择、编辑和返回都更直观。"
  >
    <template #actions>
      <div class="page-actions">
        <van-button plain type="primary" :loading="pageBusy" @click="reloadAll">刷新</van-button>
        <van-button type="primary" :disabled="pageBusy" @click="openCreateMode">新增知识库</van-button>
      </div>
    </template>

    <section class="knowledge-layout" data-guide="knowledge-base">
      <section class="panel panel--list">
        <div class="panel-title">知识库列表</div>
        <div class="panel-hint">左侧选择知识库，右侧进入编辑。新增和编辑表单默认收起，随时都可以返回列表视图。</div>

        <div class="query-grid">
          <van-field v-model.trim="state.baseQuery.keywords" label="关键字" placeholder="按知识库名称搜索" />
          <div class="select-field">
            <span class="select-label">领域</span>
            <select v-model="state.baseQuery.domainType">
              <option value="">全部</option>
              <option v-for="item in domainOptions" :key="item" :value="item">{{ item }}</option>
            </select>
          </div>
          <div class="select-field">
            <span class="select-label">状态</span>
            <select v-model="state.baseQuery.status">
              <option value="">全部</option>
              <option value="1">启用</option>
              <option value="0">停用</option>
            </select>
          </div>
        </div>

        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.baseLoading" @click="fetchBases">查询</van-button>
          <van-button size="small" plain :disabled="state.baseLoading" @click="resetBaseQuery">重置</van-button>
        </div>

        <div class="scope-summary">
          <span>共 {{ state.baseList.length }} 个知识库</span>
          <span>当前工作知识库：{{ activeBaseName }}</span>
        </div>

        <van-loading v-if="state.baseLoading" class="state-block" size="24px" vertical>知识库加载中...</van-loading>
        <van-empty v-else-if="!state.baseList.length" description="暂无知识库，点击右上角新增即可开始。" />

        <div v-else class="knowledge-card-list">
          <article
            v-for="item in state.baseList"
            :key="item.id"
            class="knowledge-card"
            :class="{
              'knowledge-card--selected': state.selectedBaseId === item.id,
              'knowledge-card--current': item.current
            }"
            @click="fillBaseForm(item)"
          >
            <div class="knowledge-card__head">
              <div>
                <div class="knowledge-card__title">{{ item.baseName }}</div>
                <div class="knowledge-card__domain">{{ item.domainType || '未分类' }}</div>
              </div>
              <div class="tag-row">
                <van-tag v-if="state.selectedBaseId === item.id" color="#f59e0b">已选中</van-tag>
                <van-tag v-if="item.current" type="primary">当前</van-tag>
                <van-tag :type="Number(item.status) === 1 ? 'success' : 'danger'">
                  {{ Number(item.status) === 1 ? '启用' : '停用' }}
                </van-tag>
              </div>
            </div>

            <div class="knowledge-card__desc">{{ item.description || '暂无说明' }}</div>

            <div class="knowledge-card__meta">
              <span>文档 {{ item.documentCount || 0 }}</span>
              <span>切片 {{ item.chunkCount || 0 }}</span>
              <span>更新于 {{ formatDateTime(item.updateTime) }}</span>
            </div>

            <div class="action-row action-row--compact">
              <van-button
                size="small"
                plain
                type="primary"
                :disabled="pageBusy || item.current || Number(item.status) !== 1"
                @click.stop="handleSetCurrentBase(item)"
              >
                {{ item.current ? '当前使用中' : '设为当前' }}
              </van-button>
              <van-button size="small" plain type="warning" :disabled="pageBusy" @click.stop="fillBaseForm(item)">编辑</van-button>
              <van-button
                size="small"
                plain
                :type="Number(item.status) === 1 ? 'danger' : 'success'"
                :loading="state.baseTogglingId === item.id"
                :disabled="pageBusy && state.baseTogglingId !== item.id"
                @click.stop="handleToggleBaseStatus(item)"
              >
                {{ Number(item.status) === 1 ? '停用' : '启用' }}
              </van-button>
              <van-button
                size="small"
                plain
                type="danger"
                :loading="state.baseDeletingId === item.id"
                :disabled="pageBusy && state.baseDeletingId !== item.id"
                @click.stop="handleDeleteBase(item)"
              >
                删除
              </van-button>
            </div>
          </article>
        </div>
      </section>

      <section class="panel panel--form">
        <div class="panel-head">
          <div class="panel-head__content">
            <div class="panel-title">{{ formModeTitle }}</div>
            <div class="panel-hint">{{ formModeHint }}</div>
          </div>
          <div class="panel-head__actions">
            <van-button v-if="state.baseEditorVisible" size="small" plain :disabled="state.baseSubmitting" @click="closeBaseEditor">
              {{ state.baseMode === 'edit' ? '返回列表' : '收起表单' }}
            </van-button>
            <van-button
              v-else-if="selectedBaseItem"
              size="small"
              plain
              type="primary"
              :disabled="pageBusy"
              @click="reopenSelectedBaseEditor"
            >
              继续编辑
            </van-button>
          </div>
        </div>

        <template v-if="state.baseEditorVisible">
          <van-field v-model.trim="state.baseForm.baseName" label="知识库名称" placeholder="例如：人才政策知识库" :disabled="state.baseSubmitting" required />
          <van-field v-model.trim="state.baseForm.domainType" label="领域 / 分类" placeholder="例如：人才政策、周报、内部制度" :disabled="state.baseSubmitting" required />
          <van-field
            v-model="state.baseForm.description"
            label="知识库说明"
            type="textarea"
            rows="4"
            autosize
            placeholder="说明知识库用途和适用人群"
            :disabled="state.baseSubmitting"
            required
          />
          <div class="select-field">
            <span class="select-label">状态</span>
            <select v-model="state.baseForm.status" :disabled="state.baseSubmitting">
              <option :value="1">启用</option>
              <option :value="0">停用</option>
            </select>
          </div>
          <van-field v-model="state.baseForm.remark" label="备注" type="textarea" rows="3" autosize placeholder="选填备注" :disabled="state.baseSubmitting" />

          <details class="advanced-box" :open="state.baseMode === 'edit' && state.showAdvanced">
            <summary @click.prevent="state.showAdvanced = !state.showAdvanced">更多信息</summary>
            <div class="advanced-box__content">
              <div class="meta-line">知识库编码：{{ state.baseForm.baseCode || '保存后自动生成' }}</div>
              <div class="meta-line">创建时间：{{ formatDateTime(state.baseForm.createTime) }}</div>
              <div class="meta-line">更新时间：{{ formatDateTime(state.baseForm.updateTime) }}</div>
            </div>
          </details>

          <div class="action-row">
            <van-button block type="primary" :loading="state.baseSubmitting" @click="handleSaveBase">
              {{ state.baseMode === 'edit' ? '保存修改' : '保存知识库' }}
            </van-button>
            <van-button block plain :disabled="state.baseSubmitting" @click="closeBaseEditor">
              {{ state.baseMode === 'edit' ? '返回列表' : '收起表单' }}
            </van-button>
          </div>

          <div v-if="state.baseMode === 'edit' && state.baseForm.id" class="action-row">
            <van-button
              block
              plain
              type="primary"
              :disabled="pageBusy || state.activeBaseId === state.baseForm.id || Number(state.baseForm.status) !== 1"
              @click="handleSetCurrentBase(state.baseForm)"
            >
              {{ state.activeBaseId === state.baseForm.id ? '当前使用中' : '设为当前知识库' }}
            </van-button>
            <van-button
              block
              plain
              :type="Number(state.baseForm.status) === 1 ? 'danger' : 'success'"
              :disabled="pageBusy"
              @click="handleToggleBaseStatus(state.baseForm)"
            >
              {{ Number(state.baseForm.status) === 1 ? '停用知识库' : '启用知识库' }}
            </van-button>
          </div>
        </template>

        <div v-else class="form-placeholder">
          <div class="form-placeholder__title">{{ collapsedFormTitle }}</div>
          <div class="panel-hint">{{ collapsedFormHint }}</div>
          <div class="action-row">
            <van-button size="small" type="primary" :disabled="pageBusy" @click="openCreateMode">新增知识库</van-button>
            <van-button
              v-if="selectedBaseItem"
              size="small"
              plain
              type="primary"
              :disabled="pageBusy"
              @click="reopenSelectedBaseEditor"
            >
              编辑已选知识库
            </van-button>
          </div>
        </div>
      </section>
    </section>

    <section v-if="showPermissionPanel" class="panel" data-guide="knowledge-permission">
      <div class="panel-title">权限设置</div>
      <div class="panel-hint">{{ permissionManageHint }}</div>
      <div class="select-field">
        <span class="select-label">授权用户</span>
        <select v-model="state.permissionForm.userId" :disabled="state.permissionSaving || state.permissionLoading">
          <option value="">请选择用户</option>
          <option v-for="item in assignableUserOptions" :key="item.id" :value="String(item.id)">
            {{ item.realName || item.username }} ({{ item.username }})
          </option>
        </select>
      </div>
      <div class="switch-list switch-list--permission">
        <div class="switch-row"><span>查看知识库</span><van-switch v-model="state.permissionForm.canView" :disabled="state.permissionSaving || state.permissionLoading" /></div>
        <div class="switch-row"><span>上传文档</span><van-switch v-model="state.permissionForm.canUpload" :disabled="state.permissionSaving || state.permissionLoading" /></div>
        <div class="switch-row"><span>训练技能</span><van-switch v-model="state.permissionForm.canTrainSkill" :disabled="state.permissionSaving || state.permissionLoading" /></div>
        <div class="switch-row"><span>AI 分析</span><van-switch v-model="state.permissionForm.canAnalyze" :disabled="state.permissionSaving || state.permissionLoading" /></div>
      </div>
      <div class="select-field">
        <span class="select-label">状态</span>
        <select v-model="state.permissionForm.status" :disabled="state.permissionSaving || state.permissionLoading">
          <option :value="1">启用</option>
          <option :value="0">停用</option>
        </select>
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.permissionSaving" :disabled="state.permissionLoading" @click="handleSaveKnowledgePermission">保存权限</van-button>
        <van-button size="small" plain :disabled="state.permissionSaving || state.permissionLoading" @click="resetPermissionForm">清空</van-button>
      </div>

      <van-loading v-if="state.permissionLoading" class="state-block" size="24px" vertical>权限列表加载中...</van-loading>
      <van-empty v-else-if="!state.permissionList.length" description="暂无额外授权记录，创建人默认拥有完整权限。" />
      <div v-else class="permission-list">
        <article
          v-for="item in state.permissionList"
          :key="`${item.userId || 'owner'}-${item.baseId}`"
          class="permission-item"
          :class="{
            'permission-item--owner': Number(item.userId) === Number(state.baseForm.ownerUserId),
            'permission-item--selected': state.selectedPermissionUserId === String(item.userId)
          }"
          @click="fillPermissionForm(item)"
        >
          <div class="permission-item__head">
            <div>
              <div class="permission-title">{{ item.realName || item.username || '未命名用户' }}</div>
              <div class="meta-line">{{ item.username || '-' }}</div>
            </div>
            <div class="tag-row">
              <van-tag v-if="state.selectedPermissionUserId === String(item.userId)" color="#f59e0b">已选中</van-tag>
              <van-tag v-if="Number(item.userId) === Number(state.baseForm.ownerUserId)" type="primary">创建人默认权限</van-tag>
              <van-tag :type="Number(item.status) === 1 ? 'success' : 'danger'">
                {{ Number(item.status) === 1 ? '启用' : '停用' }}
              </van-tag>
            </div>
          </div>
          <div class="meta-line">查看：{{ boolText(item.canView) }}，上传：{{ boolText(item.canUpload) }}，训练：{{ boolText(item.canTrainSkill) }}，分析：{{ boolText(item.canAnalyze) }}</div>
          <div class="meta-line">授权时间：{{ formatDateTime(item.grantTime) }}</div>
        </article>
      </div>
    </section>

    <section class="panel" data-guide="knowledge-import">
      <div class="panel-title">文档导入</div>
      <div class="panel-hint">当前工作知识库：{{ activeBaseName }}</div>
      <div class="panel-hint">支持导入 Word 和 PDF 文档。</div>
      <div v-if="!state.activeBaseId" class="panel-hint">请先设置当前工作知识库，再导入文档。</div>
      <template v-else>
        <van-field v-model.trim="state.importForm.policyRegion" label="适用地区" placeholder="例如：上海市" />
        <van-field v-model.trim="state.importForm.policyLevel" label="政策级别" placeholder="例如：市级" />
        <van-field v-model.trim="state.importForm.keywords" label="关键字" placeholder="多个关键字用逗号分隔" />
        <van-field v-model="state.importForm.summary" label="摘要" type="textarea" rows="3" autosize placeholder="可选摘要" />
        <van-field v-model="state.importForm.effectiveDate" label="生效日期" placeholder="YYYY-MM-DD" />
        <van-field v-model="state.importForm.expireDate" label="失效日期" placeholder="YYYY-MM-DD" />
        <div class="file-field">
          <span class="select-label">文件</span>
          <input type="file" accept=".docx,.pdf" @change="handleFileChange" />
        </div>
        <div class="panel-hint">已选择：{{ state.importFile ? state.importFile.name : '未选择文件' }}</div>
        <div class="action-row">
          <van-button size="small" type="primary" :loading="state.importing" @click="handleImport">上传并导入</van-button>
          <van-button size="small" plain @click="clearImportForm">清空</van-button>
        </div>
        <div v-if="state.lastImportResult" class="panel-hint import-result">
          最近一次导入：状态 {{ state.lastImportResult.jobStatus || '-' }}，文档 ID {{ state.lastImportResult.documentId || '-' }}，切片 {{ state.lastImportResult.successChunks || 0 }}/{{ state.lastImportResult.totalChunks || 0 }}
        </div>
      </template>
    </section>

    <section class="panel-grid">
      <section class="panel">
        <div class="panel-title">文档列表</div>
        <div class="panel-hint">当前工作知识库：{{ activeBaseName }}</div>
        <van-field v-model.trim="state.documentQuery.keywords" label="关键字" placeholder="按标题或关键字搜索" :disabled="!state.activeBaseId" />
        <van-field v-model.trim="state.documentQuery.policyRegion" label="适用地区" placeholder="例如：上海市" :disabled="!state.activeBaseId" />
        <div class="select-field">
          <span class="select-label">解析状态</span>
          <select v-model="state.documentQuery.parseStatus" :disabled="!state.activeBaseId">
            <option value="">全部</option>
            <option value="SUCCESS">成功</option>
            <option value="FAILED">失败</option>
            <option value="PENDING">处理中</option>
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
            <div class="meta-line">解析状态：{{ item.parseStatus || '-' }}</div>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-title">文档详情与切片</div>
        <div v-if="!state.documentDetail" class="panel-hint">请选择左侧文档查看详情和切片内容。</div>
        <template v-else>
          <div class="meta-line">标题：{{ state.documentDetail.docTitle }}</div>
          <div class="meta-line">文件名：{{ state.documentDetail.fileName || '-' }}</div>
          <div class="meta-line">地区：{{ state.documentDetail.policyRegion || '-' }}</div>
          <div class="meta-line">级别：{{ state.documentDetail.policyLevel || '-' }}</div>
          <div class="meta-line">摘要：{{ state.documentDetail.summary || '-' }}</div>
          <div class="meta-line">关键字：{{ state.documentDetail.keywords || '-' }}</div>
          <div class="meta-line">状态：{{ state.documentDetail.parseStatus || '-' }}</div>
          <div class="meta-line">更新时间：{{ formatDateTime(state.documentDetail.updateTime) }}</div>
          <div class="chunk-list">
            <div v-for="chunk in state.chunkList" :key="chunk.id" class="chunk-item">
              <div class="chunk-head">片段 #{{ chunk.chunkNo }} / {{ chunk.headingPath || '无标题路径' }}</div>
              <div class="chunk-text">{{ chunk.contentText }}</div>
            </div>
          </div>
        </template>
      </section>
    </section>

    <section class="panel" data-guide="knowledge-search">
      <div class="panel-title">知识检索</div>
      <div class="panel-hint">这里用于验证检索效果，也可作为后续技能训练和 AI 分析的基础能力。</div>
      <van-field v-model.trim="state.searchForm.keywords" label="问题 / 关键字" placeholder="例如：A 类人才补贴" :disabled="!state.activeBaseId" />
      <van-field v-model.trim="state.searchForm.policyRegion" label="适用地区" placeholder="例如：上海市" :disabled="!state.activeBaseId" />
      <van-field v-model="state.searchForm.topN" label="返回条数" type="number" placeholder="默认 10" :disabled="!state.activeBaseId" />
      <div class="switch-row">
        <span class="select-label">仅生效内容</span>
        <van-switch v-model="state.searchForm.effectiveOnly" :disabled="!state.activeBaseId" />
      </div>
      <div class="action-row">
        <van-button size="small" type="primary" :loading="state.searchLoading" :disabled="!state.activeBaseId" @click="handleSearch">检索</van-button>
        <van-button size="small" plain :disabled="!state.activeBaseId" @click="resetSearchForm">重置</van-button>
      </div>
      <van-empty v-if="state.activeBaseId && !state.searchLoading && !state.searchResults.length" description="暂无结果" />
      <van-loading v-else-if="state.searchLoading" class="state-block" size="24px" vertical>检索中...</van-loading>
      <div v-else class="search-list">
        <div v-for="item in state.searchResults" :key="`${item.chunkId}-${item.documentId}`" class="search-item">
          <div class="search-item-title">{{ item.docTitle }}</div>
          <div class="meta-line">地区：{{ item.policyRegion || '-' }}</div>
          <div class="meta-line">位置：{{ item.headingPath || '无标题路径' }} / 片段 #{{ item.chunkNo }}</div>
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
import { queryGrantableKnowledgeUsers, queryUserKnowledgePermissionList, saveUserKnowledgePermission } from '@/api/ai'
import {
  deleteKnowledgeBase,
  getKnowledgeDocumentDetail,
  importKnowledgeDocx,
  queryKnowledgeBaseList,
  queryKnowledgeChunkList,
  queryKnowledgeDocumentList,
  saveKnowledgeBase,
  searchKnowledge,
  setCurrentKnowledgeBase,
  toggleKnowledgeBaseStatus
} from '@/api/knowledge'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const state = reactive({
  baseLoading: false,
  baseSubmitting: false,
  baseTogglingId: null,
  baseDeletingId: null,
  baseSettingCurrentId: null,
  permissionLoading: false,
  permissionSaving: false,
  importing: false,
  documentLoading: false,
  searchLoading: false,
  baseList: [],
  permissionUserOptions: [],
  permissionList: [],
  documentList: [],
  chunkList: [],
  searchResults: [],
  documentDetail: null,
  activeBaseId: null,
  activeDocumentId: null,
  importFile: null,
  lastImportResult: null,
  baseEditorVisible: false,
  selectedBaseId: null,
  selectedPermissionUserId: '',
  baseMode: 'create',
  showAdvanced: false,
  baseQuery: {
    keywords: '',
    domainType: '',
    status: ''
  },
  baseForm: createEmptyBaseForm(),
  permissionForm: createEmptyPermissionForm(),
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

const pageBusy = computed(() => {
  return Boolean(
    state.baseLoading ||
    state.baseSubmitting ||
    state.baseTogglingId ||
    state.baseDeletingId ||
    state.baseSettingCurrentId ||
    state.permissionLoading ||
    state.permissionSaving ||
    state.importing ||
    state.documentLoading ||
    state.searchLoading
  )
})

const currentUserId = computed(() => Number(userStore.userInfo?.userId || 0))
const isSuperAdmin = computed(() => Boolean(userStore.userInfo?.superAdmin))
const activeBaseName = computed(() => {
  const matched = state.baseList.find((item) => item.id === state.activeBaseId)
  return matched ? matched.baseName : '未设置'
})
const selectedBaseItem = computed(() => {
  return state.baseList.find((item) => item.id === state.selectedBaseId) || null
})
const collapsedFormTitle = computed(() => {
  return selectedBaseItem.value ? '编辑表单已收起' : '新增和编辑表单默认收起'
})
const collapsedFormHint = computed(() => {
  if (selectedBaseItem.value) {
    return `当前已选：${selectedBaseItem.value.baseName}。点击“编辑已选知识库”可继续编辑，也可以在左侧切换其他知识库。`
  }
  return '点击“新增知识库”开始创建，或在左侧选择知识库进入编辑。'
})
const canManageBasePermission = computed(() => {
  if (!state.baseForm.id) {
    return false
  }
  return Boolean(isSuperAdmin.value || Number(state.baseForm.ownerUserId) === currentUserId.value)
})
const showPermissionPanel = computed(() => {
  return Boolean(state.baseEditorVisible && state.baseMode === 'edit' && state.baseForm.id && canManageBasePermission.value)
})
const assignableUserOptions = computed(() => {
  return state.permissionUserOptions.filter((item) => Number(item.id) !== Number(state.baseForm.ownerUserId))
})
const permissionManageHint = computed(() => {
  if (!state.baseForm.id) {
    return ''
  }
  if (isSuperAdmin.value) {
    return '超级管理员可以为当前知识库维护任意用户的权限。'
  }
  return '知识库创建人可以为可授权用户配置访问权限。'
})
const formModeTitle = computed(() => {
  if (!state.baseEditorVisible) {
    return '知识库表单'
  }
  return state.baseMode === 'edit' ? '编辑知识库' : '新增知识库'
})
const formModeHint = computed(() => {
  if (!state.baseEditorVisible) {
    return '表单默认收起，需要时再展开，避免页面始终占满视线。'
  }
  if (state.baseMode === 'edit' && state.baseForm.baseName) {
    return `正在编辑 ${state.baseForm.baseName}，保存后可继续设置权限或调整状态。`
  }
  return '填写名称、领域和说明后保存即可。'
})
const domainOptions = computed(() => {
  return Array.from(new Set(state.baseList.map((item) => (item.domainType || '').trim()).filter(Boolean)))
})

function createEmptyBaseForm() {
  return {
    id: null,
    baseCode: '',
    baseName: '',
    domainType: '',
    description: '',
    remark: '',
    status: 1,
    ownerUserId: null,
    createTime: '',
    updateTime: ''
  }
}

function createEmptyPermissionForm() {
  return {
    userId: '',
    canView: true,
    canUpload: false,
    canTrainSkill: false,
    canAnalyze: true,
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

function normalizeFriendlyMessage(error, fallback) {
  return error?.message || fallback
}

function openCreateMode() {
  state.baseEditorVisible = true
  state.baseMode = 'create'
  state.selectedBaseId = null
  state.showAdvanced = false
  state.baseForm = createEmptyBaseForm()
  resetPermissionState()
}

function closeBaseEditor() {
  state.baseEditorVisible = false
  state.showAdvanced = false
  if (!state.baseForm.id) {
    state.baseForm = createEmptyBaseForm()
  }
}

function reopenSelectedBaseEditor() {
  if (selectedBaseItem.value) {
    fillBaseForm(selectedBaseItem.value)
    return
  }
  openCreateMode()
}

function resetBaseForm() {
  closeBaseEditor()
}

function fillBaseForm(item) {
  state.baseEditorVisible = true
  state.baseMode = 'edit'
  state.selectedBaseId = item.id
  state.selectedPermissionUserId = ''
  state.showAdvanced = false
  state.baseForm = {
    id: item.id,
    baseCode: item.baseCode || '',
    baseName: item.baseName || '',
    domainType: item.domainType || '',
    description: item.description || '',
    remark: item.remark || '',
    status: Number(item.status) === 1 ? 1 : 0,
    ownerUserId: item.ownerUserId || null,
    createTime: item.createTime || '',
    updateTime: item.updateTime || ''
  }
  refreshKnowledgePermissionPanel()
}

function resetBaseQuery() {
  state.baseQuery.keywords = ''
  state.baseQuery.domainType = ''
  state.baseQuery.status = ''
  fetchBases()
}

function syncActiveBaseFromList() {
  const currentBase = state.baseList.find((item) => item.current)
  if (currentBase) {
    state.activeBaseId = currentBase.id
    return
  }
  if (state.activeBaseId && state.baseList.some((item) => item.id === state.activeBaseId)) {
    return
  }
  state.activeBaseId = state.baseList[0]?.id || null
}

async function fetchBases() {
  state.baseLoading = true
  try {
    const data = ensureSuccess(await queryKnowledgeBaseList({
      keywords: state.baseQuery.keywords || undefined,
      domainType: state.baseQuery.domainType || undefined,
      status: state.baseQuery.status === '' ? undefined : Number(state.baseQuery.status)
    }), '知识库加载失败')
    state.baseList = Array.isArray(data) ? data : []
    syncActiveBaseFromList()

    if (!state.baseList.length) {
      state.activeBaseId = null
      state.activeDocumentId = null
      state.documentList = []
      state.documentDetail = null
      state.chunkList = []
      state.searchResults = []
      state.selectedBaseId = null
      state.baseEditorVisible = false
      state.baseForm = createEmptyBaseForm()
      resetPermissionState()
      return
    }

    if (state.selectedBaseId && !state.baseList.some((item) => item.id === state.selectedBaseId)) {
      state.selectedBaseId = null
    }

    if (state.baseMode === 'edit' && state.baseForm.id) {
      const latest = state.baseList.find((item) => item.id === state.baseForm.id)
      if (latest) {
        fillBaseForm(latest)
      } else {
        state.baseEditorVisible = false
        state.baseForm = createEmptyBaseForm()
        resetPermissionState()
      }
    }
  } catch (error) {
    showToast(normalizeFriendlyMessage(error, '知识库加载失败'))
  } finally {
    state.baseLoading = false
  }
}

async function handleSaveBase() {
  if (!state.baseForm.baseName.trim()) {
    showToast('请输入知识库名称')
    return
  }
  if (!state.baseForm.domainType.trim()) {
    showToast('请输入所属领域')
    return
  }
  if (!state.baseForm.description.trim()) {
    showToast('请输入知识库说明')
    return
  }

  state.baseSubmitting = true
  try {
    const savedId = ensureSuccess(await saveKnowledgeBase({
      id: state.baseForm.id || undefined,
      baseCode: state.baseForm.id ? state.baseForm.baseCode || undefined : undefined,
      baseName: state.baseForm.baseName.trim(),
      domainType: state.baseForm.domainType.trim(),
      description: state.baseForm.description.trim(),
      remark: state.baseForm.remark?.trim() || undefined,
      status: Number(state.baseForm.status)
    }), '知识库保存失败')
    showToast(state.baseForm.id ? '知识库已更新' : '知识库已创建')
    await fetchBases()
    const latest = state.baseList.find((item) => item.id === savedId)
    if (latest) {
      fillBaseForm(latest)
    }
    if (state.activeBaseId) {
      await fetchDocuments()
    }
  } catch (error) {
    showToast(normalizeFriendlyMessage(error, '知识库保存失败'))
  } finally {
    state.baseSubmitting = false
  }
}

async function handleToggleBaseStatus(item) {
  const nextStatus = Number(item.status) === 1 ? 0 : 1
  state.baseTogglingId = item.id
  try {
    await showConfirmDialog({
      title: nextStatus === 1 ? '确认启用该知识库？' : '确认停用该知识库？',
      message: item.baseName
    })
    ensureSuccess(await toggleKnowledgeBaseStatus({ id: item.id, status: nextStatus }), '知识库状态更新失败')
    showToast(nextStatus === 1 ? '知识库已启用' : '知识库已停用')
    await fetchBases()
    if (state.activeBaseId) {
      await fetchDocuments()
    }
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(normalizeFriendlyMessage(error, '知识库状态更新失败'))
    }
  } finally {
    state.baseTogglingId = null
  }
}

async function handleSetCurrentBase(item) {
  state.baseSettingCurrentId = item.id
  try {
    ensureSuccess(await setCurrentKnowledgeBase({ id: item.id }), '切换当前知识库失败')
    showToast('当前知识库已切换')
    await fetchBases()
    await fetchDocuments()
  } catch (error) {
    showToast(normalizeFriendlyMessage(error, '切换当前知识库失败'))
  } finally {
    state.baseSettingCurrentId = null
  }
}

async function handleDeleteBase(item) {
  state.baseDeletingId = item.id
  try {
    await showConfirmDialog({
      title: '确认删除该知识库？',
      message: '删除后不可恢复。'
    })
    ensureSuccess(await deleteKnowledgeBase(item.id), '删除知识库失败')
    showToast('知识库已删除')
    await fetchBases()
    if (state.activeBaseId) {
      await fetchDocuments()
    } else {
      state.documentList = []
      state.documentDetail = null
      state.chunkList = []
      state.searchResults = []
    }
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(normalizeFriendlyMessage(error, '删除知识库失败'))
    }
  } finally {
    state.baseDeletingId = null
  }
}

function clearImportForm() {
  state.importForm = createEmptyImportForm()
  state.importFile = null
}

function resetPermissionState() {
  state.permissionForm = createEmptyPermissionForm()
  state.permissionUserOptions = []
  state.permissionList = []
  state.selectedPermissionUserId = ''
}

function resetPermissionForm() {
  state.permissionForm = createEmptyPermissionForm()
  state.selectedPermissionUserId = ''
}

function boolText(value) {
  return value ? '是' : '否'
}

function fillPermissionForm(item) {
  state.selectedPermissionUserId = String(item.userId || '')
  if (Number(item.userId) === Number(state.baseForm.ownerUserId)) {
    showToast('创建人默认拥有完整权限，无需重复授权')
    return
  }
  state.permissionForm = {
    userId: String(item.userId),
    canView: Boolean(item.canView),
    canUpload: Boolean(item.canUpload),
    canTrainSkill: Boolean(item.canTrainSkill),
    canAnalyze: Boolean(item.canAnalyze),
    status: Number(item.status) === 1 ? 1 : 0
  }
}

async function refreshKnowledgePermissionPanel() {
  if (!state.baseForm.id || !canManageBasePermission.value) {
    resetPermissionState()
    return
  }
  state.permissionLoading = true
  try {
    const [usersResponse, permissionResponse] = await Promise.all([
      queryGrantableKnowledgeUsers(state.baseForm.id),
      queryUserKnowledgePermissionList({ baseId: state.baseForm.id })
    ])
    const users = ensureSuccess(usersResponse, '可授权用户加载失败')
    state.permissionUserOptions = Array.isArray(users) ? users : Array.isArray(users?.list) ? users.list : []
    state.permissionList = ensureSuccess(permissionResponse, '权限列表加载失败') || []
    if (state.selectedPermissionUserId && !state.permissionList.some((item) => String(item.userId) === state.selectedPermissionUserId)) {
      state.selectedPermissionUserId = ''
    }
  } catch (error) {
    resetPermissionState()
    showToast(normalizeFriendlyMessage(error, '权限列表加载失败'))
  } finally {
    state.permissionLoading = false
  }
}

async function handleSaveKnowledgePermission() {
  if (!state.baseForm.id) {
    showToast('请先选择知识库')
    return
  }
  if (!state.permissionForm.userId) {
    showToast('请选择授权用户')
    return
  }
  const savedUserId = String(state.permissionForm.userId)
  state.permissionSaving = true
  try {
    ensureSuccess(await saveUserKnowledgePermission({
      userId: Number(state.permissionForm.userId),
      baseId: Number(state.baseForm.id),
      canView: Boolean(state.permissionForm.canView),
      canUpload: Boolean(state.permissionForm.canUpload),
      canTrainSkill: Boolean(state.permissionForm.canTrainSkill),
      canAnalyze: Boolean(state.permissionForm.canAnalyze),
      status: Number(state.permissionForm.status)
    }), '权限保存失败')
    showToast('权限已保存')
    await refreshKnowledgePermissionPanel()
    const matchedPermission = state.permissionList.find((item) => String(item.userId) === savedUserId)
    if (matchedPermission) {
      fillPermissionForm(matchedPermission)
    } else {
      state.selectedPermissionUserId = savedUserId
    }
    await fetchBases()
  } catch (error) {
    showToast(normalizeFriendlyMessage(error, '权限保存失败'))
  } finally {
    state.permissionSaving = false
  }
}

function handleFileChange(event) {
  const files = event?.target?.files
  state.importFile = files && files.length ? files[0] : null
}

async function handleImport() {
  if (!state.activeBaseId) {
    showToast('请先设置当前工作知识库')
    return
  }
  if (!state.importFile) {
    showToast('请选择 Word 或 PDF 文件')
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
    showToast('文档导入成功')
    clearImportForm()
    await fetchDocuments()
    await fetchBases()
  } catch (error) {
    showToast(normalizeFriendlyMessage(error, '文档导入失败'))
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
    showToast(normalizeFriendlyMessage(error, '文档列表加载失败'))
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
    state.chunkList = ensureSuccess(chunkResponse, '切片内容加载失败') || []
  } catch (error) {
    showToast(normalizeFriendlyMessage(error, '文档详情加载失败'))
  }
}

async function handleSearch() {
  if (!state.activeBaseId) {
    showToast('请先设置当前工作知识库')
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
    showToast(normalizeFriendlyMessage(error, '知识检索失败'))
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

async function reloadAll() {
  await fetchBases()
  if (state.activeBaseId) {
    await fetchDocuments()
  }
}

onMounted(async () => {
  await fetchBases()
  if (state.activeBaseId) {
    await fetchDocuments()
  }
})
</script>

<style scoped>
.knowledge-layout,
.panel-grid {
  display: grid;
  grid-template-columns: minmax(320px, 1.1fr) minmax(320px, 0.9fr);
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

.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.panel-head__content {
  flex: 1;
  min-width: 0;
}

.panel-head__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.panel-hint,
.meta-line,
.snippet-text,
.chunk-text,
.scope-summary {
  color: #4b5563;
  font-size: 13px;
  line-height: 1.7;
}

.page-actions,
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-row--compact :deep(.van-button) {
  min-width: 0;
}

.query-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 8px;
}

.scope-summary {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 8px;
  margin-top: 12px;
}

.mode-switch {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
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
  flex: 0 0 88px;
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

.knowledge-card-list,
.permission-list,
.doc-list,
.chunk-list,
.search-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-top: 12px;
}

.knowledge-card,
.permission-item,
.doc-item,
.chunk-item,
.search-item {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fafafa;
}

.knowledge-card {
  cursor: pointer;
  transition: border-color 0.2s ease, background 0.2s ease;
}

.knowledge-card--selected {
  border-color: #f59e0b;
  background: #fffbeb;
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.16);
}

.knowledge-card--current {
  border-color: #1677ff;
  background: #eff6ff;
}

.knowledge-card--current.knowledge-card--selected {
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.16);
}

.knowledge-card__head,
.permission-item__head,
.tag-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.knowledge-card__title,
.permission-title,
.doc-item-title,
.search-item-title,
.chunk-head {
  margin-bottom: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.knowledge-card__domain {
  font-size: 12px;
  color: #6b7280;
}

.knowledge-card__desc {
  margin: 10px 0;
  color: #374151;
  font-size: 13px;
  line-height: 1.7;
}

.knowledge-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-bottom: 10px;
  color: #6b7280;
  font-size: 12px;
}

.advanced-box {
  margin: 12px 0;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
}

.advanced-box summary {
  padding: 12px;
  cursor: pointer;
  font-size: 14px;
  color: #111827;
}

.advanced-box__content {
  padding: 0 12px 12px;
}

.switch-list--permission {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 12px 0;
}

.permission-item {
  cursor: pointer;
}

.permission-item--owner {
  border-color: #1677ff;
  background: #eff6ff;
}

.permission-item--selected {
  border-color: #f59e0b;
  background: #fffbeb;
  box-shadow: 0 0 0 2px rgba(245, 158, 11, 0.16);
}

.permission-item--owner.permission-item--selected {
  background: linear-gradient(135deg, #eff6ff 0%, #fffbeb 100%);
}

.form-placeholder {
  padding: 16px;
  border: 1px dashed #d1d5db;
  border-radius: 12px;
  background: #f8fafc;
}

.form-placeholder__title {
  margin-bottom: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.doc-item {
  cursor: pointer;
}

.doc-item--active {
  border-color: #1677ff;
  background: #eff6ff;
}

.import-result {
  margin-top: 10px;
}

.panel-title--sub {
  margin-top: 16px;
}

.preview-block {
  margin: 10px 0;
  padding: 12px;
  border: 1px solid #ebedf0;
  border-radius: 8px;
  background: #f8fafc;
}

.preview-block__label {
  margin-bottom: 6px;
  font-size: 13px;
  color: #6b7280;
}

.preview-block__value {
  color: #1f2937;
  font-size: 13px;
  line-height: 1.7;
  word-break: break-all;
}

.feedback-box {
  margin-top: 14px;
}

.feedback-text {
  white-space: pre-wrap;
  word-break: break-word;
  padding: 12px;
  border-radius: 8px;
  background: #0f172a;
  color: #e2e8f0;
  font-size: 12px;
  line-height: 1.8;
}

.state-block {
  padding: 20px 0;
}

@media (max-width: 960px) {
  .knowledge-layout,
  .panel-grid {
    grid-template-columns: 1fr;
  }

  .scope-summary {
    flex-direction: column;
  }

  .panel-head {
    flex-direction: column;
  }
}
</style>

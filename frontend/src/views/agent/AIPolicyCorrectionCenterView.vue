<template>
  <AppPageShell
    title="政策智能体纠错中心"
    description="集中处理用户反馈，形成“人工审核、处理留痕、知识补强前置”的安全闭环。"
  >
    <section class="safety-banner">
      <div>
        <strong>安全边界</strong>
        <p>用户反馈仅作为人工审核线索，不会自动修改正式政策知识库。</p>
      </div>
      <van-tag type="primary" plain>Phase C1 · 仅任务流转</van-tag>
    </section>

    <section class="filter-panel">
      <div class="filter-grid">
        <label class="field">
          <span>状态</span>
          <select v-model="state.query.status">
            <option value="">全部</option>
            <option v-for="item in statusOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label class="field">
          <span>反馈类型</span>
          <select v-model="state.query.feedbackType">
            <option value="">全部</option>
            <option v-for="item in feedbackTypeOptions" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label class="field">
          <span>policyKey</span>
          <input v-model.trim="state.query.policyKey" placeholder="double_hundred" />
        </label>
        <label class="field">
          <span>topicType</span>
          <input v-model.trim="state.query.topicType" placeholder="material / department" />
        </label>
        <label class="field">
          <span>开始时间</span>
          <input v-model="state.query.startTime" type="datetime-local" />
        </label>
        <label class="field">
          <span>结束时间</span>
          <input v-model="state.query.endTime" type="datetime-local" />
        </label>
        <label class="field field--wide">
          <span>关键词</span>
          <input v-model.trim="state.query.keyword" placeholder="按问题、答案、反馈说明或政策检索" @keyup.enter="reloadFirstPage" />
        </label>
      </div>
      <div class="action-row">
        <van-button type="primary" size="small" :loading="state.loading" @click="reloadFirstPage">查询</van-button>
        <van-button plain size="small" :disabled="state.loading" @click="resetQuery">重置</van-button>
      </div>
    </section>

    <section class="task-panel">
      <div class="panel-head">
        <div>
          <strong>反馈任务</strong>
          <span>共 {{ state.total }} 条</span>
        </div>
        <van-button plain size="small" :loading="state.loading" @click="fetchList">刷新</van-button>
      </div>

      <div v-if="state.loading" class="empty-block">加载中...</div>
      <div v-else-if="!state.list.length" class="empty-block">暂无纠错任务</div>
      <div v-else class="task-list">
        <article v-for="item in state.list" :key="item.id" class="task-card">
          <div class="task-card__top">
            <div>
              <div class="task-title">{{ item.question || '未记录问题' }}</div>
              <div class="task-meta">反馈时间：{{ formatDateTime(item.createdAt) }}</div>
            </div>
            <van-tag :type="statusTagType(item.status)">{{ statusLabel(item.status) }}</van-tag>
          </div>
          <div class="task-grid">
            <span>反馈类型：{{ feedbackTypeLabel(item.feedbackType) }}</span>
            <span>政策：{{ item.policyKey || '-' }}</span>
            <span>topic：{{ item.topicType || '-' }}</span>
            <span>处理人：{{ item.handlerId || '-' }}</span>
          </div>
          <p class="task-feedback">{{ item.feedbackContent || '用户未填写补充说明' }}</p>
          <div class="action-row">
            <van-button size="small" type="primary" plain @click="openDetail(item.id)">查看</van-button>
            <van-button size="small" plain type="success" @click="openPromptDialog(item)">生成提字词</van-button>
            <van-button size="small" plain @click="quickStatus(item, 'PROCESSING')">标记处理中</van-button>
            <van-button size="small" plain type="warning" @click="quickStatus(item, 'NEED_KNOWLEDGE_FIX')">需知识补强</van-button>
            <van-button size="small" plain type="danger" @click="closeTask(item)">关闭</van-button>
          </div>
        </article>
      </div>

      <div class="pager">
        <van-button size="small" plain :disabled="state.query.pageNo <= 1 || state.loading" @click="prevPage">上一页</van-button>
        <span>第 {{ state.query.pageNo }} 页 / {{ totalPages }} 页</span>
        <van-button size="small" plain :disabled="state.query.pageNo >= totalPages || state.loading" @click="nextPage">下一页</van-button>
      </div>
    </section>

    <van-popup v-model:show="state.detailVisible" position="right" class="detail-popup">
      <section class="detail-shell">
        <div class="detail-head">
          <div>
            <strong>纠错任务详情</strong>
            <p>#{{ state.detail?.task?.id || '-' }} · {{ statusLabel(state.detail?.task?.status) }}</p>
          </div>
          <div class="detail-head__actions">
            <van-button v-if="state.detail?.task" size="small" plain type="success" @click="openPromptDialog(state.detail.task)">生成 AI 提字词</van-button>
            <van-button v-if="state.detail?.task" size="small" plain @click="copyPromptForTask(state.detail.task)">复制提字词</van-button>
            <van-button size="small" plain @click="state.detailVisible = false">关闭</van-button>
          </div>
        </div>

        <div v-if="state.detailLoading" class="empty-block">加载详情中...</div>
        <template v-else-if="state.detail?.task">
          <div class="detail-grid">
            <DetailItem label="原问题" :value="state.detail.task.question" wide />
            <DetailItem label="原答案" :value="state.detail.task.answer" wide pre />
            <DetailItem label="反馈类型" :value="feedbackTypeLabel(state.detail.task.feedbackType)" />
            <DetailItem label="用户说明" :value="state.detail.task.feedbackContent" wide />
            <DetailItem label="session_id" :value="state.detail.task.sessionId" />
            <DetailItem label="message_id" :value="state.detail.task.messageId" />
            <DetailItem label="trace_id" :value="state.detail.task.traceId" />
            <DetailItem label="policy_key" :value="state.detail.task.policyKey" />
            <DetailItem label="topic_type" :value="state.detail.task.topicType" />
            <DetailItem label="question_type" :value="state.detail.task.questionType" />
            <DetailItem label="evidence_ids" :value="state.detail.task.evidenceIds" wide />
            <DetailItem label="evidence_source" :value="state.detail.task.evidenceSource" wide />
            <DetailItem label="route_plan" :value="state.detail.task.routePlan" wide />
            <DetailItem label="validation_summary" :value="state.detail.task.validationSummary" wide />
          </div>

          <section class="trace-panel">
            <div class="panel-head">
              <div>
                <strong>回答链路</strong>
                <span>只读追溯，不改变任务状态</span>
              </div>
              <van-button size="small" plain :loading="state.traceLoading" @click="loadTrace(state.detail.task.id)">刷新链路</van-button>
            </div>
            <div v-if="state.traceLoading" class="empty-block">加载回答链路中...</div>
            <div v-else class="trace-grid">
              <DetailItem label="answer_mode" :value="traceValue('answerMode')" />
              <DetailItem label="route_plan" :value="traceValue('routePlan')" wide />
              <DetailItem label="faq_hit" :value="traceBoolean('faqHit')" />
              <DetailItem label="fallback_flag" :value="traceBoolean('fallbackFlag')" />
              <DetailItem label="matched_intent_code" :value="traceValue('matchedIntentCode')" />
              <DetailItem label="candidate_generated" :value="traceBoolean('candidateGenerated')" />
              <DetailItem label="hit_chunk_ids" :value="traceValue('hitChunkIds')" wide />
              <DetailItem label="validation_summary" :value="traceValue('validationSummary')" wide />
              <DetailItem label="final_answer" :value="traceValue('finalAnswer')" wide pre />
            </div>
          </section>

          <section class="trace-panel">
            <div class="panel-head">
              <div>
                <strong>命中证据</strong>
                <span>展开专题卡、原始知识切片或会话消息</span>
              </div>
              <van-button size="small" plain :loading="state.evidenceLoading" @click="loadEvidence(state.detail.task.id)">刷新证据</van-button>
            </div>
            <div v-if="state.evidenceLoading" class="empty-block">加载命中证据中...</div>
            <div v-else-if="!state.evidence.length" class="empty-block">暂无可展开证据</div>
            <article v-for="item in state.evidence" :key="evidenceKey(item)" class="evidence-item">
              <div class="evidence-head">
                <div>
                  <strong>{{ item.title || '-' }}</strong>
                  <p>#{{ item.id || '-' }} · {{ item.policyName || item.policyKey || '-' }} · {{ item.topicType || '-' }}</p>
                </div>
                <div class="evidence-tags">
                  <van-tag :type="evidenceTagType(item.evidenceType)" plain>{{ evidenceTypeLabel(item.evidenceType) }}</van-tag>
                  <van-tag v-if="item.sourceScene === 'POLICY_STANDARDIZATION_PHASE1'" type="success" plain>标准化专题卡</van-tag>
                  <van-tag v-else-if="item.sourceScene" plain>{{ item.sourceScene }}</van-tag>
                </div>
              </div>
              <p class="evidence-preview">{{ isEvidenceExpanded(item) ? (item.fullContent || item.contentPreview || '-') : (item.contentPreview || '-') }}</p>
              <van-button v-if="item.fullContent && item.fullContent !== item.contentPreview" size="small" plain @click="toggleEvidence(item)">
                {{ isEvidenceExpanded(item) ? '收起全文' : '展开全文' }}
              </van-button>
            </article>
          </section>

          <section class="trace-panel">
            <div class="panel-head">
              <div>
                <strong>会话上下文</strong>
                <span>最近 3-6 条消息，辅助判断追问和上下文继承</span>
              </div>
            </div>
            <div v-if="!traceMessages.length" class="empty-block">暂无上下文消息</div>
            <article v-for="message in traceMessages" :key="message.id" class="context-item">
              <div class="context-head">
                <strong>{{ message.role || '-' }}</strong>
                <span>{{ formatDateTime(message.createTime) }}</span>
              </div>
              <p>{{ message.content || '-' }}</p>
              <small v-if="message.citedChunkIds">cited_chunk_ids：{{ message.citedChunkIds }}</small>
            </article>
          </section>

          <section class="trace-panel">
            <div class="panel-head">
              <div>
                <strong>答案价值诊断</strong>
                <span>规则诊断，不调用 AI，不写正式知识库</span>
              </div>
              <div class="action-row action-row--compact">
                <van-button size="small" type="primary" plain @click="generateQualityDiagnosis">生成价值诊断</van-button>
                <van-button size="small" plain type="success" @click="openDraftDialog('FAQ_DRAFT')">生成 FAQ 草稿</van-button>
                <van-button size="small" plain type="success" @click="openDraftDialog('POLICY_CHUNK_DRAFT')">生成专题卡草稿</van-button>
              </div>
            </div>
            <div v-if="!state.qualityDiagnosis" class="empty-block">尚未生成诊断。诊断仅作为人工审核线索，不会自动改变答案。</div>
            <div v-else class="diagnosis-card">
              <div class="diagnosis-head">
                <van-tag :type="qualityTagType(state.qualityDiagnosis.qualityLevel)">{{ state.qualityDiagnosis.qualityLevel }}</van-tag>
                <strong>{{ state.qualityDiagnosis.diagnosisType }}</strong>
                <span>{{ state.qualityDiagnosis.recommendedFix }}</span>
              </div>
              <div class="diagnosis-grid">
                <span v-for="(value, key) in state.qualityDiagnosis.answerValueChecklist" :key="key">
                  {{ key }}：{{ value ? '是' : '否' }}
                </span>
              </div>
              <ul class="diagnosis-list">
                <li v-for="item in state.qualityDiagnosis.problems" :key="item">{{ item }}</li>
              </ul>
              <p class="diagnosis-note">建议草稿类型：{{ state.qualityDiagnosis.suggestedDraftType }}</p>
            </div>
          </section>

          <section class="detail-actions">
            <div class="panel-head">
              <strong>处理操作</strong>
            </div>
            <div class="status-buttons">
              <van-button v-for="item in statusOptions.slice(1)" :key="item.value" size="small" plain @click="setDetailStatus(item.value)">
                {{ item.label }}
              </van-button>
            </div>
            <van-field
              v-model="state.handleResult"
              rows="3"
              autosize
              type="textarea"
              placeholder="填写处理意见，例如：已确认为材料清单不完整，后续进入知识补强。"
            />
            <div class="action-row">
              <van-button type="primary" size="small" :loading="state.actionSubmitting" @click="submitComment">添加处理意见</van-button>
              <van-button type="danger" plain size="small" :loading="state.actionSubmitting" @click="closeDetailTask">关闭任务</van-button>
            </div>
          </section>

          <section class="action-timeline">
            <div class="panel-head">
              <strong>处理记录</strong>
            </div>
            <div v-if="!(state.detail.actions || []).length" class="empty-block">暂无处理记录</div>
            <article v-for="action in state.detail.actions || []" :key="action.id" class="action-item">
              <div class="action-item__head">
                <strong>{{ action.actionType || '-' }}</strong>
                <span>{{ formatDateTime(action.createdAt) }}</span>
              </div>
              <p>{{ action.actionContent || '-' }}</p>
              <div v-if="action.draftPayload" class="draft-preview">
                <van-tag type="success" plain>{{ action.draftType || 'DRAFT' }}</van-tag>
                <span>目标表：{{ action.targetTable || '-' }}</span>
                <van-button size="small" plain @click="copyActionDraft(action)">复制草稿</van-button>
                <pre>{{ action.draftPayload }}</pre>
              </div>
              <small>operator_id：{{ action.operatorId || '-' }}</small>
            </article>
          </section>
        </template>
      </section>
    </van-popup>

    <van-popup v-model:show="state.promptVisible" round class="prompt-popup">
      <section class="prompt-dialog">
        <div class="prompt-head">
          <div>
            <strong>AI 分析提字词</strong>
            <p>仅在前端生成，不会请求 AI，也不会写入数据库。</p>
          </div>
          <van-button size="small" plain @click="state.promptVisible = false">关闭</van-button>
        </div>
        <textarea
          ref="promptTextareaRef"
          v-model="state.promptText"
          class="prompt-textarea"
          readonly
          aria-label="AI 分析提字词"
        ></textarea>
        <div class="prompt-actions">
          <van-button type="primary" @click="copyPromptText">复制</van-button>
          <van-button plain @click="state.promptVisible = false">关闭</van-button>
        </div>
      </section>
    </van-popup>

    <van-popup v-model:show="state.draftVisible" round class="prompt-popup">
      <section class="prompt-dialog">
        <div class="prompt-head">
          <div>
            <strong>{{ state.draftType === 'FAQ_DRAFT' ? 'FAQ 草稿' : '专题卡草稿' }}</strong>
            <p>仅保存为待审核草稿，不写正式知识库，不参与线上问答。</p>
          </div>
          <van-button size="small" plain @click="state.draftVisible = false">关闭</van-button>
        </div>
        <textarea
          ref="draftTextareaRef"
          v-model="state.draftText"
          class="prompt-textarea"
          aria-label="知识补强草稿"
        ></textarea>
        <div class="prompt-actions">
          <van-button plain @click="copyDraftText">复制草稿</van-button>
          <van-button type="primary" :loading="state.draftSaving" @click="saveDraft">保存为待审核草稿</van-button>
          <van-button plain @click="state.draftVisible = false">关闭</van-button>
        </div>
      </section>
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { computed, defineComponent, h, nextTick, onMounted, reactive, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import {
  closePolicyCorrectionTask,
  commentPolicyCorrectionTask,
  getPolicyCorrectionEvidence,
  getPolicyCorrectionTask,
  getPolicyCorrectionTrace,
  queryPolicyCorrectionTasks,
  savePolicyCorrectionChunkDraft,
  savePolicyCorrectionFaqDraft,
  updatePolicyCorrectionStatus
} from '@/api/agent'

const DetailItem = defineComponent({
  props: {
    label: String,
    value: [String, Number],
    wide: Boolean,
    pre: Boolean
  },
  setup(props) {
    return () => h('div', { class: ['detail-item', props.wide ? 'detail-item--wide' : ''] }, [
      h('span', props.label),
      h('p', { class: props.pre ? 'detail-item__pre' : '' }, props.value || '-')
    ])
  }
})

const statusOptions = [
  { value: 'PENDING', label: '待处理' },
  { value: 'PROCESSING', label: '处理中' },
  { value: 'NEED_KNOWLEDGE_FIX', label: '需知识补强' },
  { value: 'NEED_CODE_FIX', label: '需代码修复' },
  { value: 'NO_ACTION', label: '无需处理' },
  { value: 'RESOLVED', label: '已修复' },
  { value: 'CLOSED', label: '已关闭' }
]

const feedbackTypeOptions = [
  { value: 'HELPFUL', label: '有帮助' },
  { value: 'INACCURATE', label: '不准确' },
  { value: 'LACK_EVIDENCE', label: '缺少依据' },
  { value: 'MATERIAL_INCOMPLETE', label: '材料不完整' },
  { value: 'POLICY_OUTDATED', label: '政策已更新' },
  { value: 'OTHER', label: '其他' }
]

function defaultQuery() {
  return {
    pageNo: 1,
    pageSize: 20,
    status: '',
    feedbackType: '',
    policyKey: '',
    topicType: '',
    keyword: '',
    startTime: '',
    endTime: ''
  }
}

const state = reactive({
  loading: false,
  detailLoading: false,
  actionSubmitting: false,
  detailVisible: false,
  query: defaultQuery(),
  list: [],
  total: 0,
  detail: null,
  trace: null,
  evidence: [],
  evidenceTaskId: null,
  traceLoading: false,
  evidenceLoading: false,
  expandedEvidence: {},
  handleResult: '',
  promptVisible: false,
  promptText: '',
  qualityDiagnosis: null,
  draftVisible: false,
  draftType: '',
  draftText: '',
  draftSaving: false
})

const promptTextareaRef = ref(null)
const draftTextareaRef = ref(null)
const totalPages = computed(() => Math.max(1, Math.ceil((state.total || 0) / state.query.pageSize)))
const traceMessages = computed(() => Array.isArray(state.trace?.messages) ? state.trace.messages : [])

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function buildQueryPayload() {
  return {
    ...state.query,
    startTime: normalizeDateTime(state.query.startTime),
    endTime: normalizeDateTime(state.query.endTime)
  }
}

function normalizeDateTime(value) {
  return value ? String(value).replace('T', ' ') : ''
}

function formatDateTime(value) {
  return value ? String(value).replace('T', ' ').replace(/\.\d+.*$/, '') : '-'
}

function statusLabel(value) {
  return statusOptions.find((item) => item.value === value)?.label || value || '-'
}

function feedbackTypeLabel(value) {
  return feedbackTypeOptions.find((item) => item.value === value)?.label || value || '-'
}

function statusTagType(value) {
  if (value === 'RESOLVED') return 'success'
  if (value === 'CLOSED' || value === 'NO_ACTION') return 'default'
  if (value === 'NEED_CODE_FIX' || value === 'NEED_KNOWLEDGE_FIX') return 'warning'
  return 'primary'
}

async function fetchList() {
  state.loading = true
  try {
    const data = ensureSuccess(await queryPolicyCorrectionTasks(buildQueryPayload()), '加载纠错任务失败') || {}
    state.list = Array.isArray(data.list) ? data.list : []
    state.total = Number(data.total || 0)
    state.query.pageNo = Number(data.pageNo || state.query.pageNo || 1)
    state.query.pageSize = Number(data.pageSize || state.query.pageSize || 20)
  } catch (error) {
    showToast(error.message || '加载纠错任务失败')
  } finally {
    state.loading = false
  }
}

function reloadFirstPage() {
  state.query.pageNo = 1
  fetchList()
}

function resetQuery() {
  Object.assign(state.query, defaultQuery())
  fetchList()
}

function prevPage() {
  if (state.query.pageNo > 1) {
    state.query.pageNo -= 1
    fetchList()
  }
}

function nextPage() {
  if (state.query.pageNo < totalPages.value) {
    state.query.pageNo += 1
    fetchList()
  }
}

async function openDetail(id) {
  state.detailVisible = true
  state.detailLoading = true
  state.handleResult = ''
  resetTraceState()
  try {
    state.detail = ensureSuccess(await getPolicyCorrectionTask(id), '加载详情失败')
    await Promise.allSettled([loadTrace(id), loadEvidence(id)])
  } catch (error) {
    showToast(error.message || '加载详情失败')
  } finally {
    state.detailLoading = false
  }
}

async function refreshDetail() {
  const id = state.detail?.task?.id
  if (id) {
    state.detail = ensureSuccess(await getPolicyCorrectionTask(id), '刷新详情失败')
  }
}

function resetTraceState() {
  state.trace = null
  state.evidence = []
  state.evidenceTaskId = null
  state.expandedEvidence = {}
  state.qualityDiagnosis = null
}

async function loadTrace(id) {
  if (!id) return
  state.traceLoading = true
  try {
    state.trace = ensureSuccess(await getPolicyCorrectionTrace(id), '加载回答链路失败')
  } catch (error) {
    showToast(error.message || '加载回答链路失败')
  } finally {
    state.traceLoading = false
  }
}

async function loadEvidence(id) {
  if (!id) return
  state.evidenceLoading = true
  try {
    const data = ensureSuccess(await getPolicyCorrectionEvidence(id), '加载命中证据失败')
    state.evidence = Array.isArray(data) ? data : []
    state.evidenceTaskId = id
  } catch (error) {
    showToast(error.message || '加载命中证据失败')
  } finally {
    state.evidenceLoading = false
  }
}

function traceValue(field) {
  return state.trace?.[field] ?? state.detail?.task?.[field] ?? '-'
}

function traceBoolean(field) {
  const value = state.trace?.[field]
  if (value === true) return 'true'
  if (value === false) return 'false'
  return '-'
}

function evidenceKey(item) {
  return `${item?.evidenceType || 'UNKNOWN'}-${item?.id || item?.title || 'empty'}`
}

function evidenceTypeLabel(value) {
  if (value === 'POLICY_CHUNK') return '专题卡'
  if (value === 'KNOWLEDGE_CHUNK') return '原始知识切片'
  if (value === 'FAQ') return 'FAQ'
  if (value === 'CONDITION_INDEX') return '条件索引'
  if (value === 'MESSAGE') return '会话消息'
  return value || '证据'
}

function evidenceTagType(value) {
  if (value === 'POLICY_CHUNK') return 'success'
  if (value === 'KNOWLEDGE_CHUNK') return 'primary'
  if (value === 'MESSAGE') return 'default'
  return 'warning'
}

function isEvidenceExpanded(item) {
  return Boolean(state.expandedEvidence[evidenceKey(item)])
}

function toggleEvidence(item) {
  const key = evidenceKey(item)
  state.expandedEvidence = {
    ...state.expandedEvidence,
    [key]: !state.expandedEvidence[key]
  }
}

function generateQualityDiagnosis() {
  const task = state.detail?.task || {}
  const answer = String(state.trace?.finalAnswer || task.answer || '')
  const question = String(task.question || '')
  const feedbackContent = String(task.feedbackContent || '')
  const feedbackType = String(task.feedbackType || '')
  const topicType = inferTopicType(task)
  const questionType = String(task.questionType || '').toUpperCase()
  const routePlan = String(state.trace?.routePlan || task.routePlan || '')
  const problems = []
  const hasNegativeFeedback = feedbackType && feedbackType !== 'HELPFUL'
  const responsibilityGap = isResponsibilityFeedbackGap(task, feedbackContent)
  const checklist = {
    directAnswer: answer.length > 20 && !hasAny(answer, ['是否能给出完整', '建议先补充具体政策名称']),
    clearSubject: hasAny(answer, ['对象', '主体', '类别', '个人', '团队', '企业', '单位', '部门']),
    stepByStep: /(^|\n|\s)(1[.、]|一、|第一|步骤|流程|环节)/.test(answer),
    categorySplit: hasAny(answer, ['分类', '类别', '路径', '创新个人', '创新团队', '创业人才', '市级', '省级']),
    actionableAdvice: hasAny(answer, ['建议', '办理', '准备', '关注', '核验', '咨询']),
    evidenceBased: Boolean(task.evidenceIds || task.evidenceSource || state.trace?.hitChunkIds || state.evidence.length),
    dynamicBoundary: hasAny(answer, ['年度通知', '主管部门最新要求', '以正式通知为准', '以年度', '为准'])
  }
  if (!checklist.directAnswer) problems.push('没有正面回答用户问题，或存在通用模板痕迹')
  if (!checklist.actionableAdvice) problems.push('缺少可执行的办理建议')
  if (!checklist.evidenceBased) problems.push('缺少可追溯证据或证据记录')
  if (!checklist.dynamicBoundary) problems.push('未区分确定事实和年度动态事项')
  if (isOnlyBoundaryAnswer(answer)) problems.push('过度依赖“以年度通知为准”，缺少实质内容')
  if (hasAny(answer, ['当前知识库未命中足够证据', '取决于知识库是否收录', '泛问'])) problems.push('出现泛化模板或兜底话术')

  if (topicType === 'department') {
    if (!hasAny(answer, ['牵头', '主管', '受理', '责任', '联审', '审定', '专项办', '人社局', '科技局'])) problems.push('没有说明主管部门、牵头主体或责任链条')
    if (!checklist.categorySplit) problems.push('没有按申报类别或办理路径拆解主管口径')
    if (!hasAll(answer, ['创新个人', '创新团队', '创业人才'])) problems.push('没有明确分类说明创新个人、创新团队、创业人才的责任链条')
    if (responsibilityGap && hasAny(answer, ['创新个人', '创新团队', '创业人才', '专项办', '联审', '审定'])) {
      problems.push('用户反馈指出创业人才、创新人才的执行主体仍不够明确')
      problems.push('当前回答已有责任链条，但对分类执行部门还不够直接')
      problems.push('需要补强 department 专题卡或 FAQ 中的分类执行主体')
    }
  } else if (topicType === 'process') {
    if (!checklist.stepByStep) problems.push('流程类回答缺少步骤编号或环节拆解')
    if (!hasAny(answer, ['申报主体', '申报人', '单位', '审核', '评审', '公示', '确认'])) problems.push('流程类回答缺少申报主体或审核链条')
  } else if (topicType === 'material') {
    if (!hasAny(answer, ['身份证明', '学历学位', '单位推荐', '项目成果', '申报表', '承诺书', '资格证书'])) problems.push('材料类回答缺少材料分类和准备方向')
  } else if (topicType === 'boundary' || topicType === 'city_province_boundary') {
    if (!hasAny(answer, ['层级', '适用范围', '主管体系', '支持事项', '不能混用', '市级', '省级'])) problems.push('边界类回答缺少省市层级、适用范围或主管体系对比')
  }
  if (questionType === 'CONDITION' || routePlan.includes('condition_index')) {
    if (!hasAny(answer, ['命中条件', '缺失条件', '核验', '不等同于最终符合'])) problems.push('条件反查回答缺少命中条件、缺失条件或下一步核验建议')
  }
  const diagnosisType = responsibilityGap ? 'TOPIC_CARD_NEEDS_DETAIL' : inferDiagnosisType(problems, checklist, routePlan)
  const suggestedDraftType = diagnosisType === 'ROUTE_ERROR' ? 'POLICY_CHUNK_DRAFT' : (problems.length >= 2 ? 'POLICY_CHUNK_DRAFT' : 'FAQ_DRAFT')
  const allowHigh = (!hasNegativeFeedback || feedbackType === 'HELPFUL') && !feedbackContent.trim()
  const qualityLevel = problems.length >= 4 || (!hasAll(answer, ['创新个人', '创新团队', '创业人才']) && topicType === 'department')
    ? 'LOW'
    : problems.length >= 2 || hasNegativeFeedback || responsibilityGap
      ? 'MEDIUM'
      : allowHigh
        ? 'HIGH'
        : 'MEDIUM'
  state.qualityDiagnosis = {
    qualityLevel,
    diagnosisType,
    problems: problems.length ? problems : ['当前回答价值基本可接受，建议人工复核证据边界'],
    recommendedFix: responsibilityGap ? '补强 department 专题卡或 FAQ 中的分类执行主体' : suggestedDraftType === 'FAQ_DRAFT' ? '补充 FAQ 直接问答草稿' : `补强 policy_chunk ${topicType || 'topic'} 专题卡`,
    suggestedDraftType,
    answerValueChecklist: checklist,
    regressionQuestions: buildRegressionQuestions(task)
  }
}

function hasAny(text, words) {
  return words.some((word) => String(text || '').includes(word))
}

function hasAll(text, words) {
  return words.every((word) => String(text || '').includes(word))
}

function isResponsibilityFeedbackGap(task = {}, feedbackContent = '') {
  const topicType = inferTopicType(task)
  const question = String(task.question || '')
  const feedback = String(feedbackContent || '')
  return (topicType === 'department' || hasAny(question, ['主管部门', '谁负责', '找谁办理', '哪个部门']))
    && hasAny(feedback, ['谁执行', '谁负责', '哪个部门', '主管部门', '受理部门', '创业', '创新', '创新个人', '创新团队', '创业人才', '执行部门'])
}

function isOnlyBoundaryAnswer(answer) {
  const text = String(answer || '')
  return hasAny(text, ['以年度通知为准', '以正式通知为准', '以主管部门最新要求为准']) && text.length < 120
}

function inferDiagnosisType(problems, checklist, routePlan) {
  if (!checklist.evidenceBased) return 'EVIDENCE_MISSING'
  if (String(routePlan || '').includes('condition-index-first') && problems.some((item) => item.includes('边界'))) return 'ROUTE_ERROR'
  if (problems.some((item) => item.includes('泛化模板') || item.includes('兜底'))) return 'ANSWER_TEMPLATE_WEAK'
  if (problems.some((item) => item.includes('缺少实质内容') || item.includes('分类'))) return 'TOPIC_CARD_TOO_SHALLOW'
  return problems.length ? 'KNOWLEDGE_GAP' : 'OK'
}

function qualityTagType(level) {
  if (level === 'HIGH') return 'success'
  if (level === 'MEDIUM') return 'warning'
  return 'danger'
}

function buildRegressionQuestions(task = {}) {
  const policyName = naturalPolicyName(task)
  const topicType = inferTopicType(task)
  if (topicType === 'department') {
    if (task.policyKey === 'double_hundred' || policyName.includes('双百')) {
      return [
        '双百计划主管部门是谁',
        '双百计划创业人才由谁执行',
        '双百计划创新个人由谁负责',
        '双百计划创新团队谁审核',
        '双百计划找哪个部门办理',
        '双百计划专项办负责什么'
      ]
    }
    return [`${policyName}主管部门是谁`, `${policyName}由谁负责`, `${policyName}找哪个部门办理`, `${policyName}受理部门是谁`]
  }
  if (topicType === 'process') {
    return [`${policyName}怎么申请`, `${policyName}申报流程是什么`, `${policyName}办理步骤有哪些`]
  }
  if (topicType === 'material') {
    return [`${policyName}需要什么材料`, `${policyName}材料清单是什么`, `${policyName}申报表和证明材料有哪些`]
  }
  if (topicType === 'boundary' || topicType === 'city_province_boundary') {
    return [`${policyName}和上级政策有什么区别`, `${policyName}省市政策能否混用`, `${policyName}适用范围是什么`]
  }
  return [task.question || `${policyName}常见问题`, `${policyName}如何办理`, `${policyName}需要注意什么`]
}

function openDraftDialog(type) {
  if (!state.qualityDiagnosis) {
    generateQualityDiagnosis()
  }
  state.draftType = type
  state.draftText = JSON.stringify(type === 'FAQ_DRAFT' ? buildFaqDraftPayload() : buildPolicyChunkDraftPayload(), null, 2)
  state.draftVisible = true
  nextTick(() => {
    draftTextareaRef.value?.focus?.()
  })
}

function buildDraftSource() {
  const task = state.detail?.task || {}
  const evidenceSummary = buildEvidenceSummary()
  return {
    sourceFeedbackTaskId: task.id || null,
    sourceQuestion: task.question || '待人工确认',
    sourceAnswer: task.answer || '待人工确认',
    sourceFeedbackType: task.feedbackType || '待人工确认',
    sourceFeedbackContent: task.feedbackContent || '待人工确认',
    sourceEvidenceIds: state.trace?.hitChunkIds || task.evidenceIds || '待人工确认',
    sourceEvidenceSource: task.evidenceSource || '待人工确认',
    routePlan: state.trace?.routePlan || task.routePlan || '待人工确认',
    validationSummary: state.trace?.validationSummary || task.validationSummary || '待人工确认',
    evidenceSummary
  }
}

function buildEvidenceSummary() {
  return [...state.evidence]
    .map((item) => ({
      evidenceType: item.evidenceType || 'UNKNOWN',
      id: item.id || null,
      title: item.title || '待人工确认',
      policyKey: item.policyKey || '',
      policyName: item.policyName || '',
      topicType: item.topicType || '',
      sourceScene: item.sourceScene || '',
      evidenceRole: item.evidenceType === 'MESSAGE' ? '仅作会话上下文，不作为政策依据' : '可作为待人工核验的候选依据',
      contentPreview: item.contentPreview || ''
    }))
    .filter((item) => item.evidenceType !== 'KNOWLEDGE_CHUNK' || !String(item.title || '').includes('文档说明'))
    .sort((a, b) => evidencePriority(a) - evidencePriority(b))
}

function evidencePriority(item) {
  if (item.evidenceType === 'POLICY_CHUNK' && item.sourceScene === 'POLICY_STANDARDIZATION_PHASE1') return 1
  if (item.evidenceType === 'POLICY_CHUNK') return 2
  if (item.evidenceType === 'KNOWLEDGE_CHUNK') return 3
  if (item.evidenceType === 'MESSAGE') return 9
  return 6
}

function buildDraftSafety() {
  return {
    requiresHumanReview: true,
    notForOnlineAnswer: true,
    userFeedbackIsNotPolicyEvidence: true,
    noAutoPublish: true,
    dynamicFieldsRule: '申报入口、截止时间、附件模板、年度批次、金额标准必须以年度通知或主管部门最新要求为准'
  }
}

function buildFaqDraftPayload() {
  const task = state.detail?.task || {}
  const diagnosis = state.qualityDiagnosis || {}
  return {
    draftType: 'FAQ_DRAFT',
    reviewStatus: 'PENDING_REVIEW',
    targetTable: 'ai_policy_faq',
    faq: {
      baseId: task.baseId || null,
      regionScope: task.regionScope || 'XM',
      policyKey: task.policyKey || '待人工确认',
      policyName: policyDisplayName(task),
      slotCode: inferTopicType(task) || task.questionType || '待人工确认',
      standardQuestion: task.question || '待人工确认',
      questionPattern: buildQuestionPattern(task),
      standardAnswer: buildDraftAnswerSkeleton(task, { useSourceAnswer: true }),
      evidenceSource: 'FEEDBACK_DRAFT',
      priority: 100,
      enabled: false
    },
    diagnosis,
    source: buildDraftSource(),
    safety: buildDraftSafety(),
    createdAt: new Date().toISOString()
  }
}

function buildPolicyChunkDraftPayload() {
  const task = state.detail?.task || {}
  const diagnosis = state.qualityDiagnosis || {}
  return {
    draftType: 'POLICY_CHUNK_DRAFT',
    reviewStatus: 'PENDING_REVIEW',
    targetTable: 'ai_policy_chunk',
    policyChunk: {
      baseId: task.baseId || null,
      documentId: null,
      regionScope: task.regionScope || 'XM',
      policyKey: task.policyKey || '待人工确认',
      policyName: policyDisplayName(task),
      policyAliases: '待人工确认',
      docType: 'topic',
      topicType: inferTopicType(task) || '待人工确认',
      questionType: normalizeDraftQuestionType(task),
      title: task.question || `${naturalPolicyName(task)}${inferTopicType(task) || '专题'}补强草稿`,
      content: buildTopicCardSkeleton(task),
      answerLevel: 'STANDARD',
      priority: 100,
      sourceScene: 'FEEDBACK_DRAFT',
      sortNo: 100,
      enabled: false
    },
    diagnosis,
    source: buildDraftSource(),
    safety: buildDraftSafety(),
    createdAt: new Date().toISOString()
  }
}

function buildQuestionPattern(task = {}) {
  const question = task.question || ''
  const policy = naturalPolicyName(task)
  const topic = inferTopicType(task)
  if (task.policyKey === 'double_hundred' || policy.includes('双百')) {
    if (topic === 'department') {
      return [
        question,
        '双百计划主管部门是谁',
        '双百计划由谁负责',
        '双百计划找哪个部门',
        '双百计划找谁办理',
        '双百计划受理部门是谁',
        '双百计划创业人才由谁执行',
        '双百计划创新人才由谁执行',
        '双百计划创新团队谁审核'
      ].filter(Boolean).join('|')
    }
  }
  const patterns = [question, `${policy}主管部门是谁`, `${policy}由谁负责`, `${policy}找谁办理`, `${policy}需要注意什么`]
  return [...new Set(patterns.filter((item) => item && !isInternalPolicyKey(item)))].join('|') || '待人工确认'
}

function buildDraftAnswerSkeleton(task = {}, options = {}) {
  const topic = inferTopicType(task)
  const sourceAnswer = String(state.trace?.finalAnswer || task.answer || '').trim()
  const feedbackContent = String(task.feedbackContent || '').trim()
  if (options.useSourceAnswer && sourceAnswer && topic === 'department') {
    return [
      sourceAnswer,
      feedbackContent ? `待人工核验项：用户反馈关注“${feedbackContent}”，需要政策人员进一步核验正式文件中关于创业人才、创新个人、创新团队具体执行部门或责任主体的表述。` : '待人工核验项：请政策人员核验正式文件中分类执行主体的准确表述。',
      '当前边界：如证据没有明确“创业人才由某部门执行 / 创新人才由某部门执行”，草稿不能写死；年度入口、批次、材料模板和截止时间以年度通知和遴选系统要求为准。'
    ].join('\n\n')
  }
  if (topic === 'department') {
    return '结论：待政策人员补充牵头部门、受理部门、责任链条和分类办理口径。\n办理建议：先区分申报类别，再核对年度通知和主管部门最新要求。\n当前边界：申报入口、批次、材料模板和截止时间以年度通知或主管部门最新要求为准。'
  }
  if (topic === 'process') {
    return '结论：待政策人员补充按申报类别拆解的流程。\n办理建议：应说明申报主体、提交方式、审核评审、公示确认和兑现环节。\n当前边界：具体入口、批次和截止时间以年度通知或主管部门最新要求为准。'
  }
  if (topic === 'material') {
    return '结论：待政策人员补充材料分类和准备方向。\n办理建议：可按身份资质、单位推荐、项目成果、申报表、承诺书等方向准备。\n当前边界：正式材料清单和模板以年度通知或申报系统为准。'
  }
  return '结论：待政策人员基于正式政策依据完善。\n办理建议：补充可执行建议和证据边界。\n当前边界：动态事项以年度通知或主管部门最新要求为准。'
}

function buildTopicCardSkeleton(task = {}) {
  const diagnosis = state.qualityDiagnosis || {}
  return [
    `适用场景：${task.question || '待人工确认'}`,
    `核心答复：${buildDraftAnswerSkeleton(task, { useSourceAnswer: true })}`,
    `价值诊断：${(diagnosis.problems || []).join('；') || '待人工确认'}`,
    '办理建议：待政策人员结合正式文件和年度通知补充可执行步骤。',
    '当前边界：用户反馈不能作为政策事实；申报入口、截止时间、附件模板、年度批次、金额标准必须以年度通知或主管部门最新要求为准。'
  ].join('\n\n')
}

function inferTopicType(task = {}) {
  const current = String(task.topicType || '').toLowerCase()
  if (current) return current
  const text = `${task.question || ''} ${task.feedbackContent || ''}`
  if (hasAny(text, ['主管部门', '受理部门', '责任部门', '归口部门', '哪个部门', '找谁办理', '谁负责', '谁执行'])) return 'department'
  if (hasAny(text, ['材料', '申报表', '证明', '清单'])) return 'material'
  if (hasAny(text, ['流程', '怎么申请', '如何申报', '步骤'])) return 'process'
  if (hasAny(text, ['区别', '不同', '对比', '边界'])) return 'boundary'
  return current
}

function normalizeDraftQuestionType(task = {}) {
  const raw = String(task.questionType || '').toUpperCase()
  if (!raw || ['DEPARTMENT', 'MATERIAL', 'PROCESS', 'BOUNDARY', 'CITY_PROVINCE_BOUNDARY'].includes(raw)) {
    return 'POLICY_QA'
  }
  return raw
}

function policyDisplayName(task = {}) {
  if (task.policyName) return task.policyName
  if (task.policyKey === 'double_hundred') return '厦门市引进高层次创新创业人才“双百计划”实施意见'
  if (task.policyKey === 'xiamen_finance') return '厦门市金融服务产业人才项目实施办法'
  if (task.policyKey === 'postdoc') return '博士后政策'
  return '待人工确认'
}

function naturalPolicyName(task = {}) {
  if (task.policyKey === 'double_hundred' || String(task.policyName || '').includes('双百')) return '双百计划'
  if (task.policyKey === 'xiamen_finance' || String(task.policyName || '').includes('金融服务')) return '金融服务产业人才项目'
  if (task.policyKey === 'postdoc' || String(task.policyName || '').includes('博士后')) return '博士后政策'
  return task.policyName || '该政策'
}

function isInternalPolicyKey(value) {
  return /^[a-z][a-z0-9_]+$/.test(String(value || '').trim())
}

async function saveDraft() {
  const task = state.detail?.task
  if (!task?.id || !state.draftText.trim()) {
    showToast('请先生成草稿')
    return
  }
  try {
    JSON.parse(state.draftText)
  } catch (error) {
    showToast('草稿必须是合法 JSON')
    return
  }
  state.draftSaving = true
  try {
    const payload = {
      draftPayload: state.draftText,
      actionContent: '生成知识补强草稿，仅供人工审核，不写入正式知识库'
    }
    if (state.draftType === 'FAQ_DRAFT') {
      ensureSuccess(await savePolicyCorrectionFaqDraft(task.id, payload), '保存 FAQ 草稿失败')
    } else {
      ensureSuccess(await savePolicyCorrectionChunkDraft(task.id, payload), '保存专题卡草稿失败')
    }
    showToast('草稿已保存为待审核建议')
    state.draftVisible = false
    await refreshDetail()
  } catch (error) {
    showToast(error.message || '保存草稿失败')
  } finally {
    state.draftSaving = false
  }
}

async function copyDraftText() {
  await copyText(state.draftText, '草稿已复制')
}

async function copyActionDraft(action) {
  await copyText(action?.draftPayload || '', '草稿已复制')
}

async function copyText(text, successMessage) {
  if (!text) {
    showToast('没有可复制内容')
    return
  }
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
      showToast(successMessage)
      return
    }
  } catch (error) {
    // Fall through to manual copy hint.
  }
  showToast('当前浏览器不支持自动复制，请手动复制')
}

async function quickStatus(item, status) {
  try {
    ensureSuccess(await updatePolicyCorrectionStatus(item.id, { status, handleResult: '' }), '更新状态失败')
    showToast('状态已更新')
    await fetchList()
  } catch (error) {
    showToast(error.message || '更新状态失败')
  }
}

async function setDetailStatus(status) {
  const id = state.detail?.task?.id
  if (!id) return
  state.actionSubmitting = true
  try {
    ensureSuccess(await updatePolicyCorrectionStatus(id, { status, handleResult: state.handleResult }), '更新状态失败')
    showToast('状态已更新')
    state.handleResult = ''
    await Promise.all([refreshDetail(), fetchList()])
  } catch (error) {
    showToast(error.message || '更新状态失败')
  } finally {
    state.actionSubmitting = false
  }
}

async function submitComment() {
  const id = state.detail?.task?.id
  if (!id || !state.handleResult.trim()) {
    showToast('请先填写处理意见')
    return
  }
  state.actionSubmitting = true
  try {
    ensureSuccess(await commentPolicyCorrectionTask(id, { actionContent: state.handleResult }), '提交处理意见失败')
    showToast('处理意见已记录')
    state.handleResult = ''
    await refreshDetail()
  } catch (error) {
    showToast(error.message || '提交处理意见失败')
  } finally {
    state.actionSubmitting = false
  }
}

async function closeTask(item) {
  try {
    await showConfirmDialog({
      title: '关闭确认',
      message: '关闭后仅表示当前反馈任务结束，不会修改正式政策知识库。是否继续？'
    })
    ensureSuccess(await closePolicyCorrectionTask(item.id, { handleResult: '后台人工关闭' }), '关闭失败')
    showToast('任务已关闭')
    await fetchList()
  } catch (error) {
    if (error?.message) {
      showToast(error.message || '关闭失败')
    }
  }
}

async function closeDetailTask() {
  if (state.detail?.task) {
    await closeTask(state.detail.task)
    await refreshDetail()
  }
}

async function openPromptDialog(task) {
  await ensureTraceEvidenceForPrompt(task)
  state.promptText = buildAiAnalysisPrompt(task)
  state.promptVisible = true
  nextTick(() => {
    promptTextareaRef.value?.focus?.()
    promptTextareaRef.value?.select?.()
  })
}

async function copyPromptForTask(task) {
  await ensureTraceEvidenceForPrompt(task)
  state.promptText = buildAiAnalysisPrompt(task)
  await copyPromptText()
}

async function ensureTraceEvidenceForPrompt(task) {
  const id = task?.id
  if (!id) return
  const jobs = []
  if (!state.trace || state.trace?.task?.id !== id) {
    jobs.push(loadTrace(id))
  }
  if (state.evidenceTaskId !== id) {
    jobs.push(loadEvidence(id))
  }
  if (jobs.length) {
    await Promise.allSettled(jobs)
  }
}

async function copyPromptText() {
  if (!state.promptText) {
    showToast('请先生成提字词')
    return
  }
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(state.promptText)
      showToast('已复制，可发送给 AI 分析。')
      return
    }
    fallbackSelectPromptText()
    showToast('当前浏览器不支持自动复制，请手动复制文本框内容')
  } catch (error) {
    fallbackSelectPromptText()
    showToast('复制失败，请手动复制文本框内容')
  }
}

function fallbackSelectPromptText() {
  state.promptVisible = true
  nextTick(() => {
    promptTextareaRef.value?.focus?.()
    promptTextareaRef.value?.select?.()
  })
}

function buildAiAnalysisPrompt(task = {}) {
  return `你是政策智能体优化助手，请基于以下反馈任务，判断当前政策回答为什么不准确，并给出最小修复建议。

【重要限制】
1. 不允许凭空编造政策内容。
2. 不允许直接修改正式知识库。
3. 用户反馈只能作为人工审核线索。
4. 如果需要补充知识，只能生成待审核的 SQL 草案或专题卡草稿。
5. 涉及申报时间、入口、附件模板、截止日期，必须提示以年度通知为准。
6. 请优先判断是知识缺失、专题卡太浅、问法未命中、路由错误、回答组织问题，还是代码问题。

【用户原问题】
${promptValue(task.question)}

【系统原回答】
${promptValue(task.answer)}

【用户反馈类型】
${promptValue(task.feedbackType)}

【用户反馈说明】
${promptValue(task.feedbackContent)}

【当前路由与证据信息】
policy_key：${promptValue(task.policyKey)}
policy_name：${promptValue(task.policyName)}
topic_type：${promptValue(task.topicType)}
question_type：${promptValue(task.questionType)}
evidence_ids：${promptValue(task.evidenceIds)}
evidence_source：${promptValue(task.evidenceSource)}
route_plan：${promptValue(task.routePlan)}
validation_summary：${promptValue(task.validationSummary)}
status：${promptValue(task.status)}
created_at：${promptValue(formatDateTime(task.createdAt))}

${buildTracePromptSection()}

【请输出】
1. 该反馈属于哪类问题；
2. 当前回答不准确或价值不足的原因；
3. 应优先补 FAQ、补专题卡、补 intent phrase、补 condition_index，还是改代码；
4. 如果是知识补强，请给出推荐的专题卡结构；
5. 如果需要 SQL，请只生成 PostgreSQL SQL 草案，不执行；
6. 如果需要 CODEX 开发，请生成最小修改提字词；
7. 给出回归测试问题；
8. 明确哪些内容必须人工审核后才能生效。`
}

function buildTracePromptSection() {
  const trace = state.trace || {}
  const evidenceLines = Array.isArray(state.evidence) && state.evidence.length
    ? state.evidence.map((item, index) => [
      `${index + 1}. evidenceType：${promptValue(item.evidenceType)}`,
      `id：${promptValue(item.id)}`,
      `title：${promptValue(item.title)}`,
      `policy_key：${promptValue(item.policyKey)}`,
      `policy_name：${promptValue(item.policyName)}`,
      `topic_type：${promptValue(item.topicType)}`,
      `source_scene：${promptValue(item.sourceScene)}`,
      `content：${promptValue(item.fullContent || item.contentPreview)}`
    ].join('\n')).join('\n\n')
    : '未记录'
  const contextLines = traceMessages.value.length
    ? traceMessages.value.map((item, index) => `${index + 1}. ${promptValue(item.role)}：${promptValue(item.content)}\ncited_chunk_ids：${promptValue(item.citedChunkIds)}`).join('\n\n')
    : '未记录'
  return `【回答链路】
answer_mode：${promptValue(trace.answerMode)}
faq_hit：${promptValue(trace.faqHit)}
fallback_flag：${promptValue(trace.fallbackFlag)}
matched_intent_code：${promptValue(trace.matchedIntentCode)}
candidate_generated：${promptValue(trace.candidateGenerated)}
route_plan：${promptValue(trace.routePlan)}
validation_summary：${promptValue(trace.validationSummary)}
hit_chunk_ids：${promptValue(trace.hitChunkIds)}

【命中证据详情】
${evidenceLines}

【会话上下文】
${contextLines}`
}

function promptValue(value) {
  if (value === null || value === undefined) {
    return '未记录'
  }
  const text = String(value).trim()
  return text || '未记录'
}

onMounted(fetchList)
</script>

<style scoped>
.safety-banner,
.filter-panel,
.task-panel,
.task-card,
.detail-shell,
.detail-actions,
.action-timeline,
.trace-panel {
  border: 1px solid #dbeafe;
  border-radius: 18px;
  background: #fff;
}

.safety-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  margin-bottom: 16px;
  background: linear-gradient(135deg, #eff6ff, #ecfeff);
}

.safety-banner strong,
.panel-head strong,
.task-title,
.detail-head strong {
  color: #0f172a;
  font-weight: 800;
}

.safety-banner p,
.panel-head span,
.task-meta,
.task-grid,
.task-feedback,
.detail-head p,
.detail-item span,
.empty-block,
.action-item small {
  color: #64748b;
  font-size: 13px;
}

.filter-panel,
.task-panel {
  padding: 18px;
  margin-bottom: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 7px;
  color: #334155;
  font-size: 13px;
}

.field--wide {
  grid-column: span 2;
}

.field input,
.field select {
  min-height: 40px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 12px;
  background: #fff;
}

.action-row,
.panel-head,
.task-card__top,
.pager,
.status-buttons {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.action-row {
  margin-top: 14px;
}

.action-row--compact {
  margin-top: 0;
}

.panel-head,
.task-card__top,
.pager {
  justify-content: space-between;
}

.task-list {
  display: grid;
  gap: 14px;
  margin-top: 14px;
}

.task-card {
  padding: 16px;
  border-color: #e2e8f0;
}

.task-title {
  font-size: 16px;
}

.task-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.task-feedback {
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
}

.empty-block {
  padding: 28px;
  text-align: center;
}

.pager {
  margin-top: 16px;
}

.detail-popup {
  width: min(860px, 92vw);
  height: 100%;
  background: #f8fafc;
}

.detail-shell {
  min-height: 100%;
  padding: 20px;
  border: none;
  border-radius: 0;
  background: #f8fafc;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.detail-head__actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 8px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

:deep(.detail-item) {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #fff;
}

:deep(.detail-item--wide) {
  grid-column: 1 / -1;
}

:deep(.detail-item p) {
  margin: 7px 0 0;
  color: #0f172a;
  word-break: break-word;
}

:deep(.detail-item__pre) {
  white-space: pre-wrap;
  line-height: 1.7;
}

.detail-actions,
.action-timeline,
.trace-panel {
  padding: 16px;
  margin-top: 16px;
}

.trace-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.evidence-item,
.context-item {
  padding: 14px;
  margin-top: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #f8fafc;
}

.evidence-head,
.context-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.evidence-head strong,
.context-head strong {
  color: #0f172a;
  font-weight: 800;
}

.evidence-head p,
.context-head span,
.context-item small {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 12px;
}

.evidence-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.evidence-preview,
.context-item p {
  margin: 12px 0;
  color: #334155;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.diagnosis-card {
  padding: 14px;
  margin-top: 12px;
  border: 1px solid #bfdbfe;
  border-radius: 14px;
  background: linear-gradient(135deg, #eff6ff, #f8fafc);
}

.diagnosis-head {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.diagnosis-head strong {
  color: #0f172a;
  font-weight: 800;
}

.diagnosis-head span,
.diagnosis-note {
  color: #475569;
  font-size: 13px;
}

.diagnosis-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
  color: #334155;
  font-size: 13px;
}

.diagnosis-list {
  margin: 12px 0 0;
  padding-left: 18px;
  color: #334155;
  line-height: 1.7;
}

.draft-preview {
  display: grid;
  gap: 8px;
  margin: 10px 0;
  padding: 10px;
  border: 1px dashed #93c5fd;
  border-radius: 12px;
  background: #eff6ff;
}

.draft-preview span {
  color: #475569;
  font-size: 13px;
}

.draft-preview pre {
  max-height: 220px;
  overflow: auto;
  margin: 0;
  padding: 10px;
  border-radius: 10px;
  background: #0f172a;
  color: #dbeafe;
  font-size: 12px;
  line-height: 1.6;
  white-space: pre-wrap;
}

.status-buttons {
  margin: 12px 0;
}

.action-item {
  padding: 12px;
  margin-top: 10px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #f8fafc;
}

.action-item__head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.action-item p {
  margin: 8px 0;
  color: #334155;
}

.prompt-popup {
  width: min(760px, 92vw);
  max-height: 86vh;
  overflow: hidden;
  background: #fff;
}

.prompt-dialog {
  display: grid;
  grid-template-rows: auto minmax(320px, 1fr) auto;
  gap: 14px;
  max-height: 86vh;
  padding: 20px;
}

.prompt-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.prompt-head strong {
  color: #0f172a;
  font-size: 18px;
  font-weight: 800;
}

.prompt-head p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.prompt-textarea {
  width: 100%;
  min-height: 360px;
  resize: vertical;
  padding: 14px;
  border: 1px solid #cbd5e1;
  border-radius: 14px;
  outline: none;
  background: #f8fafc;
  color: #0f172a;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}

.prompt-textarea:focus {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.12);
}

.prompt-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 1024px) {
  .filter-grid,
  .task-grid,
  .detail-grid,
  .trace-grid,
  .diagnosis-grid {
    grid-template-columns: 1fr;
  }

  .field--wide,
  :deep(.detail-item--wide) {
    grid-column: auto;
  }
}
</style>

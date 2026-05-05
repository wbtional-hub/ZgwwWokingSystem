<template>
  <section ref="rootRef" class="leadership-members">
    <div class="leadership-members__toolbar">
      <div>
        <p class="leadership-members__eyebrow">下级考勤</p>
        <h3 class="leadership-members__title">卡片化查看下属今日状态与待审事项</h3>
      </div>
      <button type="button" class="leadership-members__refresh" @click="$emit('refresh')">
        刷新数据
      </button>
    </div>

    <div class="leadership-members__filters">
      <label class="leadership-filter">
        <span class="leadership-filter__label">状态</span>
        <select :value="status" @change="$emit('update:status', $event.target.value)">
          <option v-for="item in statusOptions" :key="item.value" :value="item.value">
            {{ item.label }}
          </option>
        </select>
      </label>

      <label class="leadership-filter">
        <span class="leadership-filter__label">部门</span>
        <select :value="department" @change="$emit('update:department', $event.target.value)">
          <option value="ALL">全部部门</option>
          <option v-for="item in departmentOptions" :key="item" :value="item">
            {{ item }}
          </option>
        </select>
      </label>

      <label class="leadership-filter leadership-filter--search">
        <span class="leadership-filter__label">姓名搜索</span>
        <input
          :value="keyword"
          type="text"
          placeholder="按姓名、账号或岗位筛选"
          @input="$emit('update:keyword', $event.target.value)"
        >
      </label>

      <button type="button" class="leadership-members__clear" @click="$emit('clear-filters')">
        清空筛选
      </button>
    </div>

    <van-loading v-if="loading" class="leadership-members__loading" size="24px" vertical>
      正在整理下属考勤卡片...
    </van-loading>

    <van-empty v-else-if="!members.length" description="当前筛选下没有可展示的成员" />

    <div v-else class="leadership-member-list">
      <article
        v-for="member in members"
        :key="member.userId"
        class="leadership-member-card"
        :data-member-id="member.userId"
        :class="{ 'leadership-member-card--active': selectedUserId === member.userId }"
      >
        <button type="button" class="leadership-member-card__header" @click="$emit('select-member', member)">
          <div class="leadership-member-card__identity-main">
            <h4 class="leadership-member-card__name">{{ member.realName }}</h4>
            <p class="leadership-member-card__meta">
              {{ member.unitName || '未归属部门' }} / {{ member.jobTitle || '岗位未填写' }}
            </p>
          </div>
          <div class="leadership-member-card__identity-side">
            <span class="leadership-member-card__status" :class="`leadership-member-card__status--${member.statusTone || 'pending'}`">
              {{ member.statusLabel || '待处理' }}
            </span>
            <span v-if="member.readonlyMode" class="leadership-member-card__readonly">跨部门只读</span>
            <span v-else-if="member.pendingReviewCount" class="leadership-member-card__pending">
              待审 {{ member.pendingReviewCount }}
            </span>
          </div>
        </button>

        <div v-if="memberSignalBadges(member).length" class="leadership-member-card__signals">
          <span
            v-for="badge in memberSignalBadges(member)"
            :key="`${member.userId}-${badge.key}`"
            class="leadership-member-card__signal"
            :class="`leadership-member-card__signal--${badge.tone}`"
          >
            {{ badge.label }}
          </span>
        </div>

        <div class="leadership-member-card__node-grid">
          <article
            v-for="node in getMemberNodes(member)"
            :key="`${member.userId}-${node.nodeCode}`"
            class="leadership-node-card"
            :class="`leadership-node-card--${node.statusTone || 'pending'}`"
            :style="nodeCardStyle(node)"
          >
            <div class="leadership-node-card__top">
              <span class="leadership-node-card__step" :style="nodePhaseStyle(node)">{{ node.stepLabel }}</span>
              <span class="leadership-node-card__status">{{ node.statusLabel }}</span>
            </div>
            <div class="leadership-node-card__title">{{ node.nodeTitleShort }}</div>
            <div class="leadership-node-card__time-row">
              <strong class="leadership-node-card__time">{{ node.ruleTimeText || '--:--' }}</strong>
              <span v-if="node.summaryText" class="leadership-node-card__summary">{{ node.summaryText }}</span>
            </div>
            <div v-if="node.timeRangeText && node.timeRangeText !== node.ruleTimeText" class="leadership-node-card__range">{{ node.timeRangeText }}</div>
            <p v-if="node.simpleRemark" class="leadership-node-card__remark">{{ node.simpleRemark }}</p>
            <div v-if="node.actions?.length" class="leadership-node-card__actions">
              <button
                v-for="action in node.actions"
                :key="`${member.userId}-${node.nodeCode}-${action.key}`"
                type="button"
                class="leadership-node-card__action"
                :class="`leadership-node-card__action--${action.tone || 'secondary'}`"
                :disabled="Boolean(reviewLoading)"
                @click.stop="$emit('review-member', { member, node, action })"
              >
                {{ action.label }}
              </button>
            </div>
          </article>
        </div>

        <p v-if="memberExceptionSummary(member)" class="leadership-member-card__hint">
          {{ memberExceptionSummary(member) }}
        </p>

        <div class="leadership-member-card__summary">
          <span>本周出勤 {{ member.weekSummary?.attendanceDays ?? 0 }} 天</span>
          <span>迟到 {{ member.weekSummary?.lateCount ?? 0 }} 次</span>
          <span>异常 {{ member.weekSummary?.abnormalCount ?? 0 }} 次</span>
        </div>

        <div class="leadership-member-card__footer">
          <button
            type="button"
            class="leadership-member-card__view"
            @click="$emit('select-member', member)"
          >
            查看
          </button>
          <button
            v-if="member.canReviewPending"
            type="button"
            class="leadership-member-card__review"
            :disabled="Boolean(reviewLoading)"
            @click="$emit('review-member', { member, action: { key: 'review', tone: 'primary' } })"
          >
            {{ member.reviewActionLabel || '审核申请' }}
          </button>
        </div>
      </article>
    </div>
  </section>
</template>

<script setup>
import { nextTick, ref, watch } from 'vue'

const props = defineProps({
  members: {
    type: Array,
    default: () => []
  },
  loading: {
    type: Boolean,
    default: false
  },
  reviewLoading: {
    type: Boolean,
    default: false
  },
  selectedUserId: {
    type: Number,
    default: null
  },
  keyword: {
    type: String,
    default: ''
  },
  status: {
    type: String,
    default: 'ALL'
  },
  department: {
    type: String,
    default: 'ALL'
  },
  departmentOptions: {
    type: Array,
    default: () => []
  },
  statusOptions: {
    type: Array,
    default: () => []
  }
})

defineEmits([
  'select-member',
  'update:keyword',
  'update:status',
  'update:department',
  'clear-filters',
  'refresh',
  'review-member'
])

const rootRef = ref(null)

const FALLBACK_NODE_META = Object.freeze([
  {
    nodeCode: 'AM_ON',
    stepLabel: '上午',
    nodeTitleShort: '上班打卡',
    timeField: 'checkInTime',
    accentColor: '#4F7DF3',
    accentSoft: 'rgba(79, 125, 243, 0.12)',
    accentBorder: 'rgba(79, 125, 243, 0.22)'
  },
  {
    nodeCode: 'AM_OFF',
    stepLabel: '上午',
    nodeTitleShort: '下班打卡',
    timeField: 'amOffTime',
    accentColor: '#33B18A',
    accentSoft: 'rgba(51, 177, 138, 0.12)',
    accentBorder: 'rgba(51, 177, 138, 0.22)'
  },
  {
    nodeCode: 'PM_ON',
    stepLabel: '下午',
    nodeTitleShort: '上班打卡',
    timeField: 'pmOnTime',
    accentColor: '#E7A93B',
    accentSoft: 'rgba(231, 169, 59, 0.14)',
    accentBorder: 'rgba(231, 169, 59, 0.24)'
  },
  {
    nodeCode: 'PM_OFF',
    stepLabel: '下午',
    nodeTitleShort: '下班打卡',
    timeField: 'checkOutTime',
    accentColor: '#6C78D8',
    accentSoft: 'rgba(108, 120, 216, 0.14)',
    accentBorder: 'rgba(108, 120, 216, 0.24)'
  }
])

watch(() => props.selectedUserId, async (selectedUserId) => {
  if (!selectedUserId) {
    return
  }
  await nextTick()
  if (typeof window === 'undefined' || !window.matchMedia('(max-width: 960px)').matches) {
    return
  }
  const selectedCard = rootRef.value?.querySelector(`[data-member-id="${selectedUserId}"]`)
  selectedCard?.scrollIntoView({
    behavior: 'smooth',
    block: 'nearest'
  })
})

function nodeCardStyle(node) {
  return {
    borderColor: node?.accentBorder || 'rgba(148, 163, 184, 0.16)',
    background: `linear-gradient(180deg, ${node?.accentSoft || 'rgba(248, 250, 252, 0.96)'}, rgba(255, 255, 255, 0.96))`
  }
}

function nodePhaseStyle(node) {
  return {
    color: node?.accentColor || '#64748b',
    background: node?.accentSoft || 'rgba(148, 163, 184, 0.14)'
  }
}

function formatShortTime(value) {
  if (!value) {
    return ''
  }
  const text = String(value)
  const match = text.match(/(\d{2}:\d{2})/)
  return match ? match[1] : text
}

function resolveRuleTimeText(nodeCode, timeRangeText = '') {
  const text = String(timeRangeText || '').trim()
  if (!text || text === '-') {
    return '--:--'
  }
  const times = text.match(/\d{1,2}:\d{2}/g) || []
  if (!times.length) {
    return text
  }
  if (nodeCode === 'AM_OFF' || nodeCode === 'PM_OFF') {
    return times[times.length - 1] || '--:--'
  }
  return times[0] || '--:--'
}

function buildNodeSummary(actualPunchTime, simpleRemark) {
  if (actualPunchTime) {
    return `实际 ${actualPunchTime}`
  }
  return simpleRemark ? '' : '暂无记录'
}

function buildNodeRemark(actualPunchTime, simpleRemark, statusTone) {
  const remark = String(simpleRemark || '').trim()
  if (!remark) {
    return ''
  }
  if (actualPunchTime && (statusTone === 'normal' || statusTone === 'approved')) {
    return ''
  }
  return remark
}

function collectMemberTexts(member) {
  return [
    member?.statusLabel,
    member?.statusSummary,
    member?.todayRecord?.checkInFailReason,
    member?.todayRecord?.checkInResult
  ]
    .filter(Boolean)
    .join(' ')
}

function containsAny(text, keywords) {
  return keywords.some(keyword => text.includes(keyword))
}

function memberSignalBadges(member) {
  const text = collectMemberTexts(member)
  const badges = []

  if (containsAny(text, ['取证待审', '取证审核中'])) {
    badges.push({ key: 'evidence-pending', label: '取证待审', tone: 'evidence' })
  }
  if (containsAny(text, ['取证已处理', '取证已通过', '取证已驳回'])) {
    badges.push({ key: 'evidence-done', label: '取证已处理', tone: 'approved' })
  }
  if (containsAny(text, ['补卡待审', '补打卡待审', '补卡审核中', '补打卡审核中'])) {
    badges.push({ key: 'makeup-pending', label: '补卡待审', tone: 'makeup' })
  }
  if (containsAny(text, ['补卡已处理', '补打卡已处理', '补卡已通过', '补卡已驳回', '补打卡已通过', '补打卡已驳回'])) {
    badges.push({ key: 'makeup-done', label: '补卡已处理', tone: 'approved' })
  }
  if (!badges.length && Number(member?.pendingReviewCount || 0) > 0) {
    badges.push({
      key: 'pending-review',
      label: `待审批 ${member.pendingReviewCount}`,
      tone: 'pending'
    })
  }

  return badges
}

function memberExceptionSummary(member) {
  const badges = memberSignalBadges(member)
  if (badges.length) {
    return `异常：${badges.map(item => item.label).join(' / ')}`
  }
  const text = String(member?.statusSummary || member?.todayRecord?.checkInFailReason || '').trim()
  return text
}

function resolveLateNodeCode(member, record) {
  const text = collectMemberTexts(member)
  if (text.includes('下午')) {
    return record?.pmOnTime ? 'PM_ON' : 'AM_ON'
  }
  return record?.checkInTime ? 'AM_ON' : (record?.pmOnTime ? 'PM_ON' : 'AM_ON')
}

function resolveEarlyNodeCode(member, record) {
  const text = collectMemberTexts(member)
  if (text.includes('上午')) {
    return record?.amOffTime ? 'AM_OFF' : 'PM_OFF'
  }
  return record?.checkOutTime ? 'PM_OFF' : (record?.amOffTime ? 'AM_OFF' : 'PM_OFF')
}

function resolveFallbackNodeStatus(meta, member, record) {
  const actualTime = formatShortTime(record?.[meta.timeField])
  if (member?.statusKey === 'rest') {
    return actualTime ? '已记录' : '非工作日'
  }

  const lateNodeCode = resolveLateNodeCode(member, record)
  const earlyNodeCode = resolveEarlyNodeCode(member, record)

  if (actualTime) {
    if (member?.statusKey === 'late' && meta.nodeCode === lateNodeCode) {
      return '迟到'
    }
    if (containsAny(collectMemberTexts(member), ['早退']) && meta.nodeCode === earlyNodeCode) {
      return '早退'
    }
    return meta.nodeCode === 'AM_ON' || meta.nodeCode === 'PM_ON' ? '已签到' : '已签退'
  }

  return '未打卡'
}

function resolveNodeToneByLabel(label, fallbackTone = 'pending') {
  const text = String(label || '')
  if (containsAny(text, ['迟到', '早退'])) {
    return 'late'
  }
  if (containsAny(text, ['取证'])) {
    return text.includes('待审') ? 'evidence' : 'approved'
  }
  if (containsAny(text, ['补卡'])) {
    return text.includes('待审') ? 'makeup' : 'approved'
  }
  if (containsAny(text, ['已签到', '已签退', '已记录', '正常'])) {
    return 'normal'
  }
  if (containsAny(text, ['未打卡'])) {
    return 'missing'
  }
  if (containsAny(text, ['待处理', '非工作日'])) {
    return 'pending'
  }
  return fallbackTone
}

function normalizeExistingNode(node, member) {
  const actualPunchTime = formatShortTime(node?.actualPunchTime)
  const statusLabel = node?.statusLabel || '待处理'
  const statusTone = node?.statusTone || resolveNodeToneByLabel(statusLabel, member?.statusTone || 'pending')
  return {
    nodeCode: node?.nodeCode || '',
    stepLabel: node?.stepLabel || '-',
    nodeTitleShort: node?.nodeTitleShort || '-',
    timeRangeText: node?.timeRangeText || '',
    ruleTimeText: resolveRuleTimeText(node?.nodeCode, node?.timeRangeText),
    actualPunchTime,
    simpleRemark: buildNodeRemark(actualPunchTime, node?.simpleRemark, statusTone),
    summaryText: buildNodeSummary(actualPunchTime, node?.simpleRemark),
    statusLabel,
    statusTone,
    accentColor: node?.accentColor || '#64748b',
    accentSoft: node?.accentSoft || 'rgba(148, 163, 184, 0.14)',
    accentBorder: node?.accentBorder || 'rgba(148, 163, 184, 0.18)',
    actions: Array.isArray(node?.actions) ? node.actions : []
  }
}

function buildFallbackNode(meta, member) {
  const record = member?.todayRecord || {}
  const actualPunchTime = formatShortTime(record?.[meta.timeField])
  const statusLabel = resolveFallbackNodeStatus(meta, member, record)
  const statusTone = resolveNodeToneByLabel(statusLabel, member?.statusTone || 'pending')
  return {
    nodeCode: meta.nodeCode,
    stepLabel: meta.stepLabel,
    nodeTitleShort: meta.nodeTitleShort,
    timeRangeText: '',
    ruleTimeText: '--:--',
    actualPunchTime,
    simpleRemark: '',
    summaryText: buildNodeSummary(actualPunchTime, ''),
    statusLabel,
    statusTone,
    accentColor: meta.accentColor,
    accentSoft: meta.accentSoft,
    accentBorder: meta.accentBorder,
    actions: []
  }
}

function getMemberNodes(member) {
  if (Array.isArray(member?.nodes) && member.nodes.length) {
    return member.nodes.slice(0, 4).map(node => normalizeExistingNode(node, member))
  }
  return FALLBACK_NODE_META.map(meta => buildFallbackNode(meta, member))
}
</script>

<style scoped>
.leadership-members {
  display: grid;
  gap: 18px;
}

.leadership-members__toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-end;
}

.leadership-members__eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.leadership-members__title {
  margin: 0;
  font-size: clamp(22px, 3vw, 28px);
  line-height: 1.2;
  color: #0f172a;
}

.leadership-members__refresh,
.leadership-members__clear,
.leadership-member-card__view,
.leadership-member-card__review,
.leadership-node-card__action {
  height: 40px;
  border: 1px solid rgba(15, 23, 42, 0.12);
  border-radius: 999px;
  background: #fff;
  color: #0f172a;
  padding: 0 16px;
  font-size: 13px;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.leadership-members__refresh:hover,
.leadership-members__clear:hover,
.leadership-member-card__view:hover,
.leadership-member-card__review:hover,
.leadership-node-card__action:hover {
  transform: translateY(-1px);
  box-shadow: 0 10px 20px rgba(15, 23, 42, 0.08);
}

.leadership-members__filters {
  display: grid;
  grid-template-columns: 140px 180px minmax(0, 1fr) auto;
  gap: 12px;
}

.leadership-filter {
  display: grid;
  gap: 8px;
}

.leadership-filter__label {
  font-size: 12px;
  color: #64748b;
}

.leadership-filter select,
.leadership-filter input {
  width: 100%;
  height: 42px;
  padding: 0 14px;
  border: 1px solid rgba(148, 163, 184, 0.32);
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.94);
  color: #0f172a;
  outline: none;
}

.leadership-filter select:focus,
.leadership-filter input:focus {
  border-color: rgba(79, 125, 243, 0.45);
  box-shadow: 0 0 0 4px rgba(79, 125, 243, 0.08);
}

.leadership-members__loading {
  min-height: 220px;
  display: grid;
  place-items: center;
}

.leadership-member-list {
  display: grid;
  gap: 14px;
}

.leadership-member-card {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 24px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(255, 255, 255, 0.92);
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease, background 0.2s ease;
}

.leadership-member-card--active {
  border-color: rgba(79, 125, 243, 0.24);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.08);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(241, 245, 249, 0.98));
}

.leadership-member-card__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
  width: 100%;
  padding: 0;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.leadership-member-card__identity-main {
  min-width: 0;
}

.leadership-member-card__identity-side {
  display: grid;
  justify-items: end;
  gap: 8px;
}

.leadership-member-card__name {
  margin: 0;
  font-size: 20px;
  line-height: 1.2;
  color: #0f172a;
}

.leadership-member-card__meta,
.leadership-member-card__hint,
.leadership-node-card__range,
.leadership-node-card__remark,
.leadership-node-card__summary {
  margin: 0;
  font-size: 13px;
  line-height: 1.55;
  color: #64748b;
}

.leadership-member-card__status,
.leadership-member-card__readonly,
.leadership-member-card__pending,
.leadership-member-card__signal,
.leadership-node-card__status {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.leadership-member-card__status--normal,
.leadership-node-card--normal .leadership-node-card__status {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.leadership-member-card__status--late,
.leadership-node-card--late .leadership-node-card__status,
.leadership-node-card--makeup .leadership-node-card__status {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.leadership-member-card__status--field,
.leadership-node-card--evidence .leadership-node-card__status {
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
}

.leadership-member-card__status--missing,
.leadership-member-card__status--abnormal,
.leadership-node-card--missing .leadership-node-card__status,
.leadership-node-card--rejected .leadership-node-card__status {
  background: rgba(248, 113, 113, 0.14);
  color: #b91c1c;
}

.leadership-member-card__status--pending,
.leadership-node-card--pending .leadership-node-card__status {
  background: rgba(148, 163, 184, 0.16);
  color: #475569;
}

.leadership-node-card--approved .leadership-node-card__status {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.leadership-member-card__readonly {
  background: rgba(14, 165, 233, 0.1);
  color: #0369a1;
}

.leadership-member-card__pending,
.leadership-member-card__signal--pending {
  background: rgba(79, 125, 243, 0.12);
  color: #315fd3;
}

.leadership-member-card__signal--evidence {
  background: rgba(14, 165, 233, 0.12);
  color: #0369a1;
}

.leadership-member-card__signal--makeup {
  background: rgba(231, 169, 59, 0.14);
  color: #b7791f;
}

.leadership-member-card__signal--approved {
  background: rgba(34, 197, 94, 0.12);
  color: #15803d;
}

.leadership-member-card__signals {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.leadership-member-card__node-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.leadership-node-card {
  display: grid;
  gap: 10px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.leadership-node-card__top {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.leadership-node-card__step {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.leadership-node-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.leadership-node-card__time-row {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
}

.leadership-node-card__time {
  font-size: 18px;
  line-height: 1.1;
  color: #0f172a;
}

.leadership-node-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.leadership-node-card__action--primary,
.leadership-member-card__review {
  border-color: rgba(79, 125, 243, 0.2);
  background: rgba(79, 125, 243, 0.1);
  color: #315fd3;
}

.leadership-member-card__view {
  border-color: rgba(148, 163, 184, 0.2);
  background: #fff;
  color: #334155;
}

.leadership-node-card__action--warning {
  border-color: rgba(231, 169, 59, 0.24);
  background: rgba(231, 169, 59, 0.12);
  color: #b7791f;
}

.leadership-member-card__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 12px;
}

.leadership-member-card__summary span {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.18);
  font-size: 12px;
  color: #334155;
}

.leadership-member-card__footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media (max-width: 960px) {
  .leadership-members__filters {
    grid-template-columns: 1fr;
  }

  .leadership-members__toolbar {
    align-items: stretch;
  }
}

@media (max-width: 640px) {
  .leadership-members {
    gap: 14px;
  }

  .leadership-member-card {
    gap: 12px;
    padding: 16px;
    border-radius: 20px;
  }

  .leadership-members__toolbar,
  .leadership-member-card__header {
    flex-direction: column;
    align-items: stretch;
  }

  .leadership-member-card__identity-side {
    justify-items: start;
  }

  .leadership-member-card__name {
    font-size: 18px;
  }

  .leadership-member-card__node-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 10px;
  }

  .leadership-node-card {
    gap: 8px;
    padding: 12px;
    border-radius: 16px;
  }

  .leadership-node-card__top {
    align-items: flex-start;
    flex-direction: column;
  }

  .leadership-node-card__title {
    font-size: 14px;
  }

  .leadership-node-card__time {
    font-size: 16px;
  }

  .leadership-node-card__range,
  .leadership-node-card__remark,
  .leadership-member-card__summary,
  .leadership-node-card__actions {
    display: none;
  }

  .leadership-member-card__hint {
    font-size: 12px;
    line-height: 1.5;
  }

  .leadership-member-card__footer {
    justify-content: stretch;
  }

  .leadership-member-card__view,
  .leadership-member-card__review {
    flex: 1 1 0;
  }
}
</style>

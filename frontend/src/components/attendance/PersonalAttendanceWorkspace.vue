<template>
  <section class="personal-workspace">
    <van-loading v-if="loading" class="personal-workspace__loading" size="24px" vertical>
      正在同步个人考勤状态...
    </van-loading>

    <template v-else>
      <article class="personal-focus-card">
        <header class="personal-focus-card__header">
          <div>
            <h2 class="personal-focus-card__title">今日考勤</h2>
            <p class="personal-focus-card__date">{{ todayDateText }}</p>
          </div>
          <span class="personal-status" :class="`personal-status--${todayStatusTone}`">
            {{ todayStatusLabel || '未打卡' }}
          </span>
        </header>

        <article v-if="notice" class="personal-notice">
          <strong>{{ notice.title }}</strong>
          <p>{{ notice.text }}</p>
        </article>

        <div class="personal-today-list">
          <article
            v-for="node in todayNodeRows"
            :key="node.nodeCode"
            class="personal-today-row"
            :class="`personal-today-row--${node.statusTone || 'pending'}`"
          >
            <div class="personal-today-row__name">
              <span class="personal-today-row__dot" :style="{ background: node.accentColor || '#94a3b8' }"></span>
              <strong>{{ node.fullTitle }}</strong>
            </div>
            <div class="personal-today-row__rule">{{ node.ruleTimeText }}</div>
            <div class="personal-today-row__state">
              <span class="personal-today-row__badge">{{ node.displayStatusLabel }}</span>
              <small v-if="node.actualTimeText">{{ node.actualTimeText }}</small>
            </div>
            <div class="personal-today-row__actions">
              <button
                v-if="node.rowAction"
                type="button"
                class="personal-today-row__action"
                :class="`personal-today-row__action--${node.rowAction.tone || 'secondary'}`"
                @click.stop="$emit('node-action', { node, action: node.rowAction })"
              >
                {{ node.rowAction.label }}
              </button>
            </div>
          </article>
        </div>

        <article class="personal-action-orb-wrap">
          <button
            type="button"
            class="personal-action-orb"
            :class="[
              `personal-action-orb--${primaryActionTone}`,
              { 'personal-action-orb--disabled': primaryActionDisabled }
            ]"
            :disabled="primaryActionDisabled"
            @click="handlePrimaryAction"
          >
            <span class="personal-action-orb__time">{{ currentClockText }}</span>
            <strong>{{ primaryActionLabel }}</strong>
          </button>
          <p class="personal-action-orb-wrap__hint">{{ checkInHint || primaryActionHint }}</p>
        </article>

        <div class="personal-focus-card__footer">
          <article class="personal-location">
            <span class="personal-location__label">打卡地点</span>
            <strong>{{ locationText || '-' }}</strong>
            <p>{{ locationDetail || '当前暂无可展示的定位信息。' }}</p>
            <small v-if="locationStatus">{{ locationStatus }}</small>
          </article>
        </div>
      </article>

      <div class="personal-workspace__supplement">
        <article class="personal-panel">
          <div class="personal-panel__head">
            <div>
              <p class="personal-panel__eyebrow">本周统计</p>
              <h3 class="personal-panel__title">本周概览</h3>
            </div>
          </div>

          <div class="personal-week-grid">
            <div v-for="item in weekCards" :key="item.key" class="personal-week-item">
              <span>{{ item.label }}</span>
              <strong>{{ item.value }}</strong>
            </div>
          </div>
        </article>

        <article class="personal-panel">
          <div class="personal-panel__head">
            <div>
              <p class="personal-panel__eyebrow">最近记录</p>
              <h3 class="personal-panel__title">最近 5 条</h3>
            </div>
          </div>

          <van-empty v-if="!recentRecords.length" description="暂无最近打卡记录" />

          <div v-else class="personal-record-list">
            <article v-for="item in recentRecords" :key="item.id || item.recordKey" class="personal-record">
              <div class="personal-record__main">
                <strong>{{ item.dateText || '-' }}</strong>
                <span>{{ item.statusLabel || '-' }}</span>
              </div>
              <div class="personal-record__meta">{{ item.timeText || '-' }}</div>
              <div class="personal-record__meta">{{ item.addressText || '-' }}</div>
            </article>
          </div>
        </article>
      </div>
    </template>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  loading: {
    type: Boolean,
    default: false
  },
  name: {
    type: String,
    default: ''
  },
  todayStatusLabel: {
    type: String,
    default: ''
  },
  todayStatusTone: {
    type: String,
    default: 'pending'
  },
  notice: {
    type: Object,
    default: null
  },
  locationText: {
    type: String,
    default: ''
  },
  locationDetail: {
    type: String,
    default: ''
  },
  locationStatus: {
    type: String,
    default: ''
  },
  canCheckIn: {
    type: Boolean,
    default: true
  },
  checkingIn: {
    type: Boolean,
    default: false
  },
  weeklyStats: {
    type: Object,
    default: () => ({
      attendanceDays: 0,
      lateCount: 0,
      missingCount: 0
    })
  },
  recentRecords: {
    type: Array,
    default: () => []
  },
  checkInHint: {
    type: String,
    default: ''
  },
  checkInButtonText: {
    type: String,
    default: '上班打卡'
  },
  nodeCards: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['check-in', 'node-action'])

const now = ref(new Date())
let clockTimer = null

onMounted(() => {
  clockTimer = window.setInterval(() => {
    now.value = new Date()
  }, 60 * 1000)
})

onBeforeUnmount(() => {
  if (clockTimer) {
    window.clearInterval(clockTimer)
    clockTimer = null
  }
})

const mainActionNode = computed(() => {
  return props.nodeCards.find(node => node?.canPunch)
    || props.nodeCards.find(node => node?.needEarlyConfirm)
    || props.nodeCards.find(node => node?.canEvidence || node?.canApplyMakeup)
    || props.nodeCards[0]
    || null
})

const mainActionPhaseStyle = computed(() => {
  const color = mainActionNode.value?.accentColor || '#4F7DF3'
  const soft = mainActionNode.value?.accentSoft || 'rgba(79, 125, 243, 0.12)'
  return {
    color,
    background: soft
  }
})

const mainActionButtonStyle = computed(() => {
  const color = mainActionNode.value?.accentColor || '#4F7DF3'
  return {
    background: color,
    boxShadow: `0 16px 28px ${withAlpha(color, 0.22)}`
  }
})

const todayDateText = computed(() => {
  const date = now.value
  const weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())} ${weekdays[date.getDay()]}`
})

const currentClockText = computed(() => {
  return `${pad2(now.value.getHours())}:${pad2(now.value.getMinutes())}`
})

const todayNodeRows = computed(() => {
  return props.nodeCards.map((node) => {
    const actualTimeText = formatNodeActualTime(node?.actualPunchTime)
    return {
      ...node,
      fullTitle: resolveNodeFullTitle(node),
      ruleTimeText: resolveNodeRuleTime(node),
      actualTimeText,
      displayStatusLabel: resolveDisplayStatusLabel(node, actualTimeText),
      rowAction: resolveNodeRowAction(node)
    }
  })
})

const primaryAction = computed(() => {
  const node = mainActionNode.value
  if (!node) {
    return {
      key: 'none',
      label: '今日无需打卡',
      disabled: true,
      node: null
    }
  }

  if (props.canCheckIn && (node.canPunch || node.needEarlyConfirm)) {
    return {
      key: 'check-in',
      label: props.checkingIn ? '正在打卡...' : resolvePrimaryPunchLabel(node),
      disabled: props.checkingIn,
      node
    }
  }

  if (node.canEvidence) {
    return {
      key: 'evidence',
      label: '补打卡',
      disabled: false,
      node
    }
  }

  if (node.canApplyMakeup) {
    return {
      key: 'makeup',
      label: '补打卡',
      disabled: false,
      node
    }
  }

  return {
    key: 'none',
    label: props.checkInButtonText || '今日无需打卡',
    disabled: true,
    node
  }
})

const primaryActionLabel = computed(() => primaryAction.value.label || '今日无需打卡')

const primaryActionDisabled = computed(() => Boolean(primaryAction.value.disabled))

const primaryActionTone = computed(() => {
  if (primaryActionDisabled.value) {
    return 'disabled'
  }
  if (primaryAction.value.key === 'evidence') {
    return 'evidence'
  }
  if (primaryAction.value.key === 'makeup') {
    return 'makeup'
  }
  return 'punchable'
})

const primaryActionHint = computed(() => {
  if (primaryAction.value.key === 'evidence') {
    return '当前节点可提交补打卡申请。'
  }
  if (primaryAction.value.key === 'makeup') {
    return '当前节点可发起补打卡申请。'
  }
  return '当前时段无需操作。'
})

const weekCards = computed(() => [
  {
    key: 'attendance',
    label: '出勤',
    value: props.weeklyStats.attendanceDays ?? 0
  },
  {
    key: 'late',
    label: '迟到',
    value: props.weeklyStats.lateCount ?? 0
  },
  {
    key: 'missing',
    label: '缺卡',
    value: props.weeklyStats.missingCount ?? 0
  }
])

function nodeCardStyle(node) {
  return {
    borderColor: node?.accentBorder || 'rgba(148, 163, 184, 0.16)',
    background: `linear-gradient(180deg, ${node?.accentSoft || 'rgba(255, 255, 255, 0.92)'}, rgba(255, 255, 255, 0.96))`
  }
}

function nodePhaseStyle(node) {
  return {
    color: node?.accentColor || '#64748b',
    background: node?.accentSoft || 'rgba(148, 163, 184, 0.14)'
  }
}

function handlePrimaryAction() {
  const action = primaryAction.value
  if (action.disabled) {
    return
  }

  if (action.key === 'check-in') {
    emit('check-in')
    return
  }

  if (action.key === 'evidence' || action.key === 'makeup') {
    emit('node-action', {
      node: action.node,
      action: {
        key: action.key,
        label: action.label
      }
    })
  }
}

function resolveNodeFullTitle(node) {
  const phase = node?.stepLabel || resolveNodePhase(node?.nodeCode)
  const title = resolveNodeShortTitle(node)
  return `${phase}${title}`
}

function resolvePrimaryPunchLabel(node) {
  return `${resolveNodeFullTitle(node)}打卡`
}

function resolveNodePhase(nodeCode) {
  return String(nodeCode || '').startsWith('PM') ? '下午' : '上午'
}

function resolveNodeShortTitle(node) {
  const code = String(node?.nodeCode || '')
  if (node?.nodeTitleShort === '上班打卡' || node?.nodeTitleShort === '下班打卡') {
    return node.nodeTitleShort.replace('打卡', '')
  }
  return code.endsWith('_OFF') ? '下班' : '上班'
}

function resolveDisplayStatusLabel(node, actualTimeText) {
  const isOffNode = String(node?.nodeCode || '').endsWith('_OFF')
  const label = String(node?.statusLabel || '').trim()
  const code = String(node?.statusCode || '').toUpperCase()

  if (actualTimeText && (!label || label === '正常' || label === '已通过')) {
    return isOffNode ? '已签退' : '已签到'
  }
  if (code.includes('APPROVED')) {
    return '已处理'
  }
  if (code.includes('PENDING')) {
    return '补卡待审'
  }
  if (node?.canEvidence) {
    return '待处理'
  }
  if (node?.canApplyMakeup) {
    return '待处理'
  }
  if (node?.canPunch || node?.needEarlyConfirm) {
    return isOffNode ? '待签退' : '待签到'
  }
  if (!label || label === '待处理') {
    return '未开始'
  }
  if (label.includes('取证')) {
    return label.replace('取证', '补卡')
  }
  return label
}

function resolveNodeRowAction(node) {
  if (node?.canEvidence) {
    return {
      key: 'evidence',
      label: '补打卡',
      tone: 'primary'
    }
  }
  if (node?.canApplyMakeup) {
    return {
      key: 'makeup',
      label: '补打卡',
      tone: 'warning'
    }
  }
  return null
}

function resolveNodeRuleTime(node) {
  const ruleTime = String(node?.ruleTimeText || '').trim()
  if (ruleTime) {
    return ruleTime
  }

  const text = String(node?.timeRangeText || '').trim()
  if (!text || text === '时间以规则为准') {
    return '--:--'
  }
  const times = text.match(/\d{1,2}:\d{2}/g)
  if (!times?.length) {
    return text
  }
  return String(node?.nodeCode || '').endsWith('_OFF') ? times[times.length - 1] : times[0]
}

function formatNodeActualTime(value) {
  if (!value || value === '未打卡') {
    return ''
  }
  const text = String(value)
  const matched = text.match(/\d{1,2}:\d{2}/)
  return matched ? matched[0] : text
}

function pad2(value) {
  return String(value).padStart(2, '0')
}

function withAlpha(hexColor, alpha) {
  const hex = String(hexColor || '').replace('#', '')
  if (hex.length !== 6) {
    return `rgba(79, 125, 243, ${alpha})`
  }
  const r = Number.parseInt(hex.slice(0, 2), 16)
  const g = Number.parseInt(hex.slice(2, 4), 16)
  const b = Number.parseInt(hex.slice(4, 6), 16)
  return `rgba(${r}, ${g}, ${b}, ${alpha})`
}
</script>

<style scoped>
.personal-workspace {
  display: grid;
  gap: 18px;
}

.personal-workspace__loading {
  min-height: 220px;
  display: grid;
  place-items: center;
}

.personal-focus-card,
.personal-panel {
  border-radius: 28px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
  box-shadow: 0 18px 38px rgba(15, 23, 42, 0.06);
}

.personal-focus-card {
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(79, 125, 243, 0.14), transparent 34%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(255, 255, 255, 0.98));
}

.personal-focus-card__header,
.personal-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.personal-focus-card__eyebrow,
.personal-panel__eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.personal-focus-card__title {
  margin: 0;
  font-size: clamp(28px, 4vw, 36px);
  line-height: 1.08;
  color: #0f172a;
}

.personal-focus-card__date {
  margin: 8px 0 0;
  color: #64748b;
  font-size: 14px;
}

.personal-panel {
  padding: 22px;
}

.personal-panel__title {
  margin: 0;
  font-size: 22px;
  line-height: 1.2;
  color: #0f172a;
}

.personal-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 40px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 700;
}

.personal-status--normal,
.personal-node-card--normal .personal-node-card__status {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.personal-status--late,
.personal-node-card--late .personal-node-card__status,
.personal-node-card--makeup .personal-node-card__status {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.personal-status--early,
.personal-node-card--early .personal-node-card__status {
  background: rgba(249, 115, 22, 0.14);
  color: #c2410c;
}

.personal-status--missing,
.personal-status--abnormal,
.personal-node-card--missing .personal-node-card__status,
.personal-node-card--rejected .personal-node-card__status {
  background: rgba(248, 113, 113, 0.14);
  color: #b91c1c;
}

.personal-status--field,
.personal-node-card--evidence .personal-node-card__status {
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
}

.personal-status--approved,
.personal-node-card--approved .personal-node-card__status {
  background: rgba(34, 197, 94, 0.14);
  color: #15803d;
}

.personal-status--pending,
.personal-node-card--pending .personal-node-card__status {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}

.personal-main-action {
  display: grid;
  gap: 14px;
  padding: 20px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.98);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.personal-main-action--hero {
  margin-top: 18px;
}

.personal-main-action__meta {
  display: grid;
  gap: 8px;
}

.personal-main-action__eyebrow {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.personal-main-action__phase {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.personal-main-action__range {
  font-size: 12px;
  color: #64748b;
}

.personal-main-action__title {
  font-size: clamp(22px, 4vw, 28px);
  line-height: 1.15;
  color: #0f172a;
}

.personal-notice {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.personal-notice strong {
  font-size: 16px;
  color: #9a3412;
}

.personal-notice p,
.personal-node-card__remark,
.personal-location p,
.personal-location small,
.personal-main-action__hint,
.personal-record__main span,
.personal-record__meta {
  margin: 0;
  line-height: 1.6;
  color: #475569;
}

.personal-node-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.personal-today-list {
  display: grid;
  gap: 10px;
  margin-top: 20px;
  padding: 14px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(148, 163, 184, 0.14);
}

.personal-today-row {
  display: grid;
  grid-template-columns: minmax(92px, 1fr) 74px minmax(112px, 1.1fr) 74px;
  gap: 10px;
  align-items: center;
  min-height: 46px;
  padding: 10px 12px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.86);
}

.personal-today-row__name {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.personal-today-row__name strong {
  font-size: 15px;
  color: #0f172a;
  white-space: nowrap;
}

.personal-today-row__dot {
  width: 8px;
  height: 8px;
  border-radius: 999px;
  flex: 0 0 auto;
}

.personal-today-row__rule {
  font-size: 15px;
  font-weight: 700;
  color: #334155;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

.personal-today-row__state {
  display: inline-flex;
  justify-content: flex-end;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.personal-today-row__badge {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
  white-space: nowrap;
}

.personal-today-row__state small {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.personal-today-row--normal .personal-today-row__badge,
.personal-today-row--approved .personal-today-row__badge {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.personal-today-row--late .personal-today-row__badge,
.personal-today-row--early .personal-today-row__badge,
.personal-today-row--missing .personal-today-row__badge,
.personal-today-row--abnormal .personal-today-row__badge,
.personal-today-row--rejected .personal-today-row__badge {
  background: rgba(249, 115, 22, 0.14);
  color: #c2410c;
}

.personal-today-row--evidence .personal-today-row__badge,
.personal-today-row--makeup .personal-today-row__badge {
  background: rgba(99, 102, 241, 0.13);
  color: #4f46e5;
}

.personal-today-row--pending .personal-today-row__badge {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}

.personal-today-row__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.personal-today-row__action {
  min-height: 28px;
  padding: 0 11px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: #fff;
  color: #0f172a;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.personal-today-row__action--primary {
  border-color: rgba(79, 125, 243, 0.24);
  background: rgba(79, 125, 243, 0.1);
  color: #315fd3;
}

.personal-today-row__action--warning {
  border-color: rgba(231, 169, 59, 0.24);
  background: rgba(231, 169, 59, 0.12);
  color: #b7791f;
}

.personal-action-orb-wrap {
  display: grid;
  justify-items: center;
  gap: 10px;
  margin-top: 22px;
}

.personal-action-orb {
  --orb-shadow-color: rgba(37, 99, 235, 0.25);
  --orb-shadow-color-strong: rgba(37, 99, 235, 0.32);
  position: relative;
  isolation: isolate;
  display: grid;
  place-items: center;
  align-content: center;
  gap: 6px;
  width: 154px;
  height: 154px;
  border: none;
  border-radius: 50%;
  color: #fff;
  cursor: pointer;
  overflow: hidden;
  background:
    radial-gradient(circle at 34% 22%, rgba(255, 255, 255, 0.62), transparent 23%),
    linear-gradient(145deg, #4f7df3 0%, #25bfd0 100%);
  box-shadow:
    0 16px 34px var(--orb-shadow-color),
    0 5px 12px rgba(15, 23, 42, 0.1),
    inset 0 2px 7px rgba(255, 255, 255, 0.45),
    inset 0 -9px 18px rgba(15, 23, 42, 0.16);
  transform: translateZ(0);
  transition: transform 0.18s ease, box-shadow 0.18s ease, opacity 0.2s ease;
  animation: personal-orb-breathe 3.8s ease-in-out infinite;
}

.personal-action-orb::before {
  content: '';
  position: absolute;
  inset: 9px;
  z-index: -1;
  border-radius: inherit;
  border: 1px solid rgba(255, 255, 255, 0.52);
  box-shadow:
    inset 0 1px 7px rgba(255, 255, 255, 0.32),
    0 0 0 1px rgba(255, 255, 255, 0.12);
}

.personal-action-orb::after {
  content: '';
  position: absolute;
  top: 12px;
  left: 27px;
  width: 72px;
  height: 38px;
  border-radius: 999px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0));
  filter: blur(1px);
  transform: rotate(-18deg);
  pointer-events: none;
}

.personal-action-orb--evidence {
  --orb-shadow-color: rgba(99, 102, 241, 0.27);
  --orb-shadow-color-strong: rgba(99, 102, 241, 0.34);
  background:
    radial-gradient(circle at 34% 22%, rgba(255, 255, 255, 0.58), transparent 23%),
    linear-gradient(145deg, #5b7cfa 0%, #7c5ce7 100%);
  box-shadow:
    0 16px 34px rgba(99, 102, 241, 0.27),
    0 5px 12px rgba(15, 23, 42, 0.1),
    inset 0 2px 7px rgba(255, 255, 255, 0.43),
    inset 0 -9px 18px rgba(49, 46, 129, 0.18);
}

.personal-action-orb--makeup {
  --orb-shadow-color: rgba(217, 119, 6, 0.26);
  --orb-shadow-color-strong: rgba(217, 119, 6, 0.33);
  background:
    radial-gradient(circle at 34% 22%, rgba(255, 255, 255, 0.6), transparent 23%),
    linear-gradient(145deg, #f7b955 0%, #ee8c36 100%);
  box-shadow:
    0 16px 34px rgba(217, 119, 6, 0.26),
    0 5px 12px rgba(15, 23, 42, 0.1),
    inset 0 2px 7px rgba(255, 255, 255, 0.43),
    inset 0 -9px 18px rgba(124, 45, 18, 0.16);
}

.personal-action-orb--disabled {
  --orb-shadow-color: rgba(100, 116, 139, 0.18);
  --orb-shadow-color-strong: rgba(100, 116, 139, 0.18);
  background:
    radial-gradient(circle at 34% 22%, rgba(255, 255, 255, 0.55), transparent 23%),
    linear-gradient(145deg, #cbd5e1 0%, #94a3b8 100%);
  box-shadow:
    0 10px 22px rgba(100, 116, 139, 0.18),
    inset 0 2px 7px rgba(255, 255, 255, 0.42),
    inset 0 -8px 16px rgba(51, 65, 85, 0.12);
  animation: none;
}

.personal-action-orb:hover:not(:disabled) {
  transform: translateY(-2px);
}

.personal-action-orb:active:not(:disabled) {
  transform: scale(0.97);
  box-shadow:
    0 8px 20px var(--orb-shadow-color, rgba(37, 99, 235, 0.2)),
    inset 0 2px 6px rgba(255, 255, 255, 0.36),
    inset 0 -5px 12px rgba(15, 23, 42, 0.18);
}

.personal-action-orb:disabled {
  cursor: not-allowed;
  opacity: 0.82;
}

.personal-action-orb__time {
  position: relative;
  z-index: 1;
  font-size: 34px;
  line-height: 1;
  font-weight: 800;
  letter-spacing: -0.04em;
  font-variant-numeric: tabular-nums;
  text-shadow: 0 2px 8px rgba(15, 23, 42, 0.18);
}

.personal-action-orb strong {
  position: relative;
  z-index: 1;
  font-size: 15px;
  line-height: 1.2;
  text-shadow: 0 1px 6px rgba(15, 23, 42, 0.16);
}

.personal-action-orb-wrap__hint {
  max-width: 260px;
  margin: 0;
  text-align: center;
  color: #64748b;
  font-size: 13px;
  line-height: 1.6;
}

@keyframes personal-orb-breathe {
  0%,
  100% {
    box-shadow:
      0 16px 34px var(--orb-shadow-color),
      0 5px 12px rgba(15, 23, 42, 0.1),
      inset 0 2px 7px rgba(255, 255, 255, 0.45),
      inset 0 -9px 18px rgba(15, 23, 42, 0.16);
  }
  50% {
    box-shadow:
      0 18px 38px var(--orb-shadow-color-strong),
      0 6px 14px rgba(15, 23, 42, 0.11),
      inset 0 2px 7px rgba(255, 255, 255, 0.48),
      inset 0 -9px 18px rgba(15, 23, 42, 0.16);
  }
}

.personal-node-card {
  display: grid;
  gap: 10px;
  padding: 16px;
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.personal-node-card__top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.personal-node-card__step,
.personal-location__label,
.personal-week-item span {
  font-size: 12px;
  color: #64748b;
}

.personal-node-card__step {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-weight: 700;
}

.personal-node-card__status {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.personal-node-card__title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.personal-node-card__range {
  font-size: 12px;
  color: #64748b;
}

.personal-node-card__time,
.personal-location strong,
.personal-week-item strong {
  font-size: 22px;
  line-height: 1.1;
  color: #0f172a;
}

.personal-node-card__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.personal-node-card__action {
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  border: 1px solid rgba(148, 163, 184, 0.2);
  background: #fff;
  color: #0f172a;
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.personal-node-card__action:hover {
  transform: translateY(-1px);
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.08);
}

.personal-node-card__action--primary {
  border-color: rgba(79, 125, 243, 0.24);
  background: rgba(79, 125, 243, 0.1);
  color: #315fd3;
}

.personal-node-card__action--warning {
  border-color: rgba(231, 169, 59, 0.24);
  background: rgba(231, 169, 59, 0.12);
  color: #b7791f;
}

.personal-focus-card__footer {
  display: grid;
  grid-template-columns: 1fr;
  gap: 18px;
  margin-top: 18px;
}

.personal-location,
.personal-week-item,
.personal-record {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 22px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.personal-checkin-button {
  width: 100%;
  min-height: 64px;
  border: none;
  border-radius: 999px;
  color: #f8fafc;
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 0.03em;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.personal-checkin-button:hover:not(:disabled) {
  transform: translateY(-1px);
}

.personal-checkin-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.personal-workspace__supplement {
  display: grid;
  grid-template-columns: minmax(280px, 0.72fr) minmax(0, 1.28fr);
  gap: 18px;
}

.personal-week-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.personal-record-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.personal-record__main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: flex-start;
}

.personal-record__main strong {
  color: #0f172a;
}

@media (max-width: 960px) {
  .personal-node-grid,
  .personal-workspace__supplement,
  .personal-week-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .personal-workspace {
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    gap: 12px;
    padding-bottom: 110px;
    overflow-x: hidden;
  }

  .personal-focus-card,
  .personal-panel {
    width: 100%;
    max-width: 100%;
    box-sizing: border-box;
    padding: 14px;
    border-radius: 20px;
    box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  }

  .personal-focus-card {
    background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
  }

  .personal-focus-card__header {
    gap: 10px;
    align-items: center;
  }

  .personal-focus-card__eyebrow {
    margin-bottom: 4px;
    font-size: 11px;
    letter-spacing: 0.08em;
  }

  .personal-focus-card__title {
    font-size: 24px;
  }

  .personal-focus-card__date {
    margin-top: 5px;
    font-size: 13px;
  }

  .personal-status {
    min-height: 30px;
    padding: 0 10px;
    font-size: 12px;
    white-space: nowrap;
  }

  .personal-notice {
    grid-template-columns: auto 1fr;
    gap: 6px;
    align-items: center;
    margin-top: 10px;
    padding: 9px 11px;
    border-radius: 14px;
  }

  .personal-notice strong {
    font-size: 13px;
  }

  .personal-notice p {
    font-size: 12px;
    line-height: 1.45;
  }

  .personal-action-orb-wrap {
    gap: 6px;
    margin-top: 10px;
  }

  .personal-action-orb {
    width: 124px;
    height: 124px;
    gap: 5px;
  }

  .personal-action-orb__time {
    font-size: 29px;
  }

  .personal-action-orb strong {
    font-size: 13px;
  }

  .personal-action-orb-wrap__hint {
    max-width: 230px;
    font-size: 11px;
    line-height: 1.35;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .personal-today-list {
    gap: 5px;
    margin-top: 10px;
    padding: 7px;
    border-radius: 16px;
  }

  .personal-today-row {
    grid-template-columns: minmax(68px, 1fr) 50px minmax(72px, 0.95fr) 58px;
    gap: 5px;
    min-height: 40px;
    padding: 6px 7px;
    border-radius: 13px;
  }

  .personal-today-row__name {
    gap: 6px;
  }

  .personal-today-row__name strong,
  .personal-today-row__rule {
    font-size: 14px;
  }

  .personal-today-row__dot {
    width: 6px;
    height: 6px;
  }

  .personal-today-row__state {
    gap: 4px;
  }

  .personal-today-row__badge {
    min-height: 22px;
    padding: 0 7px;
    font-size: 11px;
  }

  .personal-today-row__state small {
    font-size: 12px;
  }

  .personal-today-row__actions {
    display: flex;
    justify-content: flex-end;
  }

  .personal-today-row__action {
    min-height: 24px;
    padding: 0 8px;
    font-size: 11px;
  }

  .personal-focus-card__footer {
    margin-top: 12px;
  }

  .personal-location,
  .personal-week-item,
  .personal-record {
    gap: 5px;
    padding: 12px;
    border-radius: 16px;
  }

  .personal-location strong,
  .personal-week-item strong {
    font-size: 18px;
  }

  .personal-node-grid {
    gap: 10px;
  }

  .personal-main-action {
    padding: 16px;
  }
}
</style>

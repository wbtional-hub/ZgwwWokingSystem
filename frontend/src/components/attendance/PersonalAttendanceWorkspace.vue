<template>
  <section class="personal-workspace">
    <van-loading v-if="loading" class="personal-workspace__loading" size="24px" vertical>
      正在同步个人考勤状态...
    </van-loading>

    <template v-else>
      <article class="personal-focus-card">
        <header class="personal-focus-card__header">
          <div>
            <p class="personal-focus-card__eyebrow">我的考勤</p>
            <h2 class="personal-focus-card__title">{{ name || '-' }}</h2>
          </div>
          <span class="personal-status" :class="`personal-status--${todayStatusTone}`">
            {{ todayStatusLabel || '未打卡' }}
          </span>
        </header>

        <article class="personal-main-action personal-main-action--hero">
          <div class="personal-main-action__meta">
            <div class="personal-main-action__eyebrow">
              <span class="personal-main-action__phase" :style="mainActionPhaseStyle">
                {{ mainActionNode?.stepLabel || '当前时段' }}
              </span>
              <span class="personal-main-action__range">{{ mainActionNode?.timeRangeText || '时间以规则为准' }}</span>
            </div>
            <strong class="personal-main-action__title">{{ checkInButtonText }}</strong>
            <p class="personal-main-action__hint">
              {{ checkInHint || '点击后自动获取定位并刷新当前状态。' }}
            </p>
          </div>
          <button
            type="button"
            class="personal-checkin-button"
            :style="mainActionButtonStyle"
            :disabled="checkingIn || !canCheckIn"
            @click="$emit('check-in')"
          >
            {{ checkingIn ? '正在打卡...' : checkInButtonText }}
          </button>
        </article>

        <article v-if="notice" class="personal-notice">
          <strong>{{ notice.title }}</strong>
          <p>{{ notice.text }}</p>
        </article>

        <div class="personal-node-grid">
          <article
            v-for="node in nodeCards"
            :key="node.nodeCode"
            class="personal-node-card"
            :class="`personal-node-card--${node.statusTone || 'pending'}`"
            :style="nodeCardStyle(node)"
          >
            <div class="personal-node-card__top">
              <span class="personal-node-card__step" :style="nodePhaseStyle(node)">{{ node.stepLabel || '-' }}</span>
              <span class="personal-node-card__status">{{ node.statusLabel || '待处理' }}</span>
            </div>
            <div class="personal-node-card__title">{{ node.nodeTitleShort || '-' }}</div>
            <div class="personal-node-card__range">{{ node.timeRangeText || '时间以规则为准' }}</div>
            <div class="personal-node-card__time">{{ node.actualPunchTime || '未打卡' }}</div>
            <p class="personal-node-card__remark">{{ node.simpleRemark || '等待当前节点处理' }}</p>
            <div v-if="node.actions?.length" class="personal-node-card__actions">
              <button
                v-for="action in node.actions"
                :key="`${node.nodeCode}-${action.key}`"
                type="button"
                class="personal-node-card__action"
                :class="`personal-node-card__action--${action.tone || 'secondary'}`"
                @click="$emit('node-action', { node, action })"
              >
                {{ action.label }}
              </button>
            </div>
          </article>
        </div>

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
import { computed } from 'vue'

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

defineEmits(['check-in', 'node-action'])

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
  .personal-focus-card,
  .personal-panel {
    padding: 18px;
    border-radius: 22px;
  }

  .personal-focus-card__title {
    font-size: 28px;
  }

  .personal-node-grid {
    gap: 10px;
  }

  .personal-main-action {
    padding: 16px;
  }
}
</style>

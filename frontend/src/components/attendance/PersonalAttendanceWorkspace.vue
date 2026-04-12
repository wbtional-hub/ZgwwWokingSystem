<template>
  <section class="personal-workspace">
    <van-loading v-if="loading" class="personal-workspace__loading" size="24px" vertical>
      正在同步个人考勤状态...
    </van-loading>

    <template v-else>
      <div class="personal-workspace__hero">
        <article class="personal-panel personal-panel--status">
          <div class="personal-panel__head">
            <div>
              <p class="personal-panel__eyebrow">个人考勤工作台</p>
              <h2 class="personal-panel__title">{{ name || '-' }}</h2>
            </div>
            <span class="personal-status" :class="`personal-status--${todayStatusTone}`">
              {{ todayStatusLabel || '未打卡' }}
            </span>
          </div>

          <article v-if="notice" class="personal-notice">
            <strong>{{ notice.title }}</strong>
            <p>{{ notice.text }}</p>
          </article>

          <div class="personal-status-grid">
            <div class="personal-metric">
              <span>上班时间</span>
              <strong>{{ checkInTime || '-' }}</strong>
            </div>
            <div class="personal-metric">
              <span>下班时间</span>
              <strong>{{ checkOutTime || '-' }}</strong>
            </div>
          </div>

          <div class="personal-location">
            <span class="personal-location__label">定位信息</span>
            <strong>{{ locationText || '-' }}</strong>
            <p>{{ locationDetail || '当前暂无可展示的定位信息。' }}</p>
            <small v-if="locationStatus">{{ locationStatus }}</small>
          </div>
        </article>

        <div class="personal-workspace__side">
          <article class="personal-panel personal-panel--action">
            <p class="personal-panel__eyebrow">打卡动作</p>
            <button
              type="button"
              class="personal-checkin-button"
              :disabled="checkingIn || !canCheckIn"
              @click="$emit('check-in')"
            >
              {{ checkingIn ? '正在打卡...' : checkInButtonText }}
            </button>
            <p class="personal-action__hint">
              {{ checkInHint || '点击后会自动获取定位、提交打卡并刷新今日状态。' }}
            </p>
          </article>

          <article class="personal-panel personal-panel--week">
            <div class="personal-panel__head personal-panel__head--compact">
              <div>
                <p class="personal-panel__eyebrow">本周统计</p>
                <h3 class="personal-panel__subtitle">本周出勤概览</h3>
              </div>
            </div>

            <div class="personal-week-grid">
              <div v-for="item in weekCards" :key="item.key" class="personal-week-item">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </article>
        </div>
      </div>

      <article class="personal-panel personal-panel--records">
        <div class="personal-panel__head personal-panel__head--compact">
          <div>
            <p class="personal-panel__eyebrow">最近记录</p>
            <h3 class="personal-panel__subtitle">最近 5 条打卡记录</h3>
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
  checkInTime: {
    type: String,
    default: ''
  },
  checkOutTime: {
    type: String,
    default: ''
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
  }
})

defineEmits(['check-in'])

const weekCards = computed(() => [
  {
    key: 'attendance',
    label: '本周出勤',
    value: props.weeklyStats.attendanceDays ?? 0
  },
  {
    key: 'late',
    label: '迟到次数',
    value: props.weeklyStats.lateCount ?? 0
  },
  {
    key: 'missing',
    label: '缺卡次数',
    value: props.weeklyStats.missingCount ?? 0
  }
])
</script>

<style scoped>
.personal-workspace {
  display: grid;
  gap: 20px;
}

.personal-workspace__loading {
  min-height: 240px;
  display: grid;
  place-items: center;
}

.personal-workspace__hero {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.personal-workspace__side {
  display: grid;
  gap: 18px;
}

.personal-panel {
  border-radius: 28px;
  border: 1px solid rgba(148, 163, 184, 0.18);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(248, 250, 252, 0.98));
  box-shadow: 0 18px 36px rgba(15, 23, 42, 0.06);
}

.personal-panel--status {
  padding: 24px;
  background:
    radial-gradient(circle at top right, rgba(14, 165, 233, 0.14), transparent 34%),
    linear-gradient(180deg, rgba(248, 250, 252, 0.98), rgba(255, 255, 255, 0.98));
}

.personal-panel--action,
.personal-panel--week,
.personal-panel--records {
  padding: 22px;
}

.personal-panel__head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.personal-panel__head--compact {
  margin-bottom: 16px;
}

.personal-panel__eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.personal-panel__title {
  margin: 0;
  font-size: clamp(28px, 4vw, 38px);
  line-height: 1.1;
  color: #0f172a;
}

.personal-panel__subtitle {
  margin: 0;
  font-size: 22px;
  line-height: 1.2;
  color: #0f172a;
}

.personal-status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 16px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.personal-status--normal {
  background: rgba(16, 185, 129, 0.14);
  color: #047857;
}

.personal-status--late {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.personal-status--missing,
.personal-status--abnormal {
  background: rgba(248, 113, 113, 0.14);
  color: #b91c1c;
}

.personal-status--field {
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
}

.personal-status--pending {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}

.personal-notice {
  display: grid;
  gap: 8px;
  margin-top: 18px;
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(245, 158, 11, 0.12);
  border: 1px solid rgba(245, 158, 11, 0.2);
}

.personal-notice strong {
  font-size: 16px;
  color: #9a3412;
}

.personal-notice p {
  margin: 0;
  line-height: 1.7;
  color: #9a3412;
}

.personal-status-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 22px;
}

.personal-metric,
.personal-week-item {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 20px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.personal-metric span,
.personal-week-item span,
.personal-location__label,
.personal-record__meta {
  font-size: 12px;
  color: #64748b;
}

.personal-metric strong,
.personal-week-item strong {
  font-size: 24px;
  line-height: 1.1;
  color: #0f172a;
}

.personal-location {
  display: grid;
  gap: 8px;
  margin-top: 18px;
  padding: 18px;
  border-radius: 22px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.18);
}

.personal-location strong {
  font-size: 18px;
  color: #0f172a;
}

.personal-location p,
.personal-location small,
.personal-action__hint,
.personal-record__main span {
  margin: 0;
  line-height: 1.6;
  color: #475569;
}

.personal-checkin-button {
  width: 100%;
  min-height: 68px;
  border: none;
  border-radius: 24px;
  background: linear-gradient(135deg, #0f172a, #1d4ed8);
  color: #f8fafc;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: 0.04em;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, opacity 0.2s ease;
}

.personal-checkin-button:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 18px 32px rgba(29, 78, 216, 0.26);
}

.personal-checkin-button:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.personal-action__hint {
  margin-top: 14px;
  font-size: 13px;
}

.personal-week-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.personal-record-list {
  display: grid;
  gap: 12px;
}

.personal-record {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.personal-record__main {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.personal-record__main strong {
  font-size: 16px;
  color: #0f172a;
}

@media (max-width: 960px) {
  .personal-workspace__hero {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .personal-panel--status,
  .personal-panel--action,
  .personal-panel--week,
  .personal-panel--records {
    padding: 18px;
    border-radius: 22px;
  }

  .personal-panel__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .personal-status-grid,
  .personal-week-grid {
    grid-template-columns: 1fr;
  }

  .personal-checkin-button {
    min-height: 60px;
    font-size: 18px;
  }

  .personal-record__main {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>

<template>
  <section class="leadership-summary">
    <div class="leadership-summary__hero">
      <div>
        <p class="leadership-summary__eyebrow">团队考勤总览</p>
        <h2 class="leadership-summary__title">下级今日状态与本周出勤，在一页内完成判断</h2>
        <p class="leadership-summary__description">
          {{ scopeDescription || '基于当前数据权限范围，自动汇总今日打卡、本周出勤与需要关注的异常信息。' }}
        </p>
      </div>
      <div class="leadership-summary__meta">
        <span class="leadership-summary__meta-label">统计范围</span>
        <span class="leadership-summary__meta-value">{{ rangeText }}</span>
      </div>
    </div>

    <van-loading v-if="loading" class="leadership-summary__loading" size="24px" vertical>
      正在汇总团队考勤...
    </van-loading>

    <template v-else>
      <article v-if="nonWorkdayNotice" class="leadership-summary__notice">
        <div class="leadership-summary__notice-head">
          <div>
            <span class="leadership-summary__notice-tag">非工作日</span>
            <h3>{{ nonWorkdayNotice.title }}</h3>
          </div>
          <div class="leadership-summary__notice-count">
            <span>{{ nonWorkdayNotice.countLabel || '加班打卡人数' }}</span>
            <strong>{{ nonWorkdayNotice.count ?? 0 }}</strong>
          </div>
        </div>
        <p class="leadership-summary__notice-text">{{ nonWorkdayNotice.description }}</p>
        <p class="leadership-summary__notice-text">{{ nonWorkdayNotice.summary }}</p>
      </article>

      <div v-if="hasMembers" class="leadership-summary__stats">
        <article
          v-for="item in statCards"
          :key="item.key"
          class="leadership-stat"
          :class="`leadership-stat--${item.tone}`"
        >
          <span class="leadership-stat__label">{{ item.label }}</span>
          <strong class="leadership-stat__value">{{ item.value }}</strong>
          <span class="leadership-stat__hint">{{ item.hint }}</span>
        </article>
      </div>

      <div v-if="hasMembers" class="leadership-summary__alerts">
        <article
          v-for="item in alertCards"
          :key="item.key"
          class="leadership-alert"
          :class="`leadership-alert--${item.tone}`"
        >
          <div class="leadership-alert__header">
            <span class="leadership-alert__title">{{ item.title }}</span>
            <span class="leadership-alert__count">{{ item.count }}</span>
          </div>
          <p class="leadership-alert__body">{{ item.summary }}</p>
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
  scopeDescription: {
    type: String,
    default: ''
  },
  rangeText: {
    type: String,
    default: ''
  },
  overview: {
    type: Object,
    default: () => ({})
  },
  alerts: {
    type: Object,
    default: () => ({})
  },
  nonWorkdayNotice: {
    type: Object,
    default: null
  },
  hasMembers: {
    type: Boolean,
    default: true
  }
})

const statCards = computed(() => {
  const nonWorkday = Boolean(props.overview.isNonWorkday)
  return [
    {
      key: 'expected',
      label: '今日应到人数',
      value: props.overview.expectedCount ?? 0,
      hint: nonWorkday ? '非工作日不计入今日应到口径' : '当前筛选后的团队成员',
      tone: 'neutral'
    },
    {
      key: 'checked',
      label: '已打卡人数',
      value: props.overview.checkedCount ?? 0,
      hint: nonWorkday ? '非工作日打卡请查看上方加班/值班提示' : '今日已产生打卡记录',
      tone: 'success'
    },
    {
      key: 'missing',
      label: '未打卡人数',
      value: props.overview.missingCount ?? 0,
      hint: nonWorkday ? '非工作日不生成未打卡异常' : '仍需重点关注',
      tone: 'danger'
    },
    {
      key: 'late',
      label: '迟到人数',
      value: props.overview.lateCount ?? 0,
      hint: nonWorkday ? '非工作日不纳入今日迟到提醒' : '含文本识别信号',
      tone: 'warning'
    },
    {
      key: 'field',
      label: '外勤人数',
      value: props.overview.fieldCount ?? 0,
      hint: nonWorkday ? '非工作日打卡不并入工作日异常口径' : '含范围外或外勤信号',
      tone: 'accent'
    },
    {
      key: 'weekly',
      label: '本周出勤率',
      value: props.overview.weeklyAttendanceRateText ?? '0.0%',
      hint: '按本周已到工作日折算',
      tone: 'neutral'
    }
  ]
})

const alertCards = computed(() => {
  const nonWorkday = Boolean(props.alerts.isNonWorkday)
  return [
    {
      key: 'missing',
      title: '未打卡提醒',
      count: props.alerts.missingCount ?? 0,
      summary: props.alerts.missingSummary || (nonWorkday ? '今日为非工作日，不生成未打卡提醒' : '当前没有未打卡成员'),
      tone: 'danger'
    },
    {
      key: 'late',
      title: '迟到提醒',
      count: props.alerts.lateCount ?? 0,
      summary: props.alerts.lateSummary || (nonWorkday ? '今日为非工作日，不生成迟到提醒' : '当前没有迟到成员'),
      tone: 'warning'
    },
    {
      key: 'abnormal',
      title: '定位 / 外勤异常',
      count: props.alerts.abnormalCount ?? 0,
      summary: props.alerts.abnormalSummary || (nonWorkday ? '今日为非工作日，异常请以加班/值班打卡记录为准' : '当前没有定位或外勤异常成员'),
      tone: 'accent'
    }
  ]
})
</script>

<style scoped>
.leadership-summary {
  display: grid;
  gap: 18px;
  padding: 24px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 28px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.16), transparent 32%),
    linear-gradient(180deg, rgba(15, 23, 42, 0.98), rgba(30, 41, 59, 0.96));
  color: #e2e8f0;
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.18);
}

.leadership-summary__hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.leadership-summary__eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(148, 163, 184, 0.92);
}

.leadership-summary__title {
  margin: 0;
  font-size: clamp(24px, 4vw, 34px);
  line-height: 1.08;
  color: #f8fafc;
}

.leadership-summary__description {
  margin: 10px 0 0;
  max-width: 620px;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(226, 232, 240, 0.78);
}

.leadership-summary__meta {
  display: grid;
  gap: 4px;
  min-width: 180px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(15, 23, 42, 0.35);
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.leadership-summary__meta-label {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.9);
}

.leadership-summary__meta-value {
  font-size: 14px;
  line-height: 1.5;
  color: #f8fafc;
}

.leadership-summary__loading {
  min-height: 160px;
  display: grid;
  place-items: center;
}

.leadership-summary__notice {
  display: grid;
  gap: 10px;
  padding: 18px 20px;
  border-radius: 22px;
  border: 1px solid rgba(251, 191, 36, 0.24);
  background: rgba(245, 158, 11, 0.12);
}

.leadership-summary__notice-head {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.leadership-summary__notice-head h3 {
  margin: 8px 0 0;
  font-size: 20px;
  color: #fef3c7;
}

.leadership-summary__notice-count {
  display: grid;
  justify-items: end;
  gap: 6px;
}

.leadership-summary__notice-count span {
  font-size: 12px;
  color: rgba(254, 243, 199, 0.82);
}

.leadership-summary__notice-count strong {
  font-size: 34px;
  line-height: 1;
  color: #fde68a;
}

.leadership-summary__notice-tag {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.35);
  color: #fde68a;
  font-size: 12px;
  letter-spacing: 0.08em;
}

.leadership-summary__notice-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.7;
  color: rgba(254, 243, 199, 0.92);
}

.leadership-summary__stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.leadership-stat {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.leadership-stat__label {
  font-size: 13px;
  color: rgba(226, 232, 240, 0.7);
}

.leadership-stat__value {
  font-size: clamp(28px, 4vw, 38px);
  line-height: 1;
  color: #f8fafc;
}

.leadership-stat__hint {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.9);
}

.leadership-stat--success {
  background: rgba(16, 185, 129, 0.12);
}

.leadership-stat--danger {
  background: rgba(248, 113, 113, 0.12);
}

.leadership-stat--warning {
  background: rgba(245, 158, 11, 0.12);
}

.leadership-stat--accent {
  background: rgba(14, 165, 233, 0.12);
}

.leadership-summary__alerts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.leadership-alert {
  display: grid;
  gap: 10px;
  padding: 18px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.leadership-alert__header {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.leadership-alert__title {
  font-size: 15px;
  font-weight: 600;
  color: #f8fafc;
}

.leadership-alert__count {
  font-size: 22px;
  font-weight: 700;
}

.leadership-alert__body {
  margin: 0;
  font-size: 13px;
  line-height: 1.7;
  color: rgba(226, 232, 240, 0.78);
}

.leadership-alert--danger .leadership-alert__count {
  color: #fda4af;
}

.leadership-alert--warning .leadership-alert__count {
  color: #fbbf24;
}

.leadership-alert--accent .leadership-alert__count {
  color: #7dd3fc;
}

@media (max-width: 900px) {
  .leadership-summary {
    padding: 20px;
    border-radius: 24px;
  }

  .leadership-summary__hero {
    flex-direction: column;
  }

  .leadership-summary__meta {
    width: 100%;
  }

  .leadership-summary__alerts {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .leadership-summary__stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .leadership-summary__title {
    font-size: 26px;
  }

  .leadership-summary__notice-head {
    flex-direction: column;
  }

  .leadership-stat {
    padding: 16px;
  }

  .leadership-stat__value {
    font-size: 30px;
  }
}
</style>

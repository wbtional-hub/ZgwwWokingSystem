<template>
  <section class="leadership-detail">
    <template v-if="member">
      <header class="leadership-detail__header">
        <div>
          <p class="leadership-detail__eyebrow">成员详情</p>
          <h3 class="leadership-detail__title">{{ member.realName }}</h3>
          <p class="leadership-detail__subtitle">
            {{ member.unitName || '未归属部门' }} / {{ member.jobTitle || '岗位未填写' }}
          </p>
        </div>
        <span class="leadership-detail__status" :class="`leadership-detail__status--${member.statusTone}`">
          {{ member.statusLabel }}
        </span>
      </header>

      <div class="leadership-detail__sections">
        <article class="leadership-detail__section">
          <div class="leadership-detail__section-head">
            <h4>今日打卡明细</h4>
            <span>{{ todayLabel }}</span>
          </div>

          <div class="leadership-detail__metrics">
            <div class="leadership-detail__metric">
              <span>上班时间</span>
              <strong>{{ detail.todayRecord?.checkInTimeText || '-' }}</strong>
            </div>
            <div class="leadership-detail__metric">
              <span>下班时间</span>
              <strong>{{ detail.todayRecord?.checkOutTimeText || '-' }}</strong>
            </div>
            <div class="leadership-detail__metric">
              <span>定位结果</span>
              <strong>{{ detail.todayRecord?.resultLabel || '今日暂无记录' }}</strong>
            </div>
            <div class="leadership-detail__metric">
              <span>是否在范围内</span>
              <strong>{{ detail.todayRecord?.rangeLabel || '-' }}</strong>
            </div>
          </div>

          <div class="leadership-detail__meta-list">
            <p>上班地点：{{ detail.todayRecord?.checkInAddress || '-' }}</p>
            <p>下班地点：{{ detail.todayRecord?.checkOutAddress || '-' }}</p>
            <p>迟到 / 早退时长：{{ detail.todayRecord?.timeDeviationText || '当前数据未提供' }}</p>
          </div>
        </article>

        <article class="leadership-detail__section">
          <div class="leadership-detail__section-head">
            <h4>本周统计</h4>
            <span>{{ weekRangeText }}</span>
          </div>

          <div class="leadership-detail__metrics">
            <div class="leadership-detail__metric">
              <span>出勤天数</span>
              <strong>{{ detail.weekSummary.attendanceDays }}</strong>
            </div>
            <div class="leadership-detail__metric">
              <span>迟到次数</span>
              <strong>{{ detail.weekSummary.lateCount }}</strong>
            </div>
            <div class="leadership-detail__metric">
              <span>缺卡次数</span>
              <strong>{{ detail.weekSummary.missingCount }}</strong>
            </div>
            <div class="leadership-detail__metric">
              <span>外勤次数</span>
              <strong>{{ detail.weekSummary.fieldCount }}</strong>
            </div>
          </div>
        </article>

        <details class="leadership-detail__section leadership-detail__section--fold">
          <summary class="leadership-detail__section-summary">
            <div>
              <h4>本周趋势</h4>
              <span>周一到周五状态胶囊</span>
            </div>
            <span class="leadership-detail__fold-action">展开</span>
          </summary>

          <div class="leadership-detail__trend">
            <div
              v-for="item in detail.trendItems"
              :key="item.date"
              class="leadership-trend-pill"
              :class="`leadership-trend-pill--${item.tone}`"
            >
              <span class="leadership-trend-pill__day">{{ item.weekday }}</span>
              <strong>{{ item.label }}</strong>
              <span class="leadership-trend-pill__date">{{ item.shortDate }}</span>
            </div>
          </div>
        </details>

        <details class="leadership-detail__section leadership-detail__section--fold">
          <summary class="leadership-detail__section-summary">
            <div>
              <h4>最近异常记录</h4>
              <span v-if="loading">正在刷新</span>
              <span v-else>最近 3 条</span>
            </div>
            <span class="leadership-detail__fold-action">展开</span>
          </summary>

          <van-loading v-if="loading" class="leadership-detail__subloading" size="20px" vertical>
            正在加载异常记录...
          </van-loading>

          <div v-else-if="detail.recentAbnormalRecords.length" class="leadership-detail__abnormal-list">
            <article
              v-for="item in detail.recentAbnormalRecords"
              :key="item.id"
              class="leadership-detail__abnormal-item"
            >
              <div class="leadership-detail__abnormal-head">
                <strong>{{ item.attendanceDate || '-' }}</strong>
                <span>{{ item.resultLabel }}</span>
              </div>
              <p>{{ item.checkInAddress || item.checkOutAddress || '未记录打卡地点' }}</p>
              <small>{{ item.reasonText }}</small>
            </article>
          </div>

          <van-empty v-else description="最近没有异常记录" />
        </details>
      </div>
    </template>

    <van-empty v-else description="请选择一位成员查看详细考勤情况" />
  </section>
</template>

<script setup>
defineProps({
  member: {
    type: Object,
    default: null
  },
  detail: {
    type: Object,
    default: () => ({
      todayRecord: null,
      weekSummary: {
        attendanceDays: 0,
        lateCount: 0,
        missingCount: 0,
        fieldCount: 0
      },
      trendItems: [],
      recentAbnormalRecords: []
    })
  },
  todayLabel: {
    type: String,
    default: ''
  },
  weekRangeText: {
    type: String,
    default: ''
  },
  loading: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.leadership-detail {
  display: grid;
  gap: 18px;
  padding: 22px;
  border-radius: 26px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(248, 250, 252, 0.99));
  box-shadow: 0 20px 44px rgba(15, 23, 42, 0.08);
}

.leadership-detail__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.leadership-detail__eyebrow {
  margin: 0 0 6px;
  font-size: 12px;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #64748b;
}

.leadership-detail__title {
  margin: 0;
  font-size: clamp(26px, 3vw, 32px);
  line-height: 1.1;
  color: #0f172a;
}

.leadership-detail__subtitle {
  margin: 8px 0 0;
  font-size: 14px;
  color: #64748b;
}

.leadership-detail__status {
  display: inline-flex;
  align-items: center;
  padding: 8px 14px;
  border-radius: 999px;
  font-size: 13px;
  font-weight: 600;
}

.leadership-detail__status--normal {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.leadership-detail__status--late {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.leadership-detail__status--missing,
.leadership-detail__status--abnormal {
  background: rgba(248, 113, 113, 0.14);
  color: #b91c1c;
}

.leadership-detail__status--field {
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
}

.leadership-detail__status--pending {
  background: rgba(148, 163, 184, 0.18);
  color: #475569;
}

.leadership-detail__sections {
  display: grid;
  gap: 14px;
}

.leadership-detail__section {
  display: grid;
  gap: 14px;
  padding: 18px;
  border-radius: 20px;
  background: #fff;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.leadership-detail__section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.leadership-detail__section-head h4,
.leadership-detail__section-summary h4 {
  margin: 0;
  font-size: 16px;
  color: #0f172a;
}

.leadership-detail__section-head span,
.leadership-detail__section-summary span {
  font-size: 12px;
  color: #64748b;
}

.leadership-detail__metrics {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.leadership-detail__metric {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  background: #f8fafc;
}

.leadership-detail__metric span {
  font-size: 12px;
  color: #64748b;
}

.leadership-detail__metric strong {
  font-size: 20px;
  line-height: 1.2;
  color: #0f172a;
}

.leadership-detail__meta-list {
  display: grid;
  gap: 8px;
}

.leadership-detail__meta-list p {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
}

.leadership-detail__section--fold {
  padding: 0;
  overflow: hidden;
}

.leadership-detail__section-summary {
  list-style: none;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  padding: 18px;
  cursor: pointer;
}

.leadership-detail__section-summary::-webkit-details-marker {
  display: none;
}

.leadership-detail__fold-action {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid rgba(148, 163, 184, 0.16);
}

.leadership-detail__section--fold[open] .leadership-detail__fold-action::before {
  content: '收起';
}

.leadership-detail__section--fold:not([open]) .leadership-detail__fold-action::before {
  content: '展开';
}

.leadership-detail__section--fold .leadership-detail__fold-action {
  color: transparent;
}

.leadership-detail__section--fold .leadership-detail__trend,
.leadership-detail__section--fold .leadership-detail__subloading,
.leadership-detail__section--fold .leadership-detail__abnormal-list,
.leadership-detail__section--fold :deep(.van-empty) {
  margin: 0 18px 18px;
}

.leadership-detail__trend {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
}

.leadership-trend-pill {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: 18px;
  border: 1px solid transparent;
}

.leadership-trend-pill__day,
.leadership-trend-pill__date {
  font-size: 12px;
  color: inherit;
  opacity: 0.76;
}

.leadership-trend-pill strong {
  font-size: 15px;
}

.leadership-trend-pill--normal {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.leadership-trend-pill--late {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.leadership-trend-pill--missing {
  background: rgba(248, 113, 113, 0.14);
  color: #b91c1c;
}

.leadership-trend-pill--abnormal {
  background: rgba(248, 113, 113, 0.1);
  color: #b91c1c;
}

.leadership-trend-pill--field {
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
}

.leadership-trend-pill--pending {
  background: rgba(148, 163, 184, 0.16);
  color: #475569;
}

.leadership-detail__subloading {
  min-height: 120px;
  display: grid;
  place-items: center;
}

.leadership-detail__abnormal-list {
  display: grid;
  gap: 10px;
}

.leadership-detail__abnormal-item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  background: #fff7ed;
  border: 1px solid rgba(251, 146, 60, 0.2);
}

.leadership-detail__abnormal-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: baseline;
}

.leadership-detail__abnormal-head strong {
  color: #9a3412;
}

.leadership-detail__abnormal-head span {
  font-size: 12px;
  color: #c2410c;
}

.leadership-detail__abnormal-item p,
.leadership-detail__abnormal-item small {
  margin: 0;
  line-height: 1.6;
  color: #7c2d12;
}

@media (max-width: 1180px) {
  .leadership-detail__metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .leadership-detail {
    padding: 16px;
    border-radius: 22px;
    gap: 14px;
  }

  .leadership-detail__header,
  .leadership-detail__section-head,
  .leadership-detail__section-summary {
    flex-direction: column;
    align-items: flex-start;
  }

  .leadership-detail__section,
  .leadership-detail__section-summary {
    padding: 16px;
  }

  .leadership-detail__metrics,
  .leadership-detail__trend {
    grid-template-columns: 1fr;
  }

  .leadership-detail__section--fold .leadership-detail__trend,
  .leadership-detail__section--fold .leadership-detail__subloading,
  .leadership-detail__section--fold .leadership-detail__abnormal-list,
  .leadership-detail__section--fold :deep(.van-empty) {
    margin: 0 16px 16px;
  }
}
</style>

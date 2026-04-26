<template>
  <section ref="rootRef" class="leadership-members">
    <div class="leadership-members__toolbar">
      <div>
        <p class="leadership-members__eyebrow">下级考勤</p>
        <h3 class="leadership-members__title">卡片化查看下级今日状态与待审事项</h3>
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
      正在整理下级考勤卡片...
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
            <span class="leadership-member-card__status" :class="`leadership-member-card__status--${member.statusTone}`">
              {{ member.statusLabel }}
            </span>
            <span v-if="member.readonlyMode" class="leadership-member-card__readonly">跨部门只读</span>
            <span v-else-if="member.pendingReviewCount" class="leadership-member-card__pending">
              待审 {{ member.pendingReviewCount }}
            </span>
          </div>
        </button>

        <div class="leadership-member-card__node-grid">
          <article
            v-for="node in member.nodes"
            :key="`${member.userId}-${node.nodeCode}`"
            class="leadership-node-card"
            :class="`leadership-node-card--${node.statusTone || 'pending'}`"
            :style="nodeCardStyle(node)"
          >
            <div class="leadership-node-card__top">
              <span class="leadership-node-card__step" :style="nodePhaseStyle(node)">{{ node.stepLabel || '-' }}</span>
              <span class="leadership-node-card__status">{{ node.statusLabel || '待处理' }}</span>
            </div>
            <div class="leadership-node-card__title">{{ node.nodeTitleShort || '-' }}</div>
            <div class="leadership-node-card__range">{{ node.timeRangeText || '时间以规则为准' }}</div>
            <div class="leadership-node-card__time">{{ node.actualPunchTime || '未打卡' }}</div>
            <p class="leadership-node-card__remark">{{ node.simpleRemark || '暂无节点说明' }}</p>
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

        <div class="leadership-member-card__summary">
          <span>本周出勤 {{ member.weekSummary.attendanceDays }} 天</span>
          <span>迟到 {{ member.weekSummary.lateCount }} 次</span>
          <span>异常 {{ member.weekSummary.abnormalCount }} 次</span>
        </div>

        <p class="leadership-member-card__hint">{{ member.statusSummary }}</p>

        <div v-if="member.canReviewPending" class="leadership-member-card__footer">
          <button
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
  gap: 16px;
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
.leadership-node-card__remark {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.leadership-member-card__status,
.leadership-member-card__readonly,
.leadership-member-card__pending,
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

.leadership-member-card__pending {
  background: rgba(79, 125, 243, 0.12);
  color: #315fd3;
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
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.leadership-node-card__time {
  font-size: 20px;
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
}

@media (max-width: 960px) {
  .leadership-members__filters,
  .leadership-member-card__node-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .leadership-member-card {
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
}
</style>

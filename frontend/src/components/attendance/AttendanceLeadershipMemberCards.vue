<template>
  <section ref="rootRef" class="leadership-members">
    <div class="leadership-members__toolbar">
      <div>
        <p class="leadership-members__eyebrow">团队成员工作台</p>
        <h3 class="leadership-members__title">在当前页查看下级考勤，不再来回跳转详情页</h3>
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
      正在整理团队成员卡片...
    </van-loading>

    <van-empty v-else-if="!members.length" description="当前筛选下没有可展示的成员" />

    <div v-else class="leadership-member-list">
      <article
        v-for="member in members"
        :key="member.userId"
        class="leadership-member-card"
        :data-member-id="member.userId"
        :class="{
          'leadership-member-card--active': selectedUserId === member.userId
        }"
      >
        <button type="button" class="leadership-member-card__trigger" @click="$emit('select-member', member)">
          <div class="leadership-member-card__identity">
            <div class="leadership-member-card__identity-main">
              <h4 class="leadership-member-card__name">{{ member.realName }}</h4>
              <p class="leadership-member-card__meta">
                {{ member.unitName || '未归属部门' }} / {{ member.jobTitle || '岗位未填写' }}
              </p>
            </div>
            <div class="leadership-member-card__identity-side">
              <span
                class="leadership-member-card__status"
                :class="`leadership-member-card__status--${member.statusTone}`"
              >
                {{ member.statusLabel }}
              </span>
              <span class="leadership-member-card__action">
                {{ selectedUserId === member.userId ? '收起明细' : '查看明细' }}
              </span>
            </div>
          </div>

          <div class="leadership-member-card__timeline">
            <div class="leadership-member-card__time-item">
              <span class="leadership-member-card__time-label">上班</span>
              <strong>{{ member.checkInTimeText }}</strong>
            </div>
            <div class="leadership-member-card__time-item">
              <span class="leadership-member-card__time-label">下班</span>
              <strong>{{ member.checkOutTimeText }}</strong>
            </div>
          </div>

          <div class="leadership-member-card__summary">
            <span>本周出勤 {{ member.weekSummary.attendanceDays }} 天</span>
            <span>迟到 {{ member.weekSummary.lateCount }} 次</span>
            <span>异常 {{ member.weekSummary.abnormalCount }} 次</span>
          </div>

          <p class="leadership-member-card__hint">{{ member.statusSummary }}</p>
        </button>

        <transition name="leadership-detail-expand">
          <div
            v-if="selectedUserId === member.userId"
            class="leadership-member-card__detail leadership-member-card__detail--mobile"
          >
            <slot name="detail" :member="member" />
          </div>
        </transition>
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
  'refresh'
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
.leadership-members__clear {
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
.leadership-members__clear:hover {
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
  border-color: rgba(37, 99, 235, 0.45);
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.08);
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
  border-radius: 22px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: rgba(255, 255, 255, 0.9);
  overflow: hidden;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease, background 0.2s ease;
}

.leadership-member-card--active {
  border-color: rgba(37, 99, 235, 0.28);
  box-shadow: 0 18px 40px rgba(15, 23, 42, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(241, 245, 249, 0.98));
}

.leadership-member-card__trigger {
  width: 100%;
  border: none;
  background: transparent;
  padding: 18px;
  text-align: left;
  display: grid;
  gap: 16px;
  cursor: pointer;
}

.leadership-member-card__identity {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
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

.leadership-member-card__meta {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
}

.leadership-member-card__status {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 72px;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
}

.leadership-member-card__status--normal {
  background: rgba(16, 185, 129, 0.12);
  color: #047857;
}

.leadership-member-card__status--late {
  background: rgba(245, 158, 11, 0.14);
  color: #b45309;
}

.leadership-member-card__status--missing,
.leadership-member-card__status--abnormal {
  background: rgba(248, 113, 113, 0.14);
  color: #b91c1c;
}

.leadership-member-card__status--field {
  background: rgba(14, 165, 233, 0.14);
  color: #0369a1;
}

.leadership-member-card__status--pending {
  background: rgba(148, 163, 184, 0.16);
  color: #475569;
}

.leadership-member-card__action {
  font-size: 12px;
  color: #64748b;
}

.leadership-member-card__timeline {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.leadership-member-card__time-item {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 16px;
  background: #f8fafc;
}

.leadership-member-card__time-label {
  font-size: 12px;
  color: #64748b;
}

.leadership-member-card__time-item strong {
  font-size: 20px;
  color: #0f172a;
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

.leadership-member-card__hint {
  margin: 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
  display: -webkit-box;
  overflow: hidden;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.leadership-member-card__detail--mobile {
  display: none;
  padding: 0 18px 18px;
  border-top: 1px solid rgba(148, 163, 184, 0.14);
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.92), rgba(255, 255, 255, 0.98));
}

.leadership-detail-expand-enter-active,
.leadership-detail-expand-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.leadership-detail-expand-enter-from,
.leadership-detail-expand-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 960px) {
  .leadership-members__toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .leadership-members__filters {
    grid-template-columns: 1fr;
  }

  .leadership-members__refresh,
  .leadership-members__clear {
    width: 100%;
  }

  .leadership-member-card__detail--mobile {
    display: block;
  }
}

@media (max-width: 640px) {
  .leadership-members {
    gap: 14px;
  }

  .leadership-member-card__trigger {
    gap: 14px;
    padding: 16px;
  }

  .leadership-member-card__identity {
    flex-direction: column;
    gap: 10px;
  }

  .leadership-member-card__identity-side {
    width: 100%;
    justify-items: start;
  }

  .leadership-member-card__name {
    font-size: 18px;
  }

  .leadership-member-card__timeline {
    grid-template-columns: 1fr;
  }
}
</style>

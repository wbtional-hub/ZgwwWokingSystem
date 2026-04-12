<template>
  <div class="attendance-tabs">
    <button
      v-for="item in tabs"
      :key="item.path"
      type="button"
      class="attendance-tab"
      :class="{ 'attendance-tab--active': route.path === item.path }"
      @click="router.push(item.path)"
    >
      <span class="attendance-tab__title">{{ item.label }}</span>
      <span class="attendance-tab__desc">{{ item.description }}</span>
    </button>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const tabs = [
  { path: '/attendance', label: '工作台', description: '今日打卡与团队视图' },
  { path: '/attendance/stats', label: '统计', description: '应到、异常与本周概览' },
  { path: '/attendance/patch-apply', label: '补打卡申请', description: '提交并查看我的申请' },
  { path: '/attendance/patch-approvals', label: '补打卡审批', description: '审批待处理补卡' },
  { path: '/attendance/rules', label: '考勤规则', description: '配置上下班与宽限时间' }
]
</script>

<style scoped>
.attendance-tabs {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(168px, 1fr));
  gap: 10px;
  margin-bottom: 18px;
}

.attendance-tab {
  width: 100%;
  padding: 12px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  text-align: left;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.04);
}

.attendance-tab--active {
  border-color: #2563eb;
  background: linear-gradient(180deg, #eff6ff, #ffffff);
  box-shadow: 0 12px 26px rgba(37, 99, 235, 0.14);
}

.attendance-tab__title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.attendance-tab__desc {
  display: block;
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}
</style>

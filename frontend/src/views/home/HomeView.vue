<template>
  <AppPageShell title="首页" description="讲师团日志管理系统首页骨架已接入后台壳层。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary">刷新首页</van-button>
        <van-button plain type="success">查看系统说明</van-button>
      </div>
    </template>

    <PageSkeletonSection title="查询区占位" description="首页后续可补系统公告检索、快捷入口筛选和待办检索。">
      当前登录人：{{ userName }}。当前阶段先保留首页查询区骨架，后续再接真实检索能力。
    </PageSkeletonSection>

    <PageSkeletonSection title="内容区占位" description="首页后续将承载欢迎信息、快捷导航、公告卡片和待办面板。">
      <div class="home-grid">
        <div class="home-card">
          <div class="home-card-label">欢迎语</div>
          <div class="home-card-value">欢迎进入讲师团日志管理系统</div>
        </div>
        <div class="home-card">
          <div class="home-card-label">当前登录人</div>
          <div class="home-card-value">{{ userName }}</div>
        </div>
        <div class="home-card">
          <div class="home-card-label">系统状态</div>
          <div class="home-card-value">页面骨架已接入</div>
        </div>
      </div>
    </PageSkeletonSection>

    <PageSkeletonSection title="空状态提示" description="首页暂无公告、待办或快捷配置时的统一提示。" variant="empty">
      当前暂无首页动态内容，后续接入公告、待办和快捷入口后在此展示。
    </PageSkeletonSection>
  </AppPageShell>
</template>

<script setup>
import { computed } from 'vue'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageSkeletonSection from '@/components/layout/PageSkeletonSection.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const userName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '管理员')
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.home-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.home-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 16px;
  background: #f9fafb;
}

.home-card-label {
  color: #6b7280;
  font-size: 13px;
}

.home-card-value {
  margin-top: 10px;
  color: #111827;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
}

@media (max-width: 900px) {
  .home-grid {
    grid-template-columns: 1fr;
  }
}
</style>

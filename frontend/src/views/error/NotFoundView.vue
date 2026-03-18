<template>
  <AppPageShell title="404" description="404 页面骨架已接入后台壳层。">
    <template #actions>
      <router-link to="/home" class="not-found-link">返回首页</router-link>
    </template>

    <PageSkeletonSection title="查询区占位" description="异常页当前不承载真实查询，先保留统一骨架位置。">
      当前页面不存在，异常页查询区后续如有需要可承载帮助检索或常见入口。
    </PageSkeletonSection>

    <PageSkeletonSection title="内容区占位" description="异常页当前提供错误说明和返回入口。">
      <div class="not-found">
        <p>你访问的页面不存在或尚未接入。</p>
      </div>
    </PageSkeletonSection>

    <PageSkeletonSection title="空状态提示" description="异常页统一反馈。" variant="empty">
      当前没有更多可展示内容，请返回首页或切换到其他菜单继续操作。
    </PageSkeletonSection>
  </AppPageShell>
</template>

<script setup>
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageSkeletonSection from '@/components/layout/PageSkeletonSection.vue'
</script>

<style scoped>
.not-found {
  color: #4b5563;
  line-height: 1.8;
}

.not-found-link {
  color: #1677ff;
  text-decoration: none;
}
</style>

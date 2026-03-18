<template>
  <AppPageShell title="无权限" description="无权限页骨架已接入后台壳层。">
    <template #actions>
      <router-link to="/home" class="page-link-button">返回首页</router-link>
    </template>

    <PageSkeletonSection title="查询区占位" description="异常页当前不承载真实查询，先保留统一骨架位置。">
      当前页面暂无查询能力，后续如有需要可在此补帮助检索或权限申请入口。
    </PageSkeletonSection>

    <PageSkeletonSection title="内容区占位" description="无权限页当前提供权限说明和返回入口。">
      当前账号暂无访问该页面的权限，请联系管理员或返回其他菜单继续操作。
    </PageSkeletonSection>

    <PageSkeletonSection title="空状态提示" description="无权限页统一反馈。" variant="empty">
      当前没有更多可展示内容，请返回首页或切换到其他已开放页面。
    </PageSkeletonSection>
  </AppPageShell>
</template>

<script setup>
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageSkeletonSection from '@/components/layout/PageSkeletonSection.vue'
</script>

<style scoped>
.page-link-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 8px;
  background: #1677ff;
  color: #fff;
  text-decoration: none;
}
</style>

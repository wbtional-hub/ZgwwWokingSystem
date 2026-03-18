<template>
  <AppPageShell title="个人中心" description="个人中心页骨架已接入后台壳层。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary">编辑资料</van-button>
        <van-button plain type="success">修改密码</van-button>
      </div>
    </template>

    <PageSkeletonSection title="查询区占位" description="后续可补个人资料检索、快捷筛选和个人记录查询。">
      当前阶段先保留个人中心查询区骨架，后续再接真实个人资料筛选能力。
    </PageSkeletonSection>

    <PageSkeletonSection title="内容区占位" description="后续将承载个人资料、登录信息和操作记录。">
      个人中心内容区待接入，当前先保证页面可访问、可进入、可看。
    </PageSkeletonSection>

    <PageSkeletonSection title="空状态提示" description="暂无个人数据时的统一反馈。" variant="empty">
      当前暂无可展示的个人中心内容，后续接入个人资料与历史记录后在此展示。
    </PageSkeletonSection>
  </AppPageShell>
</template>

<script setup>
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageSkeletonSection from '@/components/layout/PageSkeletonSection.vue'
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>

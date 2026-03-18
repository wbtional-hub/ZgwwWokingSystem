<template>
  <AppPageShell title="个人中心" description="个人中心已补齐当前账号资料展示，可直接查看当前登录身份和角色信息。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" @click="copyUsername">复制账号</van-button>
        <van-button plain type="success" @click="copyUserId">复制用户ID</van-button>
      </div>
    </template>

    <PageSkeletonSection title="当前账号" description="个人中心先补齐最常用的当前账号资料，进入页面即可确认登录身份。">
      当前登录账号：{{ profile.username }}，角色：{{ profile.roleLabel }}。
    </PageSkeletonSection>

    <PageSkeletonSection title="资料概览" description="个人中心先提供最小可用资料卡片，方便验收当前账号上下文是否正确。">
      <div class="profile-grid">
        <div class="profile-card">
          <div class="profile-label">姓名</div>
          <div class="profile-value">{{ profile.realName }}</div>
        </div>
        <div class="profile-card">
          <div class="profile-label">账号</div>
          <div class="profile-value">{{ profile.username }}</div>
        </div>
        <div class="profile-card">
          <div class="profile-label">用户 ID</div>
          <div class="profile-value">{{ profile.userId }}</div>
        </div>
        <div class="profile-card">
          <div class="profile-label">当前角色</div>
          <div class="profile-value">{{ profile.roleLabel }}</div>
        </div>
      </div>
    </PageSkeletonSection>

    <PageSkeletonSection title="当前说明" description="当前阶段先保留个人中心的最小说明，后续再继续补资料维护和密码修改。">
      {{ profileHint }}
    </PageSkeletonSection>

    <PageSkeletonSection title="账号状态" description="个人中心补齐当前登录状态说明，方便快速确认是否处于有效登录和对应角色工作面。">
      <div class="status-list">
        <div class="status-item">
          <span class="status-name">登录状态</span>
          <span class="status-value">{{ loginStateText }}</span>
        </div>
        <div class="status-item">
          <span class="status-name">菜单范围</span>
          <span class="status-value">{{ menuScopeText }}</span>
        </div>
        <div class="status-item">
          <span class="status-name">默认工作面</span>
          <span class="status-value">{{ workbenchText }}</span>
        </div>
      </div>
    </PageSkeletonSection>
  </AppPageShell>
</template>

<script setup>
import { computed } from 'vue'
import { showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageSkeletonSection from '@/components/layout/PageSkeletonSection.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const profile = computed(() => {
  const userInfo = userStore.userInfo || {}
  const roleCode = userInfo.role || (userInfo.superAdmin ? 'ADMIN' : 'USER')
  return {
    userId: userInfo.userId || '-',
    username: userInfo.username || '-',
    realName: userInfo.realName || userInfo.username || '未命名用户',
    roleLabel: roleCode === 'ADMIN' ? '管理员' : '普通用户'
  }
})

const profileHint = computed(() =>
  profile.value.roleLabel === '管理员'
    ? '当前账号为管理员，可继续从首页或左侧菜单进入用户、单位、组织等管理主线。'
    : '当前账号为普通用户，可继续进入签到、周报、评分结果等个人工作面。'
)
const loginStateText = computed(() => (userStore.token ? '已登录' : '未登录'))
const menuScopeText = computed(() => (profile.value.roleLabel === '管理员' ? '全部后台模块' : '个人可用模块'))
const workbenchText = computed(() =>
  profile.value.roleLabel === '管理员' ? '用户管理 / 单位管理 / 组织架构' : '签到管理 / 周报管理 / 工作评分'
)

async function copyText(value, successText) {
  try {
    await navigator.clipboard.writeText(String(value))
    showToast(successText)
  } catch (error) {
    showToast('复制失败，请稍后重试')
  }
}

function copyUsername() {
  copyText(profile.value.username, '账号已复制')
}

function copyUserId() {
  copyText(profile.value.userId, '用户ID已复制')
}
</script>

<style scoped>
.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.profile-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 16px;
  background: #f9fafb;
}

.profile-label {
  color: #6b7280;
  font-size: 13px;
}

.profile-value {
  margin-top: 10px;
  color: #111827;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.5;
}

.status-list {
  display: grid;
  gap: 12px;
}

.status-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
  padding: 14px 16px;
  background: #fff;
}

.status-name {
  color: #6b7280;
  font-size: 13px;
}

.status-value {
  color: #111827;
  font-size: 14px;
  font-weight: 600;
  text-align: right;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }

  .status-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .status-value {
    text-align: left;
  }
}
</style>

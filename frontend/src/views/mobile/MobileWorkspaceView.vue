<template>
  <section class="mobile-workspace">
    <div class="mobile-workspace__hero">
      <div class="mobile-workspace__eyebrow">WeChat Mini Workspace</div>
      <div class="mobile-workspace__headline">
        {{ greetingName }}
        <span>手机端已接入 {{ visibleItems.length }} 个功能入口</span>
      </div>
      <p class="mobile-workspace__summary">
        微信绑定账号登录后，系统会自动继承该账号权限，并把可用功能按手机操作场景集中展示。
      </p>

      <div class="mobile-workspace__hero-meta">
        <span class="hero-meta-pill">当前身份 {{ roleLabel }}</span>
        <span class="hero-meta-pill">已授权 {{ featureCountText }}</span>
        <span class="hero-meta-pill">小程序端推荐从这里进入</span>
      </div>

      <button
        v-if="primaryItem"
        type="button"
        class="mobile-workspace__hero-action"
        @click="goPath(primaryItem.path)"
      >
        {{ primaryItem.actionText || '进入模块' }}
      </button>
    </div>

    <section class="mobile-workspace__panel">
      <div class="mobile-workspace__section-head">
        <div>
          <div class="mobile-workspace__section-title">今天常用</div>
          <div class="mobile-workspace__section-note">优先把最常操作的模块放在前面，后续新增模块也会自动接入这个工作台。</div>
        </div>
      </div>

      <div class="mobile-workspace__module-list">
        <button
          v-for="item in featureItems"
          :key="item.key"
          type="button"
          class="module-tile"
          @click="goPath(item.path)"
        >
          <div class="module-tile__top">
            <span class="module-tile__icon" :class="item.accentClass">{{ item.initials }}</span>
            <span class="module-tile__badge">{{ item.badge }}</span>
          </div>
          <div class="module-tile__title">{{ item.title }}</div>
          <div class="module-tile__desc">{{ item.heroDescription }}</div>
          <div class="module-tile__scene">{{ item.sceneText }}</div>
          <div class="module-tile__action">
            <span>{{ item.actionText || '进入' }}</span>
            <span>›</span>
          </div>
        </button>
      </div>
    </section>

    <section class="mobile-workspace__flow">
      <div class="mobile-workspace__section-head">
        <div>
          <div class="mobile-workspace__section-title">推荐动线</div>
          <div class="mobile-workspace__section-note">手机端尽量减少学习成本，让用户进入后就知道先做什么。</div>
        </div>
      </div>

      <div class="flow-list">
        <div
          v-for="(step, index) in recommendedFlow"
          :key="step.key"
          class="flow-step"
        >
          <div class="flow-step__index">{{ String(index + 1).padStart(2, '0') }}</div>
          <div class="flow-step__body">
            <div class="flow-step__title">{{ step.title }}</div>
            <div class="flow-step__text">{{ step.text }}</div>
          </div>
        </div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { buildAccessContext } from '@/constants/modules'
import { resolveMobileWorkspaceItems } from '@/constants/mobile-workspace'

const router = useRouter()
const userStore = useUserStore()

const accessContext = computed(() => buildAccessContext(userStore.userInfo))
const visibleItems = computed(() => resolveMobileWorkspaceItems(accessContext.value))
const featureItems = computed(() => visibleItems.value.filter((item) => item.key !== 'workspace' && item.key !== 'profile'))
const primaryItem = computed(() => {
  return featureItems.value.find((item) => item.key === 'policy')
    || featureItems.value[0]
    || visibleItems.value.find((item) => item.key === 'profile')
    || null
})

const greetingName = computed(() => userStore.userInfo?.realName || userStore.userInfo?.username || '当前用户')
const roleLabel = computed(() => accessContext.value.isAdmin ? '管理员账号' : '授权账号')
const featureCountText = computed(() => `${featureItems.value.length} 个业务模块`)
const recommendedFlow = computed(() => {
  const flow = []
  if (featureItems.value.some((item) => item.key === 'attendance')) {
    flow.push({
      key: 'attendance',
      title: '先签到',
      text: '进入签到管理后即可定位打卡，适合放在上班第一步。'
    })
  }
  if (featureItems.value.some((item) => item.key === 'weekly')) {
    flow.push({
      key: 'weekly',
      title: '再处理周报',
      text: '随时补充本周进展，手机端也可以保存草稿和提交审核。'
    })
  }
  if (featureItems.value.some((item) => item.key === 'policy')) {
    flow.push({
      key: 'policy',
      title: '有问题就咨询',
      text: '政策咨询支持按账号权限和个人习惯给出连续回答。'
    })
  }
  if (!flow.length) {
    flow.push({
      key: 'profile',
      title: '查看个人中心',
      text: '当前账号还没有分配手机端业务模块时，可以先确认身份和权限。'
    })
  }
  return flow
})

function goPath(path) {
  if (path) {
    router.push(path)
  }
}
</script>

<style scoped>
.mobile-workspace {
  --workspace-bg: #f5efe6;
  --workspace-ink: #1d2b2f;
  --workspace-muted: #68777a;
  --workspace-line: rgba(24, 48, 52, 0.08);
  --workspace-surface: rgba(255, 252, 247, 0.92);
  color: var(--workspace-ink);
}

.mobile-workspace__hero,
.mobile-workspace__panel,
.mobile-workspace__flow {
  border: 1px solid rgba(255, 255, 255, 0.62);
  border-radius: 28px;
  background: var(--workspace-surface);
  box-shadow: 0 22px 60px rgba(52, 38, 18, 0.08);
}

.mobile-workspace__hero {
  padding: 24px 20px;
  background:
    radial-gradient(circle at top right, rgba(197, 139, 58, 0.16), transparent 24%),
    linear-gradient(180deg, rgba(255, 251, 245, 0.96), rgba(246, 238, 227, 0.94));
}

.mobile-workspace__eyebrow {
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: #0f6c63;
  font-weight: 700;
}

.mobile-workspace__headline {
  margin-top: 14px;
  font-size: clamp(28px, 7vw, 42px);
  line-height: 1.04;
  letter-spacing: -0.04em;
  font-weight: 700;
}

.mobile-workspace__headline span {
  display: block;
  margin-top: 8px;
  font-size: clamp(16px, 4.6vw, 22px);
  line-height: 1.35;
  letter-spacing: -0.02em;
  color: #304347;
}

.mobile-workspace__summary {
  max-width: 30rem;
  margin: 14px 0 0;
  color: var(--workspace-muted);
  line-height: 1.75;
  font-size: 14px;
}

.mobile-workspace__hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 18px;
}

.hero-meta-pill {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(24, 48, 52, 0.08);
  color: #31464a;
  font-size: 12px;
  font-weight: 600;
}

.mobile-workspace__hero-action {
  margin-top: 22px;
  min-width: 140px;
  min-height: 46px;
  padding: 0 18px;
  border: none;
  border-radius: 999px;
  background: linear-gradient(135deg, #0f6c63, #0b4f49 72%, #9a6726);
  color: #fffef9;
  font-size: 14px;
  font-weight: 700;
  box-shadow: 0 16px 30px rgba(15, 108, 99, 0.18);
}

.mobile-workspace__panel,
.mobile-workspace__flow {
  margin-top: 16px;
  padding: 18px;
}

.mobile-workspace__section-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.mobile-workspace__section-title {
  font-size: 18px;
  font-weight: 700;
  color: #17282b;
}

.mobile-workspace__section-note {
  margin-top: 6px;
  color: var(--workspace-muted);
  font-size: 13px;
  line-height: 1.65;
}

.mobile-workspace__module-list {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.module-tile {
  padding: 16px;
  border: 1px solid var(--workspace-line);
  border-radius: 22px;
  background: linear-gradient(180deg, #ffffff, #faf6ef);
  text-align: left;
  box-shadow: 0 16px 30px rgba(20, 33, 36, 0.05);
}

.module-tile__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.module-tile__icon {
  width: 42px;
  height: 42px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.module-tile__badge {
  display: inline-flex;
  min-height: 26px;
  align-items: center;
  padding: 0 10px;
  border-radius: 999px;
  background: #f5efe6;
  color: #7f6c4b;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.module-tile__title {
  margin-top: 14px;
  font-size: 20px;
  font-weight: 700;
  color: #152528;
}

.module-tile__desc,
.module-tile__scene,
.flow-step__text {
  color: var(--workspace-muted);
  line-height: 1.7;
}

.module-tile__desc {
  margin-top: 8px;
  font-size: 14px;
}

.module-tile__scene {
  margin-top: 10px;
  font-size: 12px;
}

.module-tile__action {
  margin-top: 14px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #0f6c63;
  font-size: 13px;
  font-weight: 700;
}

.flow-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.flow-step {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 12px;
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #f9fafb);
  border: 1px solid var(--workspace-line);
}

.flow-step__index {
  width: 44px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
  background: #182f33;
  color: #fffdf9;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.flow-step__title {
  font-size: 15px;
  font-weight: 700;
  color: #16282b;
}

.flow-step__text {
  margin-top: 6px;
  font-size: 13px;
}

@media (min-width: 768px) {
  .mobile-workspace__module-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>

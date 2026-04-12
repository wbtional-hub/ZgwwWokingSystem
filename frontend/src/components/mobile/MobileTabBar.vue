<template>
  <nav v-if="items.length" class="mobile-tabbar" aria-label="手机端导航">
    <button
      v-for="item in items"
      :key="item.key || item.path"
      type="button"
      class="mobile-tabbar__item"
      :class="{ 'mobile-tabbar__item--active': isItemActive(item) }"
      @click="handleNavigate(item.path)"
    >
      <span class="mobile-tabbar__icon" :class="item.accentClass">{{ item.initials || item.shortTitle?.slice(0, 2) }}</span>
      <span class="mobile-tabbar__label">{{ item.shortTitle || item.title }}</span>
    </button>
  </nav>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'

const props = defineProps({
  items: {
    type: Array,
    default: () => []
  }
})

const route = useRoute()
const router = useRouter()

function isItemActive(item) {
  const matchPaths = Array.isArray(item?.matchPaths) ? item.matchPaths : [item?.path]
  return matchPaths.includes(route.path)
}

function handleNavigate(path) {
  if (path && path !== route.path) {
    router.push(path)
  }
}
</script>

<style scoped>
.mobile-tabbar {
  position: sticky;
  bottom: 0;
  z-index: 18;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(0, 1fr));
  gap: 8px;
  padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
  border-top: 1px solid rgba(18, 41, 45, 0.08);
  background: rgba(255, 251, 247, 0.94);
  backdrop-filter: blur(16px);
}

.mobile-tabbar__item {
  display: grid;
  justify-items: center;
  gap: 6px;
  min-height: 58px;
  padding: 8px 4px;
  border: none;
  border-radius: 18px;
  background: transparent;
  color: #687778;
  transition: background 0.22s ease, color 0.22s ease, transform 0.22s ease;
}

.mobile-tabbar__item--active {
  background: rgba(255, 255, 255, 0.92);
  color: #16363a;
  box-shadow: 0 10px 24px rgba(18, 41, 45, 0.08);
}

.mobile-tabbar__icon {
  width: 34px;
  height: 34px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  background: #edf2f1;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.08em;
}

.mobile-tabbar__label {
  font-size: 11px;
  font-weight: 600;
}

.mobile-accent-workspace {
  background: linear-gradient(135deg, #dbeafe, #eff6ff);
  color: #1d4ed8;
}

.mobile-accent-policy {
  background: linear-gradient(135deg, #d8f2ee, #edf9f6);
  color: #0f6c63;
}

.mobile-accent-attendance {
  background: linear-gradient(135deg, #fee2e2, #fff1f2);
  color: #b42318;
}

.mobile-accent-weekly {
  background: linear-gradient(135deg, #fef3c7, #fffbeb);
  color: #b7791f;
}

.mobile-accent-profile {
  background: linear-gradient(135deg, #ede9fe, #f5f3ff);
  color: #6d28d9;
}
</style>

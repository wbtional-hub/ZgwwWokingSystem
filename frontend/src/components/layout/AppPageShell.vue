<template>
  <section class="page-shell">
    <div class="page-shell-header">
      <div>
        <div class="page-shell-title-row">
          <h2 class="page-shell-title">{{ title }}</h2>
          <PageHelp v-if="resolvedHelpKey && !hasTitleExtraSlot" :page-key="resolvedHelpKey" />
          <slot name="title-extra" />
        </div>
        <p v-if="description" class="page-shell-description">{{ description }}</p>
      </div>
      <slot name="actions" />
    </div>
    <div class="page-shell-body">
      <slot />
    </div>
  </section>
</template>

<script setup>
import { computed, useSlots } from 'vue'
import { useRoute } from 'vue-router'
import PageHelp from '@/components/PageHelp.vue'

const route = useRoute()
const slots = useSlots()

const routeHelpKeyMap = {
  '/home': 'home',
  '/units': 'unit',
  '/params': 'param',
  '/operation-logs': 'operationLog',
  '/org-tree': 'orgTree',
  '/attendance': 'attendance',
  '/weekly-work': 'weeklyWork',
  '/weekly-work/editor': 'weeklyWork',
  '/statistics': 'statistics',
  '/scores': 'score',
  '/users': 'user',
  '/profile': 'profile',
  '/knowledge': 'knowledge',
  '/ai-provider': 'aiProvider',
  '/ai-permissions': 'aiPermissions',
  '/skills': 'skills',
  '/ai-workbench': 'aiWorkbench',
  '/ai-ledger': 'aiLedger',
  '/ai-monthly-report': 'aiMonthlyReport',
  '/experts': 'experts'
}

const props = defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  },
  helpKey: {
    type: String,
    default: ''
  }
})

const hasTitleExtraSlot = computed(() => Boolean(slots['title-extra']))
const resolvedHelpKey = computed(() => props.helpKey || routeHelpKeyMap[route.path] || '')
</script>

<style scoped>
.page-shell {
  background: #fff;
  border-radius: 14px;
  padding: 20px;
  border: 1px solid #e5e7eb;
}

.page-shell-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}

.page-shell-title {
  margin: 0;
  font-size: 22px;
  color: #111827;
  white-space: nowrap;
}

.page-shell-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.page-shell-description {
  margin: 8px 0 0;
  color: #6b7280;
  font-size: 14px;
}

.page-shell-body {
  min-width: 0;
}
</style>

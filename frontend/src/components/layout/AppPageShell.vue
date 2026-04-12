<template>
  <section class="page-shell">
    <div class="page-shell-header">
      <div class="page-shell-main">
        <div class="page-shell-kicker">{{ resolvedHelpKey ? resolvedHelpKeyLabel : 'Workspace' }}</div>
        <div class="page-shell-title-row">
          <h2 class="page-shell-title">{{ title }}</h2>
          <PageHelp v-if="resolvedHelpKey && !hasTitleExtraSlot" :page-key="resolvedHelpKey" />
          <slot name="title-extra" />
        </div>
        <p v-if="description" class="page-shell-description">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="page-shell-actions">
        <slot name="actions" />
      </div>
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
  '/log-center': 'operationLog',
  '/operation-logs': 'operationLog',
  '/org-tree': 'orgTree',
  '/attendance': 'attendance',
  '/attendance/stats': 'attendance',
  '/attendance/patch-apply': 'attendance',
  '/attendance/patch-approvals': 'attendance',
  '/attendance/rules': 'attendance',
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

const routeHelpLabelMap = {
  home: 'Workspace',
  unit: 'System',
  param: 'System',
  operationLog: 'Audit',
  orgTree: 'Structure',
  attendance: 'Attendance',
  weeklyWork: 'Weekly Work',
  statistics: 'Analytics',
  score: 'Score',
  user: 'Members',
  profile: 'Profile',
  knowledge: 'Knowledge',
  aiProvider: 'AI Provider',
  aiPermissions: 'AI Governance',
  skills: 'Skills Center',
  aiWorkbench: 'AI Workbench',
  aiLedger: 'Consultation Ledger',
  aiMonthlyReport: 'Monthly Report',
  experts: 'Expert Identity'
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
const resolvedHelpKeyLabel = computed(() => routeHelpLabelMap[resolvedHelpKey.value] || 'Workspace')
</script>

<style scoped>
.page-shell {
  padding: 24px;
  border-radius: 14px;
  background: var(--app-card);
  border: 1px solid #eef2f7;
  box-shadow: var(--app-shadow);
}

.page-shell-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
}

.page-shell-main {
  min-width: 0;
}

.page-shell-kicker {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 10px;
  border-radius: 999px;
  background: #eef2ff;
  color: #3b82f6;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.page-shell-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 14px;
}

.page-shell-title {
  margin: 0;
  color: #111827;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: -0.02em;
}

.page-shell-description {
  max-width: 760px;
  margin: 10px 0 0;
  color: #6b7280;
  font-size: 14px;
  line-height: 1.5;
}

.page-shell-actions {
  display: flex;
  align-items: center;
}

.page-shell-body {
  min-width: 0;
}

@media (max-width: 820px) {
  .page-shell {
    padding: 18px 16px;
    border-radius: 14px;
  }

  .page-shell-header {
    flex-direction: column;
    align-items: stretch;
  }

  .page-shell-title {
    font-size: 22px;
  }
}
</style>

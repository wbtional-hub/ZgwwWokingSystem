<template>
  <AppPageShell title="AI能力地图" description="这个页面是 AI 全链路导航页。">
    <section class="ai-map-page">
      <div class="ai-map-chain">
        <button
          v-for="node in nodes"
          :key="node.path"
          type="button"
          class="ai-node"
          @click="go(node.path)"
        >
          <div class="ai-node__name">{{ node.name }}</div>
          <div class="ai-node__status" :class="`ai-node__status--${node.status.type}`">{{ node.status.label }}</div>
          <div class="ai-node__summary">{{ node.status.summary }}</div>
        </button>
      </div>
    </section>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { queryCurrentAiPermission, queryAiProviderList } from '@/api/ai'
import { queryKnowledgeBaseList } from '@/api/knowledge'
import { querySkillList } from '@/api/skill'
import { queryAgentSessionStats } from '@/api/agent'

const router = useRouter()

const state = reactive({
  permission: null,
  providers: [],
  bases: [],
  skills: [],
  workbenchStats: {},
  mobileStats: {}
})

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

function go(path) {
  router.push(path)
}

function buildStatus(type, label, summary) {
  return { type, label, summary }
}

const nodes = computed(() => {
  const canUseAgent = Boolean(state.permission?.admin || (state.permission?.aiPermissions || []).some((item) => item.canUseAgent))
  const canUseAi = Boolean(state.permission?.admin || (state.permission?.aiPermissions || []).some((item) => item.canUseAi))
  const providerReady = (state.providers || []).some((item) => Number(item.status) === 1 && item.connectStatus === 'SUCCESS')
  const enabledBaseCount = (state.bases || []).filter((item) => Number(item.status) === 1).length
  const publishedSkillCount = (state.skills || []).filter((item) => item.publishStatus === 'PUBLISHED').length
  const workbenchReady = Number(state.workbenchStats?.totalSessionCount || 0) > 0
  const mobileReady = Number(state.mobileStats?.totalSessionCount || 0) > 0

  return [
    {
      name: 'AI接入区',
      path: '/ai-provider',
      status: providerReady ? buildStatus('ready', '已配置', '已有可用 Provider') : buildStatus('risk', '有风险', '还没有测试成功的 Provider')
    },
    {
      name: 'AI权限配置',
      path: '/ai-permissions',
      status: canUseAgent ? buildStatus('ready', '已配置', '当前账号具备 AI 使用能力') : buildStatus('todo', '待完善', '当前账号还没开通 AI 主链权限')
    },
    {
      name: '知识库中心',
      path: '/knowledge',
      status: enabledBaseCount > 0 ? buildStatus('ready', '已配置', `已启用 ${enabledBaseCount} 个知识库`) : buildStatus('todo', '待完善', '还没有可用知识库')
    },
    {
      name: 'Skills中心',
      path: '/skills',
      status: publishedSkillCount > 0 ? buildStatus('ready', '已配置', `已有 ${publishedSkillCount} 个已发布 Skill`) : buildStatus('todo', '待完善', '还没有已发布 Skill')
    },
    {
      name: 'AI工作台',
      path: '/ai-workbench',
      status: workbenchReady ? buildStatus('connected', '已打通', '后台问答链路已有会话') : buildStatus(canUseAgent ? 'todo' : 'risk', canUseAgent ? '待验证' : '有风险', canUseAgent ? '建议进入页面发起一次真实问答' : '当前账号不能进入 AI 工作台')
    },
    {
      name: '手机端政策咨询',
      path: '/policy-consultant',
      status: mobileReady ? buildStatus('connected', '已打通', '移动端咨询已有会话数据') : buildStatus(canUseAgent ? 'todo' : 'risk', canUseAgent ? '待验证' : '有风险', canUseAgent ? '建议用移动端入口发起一次提问' : '当前账号还不能使用主链问答')
    },
    {
      name: 'AI结果回流',
      path: '/ai-result-flow',
      status: mobileReady ? buildStatus('connected', '已打通', '移动端结果已具备回流基础') : buildStatus('todo', '待完善', '先打通移动端提问链路')
    },
    {
      name: '咨询台账',
      path: '/ai-ledger',
      status: (Number(state.workbenchStats?.totalSessionCount || 0) + Number(state.mobileStats?.totalSessionCount || 0)) > 0
        ? buildStatus('connected', '已打通', '台账已可承接会话数据')
        : buildStatus('todo', '待完善', '还没有可用于台账展示的会话')
    },
    {
      name: '月度报表',
      path: '/ai-monthly-report',
      status: (Number(state.workbenchStats?.totalSessionCount || 0) + Number(state.mobileStats?.totalSessionCount || 0)) > 0
        ? buildStatus('connected', '已打通', '月报已具备统计基础')
        : buildStatus('todo', '待完善', '先让会话和消息落库')
    },
    {
      name: '日志中台',
      path: '/log-center',
      status: canUseAi || canUseAgent ? buildStatus('ready', '可追踪', '关键链路日志已接入日志中台') : buildStatus('todo', '待验证', '需要先完成一次真实链路调用')
    }
  ]
})

onMounted(async () => {
  try {
    const permissionResponse = await queryCurrentAiPermission()
    state.permission = ensureSuccess(permissionResponse, '加载 AI 权限失败') || {}
  } catch (error) {
    state.permission = {}
  }

  const tasks = [
    queryKnowledgeBaseList({ status: 1 }).then((response) => {
      state.bases = ensureSuccess(response, '加载知识库失败') || []
    }).catch(() => {
      state.bases = []
    }),
    querySkillList({ publishStatus: 'PUBLISHED', status: 1 }).then((response) => {
      state.skills = ensureSuccess(response, '加载 Skill 失败') || []
    }).catch(() => {
      state.skills = []
    }),
    queryAgentSessionStats({ sourceScene: 'AI_WORKBENCH' }).then((response) => {
      state.workbenchStats = ensureSuccess(response, '加载工作台状态失败') || {}
    }).catch(() => {
      state.workbenchStats = {}
    }),
    queryAgentSessionStats({ sourceScene: 'MOBILE_POLICY_CONSULTANT' }).then((response) => {
      state.mobileStats = ensureSuccess(response, '加载手机端状态失败') || {}
    }).catch(() => {
      state.mobileStats = {}
    })
  ]

  if (state.permission?.admin || state.permission?.canManageProvider) {
    tasks.push(
      queryAiProviderList({ status: 1 }).then((response) => {
        state.providers = ensureSuccess(response, '加载 Provider 失败') || []
      }).catch(() => {
        state.providers = []
      })
    )
  }

  await Promise.all(tasks)
})
</script>

<style scoped>
.ai-map-chain {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 14px;
}

.ai-node {
  min-height: 180px;
  padding: 18px;
  border: 1px solid #dbe4f0;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff 0%, #f8fbff 100%);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.ai-node:hover {
  transform: translateY(-2px);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.ai-node__name {
  color: #0f172a;
  font-size: 18px;
  font-weight: 700;
}

.ai-node__status {
  display: inline-flex;
  align-items: center;
  min-height: 30px;
  padding: 0 10px;
  margin-top: 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.ai-node__status--ready {
  background: #eff6ff;
  color: #2563eb;
}

.ai-node__status--connected {
  background: #ecfdf5;
  color: #059669;
}

.ai-node__status--risk {
  background: #fff7ed;
  color: #ea580c;
}

.ai-node__status--todo {
  background: #f8fafc;
  color: #475569;
}

.ai-node__summary {
  margin-top: 12px;
  color: #64748b;
  font-size: 13px;
  line-height: 1.7;
}

@media (max-width: 1280px) {
  .ai-map-chain {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 900px) {
  .ai-map-chain {
    grid-template-columns: 1fr;
  }
}
</style>

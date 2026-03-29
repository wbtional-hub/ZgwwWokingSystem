<template>
  <div class="page-help">
    <button
      type="button"
      class="page-help__trigger"
      title="查看操作说明"
      aria-label="查看操作说明"
      @click="helpVisible = true"
    >
      <van-icon name="question-o" />
    </button>

    <van-popup v-model:show="helpVisible" round position="center" class="page-help__popup">
      <div class="page-help__panel">
        <div class="page-help__title">{{ helpConfig.title }}</div>
        <div class="page-help__content">{{ helpConfig.helpText }}</div>
        <div v-if="helpConfig.troubleshootingHint" class="page-help__hint">
          <div class="page-help__hint-title">排查提示</div>
          <div class="page-help__hint-text">{{ helpConfig.troubleshootingHint }}</div>
        </div>
        <div class="page-help__actions">
          <van-button plain size="small" @click="helpVisible = false">关闭</van-button>
          <van-button
            v-if="hasGuideSteps"
            type="primary"
            size="small"
            @click="startGuideFromHelp"
          >
            开始引导
          </van-button>
        </div>
      </div>
    </van-popup>

    <van-popup
      v-model:show="firstVisitVisible"
      round
      position="center"
      :close-on-click-overlay="false"
      class="page-help__popup page-help__popup--first-visit"
    >
      <div class="page-help__panel">
        <div class="page-help__eyebrow">首次进入提示</div>
        <div class="page-help__title">{{ helpConfig.title }}</div>
        <div class="page-help__tip">{{ helpConfig.firstVisitTip }}</div>
        <div class="page-help__subtip">本次登录会话中只会自动提示一次，后续可随时点击问号查看。</div>
        <div class="page-help__actions">
          <van-button plain size="small" @click="dismissFirstVisit">我知道了</van-button>
          <van-button v-if="hasGuideSteps" type="primary" size="small" @click="startGuideFromFirstVisit">
            开始引导
          </van-button>
        </div>
      </div>
    </van-popup>

    <PageGuide v-model:show="guideVisible" :steps="helpConfig.guideSteps || []" />
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { getPageHelp } from '@/config/pageHelp'
import PageGuide from '@/components/PageGuide.vue'

const FIRST_VISIT_SESSION_KEY = 'help:first-visit:session-shown'

const props = defineProps({
  pageKey: {
    type: String,
    required: true
  }
})

const userStore = useUserStore()
const helpVisible = ref(false)
const firstVisitVisible = ref(false)
const guideVisible = ref(false)

const helpConfig = computed(() => getPageHelp(props.pageKey) || {
  title: '页面帮助',
  helpText: '当前页面暂未配置操作说明。',
  firstVisitTip: '',
  guideSteps: []
})

const hasGuideSteps = computed(() => Array.isArray(helpConfig.value.guideSteps) && helpConfig.value.guideSteps.length > 0)

const firstVisitStorageKey = computed(() => {
  const userKey = userStore.userInfo?.userId || userStore.userInfo?.username || 'guest'
  return `help:first-visit:${props.pageKey}:${userKey}`
})

onMounted(async () => {
  if (!helpConfig.value.firstVisitTip) {
    return
  }
  if (localStorage.getItem(firstVisitStorageKey.value) === '1') {
    return
  }
  if (sessionStorage.getItem(FIRST_VISIT_SESSION_KEY) === '1') {
    return
  }
  await nextTick()
  firstVisitVisible.value = true
  sessionStorage.setItem(FIRST_VISIT_SESSION_KEY, '1')
})

function dismissFirstVisit() {
  localStorage.setItem(firstVisitStorageKey.value, '1')
  firstVisitVisible.value = false
}

function startGuideFromHelp() {
  helpVisible.value = false
  guideVisible.value = true
}

function startGuideFromFirstVisit() {
  dismissFirstVisit()
  guideVisible.value = true
}
</script>

<style scoped>
.page-help {
  display: inline-flex;
  align-items: center;
}

.page-help__trigger {
  width: 32px;
  height: 32px;
  border: 1px solid #bae6fd;
  border-radius: 999px;
  background: linear-gradient(180deg, #f0f9ff 0%, #e0f2fe 100%);
  color: #0369a1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 18px;
  transition: transform 0.18s ease, box-shadow 0.18s ease, background 0.18s ease;
}

.page-help__trigger:hover {
  transform: translateY(-1px);
  box-shadow: 0 8px 18px rgba(14, 116, 144, 0.14);
}

.page-help__trigger:focus-visible {
  outline: 2px solid #0ea5e9;
  outline-offset: 2px;
}

.page-help__popup {
  width: min(520px, calc(100vw - 24px));
}

.page-help__panel {
  padding: 18px;
}

.page-help__eyebrow {
  font-size: 12px;
  color: #9a3412;
}

.page-help__title {
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.page-help__content,
.page-help__tip {
  margin-top: 12px;
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-line;
}

.page-help__subtip {
  margin-top: 10px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.6;
}

.page-help__actions {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.page-help__hint {
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.page-help__hint-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.page-help__hint-text {
  margin-top: 6px;
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}
</style>

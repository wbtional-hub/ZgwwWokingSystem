<template>
  <teleport to="body">
    <div v-if="show && (currentStep || fallbackVisible)" class="page-guide-overlay">
      <div class="page-guide-overlay__mask" @click="handleClose" />
      <div
        v-if="highlightRect && !fallbackVisible"
        class="page-guide-overlay__highlight"
        :style="highlightStyle"
      />
      <div class="page-guide-card" :style="cardStyle">
        <div class="page-guide-card__eyebrow">页面引导</div>
        <div class="page-guide-card__title">{{ cardTitle }}</div>
        <div class="page-guide-card__content">{{ cardContent }}</div>
        <div v-if="!fallbackVisible" class="page-guide-card__progress">步骤 {{ displayStepIndex + 1 }} / {{ visibleSteps.length }}</div>
        <div class="page-guide-card__actions">
          <template v-if="fallbackVisible">
            <button type="button" class="page-guide-card__button" @click="retryGuide">重新检查</button>
            <button
              type="button"
              class="page-guide-card__button page-guide-card__button--primary"
              @click="handleClose"
            >
              关闭
            </button>
          </template>
          <template v-else>
            <button type="button" class="page-guide-card__button" :disabled="displayStepIndex === 0" @click="goPrevious">
              上一步
            </button>
            <button
              type="button"
              class="page-guide-card__button page-guide-card__button--primary"
              @click="goNext"
            >
              {{ displayStepIndex === visibleSteps.length - 1 ? '完成引导' : '下一步' }}
            </button>
          </template>
        </div>
      </div>
    </div>
  </teleport>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'

const GUIDE_CARD_WIDTH = 336
const VIEWPORT_SAFE_TOP = 72
const VIEWPORT_SAFE_BOTTOM = 120

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  steps: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['update:show'])

const visibleSteps = computed(() => {
  return (props.steps || []).filter((step) => typeof step?.target === 'string' && step.target.trim())
})

const displayStepIndex = ref(0)
const highlightRect = ref(null)
const fallbackVisible = ref(false)

let refreshRafId = 0

const currentStep = computed(() => visibleSteps.value[displayStepIndex.value] || null)

const cardTitle = computed(() => {
  if (fallbackVisible.value) {
    return '当前页面暂时没有可引导的区域'
  }
  return currentStep.value?.title || ''
})

const cardContent = computed(() => {
  if (fallbackVisible.value) {
    return '这通常表示页面内容还没有加载完成，或者页面结构已经调整。你可以先确认页面已经完全渲染，再点击“重新检查”；如果只是想看说明，也可以关闭后通过标题旁问号查看文字帮助。'
  }
  return currentStep.value?.content || ''
})

const highlightStyle = computed(() => {
  if (!highlightRect.value) {
    return {}
  }
  return {
    top: `${highlightRect.value.top}px`,
    left: `${highlightRect.value.left}px`,
    width: `${highlightRect.value.width}px`,
    height: `${highlightRect.value.height}px`
  }
})

const cardStyle = computed(() => {
  if (!highlightRect.value || fallbackVisible.value) {
    return {
      top: '50%',
      left: '50%',
      transform: 'translate(-50%, -50%)'
    }
  }
  const viewportWidth = window.innerWidth
  const viewportHeight = window.innerHeight
  const preferredTop = highlightRect.value.top + highlightRect.value.height + 18
  const fitsBelow = preferredTop + 220 < viewportHeight
  const top = fitsBelow ? preferredTop : Math.max(24, highlightRect.value.top - 238)
  const left = Math.min(
    Math.max(16, highlightRect.value.left),
    Math.max(16, viewportWidth - GUIDE_CARD_WIDTH)
  )
  return {
    top: `${top}px`,
    left: `${left}px`
  }
})

watch(
  () => props.show,
  async (show) => {
    if (!show) {
      cancelScheduledRefresh()
      highlightRect.value = null
      fallbackVisible.value = false
      return
    }
    displayStepIndex.value = 0
    fallbackVisible.value = false
    await moveToAvailableStep(0)
  }
)

watch(
  () => displayStepIndex.value,
  async () => {
    if (!props.show || fallbackVisible.value) {
      return
    }
    await focusCurrentStep()
  }
)

function getTargetElement(step) {
  if (!step?.target) {
    return null
  }
  return document.querySelector(step.target)
}

function cancelScheduledRefresh() {
  if (refreshRafId) {
    cancelAnimationFrame(refreshRafId)
    refreshRafId = 0
  }
}

function waitForAnimationFrame() {
  return new Promise((resolve) => {
    requestAnimationFrame(() => resolve())
  })
}

function needsViewportAdjustment(target) {
  const rect = target.getBoundingClientRect()
  return rect.top < VIEWPORT_SAFE_TOP || rect.bottom > window.innerHeight - VIEWPORT_SAFE_BOTTOM
}

async function scrollTargetIntoView(target) {
  if (!needsViewportAdjustment(target)) {
    return
  }
  target.scrollIntoView({ block: 'center', behavior: 'smooth' })
  await waitForAnimationFrame()
  await waitForAnimationFrame()
}

function updateHighlightRect(target) {
  const rect = target.getBoundingClientRect()
  highlightRect.value = {
    top: Math.max(8, rect.top - 6),
    left: Math.max(8, rect.left - 6),
    width: rect.width + 12,
    height: rect.height + 12
  }
}

async function moveToAvailableStep(startIndex) {
  for (let index = startIndex; index < visibleSteps.value.length; index += 1) {
    displayStepIndex.value = index
    await nextTick()
    const target = getTargetElement(visibleSteps.value[index])
    if (!target) {
      continue
    }
    fallbackVisible.value = false
    await scrollTargetIntoView(target)
    updateHighlightRect(target)
    return
  }
  highlightRect.value = null
  fallbackVisible.value = true
}

async function focusCurrentStep() {
  await nextTick()
  const step = currentStep.value
  const target = getTargetElement(step)
  if (!target) {
    await moveToAvailableStep(displayStepIndex.value + 1)
    return
  }
  fallbackVisible.value = false
  await scrollTargetIntoView(target)
  updateHighlightRect(target)
}

function refreshCurrentHighlight() {
  if (!props.show || fallbackVisible.value) {
    return
  }
  const target = getTargetElement(currentStep.value)
  if (!target) {
    highlightRect.value = null
    return
  }
  updateHighlightRect(target)
}

function scheduleHighlightRefresh() {
  cancelScheduledRefresh()
  refreshRafId = requestAnimationFrame(() => {
    refreshRafId = 0
    refreshCurrentHighlight()
  })
}

function goPrevious() {
  if (displayStepIndex.value <= 0) {
    return
  }
  displayStepIndex.value -= 1
}

async function goNext() {
  if (displayStepIndex.value >= visibleSteps.value.length - 1) {
    handleClose()
    return
  }
  await moveToAvailableStep(displayStepIndex.value + 1)
}

async function retryGuide() {
  fallbackVisible.value = false
  displayStepIndex.value = 0
  await moveToAvailableStep(0)
}

function handleClose() {
  emit('update:show', false)
}

function handleWindowResize() {
  scheduleHighlightRefresh()
}

function handleWindowScroll() {
  scheduleHighlightRefresh()
}

window.addEventListener('resize', handleWindowResize)
window.addEventListener('scroll', handleWindowScroll, true)

onBeforeUnmount(() => {
  cancelScheduledRefresh()
  window.removeEventListener('resize', handleWindowResize)
  window.removeEventListener('scroll', handleWindowScroll, true)
})
</script>

<style scoped>
.page-guide-overlay {
  position: fixed;
  inset: 0;
  z-index: 2200;
}

.page-guide-overlay__mask {
  position: absolute;
  inset: 0;
  background: rgba(15, 23, 42, 0.55);
}

.page-guide-overlay__highlight {
  position: fixed;
  border-radius: 14px;
  border: 2px solid #f59e0b;
  box-shadow: 0 0 0 9999px rgba(15, 23, 42, 0.35);
  pointer-events: none;
  transition: all 0.18s ease;
}

.page-guide-card {
  position: fixed;
  z-index: 2201;
  width: min(320px, calc(100vw - 32px));
  padding: 16px;
  border-radius: 16px;
  background: #fff;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.18);
}

.page-guide-card__eyebrow {
  font-size: 12px;
  color: #9a3412;
}

.page-guide-card__title {
  margin-top: 8px;
  font-size: 18px;
  font-weight: 700;
  color: #111827;
}

.page-guide-card__content {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: #475569;
  white-space: pre-line;
}

.page-guide-card__progress {
  margin-top: 12px;
  font-size: 12px;
  color: #64748b;
}

.page-guide-card__actions {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.page-guide-card__button {
  border: 1px solid #cbd5e1;
  border-radius: 10px;
  padding: 8px 12px;
  background: #fff;
  color: #334155;
  cursor: pointer;
}

.page-guide-card__button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.page-guide-card__button--primary {
  border-color: #0f766e;
  background: #0f766e;
  color: #fff;
}
</style>

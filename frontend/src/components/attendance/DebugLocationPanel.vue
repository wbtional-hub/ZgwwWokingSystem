<template>
  <van-popup
    :show="show"
    round
    closeable
    position="bottom"
    class="debug-location-panel"
    @update:show="handleMainPopupToggle"
  >
    <div class="debug-location-panel__body">
      <h3 class="debug-location-panel__title">定位诊断信息</h3>

      <section class="debug-section">
        <div class="debug-section__title">A. 当前定位信息</div>
        <div class="debug-grid">
          <div class="debug-item"><span>定位时间</span><strong>{{ valueOrDash(diagnostic.current.locationTime) }}</strong></div>
          <div class="debug-item"><span>locationSource</span><strong>{{ valueOrDash(diagnostic.current.locationSource) }}</strong></div>
          <div class="debug-item"><span>provider</span><strong>{{ valueOrDash(diagnostic.current.provider) }}</strong></div>
          <div class="debug-item"><span>latitude</span><strong>{{ valueOrDash(diagnostic.current.latitude) }}</strong></div>
          <div class="debug-item"><span>longitude</span><strong>{{ valueOrDash(diagnostic.current.longitude) }}</strong></div>
          <div class="debug-item"><span>accuracy</span><strong>{{ valueOrDash(diagnostic.current.accuracyMeters, '米') }}</strong></div>
          <div class="debug-item"><span>原始定位状态</span><strong>{{ valueOrDash(diagnostic.current.rawStatus) }}</strong></div>
          <div class="debug-item"><span>失败阶段 stage</span><strong>{{ valueOrDash(diagnostic.current.stage) }}</strong></div>
          <div class="debug-item"><span>错误码 errorCode</span><strong>{{ valueOrDash(diagnostic.current.errorCode) }}</strong></div>
        </div>
        <div class="debug-raw-message">
          <span>rawMessage</span>
          <p>{{ valueOrDash(diagnostic.current.rawMessage) }}</p>
        </div>
      </section>

      <section class="debug-section">
        <div class="debug-section__title">B. 打卡点信息</div>
        <div class="debug-grid">
          <div class="debug-item"><span>打卡点名称</span><strong>{{ valueOrDash(diagnostic.target.locationName) }}</strong></div>
          <div class="debug-item"><span>打卡点纬度</span><strong>{{ valueOrDash(diagnostic.target.latitude) }}</strong></div>
          <div class="debug-item"><span>打卡点经度</span><strong>{{ valueOrDash(diagnostic.target.longitude) }}</strong></div>
          <div class="debug-item"><span>允许半径</span><strong>{{ valueOrDash(diagnostic.target.radiusMeters, '米') }}</strong></div>
          <div class="debug-item"><span>主打卡点/当前适用</span><strong>{{ valueOrDash(diagnostic.target.primaryText) }}</strong></div>
        </div>
      </section>

      <section class="debug-section">
        <div class="debug-section__title">C. 范围判断信息</div>
        <div class="debug-grid">
          <div class="debug-item debug-item--highlight"><span>当前距离</span><strong>{{ valueOrDash(diagnostic.range.distanceMeters, '米') }}</strong></div>
          <div class="debug-item debug-item--highlight"><span>允许半径</span><strong>{{ valueOrDash(diagnostic.range.radiusMeters, '米') }}</strong></div>
          <div class="debug-item debug-item--highlight"><span>当前判定</span><strong>{{ valueOrDash(diagnostic.range.resultLabel) }}</strong></div>
          <div class="debug-item"><span>判定依据</span><strong>{{ valueOrDash(diagnostic.range.basis) }}</strong></div>
          <div class="debug-item"><span>decisionBranch</span><strong>{{ valueOrDash(diagnostic.range.decisionBranch) }}</strong></div>
        </div>
      </section>

      <section class="debug-section">
        <div class="debug-section__title">D. 容错/回退信息</div>
        <div class="debug-grid">
          <div class="debug-item"><span>fallbackEnabled</span><strong>{{ diagnostic.fallback.enabled ? 'true' : 'false' }}</strong></div>
          <div class="debug-item"><span>本次触发 fallback</span><strong>{{ diagnostic.fallback.triggered ? '是' : '否' }}</strong></div>
          <div class="debug-item"><span>fallbackTarget</span><strong>{{ valueOrDash(diagnostic.fallback.target) }}</strong></div>
          <div class="debug-item"><span>测试环境容错流程</span><strong>{{ diagnostic.fallback.testEnvFallback ? '是' : '否' }}</strong></div>
        </div>
        <div v-if="diagnostic.fallback.triggered" class="debug-warning">
          当前结果来自容错定位，不代表真实位置范围判断。
        </div>
      </section>

      <section class="debug-section">
        <div class="debug-section__title">E. 打卡结果信息</div>
        <div class="debug-grid">
          <div class="debug-item"><span>当前动作</span><strong>{{ valueOrDash(diagnostic.result.actionLabel) }}</strong></div>
          <div class="debug-item"><span>本次提交结果</span><strong>{{ valueOrDash(diagnostic.result.submitResultLabel) }}</strong></div>
          <div class="debug-item"><span>后端 errorCode</span><strong>{{ valueOrDash(diagnostic.result.backendErrorCode) }}</strong></div>
          <div class="debug-item"><span>后端 message</span><strong>{{ valueOrDash(diagnostic.result.backendMessage) }}</strong></div>
          <div class="debug-item"><span>httpStatus</span><strong>{{ valueOrDash(diagnostic.result.httpStatus) }}</strong></div>
        </div>
      </section>

      <div class="debug-actions">
        <van-button size="small" plain @click="$emit('close')">关闭</van-button>
        <van-button size="small" type="primary" :loading="refreshing" @click="$emit('refresh-location')">刷新定位</van-button>
        <van-button size="small" plain type="success" :disabled="!hasMapData" @click="openMapPopup">地图查看</van-button>
      </div>
    </div>
  </van-popup>

  <van-popup
    v-model:show="mapVisible"
    round
    closeable
    position="bottom"
    class="debug-location-map-popup"
    @opened="syncDebugMap"
  >
    <div class="debug-location-map-popup__body">
      <h3 class="debug-location-panel__title">定位地图查看</h3>
      <div v-if="mapError" class="debug-warning">{{ mapError }}</div>
      <div ref="mapRef" class="debug-map"></div>
      <div class="debug-map-summary">
        <div>当前距离 {{ valueOrDash(diagnostic.range.distanceMeters, '米') }}</div>
        <div>允许半径 {{ valueOrDash(diagnostic.range.radiusMeters, '米') }}</div>
        <div>当前判定 {{ valueOrDash(diagnostic.range.resultLabel) }}</div>
      </div>
    </div>
  </van-popup>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { loadAmapSdk } from '@/utils/amap'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  refreshing: {
    type: Boolean,
    default: false
  },
  diagnostic: {
    type: Object,
    default: () => ({
      current: {},
      target: {},
      range: {},
      fallback: {},
      result: {},
      map: {
        current: { latitude: null, longitude: null },
        target: { latitude: null, longitude: null }
      }
    })
  }
})

const emit = defineEmits(['close', 'refresh-location'])

const mapRef = ref(null)
const mapVisible = ref(false)
const mapError = ref('')
let amap = null
let mapInstance = null
let markerCurrent = null
let markerTarget = null
let lineInstance = null

const hasMapData = computed(() => {
  const current = props.diagnostic?.map?.current || {}
  const target = props.diagnostic?.map?.target || {}
  return (
    (Number.isFinite(Number(current.latitude)) && Number.isFinite(Number(current.longitude)))
    || (Number.isFinite(Number(target.latitude)) && Number.isFinite(Number(target.longitude)))
  )
})

function valueOrDash(value, suffix = '') {
  if (value == null || value === '') {
    return '-'
  }
  return `${value}${suffix}`
}

function handleMainPopupToggle(nextShow) {
  if (!nextShow) {
    emit('close')
  }
}

function openMapPopup() {
  mapVisible.value = true
}

async function ensureMapReady() {
  if (!mapRef.value) {
    return null
  }
  if (mapInstance && amap) {
    return mapInstance
  }
  try {
    amap = await loadAmapSdk()
    if (!mapRef.value) {
      return null
    }
    mapInstance = new amap.Map(mapRef.value, {
      zoom: 14,
      center: [118.091519, 24.478829],
      mapStyle: 'amap://styles/normal',
      viewMode: '2D'
    })
    markerCurrent = new amap.Marker({
      anchor: 'bottom-center',
      label: { content: '当前位置', direction: 'top' }
    })
    markerTarget = new amap.Marker({
      anchor: 'bottom-center',
      label: { content: '打卡点', direction: 'top' }
    })
    lineInstance = new amap.Polyline({
      strokeColor: '#0f766e',
      strokeWeight: 3,
      strokeOpacity: 0.85
    })
    mapInstance.add([lineInstance, markerCurrent, markerTarget])
    return mapInstance
  } catch (error) {
    mapError.value = error?.message || '地图加载失败，请检查高德配置'
    return null
  }
}

function toPoint(latitude, longitude) {
  const lat = Number(latitude)
  const lng = Number(longitude)
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) {
    return null
  }
  return [lng, lat]
}

function toggleOverlay(overlay, visible) {
  if (!mapInstance || !overlay) {
    return
  }
  if (visible) {
    mapInstance.add(overlay)
  } else {
    mapInstance.remove(overlay)
  }
}

async function syncDebugMap() {
  mapError.value = ''
  await nextTick()
  const instance = await ensureMapReady()
  if (!instance) {
    return
  }
  const current = props.diagnostic?.map?.current || {}
  const target = props.diagnostic?.map?.target || {}
  const currentPoint = toPoint(current.latitude, current.longitude)
  const targetPoint = toPoint(target.latitude, target.longitude)
  const fitTargets = []
  if (currentPoint) {
    markerCurrent.setPosition(currentPoint)
    toggleOverlay(markerCurrent, true)
    fitTargets.push(markerCurrent)
  } else {
    toggleOverlay(markerCurrent, false)
  }
  if (targetPoint) {
    markerTarget.setPosition(targetPoint)
    toggleOverlay(markerTarget, true)
    fitTargets.push(markerTarget)
  } else {
    toggleOverlay(markerTarget, false)
  }
  if (currentPoint && targetPoint) {
    lineInstance.setPath([currentPoint, targetPoint])
    toggleOverlay(lineInstance, true)
    fitTargets.push(lineInstance)
  } else {
    toggleOverlay(lineInstance, false)
  }
  if (fitTargets.length) {
    instance.setFitView(fitTargets, false, [28, 28, 28, 28])
  }
}

onBeforeUnmount(() => {
  if (mapInstance) {
    mapInstance.destroy()
    mapInstance = null
    amap = null
  }
})

watch(
  () => [
    mapVisible.value,
    props.diagnostic?.map?.current?.latitude,
    props.diagnostic?.map?.current?.longitude,
    props.diagnostic?.map?.target?.latitude,
    props.diagnostic?.map?.target?.longitude
  ],
  () => {
    if (mapVisible.value) {
      syncDebugMap()
    }
  }
)
</script>

<style scoped>
.debug-location-panel,
.debug-location-map-popup {
  max-height: 82vh;
}

.debug-location-panel__body,
.debug-location-map-popup__body {
  padding: 20px 16px 18px;
  overflow: auto;
}

.debug-location-panel__title {
  margin: 0 0 12px;
  font-size: 18px;
  color: #0f172a;
}

.debug-section {
  margin-top: 12px;
  padding: 12px;
  border-radius: 14px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  background: #fff;
}

.debug-section__title {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
  margin-bottom: 10px;
}

.debug-grid {
  display: grid;
  gap: 8px;
}

.debug-item {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: baseline;
  font-size: 13px;
}

.debug-item span {
  color: #64748b;
}

.debug-item strong {
  color: #0f172a;
  text-align: right;
}

.debug-item--highlight strong {
  color: #0e7490;
  font-size: 14px;
}

.debug-raw-message {
  margin-top: 8px;
  padding: 8px 10px;
  border-radius: 10px;
  background: #f8fafc;
}

.debug-raw-message span {
  display: block;
  color: #64748b;
  font-size: 12px;
}

.debug-raw-message p {
  margin: 6px 0 0;
  font-size: 12px;
  color: #334155;
  word-break: break-word;
}

.debug-warning {
  margin-top: 8px;
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid rgba(239, 68, 68, 0.3);
  background: rgba(254, 242, 242, 0.9);
  color: #b91c1c;
  font-size: 12px;
  line-height: 1.5;
}

.debug-actions {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 14px;
}

.debug-map {
  width: 100%;
  height: 260px;
  border-radius: 12px;
  border: 1px solid rgba(148, 163, 184, 0.26);
  background: #f8fafc;
}

.debug-map-summary {
  margin-top: 10px;
  display: grid;
  gap: 4px;
  font-size: 13px;
  color: #334155;
}
</style>

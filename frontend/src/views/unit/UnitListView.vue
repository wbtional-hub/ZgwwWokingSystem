<template>
  <AppPageShell title="单位管理" description="当前页提供单位列表、新增、编辑、删除和启停的最小可维护版本。">
    <template #actions>
      <div class="page-actions">
        <van-button type="primary" :loading="state.loading" @click="loadUnits">刷新</van-button>
        <van-button type="success" @click="openCreateDialog">新增单位</van-button>
      </div>
    </template>

    <div class="page-tip">单位用于管理、筛选、统计和导出；当前阶段仍以组织架构作为主关系树。</div>

    <div class="query-row">
      <van-search
        v-model="state.keyword"
        placeholder="按单位名称或编码搜索"
        @search="normalizeKeyword"
        @blur="normalizeKeyword"
      />
    </div>

    <div v-if="filteredUnits.length" class="summary-row">
      当前共 {{ filteredUnits.length }} 个单位，已启用 {{ enabledCount }} 个，停用 {{ disabledCount }} 个，已配置打卡点 {{ configuredLocationCount }} 个。
    </div>
    <div v-if="state.locationFeedback" class="feedback-row">{{ state.locationFeedback }}</div>

    <van-loading v-if="state.loading" class="page-loading" size="24px" vertical>单位列表加载中...</van-loading>
    <van-empty v-else-if="!filteredUnits.length" description="当前没有可展示的单位数据">
      <template #default>
        <div class="empty-tip">可先新增单位，后续在组织架构中为节点选择所属单位。</div>
      </template>
    </van-empty>

    <div v-else class="unit-list">
      <div v-for="item in filteredUnits" :key="item.id" class="unit-card">
        <div class="unit-title">
          <span>{{ item.unitName }}</span>
          <div class="unit-tags">
            <van-tag :type="Number(item.status) === 1 ? 'success' : 'danger'">
              {{ Number(item.status) === 1 ? '启用' : '停用' }}
            </van-tag>
            <van-tag :type="item.attendanceLocationId ? 'primary' : 'default'">
              {{ item.attendanceLocationId ? '已配置打卡点' : '未配置打卡点' }}
            </van-tag>
          </div>
        </div>
        <div class="unit-meta">单位ID：{{ item.id }}</div>
        <div class="unit-meta">单位编码：{{ item.unitCode }}</div>
        <div class="unit-meta">管理员：{{ item.adminRealName || item.adminUsername || '-' }}</div>
        <div class="location-readonly-card">
          <div class="location-card-title">打卡点详情</div>
          <div class="unit-meta">打卡点名称：{{ item.attendanceLocationName || '-' }}</div>
          <div class="unit-meta">地址：{{ item.attendanceLocationAddress || '-' }}</div>
          <div class="unit-meta">经度：{{ formatCoordinate(item.attendanceLocationLongitude) }}</div>
          <div class="unit-meta">纬度：{{ formatCoordinate(item.attendanceLocationLatitude) }}</div>
          <div class="unit-meta">半径：{{ item.attendanceLocationRadiusMeters ? `${item.attendanceLocationRadiusMeters}米` : '-' }}</div>
          <div class="unit-meta">状态：{{ formatLocationStatus(item.attendanceLocationStatus) }}</div>
        </div>
        <div class="unit-meta">创建时间：{{ formatDateTime(item.createTime) }}</div>
        <div class="unit-actions">
          <van-button size="small" plain type="primary" @click="openEditDialog(item)">编辑</van-button>
          <van-button size="small" plain type="success" @click="openLocationDialog(item)">打卡点设置</van-button>
          <van-button
            size="small"
            plain
            :type="Number(item.status) === 1 ? 'warning' : 'success'"
            @click="handleToggleStatus(item)"
          >
            {{ Number(item.status) === 1 ? '停用' : '启用' }}
          </van-button>
          <van-button size="small" plain type="danger" @click="handleDelete(item)">删除</van-button>
        </div>
      </div>
    </div>

    <van-popup v-model:show="state.dialogVisible" position="bottom" round>
      <div class="dialog-body">
        <div class="dialog-title">{{ state.dialogMode === 'create' ? '新增单位' : '编辑单位' }}</div>
        <van-form @submit="handleSubmit">
          <van-field v-model="state.form.unitName" label="单位名称" placeholder="请输入单位名称" required />
          <van-field v-model="state.form.unitCode" label="单位编码" placeholder="请输入单位编码" required />
          <van-field
            :model-value="Number(state.form.status) === 1 ? '启用' : '停用'"
            label="状态"
            readonly
            is-link
            @click="state.statusPickerVisible = true"
          />
          <div class="dialog-actions">
            <van-button block plain @click="state.dialogVisible = false">取消</van-button>
            <van-button block type="primary" native-type="submit" :loading="state.submitting">保存</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="state.statusPickerVisible" position="bottom" round>
      <van-picker :columns="statusColumns" @confirm="handleStatusConfirm" @cancel="state.statusPickerVisible = false" />
    </van-popup>

    <van-popup v-model:show="state.locationDialogVisible" position="bottom" round @opened="handleLocationPopupOpened">
      <div class="dialog-body">
        <div class="dialog-title">打卡点设置</div>
        <div class="page-tip">当前单位：{{ state.locationForm.unitName || '-' }}</div>
        <div class="location-readonly-card popup-location-card">
          <div class="location-card-title">当前打卡点详情</div>
          <div class="unit-meta">打卡点名称：{{ state.locationDetail.locationName || '-' }}</div>
          <div class="unit-meta">地址：{{ state.locationDetail.address || '-' }}</div>
          <div class="unit-meta">经度：{{ formatCoordinate(state.locationDetail.longitude) }}</div>
          <div class="unit-meta">纬度：{{ formatCoordinate(state.locationDetail.latitude) }}</div>
          <div class="unit-meta">半径：{{ state.locationDetail.radiusMeters ? `${state.locationDetail.radiusMeters}米` : '-' }}</div>
          <div class="unit-meta">状态：{{ formatLocationStatus(state.locationDetail.status) }}</div>
          <div class="unit-meta">更新时间：{{ formatDateTime(state.locationDetail.updateTime) }}</div>
        </div>
        <div class="map-mode-banner" :class="state.mapModeType">
          <div class="map-mode-title">{{ state.mapModeTitle }}</div>
          <div class="map-mode-text">{{ state.mapModeText }}</div>
        </div>
        <van-form @submit="handleLocationSubmit">
          <div class="map-panel">
            <div class="map-panel-header">地图选点区域</div>
            <div class="map-panel-subtitle">先在地图上找点和微调，再往下核对经纬度、地址和半径。</div>
            <div class="map-search-row">
              <van-field
                v-model="state.mapSearchKeyword"
                class="map-search-input"
                label="地址搜索"
                placeholder="输入地址后搜索定位候选点"
                :disabled="state.mapModeType !== 'sdk'"
                @update:model-value="handleSearchKeywordInput"
                @keyup.enter="handleSearchAddress"
              />
              <van-button
                size="small"
                plain
                type="primary"
                :loading="state.mapSearching"
                :disabled="state.mapModeType !== 'sdk'"
                @click.prevent="handleSearchAddress"
              >
                搜索定位
              </van-button>
            </div>
            <div v-if="state.mapModeType === 'sdk' && state.mapSearchCandidates.length" class="map-search-candidate-list">
              <button
                v-for="item in state.mapSearchCandidates"
                :key="item.id"
                type="button"
                class="map-search-candidate"
                :class="{ 'map-search-candidate-active': state.mapSearchSelectedId === item.id }"
                @click="handleUseSearchCandidate(item)"
              >
                <div class="map-search-candidate-title">{{ item.name }}</div>
                <div class="map-search-candidate-address">{{ item.address }}</div>
                <div v-if="state.mapSearchSelectedId === item.id" class="map-search-candidate-selected">已选用当前候选</div>
              </button>
            </div>
            <div class="map-toolbar">
              <van-button size="small" plain type="primary" :loading="state.locating" @click.prevent="handleLocateCurrent">定位当前位置</van-button>
              <span class="map-tip">{{ state.mapTip }}</span>
            </div>
            <div class="map-panel-subtitle">{{ state.mapSdkMessage }}</div>
            <div v-if="state.mapModeType === 'sdk'" :key="state.mapContainerKey" ref="mapContainerRef" class="map-canvas map-canvas-sdk">
              <div v-if="!state.mapSdkReady" class="map-canvas-loading">正式地图加载中...</div>
              <div v-else class="map-control-overlay">
                <div class="map-zoom-controls">
                  <button type="button" class="map-zoom-button" @click.stop="handleMapZoomIn">+</button>
                  <button type="button" class="map-zoom-button" @click.stop="handleMapZoomOut">-</button>
                </div>
                <div class="map-control-tip">可双击、滚轮或双指缩放地图，也可使用右上角 +/- 调整视图大小。</div>
              </div>
            </div>
            <div v-else class="map-canvas" @click="handleMapSelect">
              <div class="map-grid"></div>
              <div
                v-if="state.mapMarker.visible"
                class="map-marker"
                :style="{ left: `${state.mapMarker.x}%`, top: `${state.mapMarker.y}%` }"
              ></div>
              <div class="map-control-overlay map-control-overlay-fallback">
                <div class="map-control-tip">当前为轻量选点模式，可点击区域完成选点；如配置地图 Key，可切换正式地图并缩放拖拽、拖动 marker 微调位置，还支持地址搜索定位。</div>
              </div>
              <div class="map-canvas-text">点击地图区域选点</div>
            </div>
          </div>
          <div class="location-readonly-card popup-location-card">
            <div class="location-card-title">当前选点摘要</div>
            <div class="unit-meta">选点状态：{{ currentPointStatusText }}</div>
            <div class="unit-meta">经度：{{ currentPointLongitudeText }}</div>
            <div class="unit-meta">纬度：{{ currentPointLatitudeText }}</div>
            <div class="unit-meta">地址：{{ state.locationForm.address || '-' }}</div>
            <div class="unit-meta">当前打卡范围半径：{{ currentRadiusText }}</div>
            <div class="unit-meta">视野说明：{{ currentViewportHintText }}</div>
            <div class="unit-meta">{{ currentPointHintText }}</div>
          </div>
          <van-field v-model="state.locationForm.locationName" label="打卡点名称" placeholder="请输入打卡点名称" required />
          <van-field
            v-model="state.locationForm.radiusMeters"
            label="半径(米)"
            type="number"
            placeholder="请输入打卡半径"
            required
            @update:model-value="handleRadiusInput"
          />
          <van-field v-model="state.locationForm.longitude" label="经度" placeholder="点击地图或定位后自动回填" />
          <van-field v-model="state.locationForm.latitude" label="纬度" placeholder="点击地图或定位后自动回填" />
          <van-field v-model="state.locationForm.address" label="地址" rows="2" autosize type="textarea" placeholder="选点后自动回填，可手动调整" />

          <div class="dialog-actions">
            <van-button block plain @click="state.locationDialogVisible = false">取消</van-button>
            <van-button block type="primary" native-type="submit" :loading="state.locationSubmitting">保存打卡点</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { loadAmapSdk } from '@/utils/amap'
import {
  createUnit,
  deleteUnit,
  queryUnitAttendanceLocation,
  queryUnitList,
  saveUnitAttendanceLocation,
  toggleUnitStatus,
  updateUnit
} from '@/api/unit'

const statusColumns = [
  { text: '启用', value: 1 },
  { text: '停用', value: 0 }
]

const DEFAULT_MAP_LONGITUDE = 118.091519
const DEFAULT_MAP_LATITUDE = 24.478829

const state = reactive({
  loading: false,
  submitting: false,
  keyword: '',
  units: [],
  dialogVisible: false,
  dialogMode: 'create',
  statusPickerVisible: false,
  locationDialogVisible: false,
  locationSubmitting: false,
  locating: false,
  mapSearching: false,
  mapSearchCandidates: [],
  mapSearchSelectedId: '',
  mapContainerKey: 0,
  mapSdkReady: false,
  mapSdkMessage: '正在读取地图配置，成功后将加载正式地图；若配置缺失会自动回退轻量选点模式。',
  mapModeType: 'sdk',
  mapModeTitle: '正式地图模式',
  mapModeText: '页面会先从参数管理读取高德地图配置，再注入安全码并加载正式地图 SDK。',
  mapTip: '可先定位当前位置，再点击地图或拖拽 marker 微调坐标。',
  mapSearchKeyword: '',
  locationFeedback: '',
  mapMarker: {
    visible: false,
    x: 50,
    y: 50
  },
  form: {
    id: null,
    unitName: '',
    unitCode: '',
    status: 1
  },
  locationForm: {
    unitId: null,
    unitName: '',
    locationName: '',
    radiusMeters: 100,
    longitude: '',
    latitude: '',
    address: '',
    status: 1
  },
  locationDetail: {
    locationName: '',
    address: '',
    longitude: '',
    latitude: '',
    radiusMeters: null,
    status: null,
    updateTime: ''
  }
})

const mapContainerRef = ref(null)
let mapInstance = null
let mapMarkerInstance = null
let mapCircleInstance = null
let mapRangeLabelInstance = null
let mapGeocoder = null
let mapGeolocation = null
let mapCreateCount = 0

const filteredUnits = computed(() => {
  const keyword = state.keyword.trim()
  if (!keyword) {
    return state.units
  }
  return state.units.filter((item) =>
    String(item.unitName || '').includes(keyword) || String(item.unitCode || '').includes(keyword)
  )
})

const enabledCount = computed(() => filteredUnits.value.filter((item) => Number(item.status) === 1).length)
const disabledCount = computed(() => filteredUnits.value.filter((item) => Number(item.status) !== 1).length)
const configuredLocationCount = computed(() => filteredUnits.value.filter((item) => Boolean(item.attendanceLocationId)).length)

const currentPointReady = computed(() => {
  const longitude = Number(state.locationForm.longitude)
  const latitude = Number(state.locationForm.latitude)
  return !Number.isNaN(longitude) && !Number.isNaN(latitude)
})

const currentPointLongitudeText = computed(() => {
  return currentPointReady.value ? Number(state.locationForm.longitude).toFixed(6) : '-'
})

const currentPointLatitudeText = computed(() => {
  return currentPointReady.value ? Number(state.locationForm.latitude).toFixed(6) : '-'
})

const currentPointStatusText = computed(() => {
  return currentPointReady.value ? '已落点' : '未选点'
})

const currentRadiusText = computed(() => {
  const radius = Number(state.locationForm.radiusMeters)
  return !Number.isNaN(radius) && radius > 0 ? `${radius}米` : '-'
})

const currentViewportHintText = computed(() => {
  return state.mapModeType === 'sdk'
    ? '正式地图模式下会根据当前打卡范围自动调整视野，尽量完整展示 marker 与半径圆。'
    : '当前为轻量选点模式；正式地图模式下会自动预览打卡范围标签并调整视野。'
})

const currentPointHintText = computed(() => {
  if (currentPointReady.value) {
    return state.mapModeType === 'sdk'
      ? '当前 marker、打卡半径圆与范围标签已回显，可继续拖拽缩放地图并点击新位置调整。'
      : '当前轻量选点坐标已生效；正式地图模式下可预览打卡半径圆、范围标签并自动调整视野，还支持拖拽 marker 微调位置与地址搜索定位。'
  }
  return state.mapModeType === 'sdk'
    ? '可先定位当前位置，或拖拽/缩放地图后点击目标位置完成选点；输入半径后会显示打卡范围圆与标签，也可直接拖拽 marker 做小范围微调，或通过地址搜索快速定位。'
    : '当前为轻量选点模式，可先定位当前位置，或点击区域完成选点；正式地图模式下可预览打卡半径圆、范围标签并自动调整视野，还支持拖拽 marker 微调位置与地址搜索定位。'
})

function ensureSuccess(response, fallback = '请求失败') {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || fallback)
  }
  return response.data
}

async function loadUnits() {
  state.loading = true
  try {
    state.units = ensureSuccess(await queryUnitList(), '单位列表加载失败') || []
  } catch (error) {
    showToast(error.message || '单位列表加载失败')
  } finally {
    state.loading = false
  }
}

function normalizeKeyword() {
  state.keyword = state.keyword.trim()
}

function openCreateDialog() {
  state.dialogMode = 'create'
  state.form.id = null
  state.form.unitName = ''
  state.form.unitCode = ''
  state.form.status = 1
  state.dialogVisible = true
}

function openEditDialog(item) {
  state.dialogMode = 'edit'
  state.form.id = item.id
  state.form.unitName = item.unitName || ''
  state.form.unitCode = item.unitCode || ''
  state.form.status = Number(item.status) === 1 ? 1 : 0
  state.dialogVisible = true
}

async function openLocationDialog(item) {
  destroyMapRuntime('before-open')
  state.mapContainerKey += 1
  state.locationFeedback = ''
  state.locationDialogVisible = true
  state.locationForm.unitId = item.id
  state.locationForm.unitName = item.unitName || ''
  state.locationForm.locationName = `${item.unitName || '单位'}主打卡点`
  state.locationForm.radiusMeters = 100
  state.locationForm.longitude = ''
  state.locationForm.latitude = ''
  state.locationForm.address = ''
  state.locationForm.status = 1
  state.mapSearchKeyword = ''
  clearSearchCandidates()
  resetLocationDetail()
  state.mapMarker.visible = false
  state.mapMarker.x = 50
  state.mapMarker.y = 50
  state.mapTip = '可先定位当前位置，再点击地图或拖拽 marker 微调坐标。'
  state.mapSdkReady = false
  state.mapSdkMessage = '正在读取地图配置，成功后将加载正式地图；若配置缺失会自动回退轻量选点模式。'
  state.mapModeType = 'sdk'
  state.mapModeTitle = '正式地图模式'
  state.mapModeText = '页面会先从参数管理读取高德地图配置，再注入安全码并加载正式地图 SDK。'
  try {
    const data = ensureSuccess(await queryUnitAttendanceLocation(item.id), '打卡点加载失败')
    if (data) {
      applyLocationDetail(data)
      state.locationForm.locationName = data.locationName || state.locationForm.locationName
      state.locationForm.radiusMeters = Number(data.radiusMeters || 100)
      clearSearchCandidates()
      state.locationForm.status = Number(data.status ?? 1)
      if (data.longitude != null && data.latitude != null) {
        fillLocation({
          longitude: data.longitude,
          latitude: data.latitude,
          address: data.address || '已加载当前单位主打卡点',
          tipText: '已加载当前单位主打卡点，可继续点击地图调整。'
        })
        state.mapSearchKeyword = data.address || `${Number(data.latitude).toFixed(6)}, ${Number(data.longitude).toFixed(6)}`
      }
    }
  } catch (error) {
    showToast(error.message || '打卡点加载失败')
  }
  if (!currentPointReady.value) {
    applyDefaultLocationPreset()
  }
}

async function handleLocationPopupOpened() {
  if (!state.locationDialogVisible) {
    return
  }
  await nextTick()
  await waitForMapContainerVisible()
  const ready = await ensureMapReady()
  if (!ready) {
    return
  }
  triggerLocationStyleRefresh('popup-opened')
  await ensureMapResized('popup-opened')
  setTimeout(() => {
    triggerLocationStyleRefresh('popup-opened-300ms')
    ensureMapResized('popup-opened-300ms')
  }, 300)
  setTimeout(() => {
    triggerLocationStyleRefresh('popup-opened-800ms')
    ensureMapResized('popup-opened-800ms')
  }, 800)
  syncSdkMarkerFromForm()
}

function resetLocationDetail() {
  state.locationDetail.locationName = ''
  state.locationDetail.address = ''
  state.locationDetail.longitude = ''
  state.locationDetail.latitude = ''
  state.locationDetail.radiusMeters = null
  state.locationDetail.status = null
  state.locationDetail.updateTime = ''
}

function applyLocationDetail(data) {
  state.locationDetail.locationName = data?.locationName || ''
  state.locationDetail.address = data?.address || ''
  state.locationDetail.longitude = data?.longitude ?? ''
  state.locationDetail.latitude = data?.latitude ?? ''
  state.locationDetail.radiusMeters = data?.radiusMeters ?? null
  state.locationDetail.status = data?.status ?? null
  state.locationDetail.updateTime = data?.updateTime || data?.createTime || ''
}

async function handleSubmit() {
  const payload = {
    id: state.form.id,
    unitName: state.form.unitName.trim(),
    unitCode: state.form.unitCode.trim(),
    status: state.form.status
  }
  if (!payload.unitName || !payload.unitCode) {
    showToast('请先填写单位名称和单位编码')
    return
  }
  state.submitting = true
  try {
    if (state.dialogMode === 'create') {
      ensureSuccess(await createUnit(payload), '新增单位失败')
      showToast('新增成功')
    } else {
      ensureSuccess(await updateUnit(payload), '编辑单位失败')
      showToast('保存成功')
    }
    state.dialogVisible = false
    await loadUnits()
  } catch (error) {
    showToast(error.message || '单位保存失败')
  } finally {
    state.submitting = false
  }
}

async function handleToggleStatus(item) {
  try {
    await showConfirmDialog({
      title: Number(item.status) === 1 ? '停用单位' : '启用单位',
      message: `确认${Number(item.status) === 1 ? '停用' : '启用'}单位“${item.unitName}”吗？`
    })
    ensureSuccess(await toggleUnitStatus({ id: item.id, status: Number(item.status) === 1 ? 0 : 1 }), '更新状态失败')
    showToast('状态已更新')
    await loadUnits()
  } catch (error) {
    if (error?.message) {
      showToast(error.message)
    }
  }
}

async function handleDelete(item) {
  try {
    await showConfirmDialog({
      title: '删除单位',
      message: `确认删除单位“${item.unitName}”吗？`
    })
    ensureSuccess(await deleteUnit(item.id), '删除单位失败')
    showToast('删除成功')
    await loadUnits()
  } catch (error) {
    if (error?.message) {
      showToast(error.message)
    }
  }
}

function handleStatusConfirm(option) {
  state.form.status = Number(Array.isArray(option) ? option[0]?.selectedValues?.[0] : option.selectedValues?.[0] ?? option.value)
  state.statusPickerVisible = false
}

function fillLocation({ longitude, latitude, address, x = 50, y = 50, tipText = '' }) {
  state.locationForm.longitude = Number(longitude).toFixed(6)
  state.locationForm.latitude = Number(latitude).toFixed(6)
  state.locationForm.address = address || state.locationForm.address
  state.mapMarker.visible = true
  state.mapMarker.x = x
  state.mapMarker.y = y
  state.mapTip = tipText || `已选点：经度 ${state.locationForm.longitude}，纬度 ${state.locationForm.latitude}`
  syncSdkMarkerFromForm()
}

function applyDefaultLocationPreset() {
  fillLocation({
    longitude: DEFAULT_MAP_LONGITUDE,
    latitude: DEFAULT_MAP_LATITUDE,
    address: '默认地图中心点',
    tipText: `已使用默认中心点：经度 ${DEFAULT_MAP_LONGITUDE.toFixed(6)}，纬度 ${DEFAULT_MAP_LATITUDE.toFixed(6)}`
  })
  state.mapSearchKeyword = '默认地图中心点'
}

function clearSearchCandidates() {
  state.mapSearchCandidates = []
  state.mapSearchSelectedId = ''
}

function resolveCurrentViewportCenter() {
  const longitude = Number(state.locationForm.longitude)
  const latitude = Number(state.locationForm.latitude)
  if (!Number.isNaN(longitude) && !Number.isNaN(latitude)) {
    return [longitude, latitude]
  }
  return [DEFAULT_MAP_LONGITUDE, DEFAULT_MAP_LATITUDE]
}

function normalizeGeocodeCandidate(item, index) {
  const location = item?.location
  const longitude = typeof location?.getLng === 'function' ? location.getLng() : location?.lng
  const latitude = typeof location?.getLat === 'function' ? location.getLat() : location?.lat
  return {
    id: `${item?.formattedAddress || item?.location || 'candidate'}-${index}`,
    name: item?.formattedAddress || item?.district || item?.city || `候选点 ${index + 1}`,
    address: [item?.province, item?.city, item?.district, item?.township, item?.street, item?.number]
      .filter(Boolean)
      .join('') || item?.formattedAddress || '未返回格式化地址',
    formattedAddress: item?.formattedAddress || '',
    longitude,
    latitude
  }
}

function applySearchCandidate(item, tipText) {
  fillLocation({
    longitude: item.longitude,
    latitude: item.latitude,
    address: item.formattedAddress || item.address || state.mapSearchKeyword,
    x: 50,
    y: 50,
    tipText
  })
  state.mapSearchKeyword = item.formattedAddress || item.address || state.mapSearchKeyword
  state.mapSearchSelectedId = item.id
  if (mapInstance) {
    mapInstance.setCenter([item.longitude, item.latitude])
    mapInstance.setZoom(17)
  }
}

function handleSearchKeywordInput(value) {
  state.mapSearchKeyword = value
  clearSearchCandidates()
}

function handleUseSearchCandidate(item) {
  if (!item || item.longitude == null || item.latitude == null) {
    showToast('候选点缺少有效坐标')
    return
  }
  applySearchCandidate(item, `已选用候选点：经度 ${Number(item.longitude).toFixed(6)}，纬度 ${Number(item.latitude).toFixed(6)}`)
}

function handleRadiusInput() {
  syncSdkCircleFromForm()
}

async function handleSearchAddress() {
  const keyword = state.mapSearchKeyword.trim()
  if (state.mapModeType !== 'sdk') {
    showToast('当前为轻量选点模式，正式地图模式才支持地址搜索定位')
    return
  }
  if (!keyword) {
    showToast('请先输入地址关键词')
    return
  }
  if (!mapGeocoder) {
    showToast('地图搜索能力尚未就绪，请稍后重试')
    return
  }
  state.mapSearching = true
  try {
    const geocodes = await new Promise((resolve, reject) => {
      mapGeocoder.getLocation(keyword, (status, result) => {
        if (status === 'complete' && Array.isArray(result?.geocodes) && result.geocodes.length) {
          resolve(result.geocodes)
          return
        }
        reject(new Error('未找到匹配的地址候选点'))
      })
    })
    const candidates = geocodes
      .map((item, index) => normalizeGeocodeCandidate(item, index))
      .filter((item) => item.longitude != null && item.latitude != null)
      .slice(0, 5)
    if (!candidates.length) {
      throw new Error('地址解析成功，但未返回有效候选坐标')
    }
    state.mapSearchCandidates = candidates
    applySearchCandidate(candidates[0], `已快速选用首个候选点：经度 ${Number(candidates[0].longitude).toFixed(6)}，纬度 ${Number(candidates[0].latitude).toFixed(6)}`)
  } catch (error) {
    showToast(error.message || '地址搜索失败，请换个关键词重试')
    state.mapTip = '地址搜索未命中，当前已选点保持不变。'
  } finally {
    state.mapSearching = false
  }
}

async function handleLocateCurrent() {
  if (state.mapSdkReady && mapGeolocation) {
    state.locating = true
    try {
      const result = await new Promise((resolve, reject) => {
        mapGeolocation.getCurrentPosition((status, response) => {
          if (status === 'complete' && response?.position) {
            resolve(response)
            return
          }
          reject(new Error('地图定位失败'))
        })
      })
      const longitude = result.position.lng
      const latitude = result.position.lat
      const address = result.formattedAddress || await reverseGeocode(longitude, latitude)
    fillLocation({
      longitude,
      latitude,
      address: address || `当前位置 ${latitude.toFixed(6)}, ${longitude.toFixed(6)}`,
      x: 50,
      y: 50,
      tipText: `已定位当前位置：经度 ${longitude.toFixed(6)}，纬度 ${latitude.toFixed(6)}`
    })
    state.mapSearchKeyword = address || `${latitude.toFixed(6)}, ${longitude.toFixed(6)}`
    clearSearchCandidates()
    if (mapInstance) {
        mapInstance.setCenter([longitude, latitude])
        mapInstance.setZoom(16)
      }
      return
    } catch (error) {
      state.mapTip = '地图定位失败，已切换浏览器定位。'
    } finally {
      state.locating = false
    }
  }
  if (!navigator.geolocation) {
    showToast('当前环境不支持定位')
    return
  }
  state.locating = true
  try {
    const position = await new Promise((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 10000 })
    })
    const address = await reverseGeocode(position.coords.longitude, position.coords.latitude)
    fillLocation({
      longitude: position.coords.longitude,
      latitude: position.coords.latitude,
      address: address || `当前位置 ${position.coords.latitude.toFixed(6)}, ${position.coords.longitude.toFixed(6)}`,
      x: 50,
      y: 50,
      tipText: `已定位当前位置：经度 ${position.coords.longitude.toFixed(6)}，纬度 ${position.coords.latitude.toFixed(6)}`
    })
    state.mapSearchKeyword = address || `${position.coords.latitude.toFixed(6)}, ${position.coords.longitude.toFixed(6)}`
    clearSearchCandidates()
    if (mapInstance) {
      mapInstance.setCenter([position.coords.longitude, position.coords.latitude])
      mapInstance.setZoom(16)
    }
  } catch (error) {
    showToast('定位失败，请手动点击地图选点')
  } finally {
    state.locating = false
  }
}

function handleMapSelect(event) {
  if (state.mapSdkReady) {
    return
  }
  const rect = event.currentTarget.getBoundingClientRect()
  const relativeX = Math.min(Math.max((event.clientX - rect.left) / rect.width, 0), 1)
  const relativeY = Math.min(Math.max((event.clientY - rect.top) / rect.height, 0), 1)
  const baseLongitude = Number(state.locationForm.longitude || DEFAULT_MAP_LONGITUDE)
  const baseLatitude = Number(state.locationForm.latitude || DEFAULT_MAP_LATITUDE)
  const longitude = baseLongitude + (relativeX - 0.5) * 0.02
  const latitude = baseLatitude + (0.5 - relativeY) * 0.02
  fillLocation({
    longitude,
    latitude,
    address: `地图选点 ${latitude.toFixed(6)}, ${longitude.toFixed(6)}`,
    x: relativeX * 100,
    y: relativeY * 100,
    tipText: `已点击地图选点：经度 ${longitude.toFixed(6)}，纬度 ${latitude.toFixed(6)}`
  })
  state.mapSearchKeyword = `地图选点 ${latitude.toFixed(6)}, ${longitude.toFixed(6)}`
  clearSearchCandidates()
}

function handleMapZoomIn() {
  if (!state.mapSdkReady || !mapInstance) {
    return
  }
  mapInstance.zoomIn()
}

function handleMapZoomOut() {
  if (!state.mapSdkReady || !mapInstance) {
    return
  }
  mapInstance.zoomOut()
}

async function handleLocationSubmit() {
  const payload = {
    unitId: state.locationForm.unitId,
    locationName: state.locationForm.locationName.trim(),
    radiusMeters: Number(state.locationForm.radiusMeters || 100),
    longitude: Number(state.locationForm.longitude),
    latitude: Number(state.locationForm.latitude),
    address: state.locationForm.address.trim(),
    status: Number(state.locationForm.status || 1)
  }
  if (!payload.locationName) {
    showToast('请先填写打卡点名称')
    return
  }
  if (Number.isNaN(payload.longitude) || Number.isNaN(payload.latitude)) {
    showToast('请先定位或点击地图选点')
    return
  }
  state.locationSubmitting = true
  try {
    ensureSuccess(await saveUnitAttendanceLocation(payload), '打卡点保存失败')
    await loadUnits()
    const latestDetail = ensureSuccess(await queryUnitAttendanceLocation(payload.unitId), '打卡点详情同步失败')
    if (latestDetail) {
      applyLocationDetail(latestDetail)
      state.locationForm.locationName = latestDetail.locationName || state.locationForm.locationName
      state.locationForm.radiusMeters = Number(latestDetail.radiusMeters || state.locationForm.radiusMeters || 100)
      state.locationForm.longitude = String(latestDetail.longitude ?? state.locationForm.longitude)
      state.locationForm.latitude = String(latestDetail.latitude ?? state.locationForm.latitude)
      state.locationForm.address = latestDetail.address || state.locationForm.address
      state.mapSearchKeyword = latestDetail.address || state.mapSearchKeyword
      clearSearchCandidates()
      state.locationForm.status = Number(latestDetail.status ?? state.locationForm.status ?? 1)
      syncSdkMarkerFromForm()
    }
    state.locationFeedback = `单位“${state.locationForm.unitName}”打卡点已保存，列表与当前详情卡片均已同步。`
    showToast('打卡点已保存')
  } catch (error) {
    showToast(error.message || '打卡点保存失败')
  } finally {
    state.locationSubmitting = false
  }
}

async function ensureMapReady() {
  if (!state.locationDialogVisible || !mapContainerRef.value) {
    return false
  }
  if (mapInstance) {
    logMapDiagnostics('reuse-existing')
    state.mapSdkReady = true
    state.mapSdkMessage = '正式地图已加载，可直接点选位置、拖拽 marker 微调、地址搜索定位，并自动回填地址。'
    state.mapModeType = 'sdk'
    state.mapModeTitle = '正式地图模式'
    state.mapModeText = '地图 SDK 已加载完成，可直接点击地图选点、拖拽 marker 微调、地址搜索定位，并自动回填地址。'
    await ensureMapResized('reuse-existing')
    return true
  }
  try {
    const AMap = await loadAmapSdk()
    if (!(await waitForMapContainerVisible())) {
      throw new Error('地图容器尚未完成显示，暂无法初始化地图')
    }
    console.info('[UnitMap] creating map instance', {
      createCount: mapCreateCount + 1,
      hasKey: true,
      hasSecurityJsCode: true,
      containerWidth: mapContainerRef.value?.clientWidth || 0,
      containerHeight: mapContainerRef.value?.clientHeight || 0
    })
    mapInstance = new AMap.Map(mapContainerRef.value, {
      zoom: 15,
      center: [DEFAULT_MAP_LONGITUDE, DEFAULT_MAP_LATITUDE],
      viewMode: '2D',
      mapStyle: 'amap://styles/normal'
    })
    mapCreateCount += 1
    mapInstance.on('complete', () => {
      console.info('[AMap] map initialized', {
        hasSecurityJsCode: true
      })
      refreshInitialViewport('complete')
      setTimeout(() => {
        refreshInitialViewport('complete-delayed-100ms')
      }, 100)
      setTimeout(() => {
        refreshInitialViewport('complete-delayed-300ms')
      }, 300)
      logMapDiagnostics('complete')
    })
    mapInstance.on('tilesloaded', () => {
      logMapDiagnostics('tilesloaded')
    })
    mapMarkerInstance = new AMap.Marker({
      offset: new AMap.Pixel(-13, -30),
      draggable: true,
      cursor: 'move'
    })
    mapCircleInstance = new AMap.Circle({
      strokeColor: '#2563eb',
      strokeWeight: 2,
      strokeOpacity: 0.8,
      fillColor: '#60a5fa',
      fillOpacity: 0.18
    })
    mapRangeLabelInstance = new AMap.Text({
      text: '',
      anchor: 'bottom-center',
      offset: new AMap.Pixel(0, -18),
      style: {
        padding: '6px 10px',
        borderRadius: '999px',
        border: '1px solid #bfdbfe',
        backgroundColor: 'rgba(255, 255, 255, 0.94)',
        color: '#0f172a',
        fontSize: '12px',
        fontWeight: '600',
        boxShadow: '0 8px 24px rgba(15, 23, 42, 0.12)'
      }
    })
    mapInstance.add(mapMarkerInstance)
    mapInstance.add(mapCircleInstance)
    mapInstance.add(mapRangeLabelInstance)
    mapInstance.on('click', async (event) => {
      const longitude = event.lnglat.getLng()
      const latitude = event.lnglat.getLat()
      const address = await reverseGeocode(longitude, latitude)
      fillLocation({
        longitude,
        latitude,
        address: address || `地图选点 ${latitude.toFixed(6)}, ${longitude.toFixed(6)}`,
        x: 50,
        y: 50,
        tipText: `已点击地图选点：经度 ${longitude.toFixed(6)}，纬度 ${latitude.toFixed(6)}`
      })
      state.mapSearchKeyword = address || `地图选点 ${latitude.toFixed(6)}, ${longitude.toFixed(6)}`
      clearSearchCandidates()
      mapInstance.setCenter([longitude, latitude])
    })
    mapMarkerInstance.on('dragend', async (event) => {
      const longitude = event.lnglat.getLng()
      const latitude = event.lnglat.getLat()
      const address = await reverseGeocode(longitude, latitude)
      fillLocation({
        longitude,
        latitude,
        address: address || `拖拽微调 ${latitude.toFixed(6)}, ${longitude.toFixed(6)}`,
        x: 50,
        y: 50,
        tipText: `已拖拽 marker 微调：经度 ${longitude.toFixed(6)}，纬度 ${latitude.toFixed(6)}`
      })
      state.mapSearchKeyword = address || `拖拽微调 ${latitude.toFixed(6)}, ${longitude.toFixed(6)}`
      clearSearchCandidates()
    })
    mapGeocoder = new AMap.Geocoder()
    mapGeolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000
    })
    mapInstance.addControl(mapGeolocation)
    state.mapSdkReady = true
    state.mapSdkMessage = '正式地图已加载，可直接点选位置、拖拽 marker 微调、地址搜索定位，并自动回填地址。'
    state.mapModeType = 'sdk'
    state.mapModeTitle = '正式地图模式'
    state.mapModeText = '地图 SDK 已加载完成，可直接点击地图选点、拖拽 marker 微调、地址搜索定位，并自动回填地址。'
    await ensureMapResized('post-init')
    setTimeout(() => {
      ensureMapResized('delayed-post-init')
    }, 50)
    logMapDiagnostics('post-init')
    return true
  } catch (error) {
    state.mapSdkReady = false
    state.mapSdkMessage = error.message || '地图 SDK 加载失败，当前改用轻量选点模式。'
    state.mapModeType = 'fallback'
    state.mapModeTitle = '轻量选点模式'
    state.mapModeText = '地图 SDK 加载失败，当前已自动回退到轻量选点模式；仍可继续选点并保存打卡点。'
    return false
  }
}

async function ensureMapResized(reason = 'manual') {
  if (!mapInstance) {
    return
  }
  await nextTick()
  requestAnimationFrame(() => {
    mapInstance?.resize()
    logMapDiagnostics(`resize:${reason}`)
  })
}

async function waitForMapContainerVisible(maxAttempts = 10) {
  for (let index = 0; index < maxAttempts; index += 1) {
    await nextTick()
    const width = mapContainerRef.value?.clientWidth || 0
    const height = mapContainerRef.value?.clientHeight || 0
    if (width > 0 && height > 0) {
      console.info('[UnitMap] container visible', { width, height, attempt: index + 1 })
      return true
    }
    await new Promise((resolve) => {
      setTimeout(resolve, 30)
    })
  }
  console.warn('[UnitMap] container still not visible before init', {
    width: mapContainerRef.value?.clientWidth || 0,
    height: mapContainerRef.value?.clientHeight || 0
  })
  return false
}

function getMapLayerCount() {
  if (!mapInstance || typeof mapInstance.getLayers !== 'function') {
    return null
  }
  const layers = mapInstance.getLayers()
  if (Array.isArray(layers)) {
    return layers.length
  }
  if (layers && typeof layers.getLength === 'function') {
    return layers.getLength()
  }
  return null
}

function refreshInitialViewport(stage = 'manual') {
  if (!mapInstance) {
    return
  }
  const center = resolveCurrentViewportCenter()
  const zoom = typeof mapInstance.getZoom === 'function' ? mapInstance.getZoom() : 15
  if (typeof mapInstance.setZoomAndCenter === 'function') {
    mapInstance.setZoomAndCenter(zoom, center)
  } else {
    mapInstance.setCenter(center)
    mapInstance.setZoom(zoom)
  }
  console.info('[UnitMap] viewport refreshed', {
    stage,
    zoom,
    center
  })
}

function triggerLocationStyleRefresh(stage = 'manual') {
  syncSdkMarkerFromForm()
  refreshInitialViewport(stage)
}

function logMapDiagnostics(stage) {
  if (!mapContainerRef.value) {
    console.info('[UnitMap] diagnostics', { stage, hasContainer: false, hasMap: Boolean(mapInstance) })
    return
  }
  const center = mapInstance && typeof mapInstance.getCenter === 'function' ? mapInstance.getCenter() : null
  console.info('[UnitMap] diagnostics', {
    stage,
    hasContainer: true,
    hasMap: Boolean(mapInstance),
    createCount: mapCreateCount,
    width: mapContainerRef.value.clientWidth || 0,
    height: mapContainerRef.value.clientHeight || 0,
    zoom: mapInstance && typeof mapInstance.getZoom === 'function' ? mapInstance.getZoom() : null,
    center: center ? {
      lng: typeof center.getLng === 'function' ? center.getLng() : center.lng,
      lat: typeof center.getLat === 'function' ? center.getLat() : center.lat
    } : null,
    layerCount: getMapLayerCount()
  })
}

function destroyMapRuntime(reason = 'manual-destroy') {
  if (mapInstance) {
    console.info('[UnitMap] destroying map instance', { reason, createCount: mapCreateCount })
    mapInstance.destroy()
  }
  mapInstance = null
  mapMarkerInstance = null
  mapCircleInstance = null
  mapRangeLabelInstance = null
  mapGeocoder = null
  mapGeolocation = null
  state.mapSdkReady = false
}

function syncSdkMarkerFromForm() {
  if (!state.mapSdkReady || !mapMarkerInstance || !mapInstance) {
    return
  }
  const longitude = Number(state.locationForm.longitude)
  const latitude = Number(state.locationForm.latitude)
  if (Number.isNaN(longitude) || Number.isNaN(latitude)) {
    mapMarkerInstance.hide()
    syncSdkCircleFromForm()
    return
  }
  mapMarkerInstance.setPosition([longitude, latitude])
  mapMarkerInstance.show()
  mapInstance.setCenter([longitude, latitude])
  syncSdkCircleFromForm()
}

function syncSdkCircleFromForm() {
  if (!state.mapSdkReady || !mapCircleInstance || !mapInstance || !mapRangeLabelInstance) {
    return
  }
  const longitude = Number(state.locationForm.longitude)
  const latitude = Number(state.locationForm.latitude)
  const radius = Number(state.locationForm.radiusMeters)
  if (Number.isNaN(longitude) || Number.isNaN(latitude)) {
    mapCircleInstance.hide()
    mapRangeLabelInstance.hide()
    return
  }
  if (Number.isNaN(radius) || radius <= 0) {
    mapCircleInstance.hide()
    mapRangeLabelInstance.hide()
    mapInstance.setCenter([longitude, latitude])
    return
  }
  mapCircleInstance.setCenter([longitude, latitude])
  mapCircleInstance.setRadius(radius)
  mapCircleInstance.show()
  mapRangeLabelInstance.setPosition([longitude, latitude])
  mapRangeLabelInstance.setText(`打卡范围 ${Math.round(radius)} 米`)
  mapRangeLabelInstance.show()
  if (typeof mapInstance.setFitView === 'function') {
    mapInstance.setFitView([mapMarkerInstance, mapCircleInstance], false, [60, 60, 60, 60])
    return
  }
  mapInstance.setCenter([longitude, latitude])
}

async function reverseGeocode(longitude, latitude) {
  if (!mapGeocoder) {
    return ''
  }
  try {
    return await new Promise((resolve) => {
      mapGeocoder.getAddress([longitude, latitude], (status, result) => {
        if (status === 'complete' && result?.regeocode?.formattedAddress) {
          resolve(result.regeocode.formattedAddress)
          return
        }
        resolve('')
      })
    })
  } catch (error) {
    return ''
  }
}

function formatLocationSummary(item) {
  if (!item.attendanceLocationId) {
    return '未配置'
  }
  const status = Number(item.attendanceLocationStatus) === 1 ? '启用' : '停用'
  const radius = item.attendanceLocationRadiusMeters ? `${item.attendanceLocationRadiusMeters}米` : '-'
  return `${status} / 半径 ${radius} / ${item.attendanceLocationAddress || '未填写地址'}`
}

function formatCoordinate(value) {
  if (value == null || value === '') {
    return '-'
  }
  return Number(value).toFixed(6)
}

function formatLocationStatus(status) {
  if (status == null) {
    return '-'
  }
  return Number(status) === 1 ? '启用' : '停用'
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  const hour = `${date.getHours()}`.padStart(2, '0')
  const minute = `${date.getMinutes()}`.padStart(2, '0')
  return `${month}-${day} ${hour}:${minute}`
}

onMounted(() => {
  loadUnits()
})

watch(
  () => state.locationDialogVisible,
  (visible) => {
    if (visible) {
      return
    }
    destroyMapRuntime('dialog-close')
  }
)

onBeforeUnmount(() => {
  destroyMapRuntime('component-unmount')
})
</script>

<style scoped>
.page-actions,
.unit-actions,
.dialog-actions {
  display: flex;
  gap: 12px;
}

.page-tip,
.summary-row,
.empty-tip,
.feedback-row {
  margin: 12px 0;
  font-size: 14px;
}

.page-tip,
.summary-row,
.empty-tip {
  color: #666;
}

.feedback-row {
  padding: 10px 12px;
  border-radius: 10px;
  background: #ecfdf5;
  color: #166534;
}

.unit-list {
  display: grid;
  gap: 12px;
}

.unit-card,
.dialog-body {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
}

.unit-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
  font-size: 16px;
  font-weight: 600;
}

.unit-tags {
  display: flex;
  gap: 8px;
}

.unit-meta {
  margin-top: 4px;
  color: #666;
  font-size: 13px;
}

.location-readonly-card {
  margin-top: 12px;
  padding: 12px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.location-card-title {
  margin-bottom: 8px;
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
}

.popup-location-card {
  margin-bottom: 16px;
}

.map-mode-banner {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 10px;
  border: 1px solid #dbeafe;
  background: #eff6ff;
}

.map-mode-banner.fallback {
  border-color: #fde68a;
  background: #fffbeb;
}

.map-mode-title {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.map-mode-text {
  margin-top: 6px;
  color: #475569;
  font-size: 13px;
  line-height: 1.5;
}

.unit-actions,
.dialog-actions {
  margin-top: 16px;
}

.dialog-title {
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
}

.map-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.map-search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0 0;
}

.map-search-input {
  flex: 1;
}

.map-search-candidate-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.map-search-candidate {
  padding: 10px 12px;
  border: 1px solid #dbeafe;
  border-radius: 10px;
  background: #f8fbff;
  text-align: left;
}

.map-search-candidate-active {
  border-color: #2563eb;
  background: #eff6ff;
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.08);
}

.map-search-candidate-title {
  color: #0f172a;
  font-size: 13px;
  font-weight: 600;
}

.map-search-candidate-address {
  margin-top: 4px;
  color: #475569;
  font-size: 12px;
  line-height: 1.5;
}

.map-search-candidate-selected {
  margin-top: 6px;
  color: #1d4ed8;
  font-size: 12px;
  font-weight: 600;
}

.map-tip {
  color: #666;
  font-size: 12px;
}

.map-panel {
  margin-top: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 12px;
  background: #f8fafc;
}

.map-panel-header {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.map-panel-subtitle {
  margin-top: 6px;
  color: #64748b;
  font-size: 12px;
  line-height: 1.5;
}

.map-canvas {
  position: relative;
  margin-top: 12px;
  min-height: 360px;
  height: 48vh;
  max-height: 520px;
  overflow: hidden;
  border-radius: 12px;
  background: linear-gradient(135deg, #dbeafe, #ecfeff);
  cursor: crosshair;
}

.map-canvas-sdk {
  border: 1px solid #bfdbfe;
  background: transparent;
}

.map-canvas-loading {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(219, 234, 254, 0.92), rgba(236, 254, 255, 0.92));
  color: #0f172a;
  font-size: 14px;
  font-weight: 600;
  z-index: 1;
}

.map-canvas-sdk :deep(.amap-maps),
.map-canvas-sdk :deep(.amap-layers),
.map-canvas-sdk :deep(.amap-layer) {
  width: 100% !important;
  height: 100% !important;
  visibility: visible !important;
  opacity: 1 !important;
}

.map-canvas-sdk :deep(.amap-layer),
.map-canvas-sdk :deep(.amap-layer canvas),
.map-canvas-sdk :deep(.amap-layer img),
.map-canvas-sdk :deep(.amap-tile) {
  display: block !important;
  visibility: visible !important;
  opacity: 1 !important;
}

.map-canvas-sdk :deep(canvas),
.map-canvas-sdk :deep(img) {
  max-width: none;
}

.map-control-overlay {
  position: absolute;
  top: 12px;
  right: 12px;
  left: 12px;
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
  pointer-events: none;
  z-index: 2;
}

.map-control-overlay-fallback {
  justify-content: flex-end;
}

.map-zoom-controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: auto;
}

.map-zoom-button {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.92);
  color: #0f172a;
  font-size: 22px;
  line-height: 1;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.12);
}

.map-control-tip {
  max-width: 260px;
  padding: 8px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.92);
  color: #0f172a;
  font-size: 12px;
  line-height: 1.5;
}

.map-grid {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(to right, rgba(15, 23, 42, 0.08) 1px, transparent 1px),
    linear-gradient(to bottom, rgba(15, 23, 42, 0.08) 1px, transparent 1px);
  background-size: 24px 24px;
}

.map-marker {
  position: absolute;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #ef4444;
  border: 3px solid #fff;
  box-shadow: 0 6px 20px rgba(239, 68, 68, 0.35);
  transform: translate(-50%, -50%);
}

.map-canvas-text {
  position: absolute;
  right: 12px;
  bottom: 12px;
  padding: 4px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.9);
  color: #0f172a;
  font-size: 12px;
}

@media (max-width: 768px) {
  .map-search-row,
  .map-toolbar {
    align-items: flex-start;
    flex-direction: column;
  }

  .map-canvas {
    min-height: 320px;
    height: 42vh;
    max-height: 460px;
  }

  .map-control-overlay {
    flex-direction: column;
    align-items: flex-end;
  }

  .map-control-tip {
    max-width: none;
  }
}
</style>

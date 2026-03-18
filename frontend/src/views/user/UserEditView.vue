<template>
  <AppPageShell class="user-page" title="用户管理" :description="pageDescription">
    <template #actions>
      <div class="header-actions">
        <van-button plain type="primary" :loading="store.loading" :disabled="headerBlocked" @click="handleRefreshList">刷新</van-button>
        <van-button type="primary" :disabled="headerBlocked" @click="openCreateDialog">新增用户</van-button>
      </div>
    </template>

    <div class="module-tip">
      当前模块作为后台模板页，已覆盖列表、筛选、详情、表单和关键操作闭环。
    </div>

    <div class="module-overview">
      {{ moduleOverviewText }}
    </div>

    <div v-if="routeSourceHint" class="module-tip module-tip--source">
      {{ routeSourceHint }}
      <router-link v-if="returnToUnitsRoute" class="module-tip-link" :to="returnToUnitsRoute">返回来源单位</router-link>
    </div>

    <div class="module-overview">
      页面骨架：
      查询区已接入，操作按钮区已接入，列表内容区已接入，空状态提示已接入。
    </div>

    <van-search
      v-model="searchForm.keywords"
      placeholder="按账号/姓名/手机号搜索"
      show-action
      :disabled="filterBlocked"
      :readonly="filterBlocked"
      @blur="normalizeKeywords"
      @search="handleSearch"
      @cancel="handleReset"
    />

    <div class="filter-row">
      <van-dropdown-menu>
        <van-dropdown-item v-model="searchForm.status" :options="statusOptions" :disabled="filterBlocked" />
      </van-dropdown-menu>
      <van-button size="small" type="primary" :loading="store.loading" :disabled="filterBlocked" @click="handleSearch">查询</van-button>
      <van-button v-if="filterSummary" size="small" plain :disabled="filterBlocked" @click="handleReset">重置筛选</van-button>
    </div>

    <div v-if="filterSummary" class="filter-summary">
      当前筛选：{{ filterSummary }}
    </div>

    <div class="page-summary">
      <span>共 {{ store.total }} 人</span>
      <span>当前第 {{ store.pageNo }} 页</span>
      <span>{{ pageRangeText }}</span>
    </div>

    <div v-if="store.list.length" class="page-summary page-summary--subtle">
      <span>本页显示 {{ store.list.length }} 人</span>
      <span>本页已挂接组织树 {{ mountedOrgCount }} 人</span>
      <span>本页未挂接组织树 {{ unmountedOrgCount }} 人</span>
      <span>本页启用 {{ enabledCount }} 人</span>
      <span>本页停用 {{ disabledCount }} 人</span>
      <span>本页已填岗位 {{ jobFilledCount }} 人</span>
      <span>本页已填手机号 {{ mobileFilledCount }} 人</span>
      <span>本页待补资料 {{ incompleteProfileCount }} 人</span>
      <span>本页本人 {{ currentLoginUserInPageCount }} 人</span>
      <span>本页近期更新 {{ recentlyUpdatedCount }} 人</span>
      <span>{{ pageStatusSummaryText }}</span>
      <span>{{ pageProfileSummaryText }}</span>
      <span v-if="routeTargetPageHint">{{ routeTargetPageHint }}</span>
      <span v-if="routeTargetPageLabel">{{ routeTargetPageLabel }}</span>
    </div>

    <div class="sync-summary">
      列表最后同步：{{ lastSyncTime }}
    </div>

    <div v-if="detailVisible && store.currentDetail" class="focus-summary">
      正在查看：{{ store.currentDetail.realName || store.currentDetail.username }}{{ store.currentDetail.username ? ` / ${store.currentDetail.username}` : '' }}
      <span v-if="isCurrentLoginUser(store.currentDetail.id)">，当前登录用户</span>
      <span v-if="currentDetailIndex >= 0">，本页第 {{ currentDetailIndex + 1 }} / {{ store.list.length }} 位</span>
      <span>，当前状态：{{ Number(store.currentDetail.status) === 1 ? '启用' : '停用' }}</span>
      <span v-if="sourceUnitLabel">，来源单位：{{ sourceUnitLabel }}</span>
      <span v-if="sourceUnitKeywordLabel">，单位搜索：{{ sourceUnitKeywordLabel }}</span>
    </div>

    <van-loading v-if="store.loading" class="page-loading" size="24px" vertical>加载中...</van-loading>

    <template v-else>
      <van-empty v-if="!store.list.length" description="暂无用户数据">
        <template #default>
          <div v-if="filterSummary" class="empty-tip">
            当前筛选下暂无结果，可先重置后再继续验收。
          </div>
          <div v-if="sourceUnitLabel" class="empty-tip">
            当前结果来自单位 {{ sourceUnitLabel }}，可直接回到来源单位继续查看。
          </div>
          <div v-if="sourceUnitKeywordLabel" class="empty-tip">
            来源单位搜索：{{ sourceUnitKeywordLabel }}。
          </div>
          <div class="empty-actions">
            <van-button size="small" plain type="primary" :disabled="listBlocked" @click="handleReset">清空筛选</van-button>
            <router-link v-if="returnToUnitsRoute" class="empty-link-button" :to="returnToUnitsRoute">返回单位管理</router-link>
            <van-button size="small" type="primary" :disabled="listBlocked" @click="openCreateDialog">新增用户</van-button>
          </div>
        </template>
      </van-empty>

      <div v-else class="user-list">
        <van-card
          v-for="(item, index) in store.list"
          :key="item.id"
          class="user-card"
          :class="{ 'user-card--active': detailVisible && store.currentDetail?.id === item.id }"
        >
          <template #title>
            <div class="user-card-title user-card-title--clickable" :class="{ 'user-card-title--disabled': listBlocked }" @click="handleDetailShortcut(item.id)">
              <span>#{{ (store.pageNo - 1) * store.pageSize + index + 1 }} {{ item.realName }}<span class="title-sub"> / {{ item.username }}</span></span>
              <div class="title-tags">
                <van-tag v-if="routeTargetUserId === item.id" plain type="warning">目标</van-tag>
                <van-tag v-if="isCurrentLoginUser(item.id)" plain type="primary">本人</van-tag>
                <van-tag :type="Number(item.status) === 1 ? 'success' : 'danger'">
                  {{ Number(item.status) === 1 ? '启用' : '停用' }}
                </van-tag>
                <van-tag plain :type="item.levelNo ? 'success' : 'default'">
                  {{ item.levelNo ? '已挂接' : '未挂接' }}
                </van-tag>
                <van-tag plain :type="item.mobile ? 'primary' : 'default'">
                  {{ item.mobile ? '已填手机' : '缺手机号' }}
                </van-tag>
                <van-tag plain :type="item.jobTitle ? 'primary' : 'default'">
                  {{ item.jobTitle ? '已填岗位' : '缺岗位' }}
                </van-tag>
                <van-tag plain :type="userProfileCompletion(item) === 2 ? 'success' : 'warning'">
                  {{ userProfileCompletion(item) === 2 ? '资料完整' : '待补资料' }}
                </van-tag>
                <van-tag plain :type="isRecentlyUpdated(item) ? 'success' : 'default'">
                  {{ isRecentlyUpdated(item) ? '近期更新' : '待核对更新' }}
                </van-tag>
              </div>
            </div>
          </template>
          <template #desc>
            <div class="user-meta">列表定位：本页第 {{ index + 1 }} 位</div>
            <div class="user-meta user-meta--link" @click="copyText(item.username, '账号')">账号：{{ item.username }}</div>
            <div class="user-meta">
              所属单位：{{ item.unitName || `单位ID ${item.unitId ?? '-'}` }}
            </div>
            <div class="user-meta user-meta--link" @click="copyText(item.unitId, '单位ID')">单位ID：{{ item.unitId }}</div>
            <div class="user-meta">
              组织关系：{{ item.levelNo ? `已挂接，层级 ${item.levelNo}` : '未展示层级信息' }}
            </div>
            <div class="user-meta">岗位：{{ item.jobTitle || '-' }}</div>
            <div
              class="user-meta"
              :class="{ 'user-meta--link': Boolean(item.mobile) }"
              @click="item.mobile && copyText(item.mobile, '手机号')"
            >
              手机号：{{ item.mobile || '-' }}
            </div>
            <div class="user-meta">资料完成度：{{ userProfileCompletion(item) }} / 2</div>
            <div class="user-meta">资料缺口：{{ userProfileGapText(item) }}</div>
            <div class="user-meta">最近更新：{{ formatDateTime(item.updateTime) }}</div>
            <div class="user-meta">更新状态：{{ userUpdateFreshnessText(item) }}</div>
            <div class="user-meta">账号状态摘要：{{ userStatusSummaryText(item) }}</div>
            <div v-if="isCurrentLoginUser(item.id)" class="user-meta user-meta--hint">
              当前登录账号在本页第 {{ index + 1 }} 位。
            </div>
            <div v-if="routeTargetUserId === item.id" class="user-meta user-meta--hint">
              当前卡片就是本次来源链路命中的目标用户。
            </div>
            <div v-if="isCurrentLoginUser(item.id)" class="user-meta user-meta--hint">
              当前登录账号不可删除、不可重置密码。
            </div>
          </template>
          <template #footer>
            <div class="card-actions">
              <van-button
                size="small"
                plain
                type="primary"
                :disabled="listBlocked || (actingUserId === item.id && actingType !== '')"
                @click="handleDetailShortcut(item.id)"
              >
                详情
              </van-button>
              <van-button
                size="small"
                plain
                type="warning"
                :disabled="listBlocked || (actingUserId === item.id && actingType !== '')"
                @click="openEditDialog(item.id)"
              >
                编辑
              </van-button>
              <van-button
                size="small"
                plain
                :type="Number(item.status) === 1 ? 'danger' : 'success'"
                :loading="actingUserId === item.id && actingType === 'toggleStatus'"
                :disabled="listBlocked || isCurrentLoginUser(item.id) || (actingUserId === item.id && actingType !== '' && actingType !== 'toggleStatus')"
                @click="handleToggleStatus(item)"
              >
                {{ Number(item.status) === 1 ? '停用' : '启用' }}
              </van-button>
              <van-button
                size="small"
                plain
                type="success"
                :loading="actingUserId === item.id && actingType === 'resetPassword'"
                :disabled="listBlocked || (actingUserId === item.id && actingType === 'delete') || isCurrentLoginUser(item.id)"
                @click="handleResetPassword(item.id)"
              >
                重置密码
              </van-button>
              <van-button
                size="small"
                plain
                type="danger"
                :loading="actingUserId === item.id && actingType === 'delete'"
                :disabled="listBlocked || (actingUserId === item.id && actingType === 'resetPassword') || isCurrentLoginUser(item.id)"
                @click="handleDelete(item.id)"
              >
                删除
              </van-button>
            </div>
          </template>
        </van-card>
      </div>

      <div class="pagination-wrap">
        <van-pagination
          v-model="store.pageNo"
          :total-items="store.total"
          :items-per-page="store.pageSize"
          :disabled="listBlocked"
          mode="simple"
          @change="handlePageChange"
        />
      </div>
    </template>

    <UserFormDialog
      v-model="dialog.visible"
      :mode="dialog.mode"
      :form-data="dialog.formData"
      :saving="store.submitting"
      @submit="handleSubmit"
    />

    <van-popup v-model:show="detailVisible" position="bottom" round :close-on-click-overlay="!pageBusy">
      <div class="detail-panel">
        <div class="detail-title">
          用户详情
          <span v-if="store.currentDetail?.realName" class="detail-title-sub">
            · {{ store.currentDetail.realName }}{{ store.currentDetail?.username ? ` / ${store.currentDetail.username}` : '' }}
          </span>
          <span v-if="store.currentDetail?.id && isCurrentLoginUser(store.currentDetail.id)" class="detail-title-sub"> · 当前登录用户</span>
          <span v-if="currentDetailIndex >= 0" class="detail-title-sub"> · 本页第 {{ currentDetailIndex + 1 }} / {{ store.list.length }} 位</span>
        </div>
        <van-loading v-if="store.detailLoading" size="24px" vertical>加载中...</van-loading>
        <template v-else-if="store.currentDetail">
          <div class="detail-summary">
            <div class="summary-title">组织关系摘要</div>
            <div class="summary-text">
              {{
                store.currentDetail.treePath
                  ? `当前已挂接组织树，层级 ${store.currentDetail.levelNo ?? '-'}，上级 ${store.currentDetail.parentUserId ?? '-'}`
                  : '当前详情页仅展示组织树字段，关系维护请走组织架构模块。'
              }}
            </div>
            <div class="summary-text">所属单位：{{ store.currentDetail.unitName || store.currentDetail.unitId || '-' }}</div>
            <div class="summary-text">当前状态：{{ Number(store.currentDetail.status) === 1 ? '启用' : '停用' }}</div>
            <div v-if="isCurrentLoginUser(store.currentDetail.id)" class="summary-text">
              当前账号就是本次登录账号，用户页已限制删除、重置密码与停用自己。
            </div>
            <div class="summary-text">最近更新时间：{{ formatDateTime(store.currentDetail.updateTime) }}</div>
            <div class="summary-text">岗位：{{ store.currentDetail.jobTitle || '未填写' }}</div>
            <div class="summary-text">联系方式：{{ store.currentDetail.mobile ? '已填写手机号' : '未填写手机号' }}</div>
            <div class="summary-text">关键信息完成度：{{ detailCompletionText }}</div>
            <div class="summary-text">{{ detailBrowseHint }}</div>
            <div v-if="sourceUnitLabel" class="summary-text">当前详情来源单位：{{ sourceUnitLabel }}。</div>
            <div v-if="sourceUnitKeywordLabel" class="summary-text">来源单位搜索：{{ sourceUnitKeywordLabel }}。</div>
            <div v-if="returnToUnitsRoute" class="summary-text">
              当前详情来自单位管理，可直接返回来源单位继续查看。
            </div>
            <div v-if="previousUserLabel || nextUserLabel" class="summary-text">
              邻近用户：上一位 {{ previousUserLabel || '无' }}，下一位 {{ nextUserLabel || '无' }}
            </div>
            <div v-if="detailRemainingHint" class="summary-text">{{ detailRemainingHint }}</div>
            <div v-if="detailEdgeHint" class="summary-text">{{ detailEdgeHint }}</div>
          </div>
          <van-cell title="用户ID" is-link :value="store.currentDetail.id" @click="copyText(store.currentDetail.id, '用户ID')" />
          <van-cell title="所属单位" :value="store.currentDetail.unitName || '-'" />
          <van-cell title="单位ID" is-link :value="store.currentDetail.unitId" @click="copyText(store.currentDetail.unitId, '单位ID')" />
          <van-cell title="账号" is-link :value="store.currentDetail.username" @click="copyText(store.currentDetail.username, '账号')" />
          <van-cell title="姓名" :value="store.currentDetail.realName" />
          <van-cell title="上级ID" :value="store.currentDetail.parentUserId ?? '未接入'" />
          <van-cell title="层级" :value="store.currentDetail.levelNo ?? '未接入'" />
          <van-cell
            title="树路径"
            :is-link="Boolean(store.currentDetail.treePath)"
            :value="store.currentDetail.treePath || '未接入'"
            @click="copyText(store.currentDetail.treePath, '树路径')"
          />
          <van-cell title="岗位名称" :value="store.currentDetail.jobTitle || '-'" />
          <van-cell
            title="手机号"
            :is-link="Boolean(store.currentDetail.mobile)"
            :value="store.currentDetail.mobile || '-'"
            @click="copyText(store.currentDetail.mobile, '手机号')"
          />
          <van-cell title="状态" :value="Number(store.currentDetail.status) === 1 ? '启用' : '停用'" />
          <van-cell title="创建时间" :value="formatDateTime(store.currentDetail.createTime)" />
          <van-cell title="更新时间" :value="formatDateTime(store.currentDetail.updateTime)" />
          <div class="detail-actions">
            <div class="detail-switch">
              <van-button plain block :disabled="pageBusy || !previousUserId" @click="handleDetailShortcut(previousUserId, { allowFromDetail: true })">
                {{ previousButtonText }}
              </van-button>
              <van-button plain block :disabled="pageBusy || !nextUserId" @click="handleDetailShortcut(nextUserId, { allowFromDetail: true })">
                {{ nextButtonText }}
              </van-button>
            </div>
            <van-button plain block type="primary" :loading="store.detailLoading" :disabled="pageBusy" @click="refreshCurrentDetail">
              刷新当前详情
            </van-button>
            <router-link v-if="returnToUnitsRoute" class="detail-link-button" :to="returnToUnitsRoute">返回来源单位</router-link>
            <van-button plain block type="warning" :disabled="pageBusy" @click="openEditFromDetail">编辑当前用户</van-button>
            <van-button
              block
              type="primary"
              :loading="store.submitting"
              :disabled="pageBusy || isCurrentLoginUser(store.currentDetail.id)"
              @click="handleResetPassword(store.currentDetail.id, { allowFromDetail: true })"
            >
              重置当前用户密码
            </van-button>
            <van-button plain block :disabled="pageBusy" @click="backToListTop">回到列表顶部</van-button>
            <van-button plain block :disabled="pageBusy" @click="closeDetail">关闭详情</van-button>
          </div>
        </template>
        <van-empty v-else description="当前详情暂不可用">
          <template #default>
            <div class="empty-tip">
              {{ invalidDetailTip }}
            </div>
            <div class="empty-actions">
              <router-link v-if="returnToUnitsRoute" class="empty-link-button" :to="returnToUnitsRoute">返回来源单位</router-link>
              <van-button size="small" plain type="primary" :disabled="pageBusy" @click="closeDetail">关闭详情</van-button>
              <van-button size="small" type="primary" :disabled="pageBusy" @click="refreshListFromDetailEmptyState">刷新列表</van-button>
            </div>
          </template>
        </van-empty>
      </div>
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { useRoute, useRouter } from 'vue-router'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import UserFormDialog from '@/components/user/UserFormDialog.vue'
import { useUserManagementStore } from '@/stores/user-management'
import { useUserStore } from '@/stores/user'

const store = useUserManagementStore()
const userStore = useUserStore()
const route = useRoute()
const router = useRouter()
const detailVisible = ref(false)
const lastSyncTime = ref('-')
const actingUserId = ref(null)
const actingType = ref('')
const resettingFilters = ref(false)
const searchForm = reactive({
  keywords: '',
  status: ''
})
const dialog = reactive({
  visible: false,
  mode: 'create',
  editingId: null,
  formData: {},
  restoreDetailId: null,
  restoringAfterSubmit: false
})
const statusOptions = [
  { text: '全部状态', value: '' },
  { text: '启用', value: 1 },
  { text: '停用', value: 0 }
]
const pageDescription = '后台模板模块，已接入列表筛选、详情浏览、表单维护与关键操作。'
const moduleOverviewText = computed(() => `当前共 ${store.total} 人，位于第 ${store.pageNo} 页${filterSummary.value ? `，筛选条件：${filterSummary.value}` : '，当前未使用筛选条件'}。`)
const routeSourceHint = computed(() => {
  if (route.query.from !== 'units') {
    return ''
  }
  const unitId = typeof route.query.unitId === 'string' ? route.query.unitId : ''
  const unitKeyword = typeof route.query.unitKeyword === 'string' ? route.query.unitKeyword.trim() : ''
  const directUserId = resolveRouteUserId(route.query.userId)
  const directUserLabel = store.currentDetail && directUserId && Number(store.currentDetail.id) === directUserId
    ? `${store.currentDetail.realName || store.currentDetail.username}${store.currentDetail.username ? ` / ${store.currentDetail.username}` : ''}`
    : ''
  if (unitId && directUserId) {
    return directUserLabel
      ? `当前内容来自单位管理页，来源单位：${unitId}${unitKeyword ? `，单位搜索：${unitKeyword}` : ''}，并已直达 ${directUserLabel} 的详情。`
      : `当前内容来自单位管理页，来源单位：${unitId}${unitKeyword ? `，单位搜索：${unitKeyword}` : ''}，并已按目标用户直达详情。`
  }
  if (unitId) {
    return hasReturnUserContext.value
      ? `当前筛选来自单位管理页，来源单位：${unitId}${unitKeyword ? `，单位搜索：${unitKeyword}` : ''}，并保留了返回用户工作面的上下文。`
      : `当前筛选来自单位管理页，来源单位：${unitId}${unitKeyword ? `，单位搜索：${unitKeyword}` : ''}。`
  }
  if (directUserId) {
    return '当前内容来自单位管理页，并已按目标用户直达详情。'
  }
  return '当前筛选来自单位管理页。'
})
const returnToUnitsRoute = computed(() => {
  if (route.query.from !== 'units') {
    return null
  }
  const query = { from: 'users' }
  if (typeof route.query.unitId === 'string' && route.query.unitId.trim()) {
    query.unitId = route.query.unitId.trim()
  }
  if (typeof route.query.unitKeyword === 'string' && route.query.unitKeyword.trim()) {
    query.keyword = route.query.unitKeyword.trim()
  }
  if (searchForm.keywords.trim()) {
    query.returnKeywords = searchForm.keywords.trim()
  }
  if (searchForm.status !== '') {
    query.returnStatus = String(searchForm.status)
  }
  const activeUserId = detailVisible.value && store.currentDetail?.id
    ? String(store.currentDetail.id)
    : typeof route.query.userId === 'string' && route.query.userId.trim()
      ? route.query.userId.trim()
      : ''
  if (activeUserId) {
    query.returnUserId = activeUserId
  }
  return { path: '/units', query }
})
const returnToUsersRoute = computed(() => {
  if (route.query.from !== 'users') {
    return null
  }
  const query = {}
  if (typeof route.query.unitId === 'string' && route.query.unitId.trim()) {
    query.unitId = route.query.unitId.trim()
  }
  return { path: '/users', query }
})
const hasReturnUserContext = computed(() => (
  Boolean(returnToUsersRoute.value)
  || Boolean(typeof route.query.unitKeyword === 'string' && route.query.unitKeyword.trim())
))
const filterSummary = computed(() => {
  const parts = []
  if (searchForm.keywords.trim()) {
    parts.push(`关键词：${searchForm.keywords.trim()}`)
  }
  if (searchForm.status !== '') {
    parts.push(`状态：${Number(searchForm.status) === 1 ? '启用' : '停用'}`)
  }
  return parts.join('，')
})
const currentDetailIndex = computed(() =>
  store.list.findIndex((item) => item.id === store.currentDetail?.id)
)
const previousUserId = computed(() =>
  currentDetailIndex.value > 0 ? store.list[currentDetailIndex.value - 1]?.id : null
)
const nextUserId = computed(() =>
  currentDetailIndex.value >= 0 && currentDetailIndex.value < store.list.length - 1
    ? store.list[currentDetailIndex.value + 1]?.id
    : null
)
const previousUserLabel = computed(() => {
  if (currentDetailIndex.value <= 0) {
    return ''
  }
  const user = store.list[currentDetailIndex.value - 1]
  return user ? `${user.realName || user.username}${user.username ? ` / ${user.username}` : ''}` : ''
})
const nextUserLabel = computed(() => {
  if (currentDetailIndex.value < 0 || currentDetailIndex.value >= store.list.length - 1) {
    return ''
  }
  const user = store.list[currentDetailIndex.value + 1]
  return user ? `${user.realName || user.username}${user.username ? ` / ${user.username}` : ''}` : ''
})
const detailEdgeHint = computed(() => {
  if (store.list.length <= 1 || currentDetailIndex.value < 0) {
    return ''
  }
  if (currentDetailIndex.value === 0) {
    return '当前已到本页首位，可继续查看下一位。'
  }
  if (currentDetailIndex.value === store.list.length - 1) {
    return '当前已到本页末位，可回看上一位。'
  }
  return ''
})
const detailBrowseHint = computed(() => (
  store.list.length > 1
    ? '可使用下方“上一位 / 下一位”在当前页内连续浏览。'
    : '当前页仅此一位用户，暂无同页邻近用户可切换。'
))
const detailRemainingHint = computed(() => {
  if (store.list.length <= 1 || currentDetailIndex.value < 0) {
    return ''
  }
  return `前方剩余 ${currentDetailIndex.value} 位，后方剩余 ${store.list.length - currentDetailIndex.value - 1} 位。`
})
const previousButtonText = computed(() => (
  previousUserLabel.value ? `上一位 · ${previousUserLabel.value}` : '已到首位'
))
const nextButtonText = computed(() => (
  nextUserLabel.value ? `下一位 · ${nextUserLabel.value}` : '已到末位'
))
const sourceUnitLabel = computed(() => (
  route.query.from === 'units' && typeof route.query.unitId === 'string' && route.query.unitId.trim()
    ? route.query.unitId.trim()
    : ''
))
const sourceUnitKeywordLabel = computed(() => (
  route.query.from === 'units' && typeof route.query.unitKeyword === 'string' && route.query.unitKeyword.trim()
    ? route.query.unitKeyword.trim()
    : ''
))
const invalidDetailTip = computed(() => {
  const routeUserId = resolveRouteUserId(route.query.userId)
  if (routeUserId && sourceUnitLabel.value) {
    return `来源单位 ${sourceUnitLabel.value} 的目标用户详情（ID ${routeUserId}）暂不可用，可关闭后重新从列表进入。`
  }
  return routeUserId
    ? `目标用户详情（ID ${routeUserId}）暂不可用，可关闭后重新从列表进入。`
    : '当前详情已失效，可关闭后重新从列表进入。'
})
const currentLoginUserId = computed(() => userStore.userInfo?.userId ?? null)
const pageBusy = computed(() => store.loading || store.submitting || store.detailLoading)
const interactionBlocked = computed(() => pageBusy.value || dialog.visible)
const headerBlocked = computed(() => interactionBlocked.value || detailVisible.value)
const filterBlocked = computed(() => interactionBlocked.value || detailVisible.value)
const listBlocked = computed(() => interactionBlocked.value || detailVisible.value)
const detailCompletionText = computed(() => {
  const detail = store.currentDetail
  if (!detail) {
    return '-'
  }
  const checks = [
    Boolean(detail.realName),
    Boolean(detail.jobTitle),
    Boolean(detail.mobile)
  ]
  const done = checks.filter(Boolean).length
  return `${done} / ${checks.length}`
})
const enabledCount = computed(() => store.list.filter((item) => Number(item.status) === 1).length)
const disabledCount = computed(() => store.list.filter((item) => Number(item.status) !== 1).length)
const mountedOrgCount = computed(() => store.list.filter((item) => item.levelNo != null && item.levelNo !== '').length)
const unmountedOrgCount = computed(() => store.list.length - mountedOrgCount.value)
const jobFilledCount = computed(() => store.list.filter((item) => Boolean(item.jobTitle)).length)
const mobileFilledCount = computed(() => store.list.filter((item) => Boolean(item.mobile)).length)
const incompleteProfileCount = computed(() => store.list.filter((item) => userProfileCompletion(item) < 2).length)
const currentLoginUserInPageCount = computed(() => store.list.filter((item) => isCurrentLoginUser(item.id)).length)
const recentlyUpdatedCount = computed(() => store.list.filter((item) => isRecentlyUpdated(item)).length)
const pageRangeText = computed(() => {
  if (!store.total || !store.list.length) {
    return '当前页暂无可展示记录'
  }
  const start = (store.pageNo - 1) * store.pageSize + 1
  const end = start + store.list.length - 1
  return `当前展示第 ${start}-${end} 条`
})
const routeTargetUserId = computed(() => resolveRouteUserId(route.query.userId))
const routeTargetIndex = computed(() =>
  store.list.findIndex((item) => item.id === routeTargetUserId.value)
)
const pageStatusSummaryText = computed(() => `本页状态结构：启用 ${enabledCount.value} / 停用 ${disabledCount.value}`)
const pageProfileSummaryText = computed(() => `本页资料完整 ${store.list.filter((item) => userProfileCompletion(item) === 2).length} 人`)
const routeTargetPageHint = computed(() => {
  if (!routeTargetUserId.value || !store.list.length) {
    return ''
  }
  if (routeTargetIndex.value >= 0) {
    return `目标用户在本页第 ${routeTargetIndex.value + 1} 位`
  }
  return `目标用户 ID ${routeTargetUserId.value} 不在当前页`
})
const routeTargetPageLabel = computed(() => {
  if (routeTargetIndex.value < 0) {
    return ''
  }
  const user = store.list[routeTargetIndex.value]
  if (!user) {
    return ''
  }
  return `目标用户：${user.realName || user.username}${user.username ? ` / ${user.username}` : ''}`
})

onMounted(() => {
  applyRouteFilters(route.query)
})

watch(
  () => searchForm.status,
  (value, oldValue) => {
    if (value === oldValue || resettingFilters.value) {
      return
    }
    fetchPage({ pageNo: 1 })
  }
)

watch(
  () => [route.query.keywords, route.query.status, route.query.userId],
  () => {
    applyRouteFilters(route.query)
  }
)

watch(detailVisible, (value) => {
  if (!value) {
    syncUnitsRouteDetail(null)
    store.clearDetail()
  }
})

watch(
  () => dialog.visible,
  async (value) => {
    if (value) {
      return
    }
    const restoreDetailId = dialog.restoringAfterSubmit ? null : dialog.restoreDetailId
    dialog.mode = 'create'
    dialog.editingId = null
    dialog.formData = {}
    dialog.restoreDetailId = null
    dialog.restoringAfterSubmit = false
    if (restoreDetailId != null) {
      await showDetail(restoreDetailId)
    }
  }
)

watch(
  () => store.list,
  (list) => {
    if (!detailVisible.value || !store.currentDetail?.id) {
      return
    }
    const existsInCurrentPage = list.some((item) => item.id === store.currentDetail.id)
    if (!existsInCurrentPage) {
      detailVisible.value = false
      store.clearDetail()
    }
  },
  { deep: true }
)

async function fetchPage(extra = {}) {
  if (pageBusy.value) {
    return
  }
  try {
    if (Object.prototype.hasOwnProperty.call(extra, 'keywords') && typeof extra.keywords === 'string') {
      searchForm.keywords = extra.keywords.trim()
    }
    await store.fetchPage({
      keywords: searchForm.keywords,
      status: searchForm.status,
      ...extra
    })
    await syncDetailFromRoute(extra)
    lastSyncTime.value = formatDateTime(new Date().toISOString())
  } catch (error) {
    showToast(error.message || '用户列表加载失败')
  }
}

function applyRouteFilters(query) {
  const nextKeywords = typeof query?.keywords === 'string' ? query.keywords.trim() : ''
  const nextStatus = query?.status === '1' || query?.status === 1
    ? 1
    : query?.status === '0' || query?.status === 0
      ? 0
      : ''
  const noFilterChange = searchForm.keywords === nextKeywords && searchForm.status === nextStatus
  resettingFilters.value = true
  searchForm.keywords = nextKeywords
  searchForm.status = nextStatus
  resettingFilters.value = false
  if (!store.list.length || !noFilterChange) {
    fetchPage({ pageNo: 1, keywords: nextKeywords, status: nextStatus })
    return
  }
  syncDetailFromRoute()
}

function userProfileCompletion(user) {
  return [Boolean(user?.jobTitle), Boolean(user?.mobile)].filter(Boolean).length
}

function userProfileGapText(user) {
  const gaps = []
  if (!user?.jobTitle) {
    gaps.push('岗位')
  }
  if (!user?.mobile) {
    gaps.push('手机号')
  }
  return gaps.length ? gaps.join('、') : '无'
}

function userUpdateFreshnessText(user) {
  if (!user?.updateTime) {
    return '暂无更新时间'
  }
  const updatedAt = new Date(user.updateTime).getTime()
  if (Number.isNaN(updatedAt)) {
    return '更新时间格式异常'
  }
  const diffHours = Math.floor((Date.now() - updatedAt) / (1000 * 60 * 60))
  if (diffHours < 24) {
    return '24 小时内更新'
  }
  if (diffHours < 24 * 7) {
    return '7 天内更新'
  }
  return '7 天前更新'
}

function isRecentlyUpdated(user) {
  if (!user?.updateTime) {
    return false
  }
  const updatedAt = new Date(user.updateTime).getTime()
  if (Number.isNaN(updatedAt)) {
    return false
  }
  return Date.now() - updatedAt < 1000 * 60 * 60 * 24 * 7
}

function userStatusSummaryText(user) {
  const parts = [Number(user?.status) === 1 ? '启用' : '停用']
  parts.push(user?.levelNo ? '已挂接' : '未挂接')
  return parts.join(' / ')
}

function handleRefreshList() {
  if (headerBlocked.value) {
    return
  }
  fetchPage()
}

function handleSearch() {
  if (filterBlocked.value) {
    return
  }
  normalizeKeywords()
  const changed = syncListRouteQuery({
    keywords: searchForm.keywords,
    status: searchForm.status,
    userId: route.query.userId
  })
  if (!changed) {
    fetchPage({ pageNo: 1, keywords: searchForm.keywords })
  }
}

function normalizeKeywords() {
  if (pageBusy.value) {
    return
  }
  searchForm.keywords = searchForm.keywords.trim()
}

async function handleReset() {
  if (filterBlocked.value) {
    return
  }
  resettingFilters.value = true
  searchForm.keywords = ''
  searchForm.status = ''
  try {
    const changed = syncListRouteQuery({
      keywords: '',
      status: '',
      userId: route.query.userId
    })
    if (!changed) {
      await fetchPage({ pageNo: 1, keywords: '', status: '' })
    }
  } finally {
    resettingFilters.value = false
  }
}

function handlePageChange(pageNo) {
  if (listBlocked.value || Number(pageNo) === Number(store.pageNo)) {
    return
  }
  fetchPage({ pageNo })
  scrollToTop()
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ').slice(0, 16)
}

async function copyText(value, label) {
  if (!value || pageBusy.value) {
    return
  }
  try {
    if (navigator?.clipboard?.writeText) {
      await navigator.clipboard.writeText(String(value))
    } else {
      const input = document.createElement('textarea')
      input.value = String(value)
      input.setAttribute('readonly', 'readonly')
      input.style.position = 'fixed'
      input.style.opacity = '0'
      document.body.appendChild(input)
      input.select()
      document.execCommand('copy')
      document.body.removeChild(input)
    }
    showToast(`${label}已复制`)
  } catch (error) {
    showToast(`${label}复制失败，请手动复制`)
  }
}

function scrollToTop() {
  if (pageBusy.value) {
    return
  }
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function backToListTop() {
  if (pageBusy.value) {
    return
  }
  closeDetail()
  scrollToTop()
}

function openCreateDialog() {
  if (headerBlocked.value) {
    return
  }
  if (dialog.visible && dialog.mode === 'create') {
    return
  }
  const defaultUnitId = store.currentDetail?.unitId || sourceUnitLabel.value
  store.clearDetail()
  detailVisible.value = false
  dialog.visible = true
  dialog.mode = 'create'
  dialog.editingId = null
  dialog.formData = defaultUnitId != null
    ? { unitId: defaultUnitId }
    : {}
}

async function openEditDialog(userId, options = {}) {
  if (interactionBlocked.value || (!options.allowFromDetail && listBlocked.value)) {
    return
  }
  if (dialog.visible && dialog.mode === 'edit' && Number(dialog.editingId) === Number(userId)) {
    return
  }
  try {
    const detail = options.detail || (store.currentDetail?.id === userId
      ? store.currentDetail
      : await store.fetchDetail(userId))
    detailVisible.value = false
    dialog.visible = true
    dialog.mode = 'edit'
    dialog.editingId = userId
    dialog.restoreDetailId = options.restoreDetailOnClose ? userId : null
    dialog.restoringAfterSubmit = false
    dialog.formData = {
      ...detail,
      isCurrentLoginUser: isCurrentLoginUser(userId)
    }
  } catch (error) {
    showToast(error.message || '用户详情加载失败')
  }
}

async function showDetail(userId) {
  if (interactionBlocked.value) {
    return
  }
  if (detailVisible.value && store.currentDetail?.id === userId) {
    return
  }
  detailVisible.value = true
  try {
    await store.fetchDetail(userId)
    syncUnitsRouteDetail(userId)
  } catch (error) {
    detailVisible.value = false
    showToast(error.message || '用户详情加载失败')
  }
}

async function syncDetailFromRoute(extra = {}) {
  const routeUserId = resolveRouteUserId(extra.userId ?? route.query.userId)
  if (!routeUserId || dialog.visible) {
    return
  }
  if (detailVisible.value && Number(store.currentDetail?.id) === routeUserId) {
    return
  }
  await showDetail(routeUserId)
}

function handleDetailShortcut(userId, options = {}) {
  if (!userId || (!options.allowFromDetail && listBlocked.value) || (options.allowFromDetail && pageBusy.value)) {
    return
  }
  showDetail(userId)
}

async function openEditFromDetail() {
  if (!store.currentDetail?.id || pageBusy.value) {
    return
  }
  await openEditDialog(store.currentDetail.id, {
    detail: store.currentDetail,
    restoreDetailOnClose: true,
    allowFromDetail: true
  })
}

async function refreshCurrentDetail() {
  if (!store.currentDetail?.id || pageBusy.value) {
    return
  }
  try {
    await store.fetchDetail(store.currentDetail.id)
    showToast('详情已刷新')
  } catch (error) {
    showToast(error.message || '详情刷新失败')
  }
}

function closeDetail() {
  if (pageBusy.value) {
    return
  }
  detailVisible.value = false
}

async function refreshListFromDetailEmptyState() {
  if (pageBusy.value) {
    return
  }
  closeDetail()
  await fetchPage()
}

async function handleSubmit(formData) {
  if (pageBusy.value) {
    return
  }
  try {
    if (dialog.mode === 'create') {
      const createdUserId = await store.createUser(formData)
      showToast('新增成功')
      searchForm.keywords = ''
      searchForm.status = ''
      dialog.visible = false
      syncListRouteQuery({
        keywords: '',
        status: '',
        userId: createdUserId
      })
      await fetchPage({ pageNo: 1, keywords: '', status: '' })
      if (createdUserId) {
        await showDetail(createdUserId)
      }
    } else {
      const editedUserId = dialog.editingId
      const restoreDetailId = dialog.restoreDetailId
      await store.updateUser(dialog.editingId, {
        realName: formData.realName,
        jobTitle: formData.jobTitle,
        mobile: formData.mobile,
        status: formData.status
      })
      showToast('编辑成功')
      dialog.restoringAfterSubmit = Boolean(restoreDetailId)
      dialog.visible = false
      syncListRouteQuery({
        keywords: searchForm.keywords,
        status: searchForm.status,
        userId: editedUserId
      })
      await fetchPage()
      if (restoreDetailId != null) {
        await showDetail(restoreDetailId)
      } else if (detailVisible.value && store.currentDetail?.id === editedUserId) {
        await showDetail(editedUserId)
      }
    }
  } catch (error) {
    showToast(error.message || '保存失败')
  }
}

async function handleDelete(userId) {
  if (listBlocked.value) {
    return
  }
  try {
    if (isCurrentLoginUser(userId)) {
      showToast('不能删除当前登录用户')
      return
    }
    const target = store.list.find((item) => item.id === userId)
    await showConfirmDialog({
      title: '删除确认',
      message: `将删除 ${target?.realName || '该用户'}${target?.username ? `（${target.username}）` : ''}，删除后该用户将从列表中隐藏，是否继续？`
    })
    actingUserId.value = userId
    actingType.value = 'delete'
    await store.deleteUser(userId)
    syncListRouteQuery({
      keywords: searchForm.keywords,
      status: searchForm.status,
      userId: Number(route.query.userId) === Number(userId) ? undefined : route.query.userId
    })
    if (store.currentDetail?.id === userId) {
      closeDetail()
    }
    showToast('删除成功')
    if (store.list.length === 1 && store.pageNo > 1) {
      await fetchPage({ pageNo: store.pageNo - 1 })
      return
    }
    await fetchPage()
  } catch (error) {
    if (!isActionCancelled(error) && error?.message) {
      showToast(error.message)
    }
  } finally {
    actingUserId.value = null
    actingType.value = ''
  }
}

async function handleToggleStatus(user) {
  if (!user?.id || listBlocked.value) {
    return
  }
  try {
    if (isCurrentLoginUser(user.id)) {
      showToast('不能停用当前登录用户')
      return
    }
    const nextStatus = Number(user.status) === 1 ? 0 : 1
    await showConfirmDialog({
      title: `${nextStatus === 1 ? '启用' : '停用'}确认`,
      message: `${nextStatus === 1 ? '启用' : '停用'} ${user.realName || '该用户'}${user.username ? `（${user.username}）` : ''}，是否继续？`
    })
    actingUserId.value = user.id
    actingType.value = 'toggleStatus'
    await store.updateUser(user.id, {
      realName: user.realName,
      jobTitle: user.jobTitle,
      mobile: user.mobile,
      status: nextStatus
    })
    showToast(nextStatus === 1 ? '启用成功' : '停用成功')
    await fetchPage()
    if (detailVisible.value && store.currentDetail?.id === user.id) {
      await showDetail(user.id)
    }
  } catch (error) {
    if (!isActionCancelled(error) && error?.message) {
      showToast(error.message)
    }
  } finally {
    actingUserId.value = null
    actingType.value = ''
  }
}

async function handleResetPassword(userId, options = {}) {
  if (pageBusy.value || (!options.allowFromDetail && listBlocked.value)) {
    return
  }
  try {
    if (isCurrentLoginUser(userId)) {
      showToast('不能重置当前登录用户密码')
      return
    }
    const target = store.list.find((item) => item.id === userId) || store.currentDetail
    await showConfirmDialog({
      title: '重置密码确认',
      message: `将为 ${target?.realName || '该用户'}${target?.username ? `（${target.username}）` : ''} 重置密码，是否继续？`
    })
    actingUserId.value = userId
    actingType.value = 'resetPassword'
    await store.resetPassword(userId)
    showToast('密码已重置为默认值 123456')
  } catch (error) {
    if (!isActionCancelled(error) && error?.message) {
      showToast(error.message)
    }
  } finally {
    actingUserId.value = null
    actingType.value = ''
  }
}

function isActionCancelled(error) {
  return error === 'cancel'
    || error === 'close'
    || error?.message === 'cancel'
    || error?.message === 'close'
}

function isCurrentLoginUser(userId) {
  return currentLoginUserId.value != null && Number(userId) === Number(currentLoginUserId.value)
}

function resolveRouteUserId(value) {
  if (value == null || value === '') {
    return null
  }
  const nextId = Number(value)
  return Number.isFinite(nextId) ? nextId : null
}

function syncUnitsRouteDetail(userId) {
  if (route.query.from !== 'units') {
    return
  }
  const nextUserId = userId == null ? undefined : String(userId)
  const currentUserId = typeof route.query.userId === 'string' ? route.query.userId : undefined
  if (currentUserId === nextUserId) {
    return
  }
  const nextQuery = { ...route.query }
  if (nextUserId == null) {
    delete nextQuery.userId
  } else {
    nextQuery.userId = nextUserId
  }
  router.replace({ query: nextQuery })
}

function syncListRouteQuery({ keywords = searchForm.keywords, status = searchForm.status, userId = route.query.userId } = {}) {
  const nextQuery = { ...route.query }
  const nextKeywords = typeof keywords === 'string' ? keywords.trim() : ''
  const nextStatus = status === 1 || status === '1'
    ? '1'
    : status === 0 || status === '0'
      ? '0'
      : ''
  const nextUserId = userId == null || userId === '' ? '' : String(userId)

  if (nextKeywords) {
    nextQuery.keywords = nextKeywords
  } else {
    delete nextQuery.keywords
  }
  if (nextStatus) {
    nextQuery.status = nextStatus
  } else {
    delete nextQuery.status
  }
  if (nextUserId) {
    nextQuery.userId = nextUserId
  } else {
    delete nextQuery.userId
  }

  const currentQuery = JSON.stringify(route.query)
  const targetQuery = JSON.stringify(nextQuery)
  if (currentQuery === targetQuery) {
    return false
  }
  router.replace({ query: nextQuery })
  return true
}
</script>

<style scoped>
.user-page {
  min-width: 0;
}

.title-tags {
  display: flex;
  align-items: center;
  gap: 6px;
}

.user-card-title--disabled {
  opacity: 0.6;
}

.user-meta--hint {
  color: #576b95;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.module-tip {
  margin-bottom: 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #eef6ff;
  color: #245b9e;
  font-size: 13px;
  line-height: 1.5;
}

.module-tip--source {
  background: #fff7e8;
  color: #8a5a00;
}

.module-tip-link {
  margin-left: 8px;
  color: inherit;
  text-decoration: underline;
}

.module-overview {
  margin-bottom: 16px;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0 16px;
}

.filter-row :deep(.van-dropdown-menu) {
  flex: 1;
}

.filter-summary {
  margin: 0 0 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #fff7e8;
  color: #8a5a00;
  font-size: 13px;
  line-height: 1.5;
}

.page-summary {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
  margin: 0 0 16px;
  color: #667085;
  font-size: 13px;
}

.page-summary--subtle {
  margin-top: -8px;
}

.focus-summary {
  margin: 0 0 16px;
  padding: 10px 12px;
  border-radius: 10px;
  background: #eef6ff;
  color: #245b9e;
  font-size: 13px;
  line-height: 1.5;
}

.empty-tip {
  margin-bottom: 12px;
  color: #667085;
  font-size: 13px;
  line-height: 1.5;
}

.page-loading {
  padding: 48px 0;
}

.empty-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 12px;
  flex-wrap: wrap;
}

.empty-link-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 96px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid #1989fa;
  color: #1989fa;
  text-decoration: none;
  font-size: 14px;
  background: #fff;
}

.detail-link-button {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 12px;
  border-radius: 999px;
  border: 1px solid #1989fa;
  color: #1989fa;
  text-decoration: none;
  background: #fff;
}

.user-list {
  display: grid;
  gap: 12px;
}

.user-card-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-card-title--clickable {
  cursor: pointer;
}

.user-meta {
  line-height: 1.8;
  color: #666;
}

.user-meta--link {
  cursor: pointer;
}

.user-card--active {
  box-shadow: 0 0 0 2px #1989fa inset;
}

.title-sub {
  color: #667085;
  font-size: 13px;
  font-weight: 400;
}

.card-actions {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
  flex-wrap: wrap;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.detail-panel {
  padding: 20px 16px 28px;
}

.detail-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 12px;
}

.detail-title-sub {
  color: #667085;
  font-size: 14px;
  font-weight: 400;
}

.detail-summary {
  margin-bottom: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f2f7ff;
}

.summary-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f3a5f;
  margin-bottom: 4px;
}

.summary-text {
  font-size: 13px;
  line-height: 1.6;
  color: #4b5b76;
}

.detail-actions {
  display: grid;
  gap: 12px;
  padding: 16px;
}

.detail-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
</style>

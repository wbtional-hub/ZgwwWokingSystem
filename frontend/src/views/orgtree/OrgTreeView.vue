<template>
  <AppPageShell class="orgtree-page" title="组织架构" description="当前页已接入最小可用组织工作流：树展示、新增、编辑、删除和启停。">
    <template #title-extra>
      <PageHelp page-key="orgTree" />
    </template>
    <template #actions>
      <div class="hero-actions" data-guide="org-module-permission">
        <van-button type="primary" :loading="state.loading" @click="refreshTree">刷新组织树</van-button>
        <van-button plain type="success" :disabled="!state.selectedNode" @click="openCreateDialog(state.selectedNode)">
          为当前节点新增下级
        </van-button>
        <van-button plain type="warning" :disabled="!state.selectedNode" @click="openEditDialog(state.selectedNode)">
          编辑当前节点
        </van-button>
        <van-button plain type="primary" :disabled="!state.selectedNode" @click="openModulePermissionDialog(state.selectedNode)">
          配置可访问模块
        </van-button>
      </div>
    </template>

    <section class="hero-panel">
      <div>
        <div class="eyebrow">OrgTree</div>
        <h2>金字塔组织架构</h2>
        <p>当前页面直接完成组织树展示、节点新增、节点编辑、节点删除、上级调整和节点启停。</p>
      </div>
    </section>

    <PageSkeletonSection title="查询区占位" description="当前阶段维持最小可用版本，先通过左侧树和详情操作完成组织管理闭环。">
      组织树当前不扩展复杂筛选，先保证节点可看、可改、可删、可启停。
    </PageSkeletonSection>

    <van-loading v-if="state.loading" class="page-loading" size="24px" vertical>组织树加载中...</van-loading>

    <template v-else>
      <van-empty v-if="!state.tree.length" description="当前没有可展示的组织树数据" />

      <div v-else class="page-grid">
        <section class="tree-panel" data-guide="org-tree">
          <div class="panel-title">组织树</div>
          <div class="tree-list">
            <OrgTreeNodeItem
              v-for="node in state.tree"
              :key="node.id"
              :node="node"
              :selected-id="state.selectedNode?.id ?? null"
              @select="handleSelectNode"
              @create-child="openCreateDialog"
              @move-node="openMoveDialog"
            />
          </div>
        </section>

        <section class="detail-panel" data-guide="org-detail">
          <div class="panel-title">节点详情</div>
          <van-empty v-if="!state.selectedNode" description="请先在左侧选择一个节点" />

          <template v-else>
            <van-cell-group inset>
              <van-cell title="用户ID" :value="state.selectedNode.id" />
              <van-cell title="姓名" :value="state.selectedNode.realName || '-'" />
              <van-cell title="账号" :value="state.selectedNode.username" />
              <van-cell title="状态" :value="statusLabel(state.selectedNode.status)" />
              <van-cell title="上级信息" :value="parentSummary" />
            <van-cell title="层级" :value="state.selectedNode.levelNo" />
              <van-cell title="所属单位" :value="resolveUnitLabel(state.selectedNode.unitId)" />
              <van-cell title="直属下级数" :value="state.children.length" />
              <van-cell title="上级链长度" :value="state.ancestors.length" />
              <van-cell title="路径" :value="state.selectedNode.treePath" />
              <van-cell title="岗位" :value="state.selectedNode.jobTitle || '-'" />
              <van-cell title="手机号" :value="state.selectedNode.mobile || '-'" />
              <van-cell title="微信号" :value="state.selectedNode.wechatNo || '-'" />
              <van-cell title="微信OpenId" :value="state.selectedNode.wechatOpenId || '-'" />
              <van-cell title="微信UnionId" :value="state.selectedNode.wechatUnionId || '-'" />
            </van-cell-group>
            <div class="popup-tip subtle-tip">组织树节点默认继承上级所属单位，当前页面仅维护树结构与节点资料，不直接改单位归属。</div>

            <div class="section-head">
              <span>直属下级（{{ state.children.length }}）</span>
              <van-button size="small" plain type="primary" :loading="state.loadingChildren" @click="loadChildren(state.selectedNode.id)">
                刷新
              </van-button>
            </div>
            <van-loading v-if="state.loadingChildren" class="section-loading" size="18px">直属下级加载中...</van-loading>
            <van-empty v-else-if="!state.children.length" description="暂无直属下级" />
            <div v-else class="relation-list">
              <div
                v-for="item in state.children"
                :key="item.id"
                class="relation-card"
                :class="{ active: item.id === state.selectedNode.id }"
                @click="handleSelectNode(item)"
              >
                <div class="relation-name">{{ item.realName || item.username }}</div>
                <div class="relation-sub">{{ item.username }} · L{{ item.levelNo }}</div>
                <div class="relation-sub">状态：{{ statusLabel(item.status) }}</div>
                <div class="relation-tip">点击切换到该直属下级</div>
              </div>
            </div>

            <div class="section-head">
              <span>上级链（{{ state.ancestors.length }}）</span>
              <van-button size="small" plain type="success" :loading="state.loadingAncestors" @click="loadAncestors(state.selectedNode.id)">
                刷新
              </van-button>
            </div>
            <van-loading v-if="state.loadingAncestors" class="section-loading" size="18px">上级链加载中...</van-loading>
            <van-empty v-else-if="!state.ancestors.length" description="暂无上级链数据" />
            <div v-else class="ancestor-chain">
              <div
                v-for="item in state.ancestors"
                :key="item.id"
                class="ancestor-chip"
                :class="{ active: item.id === state.selectedNode.id }"
                @click="handleSelectNode(item)"
              >
                {{ item.realName || item.username }} · L{{ item.levelNo }}
              </div>
            </div>
            <div v-if="state.ancestors.length" class="ancestor-tip">点击上级链节点可快速切换查看</div>

            <div class="detail-actions">
              <van-button block type="success" @click="openCreateDialog(state.selectedNode)">新增下级</van-button>
              <van-button block plain type="primary" @click="openEditDialog(state.selectedNode)">编辑节点</van-button>
              <van-button block plain type="primary" @click="openModulePermissionDialog(state.selectedNode)">可访问模块</van-button>
              <van-button block plain type="warning" @click="openMoveDialog(state.selectedNode)">调整上级</van-button>
              <van-button
                block
                plain
                :type="Number(state.selectedNode.status) === 1 ? 'danger' : 'success'"
                :loading="state.togglingStatus"
                @click="handleToggleStatus(state.selectedNode)"
              >
                {{ Number(state.selectedNode.status) === 1 ? '停用节点' : '启用节点' }}
              </van-button>
              <van-button block plain type="danger" :loading="state.deleting" @click="handleDeleteNode(state.selectedNode)">
                删除节点
              </van-button>
            </div>
          </template>
        </section>
      </div>
    </template>

    <van-popup v-model:show="state.createVisible" position="bottom" round>
      <div class="popup-body">
        <div class="popup-title">新增下级用户</div>
        <div class="popup-tip">当前上级：{{ state.createParentLabel || '-' }}</div>
        <van-form @submit="submitCreate">
          <van-field :model-value="state.createForm.parentUserId ? String(state.createForm.parentUserId) : ''" label="上级ID" readonly />
          <van-field v-model="state.createForm.username" label="账号" placeholder="请输入登录账号" required />
          <van-field v-model="state.createForm.password" label="密码" type="password" placeholder="请输入初始密码" required />
          <van-field v-model="state.createForm.realName" label="姓名" placeholder="请输入姓名" required />
          <van-field v-model="state.createForm.jobTitle" label="岗位" placeholder="请输入岗位名称" />
          <van-field v-model="state.createForm.mobile" label="手机号" placeholder="请输入手机号" />
          <van-field v-model="state.createForm.wechatNo" label="微信号" placeholder="请输入微信号" />
          <van-field v-model="state.createForm.wechatOpenId" label="微信OpenId" placeholder="用于小程序授权绑定" />
          <van-field v-model="state.createForm.wechatUnionId" label="微信UnionId" placeholder="用于跨应用微信身份绑定" />
          <van-field
            :model-value="createUnitLabel"
            label="所属单位"
            readonly
            :is-link="canPickCreateUnit"
            :placeholder="canPickCreateUnit ? '请选择所属单位' : '自动继承上级节点所属单位'"
            @click="canPickCreateUnit ? openUnitPicker('create') : null"
          />
          <div class="popup-tip subtle-tip">{{ createUnitTip }}</div>
          <van-field
            :model-value="Number(state.createForm.status) === 1 ? '启用' : '停用'"
            label="状态"
            readonly
            is-link
            @click="openCreateStatusPicker"
          />
          <div class="popup-actions">
            <van-button block plain @click="state.createVisible = false">取消</van-button>
            <van-button block type="primary" native-type="submit" :loading="state.submittingCreate">保存</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="state.editVisible" position="bottom" round>
      <div class="popup-body">
        <div class="popup-title">编辑节点</div>
        <div class="popup-tip">当前节点：{{ state.editNodeLabel || '-' }}</div>
        <van-form @submit="submitEdit">
          <van-field :model-value="state.editForm.userId ? String(state.editForm.userId) : ''" label="节点ID" readonly />
          <van-field :model-value="state.editForm.username" label="账号" readonly />
          <van-field v-model="state.editForm.realName" label="姓名" placeholder="请输入姓名" required />
          <van-field v-model="state.editForm.jobTitle" label="岗位" placeholder="请输入岗位名称" />
          <van-field v-model="state.editForm.mobile" label="手机号" placeholder="请输入手机号" />
          <van-field v-model="state.editForm.wechatNo" label="微信号" placeholder="请输入微信号" />
          <van-field v-model="state.editForm.wechatOpenId" label="微信OpenId" placeholder="用于小程序授权绑定" />
          <van-field v-model="state.editForm.wechatUnionId" label="微信UnionId" placeholder="用于跨应用微信身份绑定" />
          <van-field
            :model-value="editUnitLabel"
            label="所属单位"
            readonly
            is-link
            placeholder="请选择所属单位"
            @click="openUnitPicker('edit')"
          />
          <div class="popup-tip subtle-tip">设置单位后，将同步更新当前节点及全部下级节点的所属单位。</div>
          <div class="popup-actions">
            <van-button block plain @click="state.editVisible = false">取消</van-button>
            <van-button block type="primary" native-type="submit" :loading="state.submittingEdit">保存修改</van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="state.moveVisible" position="bottom" round>
      <div class="popup-body">
        <div class="popup-title">调整上级</div>
        <div class="popup-tip">当前节点：{{ state.moveNodeLabel || '-' }}</div>
        <div v-if="moveSubmitHint" class="popup-tip subtle-tip">{{ moveSubmitHint }}</div>
        <van-form @submit="submitMove">
          <van-field :model-value="state.moveForm.userId ? String(state.moveForm.userId) : ''" label="当前节点ID" readonly />
          <van-field
            :model-value="moveTargetLabel"
            label="目标上级"
            readonly
            is-link
            placeholder="请选择目标上级"
            @click="openParentPicker"
          />
          <div class="popup-actions">
            <van-button block plain @click="state.moveVisible = false">取消</van-button>
            <van-button block type="warning" native-type="submit" :loading="state.submittingMove" :disabled="!canSubmitMove">
              提交调整
            </van-button>
          </div>
        </van-form>
      </div>
    </van-popup>

    <van-popup v-model:show="state.statusPickerVisible" position="bottom" round>
      <van-picker :columns="statusColumns" @confirm="handleStatusConfirm" @cancel="state.statusPickerVisible = false" />
    </van-popup>

    <van-popup v-model:show="state.parentPickerVisible" position="bottom" round>
      <van-picker :columns="parentColumns" @confirm="handleParentConfirm" @cancel="state.parentPickerVisible = false" />
    </van-popup>

    <van-popup v-model:show="state.unitPickerVisible" position="bottom" round>
      <van-picker :columns="unitColumns" @confirm="handleUnitConfirm" @cancel="state.unitPickerVisible = false" />
    </van-popup>

    <van-popup v-model:show="state.modulePermissionVisible" position="bottom" round>
      <div class="popup-body">
        <div class="popup-title">可访问模块配置</div>
        <div class="popup-tip">当前用户：{{ state.modulePermissionUserLabel || '-' }}</div>
        <div class="popup-tip subtle-tip">首页与个人中心保留为基础页，不参与本轮模块授权配置。</div>
        <van-loading v-if="state.modulePermissionLoading" class="section-loading" size="18px">模块权限加载中...</van-loading>
        <template v-else>
          <van-checkbox-group v-model="state.modulePermissionForm.moduleCodes" class="module-permission-list">
            <label
              v-for="item in state.moduleDefinitions"
              :key="item.moduleCode"
              class="module-permission-card"
            >
              <van-checkbox :name="item.moduleCode" />
              <div class="module-permission-card__content">
                <div class="module-permission-card__title">
                  <span>{{ item.moduleName }}</span>
                  <span v-if="item.adminOnly" class="module-permission-card__tag">ADMIN</span>
                </div>
                <div class="module-permission-card__meta">{{ item.moduleCode }} · {{ item.routePath }}</div>
              </div>
            </label>
          </van-checkbox-group>
          <van-empty v-if="!state.moduleDefinitions.length" description="当前没有可配置模块" />
        </template>
        <div class="popup-actions">
          <van-button block plain :disabled="state.modulePermissionSaving" @click="state.modulePermissionVisible = false">取消</van-button>
          <van-button block type="primary" :loading="state.modulePermissionSaving" @click="submitModulePermissions">保存配置</van-button>
        </div>
      </div>
    </van-popup>
  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageHelp from '@/components/PageHelp.vue'
import PageSkeletonSection from '@/components/layout/PageSkeletonSection.vue'
import OrgTreeNodeItem from '@/components/orgtree/OrgTreeNodeItem.vue'
import { useUserStore } from '@/stores/user'
import {
  queryCurrentUserModulePermissionsApi,
  queryModuleDefinitionListApi,
  queryUserModulePermissionsApi,
  saveUserModulePermissionsApi
} from '@/api/user-module-permission'
import {
  createChildUser,
  deleteOrgNode,
  moveOrgNode,
  queryOrgAncestors,
  queryOrgChildren,
  queryOrgTree,
  queryUnitOptions,
  toggleOrgNodeStatus,
  updateOrgNode
} from '@/api/orgtree'

const userStore = useUserStore()

function ensureSuccess(response) {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || '请求失败')
  }
  return response.data
}

const statusColumns = [
  { text: '启用', value: 1 },
  { text: '停用', value: 0 }
]

const state = reactive({
  loading: false,
  tree: [],
  selectedNode: null,
  children: [],
  ancestors: [],
  createVisible: false,
  editVisible: false,
  moveVisible: false,
  statusPickerVisible: false,
  parentPickerVisible: false,
  loadingChildren: false,
  loadingAncestors: false,
  submittingCreate: false,
  submittingEdit: false,
  submittingMove: false,
  deleting: false,
  togglingStatus: false,
  createParentLabel: '',
  editNodeLabel: '',
  moveNodeLabel: '',
  statusPickerMode: 'create',
  createForm: {
    parentUserId: null,
    username: '',
    password: '',
    realName: '',
    jobTitle: '',
    mobile: '',
    wechatNo: '',
    wechatOpenId: '',
    wechatUnionId: '',
    unitId: '',
    status: 1
  },
  editForm: {
    userId: null,
    username: '',
    realName: '',
    jobTitle: '',
    mobile: '',
    wechatNo: '',
    wechatOpenId: '',
    wechatUnionId: '',
    unitId: '',
    status: 1
  },
  unitOptions: [],
  unitPickerMode: 'create',
  unitPickerVisible: false,
  modulePermissionVisible: false,
  modulePermissionLoading: false,
  modulePermissionSaving: false,
  modulePermissionUserLabel: '',
  moduleDefinitions: [],
  modulePermissionForm: {
    userId: null,
    moduleCodes: []
  },
  moveForm: {
    userId: null,
    targetParentUserId: null
  }
})

const movableParentNodes = computed(() => flattenNodes(state.tree).filter(item => isValidMoveTarget(item)))
const parentColumns = computed(() => movableParentNodes.value.map(item => ({
  text: `${item.realName || item.username} · ${item.username} · L${item.levelNo}`,
  value: item.id
})))

const stateNodeMap = computed(() => {
  const map = new Map()
  flattenNodes(state.tree).forEach(item => map.set(item.id, item))
  return map
})

const stateMoveTarget = computed(() => stateNodeMap.value.get(state.moveForm.targetParentUserId) || null)
const stateMoveNode = computed(() => stateNodeMap.value.get(state.moveForm.userId) || null)
const selectedParentNode = computed(() => stateNodeMap.value.get(state.selectedNode?.parentUserId) || null)

const moveTargetLabel = computed(() => {
  if (!stateMoveTarget.value) {
    return ''
  }
  return `${stateMoveTarget.value.realName || stateMoveTarget.value.username} · ${stateMoveTarget.value.username}`
})

const canSubmitMove = computed(() => {
  if (!state.moveForm.userId || !state.moveForm.targetParentUserId) {
    return false
  }
  return stateMoveNode.value?.parentUserId !== state.moveForm.targetParentUserId
})

const moveSubmitHint = computed(() => {
  if (!state.moveForm.userId) {
    return '请先选择当前节点'
  }
  if (!movableParentNodes.value.length) {
    return '当前没有可选的目标上级'
  }
  if (!state.moveForm.targetParentUserId) {
    return '请选择目标上级'
  }
  if (!canSubmitMove.value) {
    return '目标上级未变更'
  }
  return ''
})

const createUnitLabel = computed(() => resolveUnitLabel(state.createForm.unitId))
const editUnitLabel = computed(() => resolveUnitLabel(state.editForm.unitId))
const canPickCreateUnit = computed(() => {
  return Boolean(userStore.userInfo?.superAdmin) && Number(state.createForm.parentUserId) === Number(userStore.userInfo?.userId)
})
const createUnitTip = computed(() => {
  return canPickCreateUnit.value
    ? '当前为超级管理员直属新增，下级节点不继承超级管理员单位，请手动选择所属单位。'
    : '新增下级会自动继承当前上级节点所属单位。'
})
const unitColumns = computed(() => state.unitOptions.map((item) => ({
  text: `${item.unitName} (${item.unitCode})`,
  value: item.id
})))

const parentSummary = computed(() => {
  if (!state.selectedNode?.parentUserId) {
    return '根节点'
  }
  if (!selectedParentNode.value) {
    return `ID ${state.selectedNode.parentUserId}`
  }
  return `${selectedParentNode.value.realName || selectedParentNode.value.username} · ${selectedParentNode.value.username}`
})

onMounted(() => {
  loadUnitOptions()
  refreshTree()
})

function statusLabel(status) {
  return Number(status) === 1 ? '启用' : '停用'
}

async function refreshTree(preferredNodeId = state.selectedNode?.id ?? null) {
  state.loading = true
  try {
    state.tree = ensureSuccess(await queryOrgTree()) || []
    const allNodes = flattenNodes(state.tree)
    const targetNode = findNodeById(allNodes, preferredNodeId) || allNodes[0] || null
    if (targetNode) {
      await handleSelectNode(targetNode)
    } else {
      state.selectedNode = null
      state.children = []
      state.ancestors = []
    }
  } catch (error) {
    showToast(error.message || '组织树加载失败')
  } finally {
    state.loading = false
  }
}

async function loadUnitOptions() {
  try {
    state.unitOptions = ensureSuccess(await queryUnitOptions()) || []
  } catch (error) {
    state.unitOptions = []
    showToast(error.message || '单位列表加载失败')
  }
}

async function handleSelectNode(node) {
  state.selectedNode = normalizeNode(node)
  await Promise.all([
    loadChildren(state.selectedNode.id),
    loadAncestors(state.selectedNode.id)
  ])
}

async function loadChildren(userId) {
  state.loadingChildren = true
  try {
    state.children = (ensureSuccess(await queryOrgChildren(userId)) || []).map(normalizeNode)
  } catch (error) {
    state.children = []
    showToast(error.message || '直属下级加载失败')
  } finally {
    state.loadingChildren = false
  }
}

async function loadAncestors(userId) {
  state.loadingAncestors = true
  try {
    state.ancestors = (ensureSuccess(await queryOrgAncestors(userId)) || []).map(normalizeNode)
  } catch (error) {
    state.ancestors = []
    showToast(error.message || '上级链加载失败')
  } finally {
    state.loadingAncestors = false
  }
}

function openCreateDialog(node) {
  const normalized = normalizeNode(node)
  const useExplicitUnitSelection = Boolean(userStore.userInfo?.superAdmin) && Number(normalized.id) === Number(userStore.userInfo?.userId)
  state.createForm.parentUserId = normalized.id
  state.createForm.username = ''
  state.createForm.password = ''
  state.createForm.realName = ''
  state.createForm.jobTitle = ''
  state.createForm.mobile = ''
  state.createForm.wechatNo = ''
  state.createForm.wechatOpenId = ''
  state.createForm.wechatUnionId = ''
  state.createForm.unitId = useExplicitUnitSelection ? '' : normalized.unitId ?? ''
  state.createForm.status = 1
  state.createParentLabel = `${normalized.realName || normalized.username} · ${normalized.username}`
  state.createVisible = true
}

function openEditDialog(node) {
  const normalized = normalizeNode(node)
  state.editForm.userId = normalized.id
  state.editForm.username = normalized.username || ''
  state.editForm.realName = normalized.realName || ''
  state.editForm.jobTitle = normalized.jobTitle || ''
  state.editForm.mobile = normalized.mobile || ''
  state.editForm.wechatNo = normalized.wechatNo || ''
  state.editForm.wechatOpenId = normalized.wechatOpenId || ''
  state.editForm.wechatUnionId = normalized.wechatUnionId || ''
  state.editForm.unitId = normalized.unitId ?? ''
  state.editForm.status = Number(normalized.status) || 0
  state.editNodeLabel = `${normalized.realName || normalized.username} · ${normalized.username}`
  state.editVisible = true
}

async function openModulePermissionDialog(node) {
  const normalized = normalizeNode(node)
  state.modulePermissionVisible = true
  state.modulePermissionLoading = true
  state.modulePermissionUserLabel = `${normalized.realName || normalized.username} · ${normalized.username}`
  state.modulePermissionForm.userId = normalized.id
  state.modulePermissionForm.moduleCodes = []
  try {
    if (!state.moduleDefinitions.length) {
      state.moduleDefinitions = ensureSuccess(await queryModuleDefinitionListApi()) || []
    }
    const permissionData = ensureSuccess(await queryUserModulePermissionsApi(normalized.id)) || {}
    state.modulePermissionForm.moduleCodes = Array.isArray(permissionData.moduleCodes) ? [...permissionData.moduleCodes] : []
  } catch (error) {
    state.modulePermissionVisible = false
    showToast(error.message || '模块权限加载失败')
  } finally {
    state.modulePermissionLoading = false
  }
}

function openMoveDialog(node) {
  const normalized = normalizeNode(node)
  state.moveForm.userId = normalized.id
  state.moveForm.targetParentUserId = normalized.parentUserId
  state.moveNodeLabel = `${normalized.realName || normalized.username} · ${normalized.username}`
  state.moveVisible = true
}

function openCreateStatusPicker() {
  state.statusPickerMode = 'create'
  state.statusPickerVisible = true
}

function openUnitPicker(mode) {
  if (!state.unitOptions.length) {
    showToast('当前没有可选单位')
    return
  }
  state.unitPickerMode = mode
  state.unitPickerVisible = true
}

function openParentPicker() {
  if (!movableParentNodes.value.length) {
    showToast('当前没有可选的目标上级')
    return
  }
  state.parentPickerVisible = true
}

function handleStatusConfirm({ selectedOptions }) {
  const status = selectedOptions[0]?.value ?? 1
  if (state.statusPickerMode === 'create') {
    state.createForm.status = status
  }
  state.statusPickerVisible = false
}

function handleParentConfirm({ selectedOptions }) {
  state.moveForm.targetParentUserId = selectedOptions[0]?.value ?? null
  state.parentPickerVisible = false
}

function handleUnitConfirm({ selectedOptions }) {
  const unitId = selectedOptions[0]?.value ?? ''
  if (state.unitPickerMode === 'create') {
    state.createForm.unitId = unitId
  } else {
    state.editForm.unitId = unitId
  }
  state.unitPickerVisible = false
}

async function submitCreate() {
  if (!state.createForm.parentUserId) {
    showToast('请先选择上级节点')
    return
  }
  if (canPickCreateUnit.value && !state.createForm.unitId) {
    showToast('请先选择所属单位')
    return
  }
  if (!state.createForm.username.trim() || !state.createForm.password.trim() || !state.createForm.realName.trim()) {
    showToast('请完整填写账号、密码、姓名')
    return
  }

  state.submittingCreate = true
  try {
    const newUserId = ensureSuccess(await createChildUser({
      ...state.createForm,
      username: state.createForm.username.trim(),
      password: state.createForm.password.trim(),
      realName: state.createForm.realName.trim(),
      jobTitle: trimToNull(state.createForm.jobTitle),
      mobile: trimToNull(state.createForm.mobile),
      wechatNo: trimToNull(state.createForm.wechatNo),
      wechatOpenId: trimToNull(state.createForm.wechatOpenId),
      wechatUnionId: trimToNull(state.createForm.wechatUnionId),
      unitId: state.createForm.unitId ? Number(state.createForm.unitId) : null
    }))
    state.createVisible = false
    showToast(`新增成功，用户ID：${newUserId}`)
    await refreshTree(newUserId)
  } catch (error) {
    showToast(error.message || '新增下级失败')
  } finally {
    state.submittingCreate = false
  }
}

async function submitEdit() {
  if (!state.editForm.userId) {
    showToast('请先选择节点')
    return
  }
  if (!state.editForm.realName.trim()) {
    showToast('请输入姓名')
    return
  }

  state.submittingEdit = true
  try {
    ensureSuccess(await updateOrgNode({
      userId: state.editForm.userId,
      realName: state.editForm.realName.trim(),
      jobTitle: trimToNull(state.editForm.jobTitle),
      mobile: trimToNull(state.editForm.mobile),
      wechatNo: trimToNull(state.editForm.wechatNo),
      wechatOpenId: trimToNull(state.editForm.wechatOpenId),
      wechatUnionId: trimToNull(state.editForm.wechatUnionId),
      unitId: state.editForm.unitId ? Number(state.editForm.unitId) : null
    }))
    state.editVisible = false
    showToast('节点编辑成功')
    await refreshTree(state.editForm.userId)
  } catch (error) {
    showToast(error.message || '节点编辑失败')
  } finally {
    state.submittingEdit = false
  }
}

async function submitMove() {
  if (!state.moveForm.userId || !state.moveForm.targetParentUserId) {
    showToast('请选择目标上级')
    return
  }
  if (!canSubmitMove.value) {
    showToast('目标上级未变更')
    return
  }

  state.submittingMove = true
  try {
    ensureSuccess(await moveOrgNode({ ...state.moveForm }))
    state.moveVisible = false
    showToast('上级调整成功')
    await refreshTree(state.moveForm.userId)
  } catch (error) {
    showToast(error.message || '调整上级失败')
  } finally {
    state.submittingMove = false
  }
}

async function submitModulePermissions() {
  if (!state.modulePermissionForm.userId) {
    showToast('请先选择节点')
    return
  }
  state.modulePermissionSaving = true
  try {
    ensureSuccess(await saveUserModulePermissionsApi(state.modulePermissionForm.userId, {
      userId: state.modulePermissionForm.userId,
      moduleCodes: state.modulePermissionForm.moduleCodes
    }))
    if (Number(userStore.userInfo?.userId) === Number(state.modulePermissionForm.userId)) {
      const currentPermissionData = ensureSuccess(await queryCurrentUserModulePermissionsApi()) || {}
      userStore.setAccessContext({
        moduleCodes: Array.isArray(currentPermissionData.moduleCodes) ? currentPermissionData.moduleCodes : []
      })
    }
    state.modulePermissionVisible = false
    showToast('模块权限保存成功')
  } catch (error) {
    showToast(error.message || '模块权限保存失败')
  } finally {
    state.modulePermissionSaving = false
  }
}

async function handleDeleteNode(node) {
  const normalized = normalizeNode(node)
  state.deleting = true
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: `确认删除节点 ${normalized.realName || normalized.username} 吗？`
    })
    ensureSuccess(await deleteOrgNode(normalized.id))
    showToast('节点删除成功')
    state.editVisible = false
    await refreshTree(normalized.parentUserId || null)
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || '节点删除失败')
    }
  } finally {
    state.deleting = false
  }
}

async function handleToggleStatus(node) {
  const normalized = normalizeNode(node)
  const nextStatus = Number(normalized.status) === 1 ? 0 : 1
  state.togglingStatus = true
  try {
    await showConfirmDialog({
      title: nextStatus === 1 ? '确认启用' : '确认停用',
      message: `${nextStatus === 1 ? '启用' : '停用'}节点 ${normalized.realName || normalized.username} 吗？`
    })
    ensureSuccess(await toggleOrgNodeStatus({
      userId: normalized.id,
      status: nextStatus
    }))
    showToast(nextStatus === 1 ? '节点已启用' : '节点已停用')
    await refreshTree(normalized.id)
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || '节点状态修改失败')
    }
  } finally {
    state.togglingStatus = false
  }
}

function flattenNodes(nodes) {
  const result = []
  for (const node of nodes || []) {
    const normalized = normalizeNode(node)
    result.push(normalized)
    result.push(...flattenNodes(normalized.children))
  }
  return result
}

function findNodeById(nodes, userId) {
  if (userId == null) {
    return null
  }
  return nodes.find(item => item.id === Number(userId)) || null
}

function isValidMoveTarget(node) {
  if (node.id === state.moveForm.userId) {
    return false
  }

  const currentNode = stateNodeMap.value.get(state.moveForm.userId)
  if (!currentNode) {
    return true
  }

  const currentTreePath = currentNode.treePath || ''
  const targetTreePath = node.treePath || ''
  return !targetTreePath.startsWith(currentTreePath)
}

function normalizeNode(node) {
  return {
    ...node,
    id: Number(node.id),
    status: Number(node.status) || 0,
    unitId: node.unitId == null || node.unitId === '' ? null : Number(node.unitId),
    parentUserId: node.parentUserId == null ? null : Number(node.parentUserId),
    levelNo: Number(node.levelNo),
    children: Array.isArray(node.children) ? node.children.map(normalizeNode) : []
  }
}

function resolveUnitLabel(unitId) {
  if (unitId == null || unitId === '') {
    return '-'
  }
  const unit = state.unitOptions.find((item) => Number(item.id) === Number(unitId))
  if (!unit) {
    return `单位ID ${unitId}`
  }
  return `${unit.unitName} (${unit.unitCode})`
}

function trimToNull(value) {
  const trimmed = (value || '').trim()
  return trimmed ? trimmed : null
}
</script>

<style scoped>
.orgtree-page {
  min-width: 0;
}

.hero-actions,
.popup-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hero-panel,
.tree-panel,
.detail-panel {
  padding: 16px;
  background: #fff;
  border-radius: 12px;
}

.hero-panel {
  margin-bottom: 16px;
}

.eyebrow {
  font-size: 12px;
  color: #64748b;
  text-transform: uppercase;
}

.page-grid {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(320px, 1.2fr);
  gap: 16px;
}

.panel-title,
.popup-title {
  margin-bottom: 12px;
  font-size: 16px;
  font-weight: 600;
}

.tree-list,
.relation-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  margin-bottom: 8px;
}

.section-loading,
.page-loading {
  padding: 20px 0;
}

.relation-card {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
}

.relation-card.active {
  border-color: #1989fa;
  background: #f0f9ff;
}

.relation-name {
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.relation-sub,
.relation-tip,
.popup-tip,
.ancestor-tip {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
}

.ancestor-chain {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.ancestor-chip {
  padding: 6px 10px;
  background: #f3f4f6;
  border-radius: 999px;
  font-size: 12px;
  cursor: pointer;
}

.ancestor-chip.active {
  background: #dbeafe;
  color: #1d4ed8;
}

.detail-actions {
  display: grid;
  gap: 8px;
  margin-top: 16px;
}

.popup-body {
  padding: 16px;
}

.module-permission-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.module-permission-card {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr);
  gap: 10px;
  align-items: flex-start;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #f8fafc;
}

.module-permission-card__content {
  min-width: 0;
}

.module-permission-card__title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #111827;
}

.module-permission-card__meta {
  margin-top: 4px;
  font-size: 12px;
  color: #6b7280;
  word-break: break-all;
}

.module-permission-card__tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  border-radius: 999px;
  background: #fee2e2;
  color: #b91c1c;
  font-size: 11px;
  font-weight: 600;
}

.subtle-tip {
  color: #9a3412;
}

@media (max-width: 960px) {
  .page-grid {
    grid-template-columns: 1fr;
  }
}
</style>

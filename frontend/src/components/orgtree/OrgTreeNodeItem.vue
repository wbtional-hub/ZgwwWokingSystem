<template>
  <div class="tree-node">
    <div
      class="tree-node-card"
      :class="{ active: node.id === selectedId }"
      @click="$emit('select', node)"
    >
      <div class="tree-node-head">
        <div>
          <div class="tree-node-name">
            {{ node.realName || node.username }}
          </div>
          <div class="tree-node-sub">
            {{ node.username }} · L{{ node.levelNo }} · {{ node.treePath }}
          </div>
        </div>
        <van-tag :type="Number(node.status) === 1 ? 'success' : 'danger'">
          {{ Number(node.status) === 1 ? '启用' : '停用' }}
        </van-tag>
      </div>

      <div class="tree-node-meta">
        <span>岗位：{{ node.jobTitle || '-' }}</span>
        <span>上级：{{ node.parentUserId ?? '根节点' }}</span>
      </div>

      <div class="tree-node-actions" @click.stop>
        <van-button size="small" plain type="primary" @click="$emit('select', node)">查看</van-button>
        <van-button size="small" plain type="success" @click="$emit('create-child', node)">新增下级</van-button>
        <van-button size="small" plain type="warning" @click="$emit('move-node', node)">调整上级</van-button>
      </div>
    </div>

    <div v-if="node.children?.length" class="tree-node-children">
      <OrgTreeNodeItem
        v-for="child in node.children"
        :key="child.id"
        :node="child"
        :selected-id="selectedId"
        @select="$emit('select', $event)"
        @create-child="$emit('create-child', $event)"
        @move-node="$emit('move-node', $event)"
      />
    </div>
  </div>
</template>

<script setup>
defineProps({
  node: {
    type: Object,
    required: true
  },
  selectedId: {
    type: Number,
    default: null
  }
})

defineEmits(['select', 'create-child', 'move-node'])
</script>

<style scoped>
.tree-node {
  position: relative;
}

.tree-node-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 14px;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.05);
  cursor: pointer;
}

.tree-node-card.active {
  border-color: #0f766e;
  box-shadow: 0 12px 28px rgba(15, 118, 110, 0.18);
}

.tree-node-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.tree-node-name {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.tree-node-sub {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
  word-break: break-all;
}

.tree-node-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  margin-top: 10px;
  font-size: 12px;
  color: #334155;
}

.tree-node-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.tree-node-children {
  margin-left: 18px;
  padding-left: 18px;
  border-left: 2px solid #cbd5e1;
  display: grid;
  gap: 12px;
  margin-top: 12px;
}
</style>

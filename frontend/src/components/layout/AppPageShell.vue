<template>
  <section class="page-shell">
    <div class="page-shell-header">
      <div>
        <h2 class="page-shell-title">{{ title }}</h2>
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
defineProps({
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
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

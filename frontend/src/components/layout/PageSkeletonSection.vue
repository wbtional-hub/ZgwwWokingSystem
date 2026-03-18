<template>
  <section class="skeleton-section">
    <div class="skeleton-section-head">
      <h3 class="skeleton-section-title">{{ title }}</h3>
      <p v-if="description" class="skeleton-section-description">{{ description }}</p>
    </div>
    <div class="skeleton-section-body" :class="`skeleton-section-body--${variant}`">
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
  },
  variant: {
    type: String,
    default: 'default'
  }
})
</script>

<style scoped>
.skeleton-section {
  margin-bottom: 16px;
}

.skeleton-section-head {
  margin-bottom: 8px;
}

.skeleton-section-title {
  margin: 0;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.skeleton-section-description {
  margin: 6px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.skeleton-section-body {
  border-radius: 12px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
  padding: 14px 16px;
  color: #475569;
  font-size: 14px;
  line-height: 1.7;
}

.skeleton-section-body--empty {
  background: #fff7ed;
  border-color: #fdba74;
  color: #9a3412;
}
</style>

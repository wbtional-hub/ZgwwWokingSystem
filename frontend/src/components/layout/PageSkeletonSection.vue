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
  margin-bottom: 24px;
}

.skeleton-section-head {
  margin-bottom: 12px;
}

.skeleton-section-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.skeleton-section-description {
  margin: 6px 0 0;
  font-size: 14px;
  line-height: 1.5;
  color: #6b7280;
}

.skeleton-section-body {
  border-radius: 14px;
  border: 1px solid #eef2f7;
  background: #ffffff;
  padding: 24px;
  color: #374151;
  font-size: 14px;
  line-height: 1.5;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.skeleton-section-body--empty {
  background: #fffaf0;
  border-color: rgba(245, 158, 11, 0.32);
  color: #b7791f;
}
</style>

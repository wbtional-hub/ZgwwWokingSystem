<template>
  <section v-if="guide" class="ai-guide-card">
    <div class="ai-guide-card__top">
      <div>
        <div class="ai-guide-card__eyebrow">AI Page Guide</div>
        <div class="ai-guide-card__title">页面说明卡</div>
      </div>
      <router-link
        v-if="guide.next?.path"
        :to="guide.next.path"
        class="ai-guide-card__next"
      >
        下一步：{{ guide.next.label }}
      </router-link>
    </div>

    <div class="ai-guide-card__grid">
      <section class="ai-guide-card__section">
        <h3>功能说明</h3>
        <p>{{ guide.feature }}</p>
      </section>
      <section class="ai-guide-card__section">
        <h3>适用角色</h3>
        <p>{{ guide.roles }}</p>
      </section>
      <section class="ai-guide-card__section ai-guide-card__section--steps">
        <h3>操作步骤</h3>
        <ol>
          <li v-for="(step, index) in guide.steps || []" :key="`${guideKey}-${index}`">
            第{{ index + 1 }}步：{{ step }}
          </li>
        </ol>
      </section>
      <section class="ai-guide-card__section">
        <h3>常见问题</h3>
        <ul>
          <li v-for="(item, index) in guide.faqs || []" :key="`${guideKey}-faq-${index}`">{{ item }}</li>
        </ul>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'
import { getAiPageGuide } from '@/config/aiPageGuides'

const props = defineProps({
  guideKey: {
    type: String,
    required: true
  }
})

const guide = computed(() => getAiPageGuide(props.guideKey))
</script>

<style scoped>
.ai-guide-card {
  margin-bottom: 18px;
  padding: 18px 20px;
  border: 1px solid #dbeafe;
  border-radius: 16px;
  background:
    radial-gradient(circle at top right, rgba(59, 130, 246, 0.12), transparent 24%),
    linear-gradient(180deg, #f8fbff 0%, #f3f8ff 100%);
}

.ai-guide-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.ai-guide-card__eyebrow {
  color: #2563eb;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.ai-guide-card__title {
  margin-top: 6px;
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
}

.ai-guide-card__next {
  display: inline-flex;
  align-items: center;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2563eb;
  text-decoration: none;
  font-size: 13px;
  font-weight: 600;
}

.ai-guide-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.ai-guide-card__section {
  padding: 14px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.82);
}

.ai-guide-card__section h3 {
  margin: 0 0 10px;
  color: #111827;
  font-size: 14px;
  font-weight: 700;
}

.ai-guide-card__section p,
.ai-guide-card__section li {
  color: #475569;
  font-size: 13px;
  line-height: 1.7;
}

.ai-guide-card__section p,
.ai-guide-card__section ol,
.ai-guide-card__section ul {
  margin: 0;
  padding-left: 0;
}

.ai-guide-card__section--steps {
  grid-column: span 2;
}

.ai-guide-card__section ol,
.ai-guide-card__section ul {
  list-style: none;
}

.ai-guide-card__section li + li {
  margin-top: 8px;
}

@media (max-width: 900px) {
  .ai-guide-card {
    padding: 16px;
  }

  .ai-guide-card__top,
  .ai-guide-card__grid {
    grid-template-columns: 1fr;
    flex-direction: column;
  }

  .ai-guide-card__section--steps {
    grid-column: auto;
  }
}
</style>

<template>
  <div class="profile-page">
    <div class="mobile-block">
      <div class="mobile-card">
        <div class="mobile-icon">AI</div>
        <div class="mobile-title">我的画像暂只支持 PC 端查看</div>
        <div class="mobile-desc">为了保证数字人画像展示效果，请在电脑端打开。</div>
      </div>
    </div>

    <div class="profile-shell">
      <div class="hero-card">
        <div class="hero-left">
          <div class="eyebrow">AI USER PROFILE</div>
          <h1>我的画像数字人</h1>
          <p>
            系统会根据你的历史咨询内容，沉淀学历身份、所在地、关注政策、权益偏好和使用习惯，
            用于让政策咨询助手更懂你的问题。
          </p>

          <div class="hero-stats">
            <div class="stat-card">
              <span>{{ activeItemCount }}</span>
              <label>画像属性</label>
            </div>
            <div class="stat-card">
              <span>{{ activeHabitCount }}</span>
              <label>习惯标签</label>
            </div>
            <div class="stat-card">
              <span>{{ summaryStatus }}</span>
              <label>摘要状态</label>
            </div>
          </div>
        </div>

        <div class="digital-human-panel">
          <div class="orbit orbit-one"></div>
          <div class="orbit orbit-two"></div>
          <div class="avatar-card">
            <div class="avatar-glow"></div>
            <div class="avatar-core">
              <div class="avatar-hair"></div>
              <div class="avatar-face">
                <div class="avatar-eye left"></div>
                <div class="avatar-eye right"></div>
                <div class="avatar-nose"></div>
                <div class="avatar-mouth"></div>
              </div>
              <div class="avatar-neck"></div>
              <div class="avatar-body"></div>
            </div>
            <div class="avatar-name">政策咨询数字人</div>
            <div class="avatar-subtitle">已绑定你的个性化画像</div>
          </div>

          <div class="floating-chip chip-one">{{ mainEducation }}</div>
          <div class="floating-chip chip-two">{{ mainCity }}</div>
          <div class="floating-chip chip-three">{{ mainPolicy }}</div>
        </div>
      </div>

      <div class="content-grid">
        <section class="panel panel-large">
          <div class="panel-header">
            <div>
              <div class="panel-kicker">PROFILE ATTRIBUTES</div>
              <h2>画像属性</h2>
            </div>
            <button class="ghost-button" @click="loadProfile">刷新画像</button>
             <button class="danger-button" @click="handleClearProfile">清空画像</button>

          </div>

          <div v-if="loading" class="empty-box">正在读取用户画像...</div>

          <div v-else-if="items.length === 0" class="empty-box">
            暂无画像属性。你可以在政策咨询中主动说明：我是本科生、我在厦门、我关注住房补贴等。
          </div>

          <div v-else class="attribute-grid">
            <div
              v-for="item in items"
              :key="`item-${item.id}`"
              class="attribute-card"
            >
              <div class="attr-top">
  <span class="attr-domain">{{ domainLabel(item.profile_domain) }}</span>
  <div class="attr-actions">
    <span class="attr-confidence">{{ confidenceText(item.confidence) }}</span>
    <button class="mini-delete-button" @click="handleDeleteItem(item)">删除</button>
  </div>
</div>
              <div class="attr-title">{{ item.display_label || item.value_text || '-' }}</div>
              <div class="attr-meta">
                {{ profileKeyLabel(item.profile_key) }} · {{ item.profile_cardinality || 'SINGLE' }}
              </div>
              <div class="attr-source" :title="item.source_text">
                来源：{{ item.source_text || '暂无来源文本' }}
              </div>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="panel-header simple">
            <div>
              <div class="panel-kicker">HABITS</div>
              <h2>用户习惯</h2>
            </div>
          </div>

          <div v-if="habits.length === 0" class="empty-box small">
            暂无习惯标签。
          </div>

          <div v-else class="habit-list">
            <div
              v-for="habit in habits"
              :key="`habit-${habit.id}`"
              class="habit-item"
            >
              <div class="habit-icon">{{ habitIcon(habit.habit_key) }}</div>
              <div class="habit-info">
                <div class="habit-title">{{ habit.display_label || habit.habit_value_text }}</div>
                <div class="habit-desc">
                  {{ habitKeyLabel(habit.habit_key) }}
                  <span v-if="habit.hit_count"> · 命中 {{ habit.hit_count }} 次</span>
                </div>
              </div>
            </div>
          </div>
        </section>

        <section class="panel prompt-panel">
          <div class="panel-header simple">
            <div>
              <div class="panel-kicker">PROMPT MEMORY</div>
              <h2>AI 可调用画像摘要</h2>
            </div>
          </div>

          <pre class="prompt-box">{{ promptSummary }}</pre>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import {
  clearCurrentUserProfile,
  deleteUserProfileItem,
  getCurrentUserProfile
} from '@/api/ai-user-profile'

const loading = ref(false)
const profile = ref({
  items: [],
  habits: [],
  summary: null
})

const items = computed(() => Array.isArray(profile.value.items) ? profile.value.items : [])
const habits = computed(() => Array.isArray(profile.value.habits) ? profile.value.habits : [])
const summary = computed(() => profile.value.summary || {})

const activeItemCount = computed(() => items.value.length)
const activeHabitCount = computed(() => habits.value.length)
const summaryStatus = computed(() => {
  if (!activeItemCount.value && !activeHabitCount.value) {
    return '已清空'
  }
  if (summary.value?.stale || summary.value?.is_stale) {
    return '待更新'
  }
  return summary.value?.prompt_summary ? '已生成' : '未生成'
})

const promptSummary = computed(() => {
  if (!activeItemCount.value && !activeHabitCount.value) {
    return '画像已清空。后续政策咨询中，系统会重新从你的明确表述和咨询习惯中学习画像。'
  }
  return summary.value?.prompt_summary || '暂无 Prompt 画像摘要。'
})

const mainEducation = computed(() => findItemLabel('education_level') || '身份待完善')
const mainCity = computed(() => findItemLabel('current_city') || findItemLabel('work_city') || '城市待完善')
const mainPolicy = computed(() => findItemLabel('policy_interest') || '政策偏好待完善')

function unwrapResponse(res) {
  if (!res) return {}
  if (res.data && (res.data.items || res.data.habits || res.data.summary)) {
    return res.data
  }
  if (res.data && res.data.data) {
    return res.data.data
  }
  if (res.items || res.habits || res.summary) {
    return res
  }
  return res.data || res || {}
}

async function loadProfile() {
  loading.value = true
  try {
    const res = await getCurrentUserProfile()
    profile.value = unwrapResponse(res)
  } catch (error) {
    showToast('读取用户画像失败')
    profile.value = {
      items: [],
      habits: [],
      summary: null
    }
  } finally {
    loading.value = false
  }
}

async function handleDeleteItem(item) {
  if (!item?.id) {
    showToast('画像项不存在')
    return
  }

  try {
    await showConfirmDialog({
      title: '删除画像属性',
      message: `确定删除“${item.display_label || item.value_text || '该画像'}”吗？删除后，后续回答将不再参考这条画像。`,
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })

    await deleteUserProfileItem(item.id)
    showToast('已删除')
    await loadProfile()
  } catch (error) {
    // 用户取消时也会进入 catch，这里不提示错误
  }
}

async function handleClearProfile() {
  try {
    await showConfirmDialog({
      title: '清空我的画像',
      message: '确定清空当前所有画像和习惯标签吗？清空后，AI 将重新从后续咨询中学习你的画像。',
      confirmButtonText: '清空',
      cancelButtonText: '取消'
    })

    await clearCurrentUserProfile()
    showToast('画像已清空')
    await loadProfile()
  } catch (error) {
    // 用户取消时不提示
  }
}

function findItemLabel(profileKey) {
  const found = items.value.find(item => item.profile_key === profileKey)
  return found?.display_label || found?.value_text || ''
}

function profileKeyLabel(key) {
  const map = {
  education_level: '学历/身份',
  gender: '性别',
  age_range: '年龄阶段',
  current_city: '当前所在地',
  work_city: '工作城市',
  target_city: '目标发展城市',
  employment_status: '工作状态',
  work_experience: '从业经验',
  startup_status: '创业状态',
  industry_direction: '行业方向',
  policy_interest: '关注政策',
  benefit_interest: '关注权益'
}
  return map[key] || key || '画像属性'
}

function domainLabel(domain) {
  const map = {
    education: '教育背景',
    location: '城市位置',
    career: '职业状态',
    industry: '产业方向',
    policy: '政策关注',
    benefit: '权益诉求',
    basic: '基础画像'
  }
  return map[domain] || domain || '画像'
}

function habitKeyLabel(key) {
  const map = {
  answer_style: '回答偏好',
  device_preference: '使用设备偏好',
  frequent_topic: '常问主题',
  repeated_policy_interest: '反复关注政策',
  frequent_question_type: '高频问题类型',
  frequent_question_topic: '高频问题主题'
}
  return map[key] || key || '习惯'
}

function habitIcon(key) {
  const map = {
  answer_style: '答',
  device_preference: '端',
  frequent_topic: '问',
  repeated_policy_interest: '策',
  frequent_question_type: '频',
  frequent_question_topic: '题'
}
  return map[key] || '习'
}

function confidenceText(value) {
  if (value === null || value === undefined || value === '') {
    return '可信度 -'
  }
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '可信度 -'
  }
  return `可信度 ${Math.round(num * 100)}%`
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background:
    radial-gradient(circle at 8% 10%, rgba(51, 128, 255, 0.26), transparent 28%),
    radial-gradient(circle at 88% 12%, rgba(105, 73, 255, 0.20), transparent 30%),
    linear-gradient(135deg, #eef4ff 0%, #f7f9fc 42%, #edf3ff 100%);
  padding: 28px;
  color: #172033;
}

.profile-shell {
  max-width: 1440px;
  margin: 0 auto;
}

.hero-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) 520px;
  gap: 28px;
  min-height: 360px;
  border: 1px solid rgba(255, 255, 255, 0.68);
  border-radius: 32px;
  padding: 34px;
  overflow: hidden;
  background:
    linear-gradient(135deg, rgba(255, 255, 255, 0.84), rgba(241, 246, 255, 0.66)),
    linear-gradient(135deg, rgba(41, 112, 255, 0.06), rgba(116, 79, 255, 0.05));
  box-shadow: 0 28px 80px rgba(64, 93, 149, 0.18);
  backdrop-filter: blur(24px);
}

.hero-card::after {
  content: "";
  position: absolute;
  right: -120px;
  top: -140px;
  width: 360px;
  height: 360px;
  border-radius: 50%;
  background: rgba(67, 118, 255, 0.16);
  filter: blur(6px);
}

.hero-left {
  position: relative;
  z-index: 2;
  padding: 12px 0;
}

.eyebrow,
.panel-kicker {
  color: #3d6df0;
  letter-spacing: 0.18em;
  font-size: 12px;
  font-weight: 800;
}

.hero-left h1 {
  margin: 16px 0 14px;
  font-size: 46px;
  line-height: 1.08;
  color: #111827;
  letter-spacing: -0.04em;
}

.hero-left p {
  max-width: 680px;
  color: #5d6b83;
  line-height: 1.9;
  font-size: 16px;
}

.hero-stats {
  display: flex;
  gap: 16px;
  margin-top: 34px;
}

.stat-card {
  width: 138px;
  padding: 18px 16px;
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.74);
  border: 1px solid rgba(219, 227, 245, 0.9);
  box-shadow: 0 16px 36px rgba(77, 104, 158, 0.12);
}

.stat-card span {
  display: block;
  font-size: 28px;
  font-weight: 900;
  color: #1f4fd6;
}

.stat-card label {
  display: block;
  margin-top: 5px;
  font-size: 13px;
  color: #6b7280;
}

.digital-human-panel {
  position: relative;
  z-index: 2;
  min-height: 320px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.orbit {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(51, 105, 255, 0.24);
}

.orbit-one {
  width: 330px;
  height: 330px;
  animation: rotateOrbit 16s linear infinite;
}

.orbit-two {
  width: 250px;
  height: 250px;
  border-style: dashed;
  animation: rotateOrbit 11s linear infinite reverse;
}

.avatar-card {
  position: relative;
  width: 230px;
  height: 300px;
  border-radius: 32px;
  background: linear-gradient(180deg, rgba(255,255,255,0.92), rgba(230,238,255,0.78));
  border: 1px solid rgba(255,255,255,0.88);
  box-shadow:
    0 26px 70px rgba(44, 80, 151, 0.23),
    inset 0 0 38px rgba(255,255,255,0.74);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.avatar-glow {
  position: absolute;
  width: 158px;
  height: 158px;
  top: 30px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(88, 139, 255, 0.32), transparent 66%);
}

.avatar-core {
  position: relative;
  width: 142px;
  height: 205px;
  margin-top: 10px;
}

.avatar-hair {
  position: absolute;
  left: 25px;
  top: 8px;
  width: 92px;
  height: 86px;
  border-radius: 48% 52% 42% 42%;
  background: linear-gradient(135deg, #1d2433, #3b465f);
  z-index: 2;
}

.avatar-face {
  position: absolute;
  left: 31px;
  top: 34px;
  width: 80px;
  height: 98px;
  border-radius: 42% 42% 46% 46%;
  background: linear-gradient(180deg, #f4cfb8, #e6a98e);
  box-shadow: inset 0 -10px 22px rgba(160, 84, 62, 0.12);
  z-index: 3;
}

.avatar-eye {
  position: absolute;
  top: 38px;
  width: 9px;
  height: 5px;
  border-radius: 9px;
  background: #1e293b;
}

.avatar-eye.left {
  left: 22px;
}

.avatar-eye.right {
  right: 22px;
}

.avatar-nose {
  position: absolute;
  left: 38px;
  top: 48px;
  width: 5px;
  height: 18px;
  border-radius: 8px;
  background: rgba(148, 82, 64, 0.25);
}

.avatar-mouth {
  position: absolute;
  left: 31px;
  top: 72px;
  width: 20px;
  height: 6px;
  border-radius: 0 0 18px 18px;
  border-bottom: 2px solid rgba(118, 50, 55, 0.46);
}

.avatar-neck {
  position: absolute;
  left: 57px;
  top: 122px;
  width: 28px;
  height: 30px;
  background: #dda084;
  border-radius: 12px;
  z-index: 1;
}

.avatar-body {
  position: absolute;
  left: 15px;
  top: 148px;
  width: 112px;
  height: 72px;
  border-radius: 42px 42px 18px 18px;
  background: linear-gradient(135deg, #2d5bdb, #7c5cff);
  box-shadow: inset 0 0 24px rgba(255,255,255,0.22);
}

.avatar-name {
  margin-top: 8px;
  font-size: 16px;
  font-weight: 800;
  color: #111827;
}

.avatar-subtitle {
  margin-top: 5px;
  color: #64748b;
  font-size: 13px;
}

.floating-chip {
  position: absolute;
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(211, 222, 247, 0.86);
  box-shadow: 0 14px 32px rgba(66, 89, 139, 0.16);
  color: #1f3b76;
  font-weight: 700;
  font-size: 13px;
  backdrop-filter: blur(14px);
}

.chip-one {
  left: 34px;
  top: 52px;
}

.chip-two {
  right: 30px;
  top: 82px;
}

.chip-three {
  right: 68px;
  bottom: 42px;
}

.content-grid {
  margin-top: 24px;
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(360px, 0.8fr);
  gap: 24px;
}

.panel {
  border-radius: 28px;
  padding: 24px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(226, 233, 247, 0.92);
  box-shadow: 0 20px 56px rgba(63, 88, 137, 0.12);
}

.panel-large {
  grid-row: span 2;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.panel-header.simple {
  align-items: flex-start;
}

.panel h2 {
  margin: 6px 0 0;
  color: #111827;
  font-size: 24px;
  letter-spacing: -0.02em;
}

.ghost-button {
  height: 38px;
  padding: 0 18px;
  border: 1px solid rgba(71, 111, 245, 0.24);
  border-radius: 999px;
  color: #2f5be8;
  background: rgba(241, 246, 255, 0.82);
  cursor: pointer;
  font-weight: 700;
}
.button-group {
  display: flex;
  align-items: center;
  gap: 10px;
}

.danger-button {
  height: 38px;
  padding: 0 18px;
  border: 1px solid rgba(239, 68, 68, 0.22);
  border-radius: 999px;
  color: #dc2626;
  background: rgba(254, 242, 242, 0.86);
  cursor: pointer;
  font-weight: 700;
}

.attr-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.mini-delete-button {
  height: 26px;
  padding: 0 10px;
  border: 1px solid rgba(239, 68, 68, 0.18);
  border-radius: 999px;
  color: #dc2626;
  background: rgba(254, 242, 242, 0.82);
  cursor: pointer;
  font-size: 12px;
  font-weight: 700;
}
.attribute-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.attribute-card {
  min-height: 148px;
  padding: 18px;
  border-radius: 22px;
  background:
    linear-gradient(180deg, rgba(255,255,255,0.92), rgba(245,248,255,0.82));
  border: 1px solid rgba(221, 230, 249, 0.88);
  box-shadow: 0 12px 32px rgba(73, 97, 145, 0.08);
}

.attr-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.attr-domain {
  font-size: 12px;
  color: #2f5be8;
  background: rgba(47, 91, 232, 0.08);
  padding: 6px 10px;
  border-radius: 999px;
  font-weight: 800;
}

.attr-confidence {
  font-size: 12px;
  color: #64748b;
}

.attr-title {
  margin-top: 18px;
  font-size: 22px;
  font-weight: 900;
  color: #172033;
}

.attr-meta {
  margin-top: 6px;
  font-size: 13px;
  color: #718096;
}

.attr-source {
  margin-top: 14px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.habit-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.habit-item {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff, #f7f9ff);
  border: 1px solid #e4ebf8;
}

.habit-icon {
  width: 44px;
  height: 44px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #2f5be8, #8b5cf6);
  color: #fff;
  font-weight: 900;
}

.habit-title {
  font-size: 16px;
  font-weight: 800;
  color: #172033;
}

.habit-desc {
  margin-top: 5px;
  color: #64748b;
  font-size: 13px;
}

.prompt-panel {
  min-height: 260px;
}

.prompt-box {
  margin: 0;
  padding: 18px;
  min-height: 188px;
  border-radius: 20px;
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.8;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", monospace;
  font-size: 13px;
  color: #334155;
  background:
    linear-gradient(180deg, rgba(248, 250, 255, 0.96), rgba(241, 245, 255, 0.96));
  border: 1px solid #e1e9f8;
}

.empty-box {
  min-height: 180px;
  border-radius: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  background: rgba(248, 250, 255, 0.78);
  border: 1px dashed #d5def2;
  text-align: center;
  padding: 20px;
}

.empty-box.small {
  min-height: 120px;
}

.mobile-block {
  display: none;
}

@keyframes rotateOrbit {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 900px) {
  .profile-page {
    padding: 18px;
  }

  .profile-shell {
    display: none;
  }

  .mobile-block {
    min-height: calc(100vh - 36px);
    display: flex;
    align-items: center;
    justify-content: center;
  }

  .mobile-card {
    width: 100%;
    max-width: 360px;
    padding: 28px;
    border-radius: 26px;
    background: rgba(255, 255, 255, 0.9);
    box-shadow: 0 18px 50px rgba(63, 88, 137, 0.16);
    text-align: center;
  }

  .mobile-icon {
    width: 66px;
    height: 66px;
    margin: 0 auto 18px;
    border-radius: 22px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #2f5be8, #8b5cf6);
    color: #fff;
    font-weight: 900;
    font-size: 22px;
  }

  .mobile-title {
    font-size: 18px;
    font-weight: 900;
    color: #111827;
  }

  .mobile-desc {
    margin-top: 10px;
    color: #64748b;
    line-height: 1.7;
  }
}

@media (max-width: 1180px) {
  .hero-card {
    grid-template-columns: 1fr;
  }

  .content-grid {
    grid-template-columns: 1fr;
  }

  .attribute-grid {
    grid-template-columns: 1fr;
  }
}
</style>
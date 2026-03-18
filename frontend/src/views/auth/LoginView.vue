<template>
  <div class="login-page">
    <h2>讲师团系统登录</h2>
    <van-form @submit="handleSubmit">
      <van-field v-model="form.username" label="账号" placeholder="请输入账号" />
      <van-field v-model="form.password" type="password" label="密码" placeholder="请输入密码" />
      <div style="margin: 16px;">
        <van-button round block type="primary" native-type="submit" :loading="state.submitting">登录</van-button>
      </div>
    </van-form>
    <p class="login-tip">当前最小闭环演示账号：admin / admin123</p>
  </div>
</template>

<script setup>
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { loginApi } from '@/api/auth'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: 'admin', password: 'admin123' })
const state = reactive({ submitting: false })

const handleSubmit = async () => {
  state.submitting = true
  try {
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    userStore.clearLogin()

    const payload = {
      username: form.username,
      password: form.password
    }
    const res = await loginApi(payload)
    if (res.code !== 0 || !res.data?.token) {
      throw new Error(res.message || '登录失败')
    }
    localStorage.setItem('token', res.data.token)
    localStorage.setItem('userInfo', JSON.stringify(res.data))
    userStore.setLogin(res.data)
    showToast('登录成功')
    router.push('/home')
  } catch (error) {
    const message = error.response?.data?.message || error.message || '登录失败'
    console.error('[login failed]', error.response?.data || error)
    showToast(message)
  } finally {
    state.submitting = false
  }
}
</script>

<style scoped>
.login-page {
  max-width: 420px;
  margin: 48px auto;
  padding: 24px 16px;
}

.login-tip {
  margin-top: 12px;
  color: #666;
  font-size: 12px;
}
</style>

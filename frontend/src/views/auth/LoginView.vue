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
import { queryCurrentUserModulePermissionsApi } from '@/api/user-module-permission'
import { buildAccessContext, findFirstAccessiblePath } from '@/constants/modules'

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
    userStore.setLogin(res.data)
    const moduleResponse = await queryCurrentUserModulePermissionsApi()
    if (moduleResponse.code !== 0) {
      throw new Error(moduleResponse.message || '模块权限加载失败')
    }
    userStore.setAccessContext({
      userInfo: res.data,
      moduleCodes: Array.isArray(moduleResponse.data?.moduleCodes) ? moduleResponse.data.moduleCodes : []
    })
    showToast('登录成功')
    router.push(findFirstAccessiblePath(buildAccessContext(userStore.userInfo)))
  } catch (error) {
    const message = error.response?.data?.message || error.message || '登录失败'
    console.error('[login failed]', error.response?.data || error)
    showToast(message)
    userStore.clearLogin()
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

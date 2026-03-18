import { defineStore } from 'pinia'
import {
  createUserApi,
  deleteUserApi,
  queryUserDetailApi,
  queryUserPageApi,
  resetUserPasswordApi,
  updateUserApi
} from '@/api/user'

function ensureSuccess(response) {
  if (!response || response.code !== 0) {
    throw new Error(response?.message || '请求失败')
  }
  return response.data
}

export const useUserManagementStore = defineStore('userManagement', {
  state: () => ({
    list: [],
    total: 0,
    scopeUserCount: 0,
    scopeType: '',
    scopeDescription: '',
    pageNo: 1,
    pageSize: 10,
    keywords: '',
    status: '',
    loading: false,
    submitting: false,
    detailLoading: false,
    currentDetail: null
  }),
  actions: {
    async fetchPage(extra = {}) {
      this.loading = true
      try {
        if (Object.prototype.hasOwnProperty.call(extra, 'pageNo')) {
          this.pageNo = extra.pageNo
        }
        if (Object.prototype.hasOwnProperty.call(extra, 'pageSize')) {
          this.pageSize = extra.pageSize
        }
        if (Object.prototype.hasOwnProperty.call(extra, 'keywords')) {
          this.keywords = typeof extra.keywords === 'string' ? extra.keywords.trim() : extra.keywords
        }
        if (Object.prototype.hasOwnProperty.call(extra, 'status')) {
          this.status = extra.status
        }

        const requestParams = {
          pageNo: this.pageNo,
          pageSize: this.pageSize,
          keywords: typeof this.keywords === 'string' ? this.keywords.trim() : this.keywords,
          status: this.status === '' ? undefined : this.status
        }
        let data = ensureSuccess(await queryUserPageApi(requestParams))
        const total = data.total || 0
        const maxPage = Math.max(1, Math.ceil(total / this.pageSize))
        if (total > 0 && this.pageNo > maxPage) {
          this.pageNo = maxPage
          data = ensureSuccess(await queryUserPageApi({
            ...requestParams,
            pageNo: this.pageNo
          }))
        }
        this.list = data.list || []
        this.total = data.total || 0
        this.scopeUserCount = data.scopeUserCount || 0
        this.scopeType = data.scopeType || ''
        this.scopeDescription = data.scopeDescription || ''
      } finally {
        this.loading = false
      }
    },
    async fetchDetail(userId) {
      this.detailLoading = true
      try {
        this.currentDetail = ensureSuccess(await queryUserDetailApi(userId))
        return this.currentDetail
      } finally {
        this.detailLoading = false
      }
    },
    async createUser(payload) {
      this.submitting = true
      try {
        return ensureSuccess(await createUserApi(payload))
      } finally {
        this.submitting = false
      }
    },
    async updateUser(userId, payload) {
      this.submitting = true
      try {
        return ensureSuccess(await updateUserApi(userId, payload))
      } finally {
        this.submitting = false
      }
    },
    async deleteUser(userId) {
      this.submitting = true
      try {
        return ensureSuccess(await deleteUserApi(userId))
      } finally {
        this.submitting = false
      }
    },
    async resetPassword(userId) {
      this.submitting = true
      try {
        return ensureSuccess(await resetUserPasswordApi(userId))
      } finally {
        this.submitting = false
      }
    },
    clearDetail() {
      this.detailLoading = false
      this.currentDetail = null
    }
  }
})

import request from '@/utils/request'

export function queryKnowledgeBaseList(data) {
  return request.post('/knowledge/base/list', data)
}

export function saveKnowledgeBase(data) {
  return request.post('/knowledge/base/save', data)
}

export function toggleKnowledgeBaseStatus(data) {
  return request.post('/knowledge/base/toggle-status', data)
}

export function queryKnowledgeDocumentList(data) {
  return request.post('/knowledge/document/list', data)
}

export function getKnowledgeDocumentDetail(id) {
  return request.get(`/knowledge/document/${id}`)
}

export function queryKnowledgeChunkList(data) {
  return request.post('/knowledge/chunk/list', data)
}

export function importKnowledgeDocx(formData) {
  return request.post('/knowledge/document/import-docx', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function searchKnowledge(data) {
  return request.post('/knowledge/search', data)
}
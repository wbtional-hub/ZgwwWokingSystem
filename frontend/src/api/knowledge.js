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

export function setCurrentKnowledgeBase(data) {
  return request.post('/knowledge/base/set-current', data)
}

export function deleteKnowledgeBase(id) {
  return request.delete(`/knowledge/base/${id}`)
}

export function previewWebKnowledge(data) {
  return request.post('/knowledge/web-preview', data)
}

export function importWebKnowledge(data) {
  return request.post('/knowledge/web-import', data)
}

export function previewSnapshotKnowledge(formData) {
  return request.post('/knowledge/snapshot-preview', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function importSnapshotKnowledge(formData) {
  return request.post('/knowledge/snapshot-import', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
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

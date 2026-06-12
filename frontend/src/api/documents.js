import { createRequest } from './http'

const request = createRequest({
  baseURL: '/',
  timeout: 120000
})

export async function uploadDocument(file) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await request.post('/api/documents/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return data
}

export async function generateDocumentSummary(fileId) {
  const { data } = await request.post(`/api/documents/${fileId}/summary`)
  return data
}

export async function askDocumentQuestion(fileId, question, history = []) {
  const { data } = await request.post(`/api/documents/${fileId}/qa`, { question, history })
  return data
}

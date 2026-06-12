import { createRequest } from './http'

const request = createRequest({
  baseURL: '/',
  timeout: 120000
})

export async function uploadPdf(file) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await request.post('/api/pdf/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return data
}

export async function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await request.post('/api/pdf/image/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
  return data
}

export async function fetchPdfContent(fileId) {
  const { data } = await request.get(`/api/pdf/${fileId}`)
  return data
}

export async function fetchImageContent(fileId) {
  const { data } = await request.get(`/api/pdf/image/${fileId}`)
  return data
}

export async function runPdfOcr(fileId) {
  const { data } = await request.post(`/api/pdf/${fileId}/ocr`)
  return data
}

export async function runImageOcr(fileId) {
  const { data } = await request.post(`/api/pdf/image/${fileId}/ocr`)
  return data
}

export async function generatePdfSummary(fileId) {
  const { data } = await request.post(`/api/pdf/${fileId}/summary`)
  return data
}

export async function askPdfQuestion(fileId, question, history = []) {
  const { data } = await request.post(`/api/pdf/${fileId}/qa`, { question, history })
  return data
}

export async function describePdfImage(fileId, imageUrl) {
  const { data } = await request.post(`/api/pdf/${fileId}/images/describe`, { imageUrl })
  return data
}

export async function describeUploadedImage(fileId) {
  const { data } = await request.post(`/api/pdf/image/${fileId}/describe`)
  return data
}

export async function inspectRedisCapability() {
  const { data } = await request.get('/api/pdf/rag/redis-capability')
  return data
}

export async function buildPdfRagIndex(fileId) {
  const { data } = await request.post(`/api/pdf/${fileId}/rag/index`)
  return data
}

export async function askPdfRagQuestion(fileId, question, history = []) {
  const { data } = await request.post(`/api/pdf/${fileId}/rag/qa`, { question, history })
  return data
}

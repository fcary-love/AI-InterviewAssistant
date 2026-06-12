import { createRequest } from './http'

const request = createRequest({
  baseURL: '/',
  timeout: 180000
})

export async function analyzeJobMatch(resumeFileId, jdText) {
  const { data } = await request.post('/api/jobs/match', {
    resumeFileId,
    jdText
  })
  return data
}

export async function fetchJobMatches(limit = 30) {
  const { data } = await request.get('/api/jobs', {
    params: { limit }
  })
  return data
}

export async function fetchJobMatchDetail(id) {
  const { data } = await request.get(`/api/jobs/${id}`)
  return data
}

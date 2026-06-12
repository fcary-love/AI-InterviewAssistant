import { createRequest } from './http'

const request = createRequest({
  baseURL: '/',
  timeout: 180000
})

export async function fetchJobProjects(limit = 30) {
  const { data } = await request.get('/api/job-projects', {
    params: { limit }
  })
  return data
}

export async function createJobProject(payload) {
  const { data } = await request.post('/api/job-projects', payload)
  return data
}

export async function fetchJobProjectDetail(id) {
  const { data } = await request.get(`/api/job-projects/${id}`)
  return data
}

export async function matchJobProject(id) {
  const { data } = await request.post(`/api/job-projects/${id}/match`)
  return data
}

export async function generateTailoredResume(id) {
  const { data } = await request.post(`/api/job-projects/${id}/tailored-resume`)
  return data
}

export async function updateJobProjectStatus(id, status) {
  const { data } = await request.patch(`/api/job-projects/${id}/status`, { status })
  return data
}

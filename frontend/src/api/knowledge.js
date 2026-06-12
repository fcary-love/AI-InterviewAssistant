import { createRequest } from './http'

const request = createRequest({
  baseURL: '/',
  timeout: 30000
})

export async function fetchSkillTree(direction = '后端开发') {
  const { data } = await request.get('/api/knowledge/tree', { params: { direction } })
  return data
}

export async function fetchWeakPoints() {
  const { data } = await request.get('/api/knowledge/weak-points')
  return data
}

export async function fetchStudySuggestions(direction = '后端开发') {
  const { data } = await request.get('/api/knowledge/suggestions', { params: { direction } })
  return data
}

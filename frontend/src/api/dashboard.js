import { createRequest } from './http'

const request = createRequest({
  baseURL: '/',
  timeout: 30000
})

export async function fetchGrowthDashboard() {
  const { data } = await request.get('/api/dashboard/growth')
  return data
}

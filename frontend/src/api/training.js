import { createRequest } from './http'

const request = createRequest({
  baseURL: '/',
  timeout: 30000
})

export async function fetchTrainingOverview() {
  const { data } = await request.get('/api/training/overview')
  return data
}

export async function fetchTrainingTasks() {
  const { data } = await request.get('/api/training/tasks')
  return data
}

export async function generateTrainingPlan() {
  const { data } = await request.post('/api/training/generate')
  return data
}

export async function updateTrainingTaskStatus(id, status) {
  const { data } = await request.patch(`/api/training/tasks/${id}/status`, { status })
  return data
}

export async function deleteTrainingTask(id) {
  const { data } = await request.delete(`/api/training/tasks/${id}`)
  return data
}

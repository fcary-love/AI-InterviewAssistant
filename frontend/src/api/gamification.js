import { createRequest } from './http'

const request = createRequest({ timeout: 15000 })

export function fetchGamificationSummary() {
  return request.get('/api/gamification/summary')
}

export function fetchAchievements() {
  return request.get('/api/gamification/achievements')
}

export function fetchDailyTasks() {
  return request.get('/api/gamification/daily-tasks')
}

export function claimDailyTask(taskType) {
  return request.post('/api/gamification/daily-tasks/claim', { taskType })
}

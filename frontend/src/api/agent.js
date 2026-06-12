import { createRequest } from './http'

const request = createRequest({ timeout: 180000 })

export async function sendAgentMessage(sessionId, message) {
  const { data } = await request.post('/api/agent/chat', { sessionId, message })
  return data
}

export async function fetchAgentSessions() {
  const { data } = await request.get('/api/agent/sessions')
  return data
}

export async function fetchAgentSessionMessages(sessionId) {
  const { data } = await request.get(`/api/agent/sessions/${sessionId}`)
  return data
}

export async function deleteAgentSession(sessionId) {
  const { data } = await request.delete(`/api/agent/sessions/${sessionId}`)
  return data
}

import { createRequest } from './http'
import { streamPost } from '../utils/sseClient'

const request = createRequest({
  baseURL: '/',
  timeout: 30000
})

export async function startInterview(payload) {
  const { data } = await request.post('/api/interviews/start', payload)
  return data
}

export async function submitInterviewAnswer(sessionId, payload) {
  const { data } = await request.post(`/api/interviews/${sessionId}/answer`, payload)
  return data
}

export function streamInterviewAnswer(sessionId, payload, { onToken, onDone, onError, signal }) {
  return streamPost(`/api/interviews/${sessionId}/answer/stream`, payload, {
    onToken,
    onDone,
    onError,
    signal
  })
}

export async function generateInterviewReport(sessionId, payload = {}) {
  const { data } = await request.post(`/api/interviews/${sessionId}/report`, payload)
  return data
}

export async function refineInterviewReport(sessionId, payload = {}) {
  const { data } = await request.post(`/api/interviews/${sessionId}/report/refine`, payload, {
    timeout: 180000
  })
  return data
}

export async function fetchInterviewReport(sessionId) {
  const { data } = await request.get(`/api/interviews/${sessionId}/report`)
  return data
}

export async function fetchInterviewReports() {
  const { data } = await request.get('/api/interviews/reports')
  return data
}

export async function deleteInterviewReport(sessionId) {
  const { data } = await request.delete(`/api/interviews/${sessionId}/report`)
  return data
}

export async function fetchInterviewTurns(sessionId) {
  const { data } = await request.get(`/api/interviews/${sessionId}/turns`)
  return data
}

export async function fetchEloTrajectory(sessionId) {
  const { data } = await request.get(`/api/interviews/${sessionId}/elo-trajectory`)
  return data
}

export async function annotateKeywords(sessionId) {
  const { data } = await request.post(`/api/interviews/${sessionId}/keywords/annotate`)
  return data
}

export async function fetchKeywords(sessionId) {
  const { data } = await request.get(`/api/interviews/${sessionId}/keywords`)
  return data
}

export async function fetchTimeAnalysis(sessionId) {
  const { data } = await request.get(`/api/interviews/${sessionId}/time-analysis`)
  return data
}

export async function compareInterviews(sessionId1, sessionId2) {
  const { data } = await request.post(`/api/interviews/compare`, null, {
    params: { sessionId1, sessionId2 }
  })
  return data
}

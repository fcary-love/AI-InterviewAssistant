import { createRequest } from './http'

const request = createRequest({
  baseURL: '/',
  timeout: 30000
})

export async function fetchQuestionOverview() {
  const { data } = await request.get('/api/questions/overview')
  return data
}

export async function fetchQuestions(params = {}) {
  const { data } = await request.get('/api/questions', { params })
  return data
}

export async function fetchWrongQuestions(params = {}) {
  const { data } = await request.get('/api/questions/wrong', { params })
  return data
}

export async function explainQuestion(question) {
  const { data } = await request.post('/api/questions/explain', { question }, {
    timeout: 120000
  })
  return data
}

export async function markQuestionReviewed(sessionId, questionNo) {
  const { data } = await request.post(`/api/questions/wrong/${sessionId}/${questionNo}/review`)
  return data
}

import { ref } from 'vue'
import { sendAgentMessage, fetchAgentSessions, fetchAgentSessionMessages, deleteAgentSession } from '../api/agent'

export function useAgentChat() {
  const sessions = ref([])
  const currentSessionId = ref(null)
  const messages = ref([])
  const inputText = ref('')
  const loading = ref(false)
  const error = ref('')

  async function sendMessage() {
    const text = inputText.value.trim()
    if (!text || loading.value) return

    inputText.value = ''
    error.value = ''

    // Add user message to UI immediately
    const userMsg = {
      id: Date.now(),
      role: 'user',
      content: text,
      createdAt: new Date().toISOString()
    }
    messages.value.push(userMsg)

    // Add placeholder for assistant response
    const assistantMsg = {
      id: Date.now() + 1,
      role: 'assistant',
      content: '',
      toolCalls: [],
      pending: true,
      createdAt: new Date().toISOString()
    }
    messages.value.push(assistantMsg)

    loading.value = true
    try {
      const response = await sendAgentMessage(currentSessionId.value, text)

      // Update session id if new
      if (!currentSessionId.value) {
        currentSessionId.value = response.data.sessionId
      }

      // Update assistant message
      assistantMsg.content = response.data.content
      assistantMsg.pending = false

      // Add tool call messages if any
      if (response.data.toolCalls && response.data.toolCalls.length > 0) {
        assistantMsg.toolCalls = response.data.toolCalls
      }

      // Refresh sessions list
      await loadSessions()
    } catch (e) {
      assistantMsg.pending = false
      assistantMsg.content = '抱歉，出现了错误：' + (e.response?.data?.message || e.message)
      error.value = e.response?.data?.message || e.message
    } finally {
      loading.value = false
    }
  }

  async function loadSessions() {
    try {
      const response = await fetchAgentSessions()
      sessions.value = response.data || []
    } catch (e) {
      // Silently fail
    }
  }

  async function switchSession(sessionId) {
    if (loading.value) return
    currentSessionId.value = sessionId
    try {
      const response = await fetchAgentSessionMessages(sessionId)
      messages.value = (response.data || []).map((m, i) => ({
        id: Date.now() + i,
        role: m.role,
        content: m.content,
        toolName: m.toolName,
        toolCallId: m.toolCallId,
        toolArguments: m.toolArguments,
        createdAt: m.createdAt
      }))
    } catch (e) {
      error.value = '加载会话失败'
    }
  }

  function startNewSession() {
    currentSessionId.value = null
    messages.value = []
    error.value = ''
  }

  async function removeSession(sessionId) {
    try {
      await deleteAgentSession(sessionId)
      if (currentSessionId.value === sessionId) {
        startNewSession()
      }
      await loadSessions()
    } catch (e) {
      error.value = '删除会话失败'
    }
  }

  return {
    sessions,
    currentSessionId,
    messages,
    inputText,
    loading,
    error,
    sendMessage,
    loadSessions,
    switchSession,
    startNewSession,
    removeSession
  }
}

import { ref, watch } from 'vue'
import { getStoredUser } from '../api/http'

const STORAGE_PREFIX = 'face_ai_coach_conversation_v1'
const MAX_MESSAGES = 80

function getStorageKey() {
  const user = getStoredUser()
  return `${STORAGE_PREFIX}_${user?.id || 'guest'}`
}

function createMessageId() {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

function loadMessages() {
  try {
    const raw = window.localStorage.getItem(getStorageKey())
    if (!raw) {
      return []
    }
    const messages = JSON.parse(raw)
    if (!Array.isArray(messages)) {
      return []
    }
    return messages.map((message) => ({
      ...message,
      typing: false
    }))
  } catch {
    return []
  }
}

export function useConversationHistory() {
  const conversationMessages = ref(loadMessages())

  watch(
    conversationMessages,
    (messages) => {
      window.localStorage.setItem(getStorageKey(), JSON.stringify(messages))
    },
    { deep: true }
  )

  function addConversationMessage(message) {
    const nextMessage = {
      id: createMessageId(),
      role: message.role || 'assistant',
      label: message.label || (message.role === 'user' ? '我' : '助手'),
      content: message.content || '',
      createdAt: new Date().toISOString(),
      typing: message.typing ?? message.role !== 'user',
      pending: message.pending || false,
      kind: message.kind || 'chat'
    }

    conversationMessages.value = [...conversationMessages.value, nextMessage].slice(-MAX_MESSAGES)
    return nextMessage
  }

  function updateConversationMessage(id, patch) {
    conversationMessages.value = conversationMessages.value.map((message) => {
      if (message.id !== id) {
        return message
      }
      return {
        ...message,
        ...patch
      }
    })
  }

  function clearConversationMessages() {
    conversationMessages.value = []
  }

  return {
    addConversationMessage,
    clearConversationMessages,
    conversationMessages,
    updateConversationMessage
  }
}

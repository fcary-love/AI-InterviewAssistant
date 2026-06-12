<template>
  <div class="agent-chat-layout">
    <AgentSessionList
      :sessions="sessions"
      :active-session-id="currentSessionId"
      @new-session="startNewSession"
      @switch-session="switchSession"
      @delete-session="removeSession"
    />

    <section class="agent-chat-main">
      <!-- Messages -->
      <div ref="messageScroller" class="agent-messages">
        <div v-if="messages.length === 0" class="agent-blank">
          <strong>AI 智能教练</strong>
          <p>分析你的面试薄弱点，制定个性化训练计划，追踪学习进度。</p>
          <div class="agent-prompts">
            <button
              v-for="prompt in quickPrompts"
              :key="prompt"
              type="button"
              :disabled="loading"
              @click="handleQuickPrompt(prompt)"
            >
              {{ prompt }}
            </button>
          </div>
        </div>

        <div
          v-for="msg in messages"
          :key="msg.id"
          class="agent-message"
          :class="[msg.role, { pending: msg.pending }]"
        >
          <!-- User message -->
          <div v-if="msg.role === 'user'" class="msg-bubble user-bubble">
            <span class="msg-label">你</span>
            <p>{{ msg.content }}</p>
          </div>

          <!-- Assistant message -->
          <div v-else-if="msg.role === 'assistant'" class="msg-bubble assistant-bubble">
            <span class="msg-label">AI 教练</span>

            <!-- Tool calls -->
            <div v-if="msg.toolCalls && msg.toolCalls.length > 0" class="tool-calls-section">
              <AgentToolCallCard
                v-for="(tc, idx) in msg.toolCalls"
                :key="idx"
                :tool-call="tc"
              />
            </div>

            <!-- Content -->
            <div v-if="msg.pending" class="thinking-indicator">
              <strong>思考中</strong>
              <i></i><i></i><i></i>
            </div>
            <p v-else class="msg-content">{{ msg.content }}</p>
          </div>

          <!-- Tool result message (from history) -->
          <div v-else-if="msg.role === 'tool'" class="msg-bubble tool-bubble">
            <span class="msg-label">{{ msg.toolName || '工具' }}</span>
            <pre class="tool-result-text">{{ msg.content }}</pre>
          </div>
        </div>
      </div>

      <!-- Input area -->
      <div class="agent-input-area">
        <div class="agent-input-row">
          <textarea
            v-model="inputText"
            class="agent-input"
            placeholder="输入你的问题，例如：分析我的薄弱点、帮我制定训练计划..."
            rows="2"
            :disabled="loading"
            @keydown.enter.exact.prevent="sendMessage"
          ></textarea>
          <button
            type="button"
            class="btn-send"
            :disabled="loading || !inputText.trim()"
            @click="sendMessage"
          >
            {{ loading ? '发送中...' : '发送' }}
          </button>
        </div>
        <p v-if="error" class="agent-error">{{ error }}</p>
      </div>
    </section>
  </div>
</template>

<script setup>
import { nextTick, onMounted, ref, watch } from 'vue'
import { useAgentChat } from '../../composables/useAgentChat'
import AgentSessionList from './AgentSessionList.vue'
import AgentToolCallCard from './AgentToolCallCard.vue'

const {
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
} = useAgentChat()

const messageScroller = ref(null)

const quickPrompts = [
  '分析我的面试薄弱点',
  '帮我制定一个训练计划',
  '我最近面试表现怎么样？',
  '推荐一些适合我的练习题'
]

onMounted(() => {
  loadSessions()
})

watch(
  () => messages.value.length,
  () => scrollToEnd()
)

watch(
  () => messages.value[messages.value.length - 1]?.content,
  () => scrollToEnd()
)

async function scrollToEnd() {
  await nextTick()
  if (messageScroller.value) {
    messageScroller.value.scrollTop = messageScroller.value.scrollHeight
  }
}

function handleQuickPrompt(prompt) {
  inputText.value = prompt
  sendMessage()
}
</script>

<style scoped>
.agent-chat-layout {
  display: flex;
  height: calc(100vh - 140px);
  min-height: 500px;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: rgba(39, 37, 47, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
}

.agent-chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.agent-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

.agent-blank {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;
  color: var(--text-secondary);
}

.agent-blank strong {
  font-size: 20px;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.agent-blank p {
  font-size: 14px;
  max-width: 400px;
  margin-bottom: 24px;
  line-height: 1.7;
}

.agent-prompts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 500px;
}

.agent-prompts button {
  background: rgba(35, 34, 41, 0.6);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-full);
  padding: 8px 16px;
  font-size: 13px;
  color: var(--text-primary);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.agent-prompts button:hover:not(:disabled) {
  background: rgba(212, 163, 106, 0.1);
  border-color: var(--gold-border);
  color: var(--gold);
}

.agent-prompts button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* Messages */
.agent-message {
  margin-bottom: 16px;
}

.msg-bubble {
  max-width: 85%;
  padding: 12px 16px;
  border-radius: var(--radius-lg);
  line-height: 1.6;
}

.msg-label {
  display: block;
  font-size: 11px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 4px;
}

.user-bubble {
  margin-left: auto;
  background: linear-gradient(135deg, rgba(187, 140, 82, 0.85), rgba(212, 163, 106, 0.75));
  color: #f8f4ec;
  border-bottom-right-radius: 4px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.2);
}

.user-bubble .msg-label {
  color: rgba(248, 244, 236, 0.7);
}

.user-bubble p {
  margin: 0;
  font-size: 14px;
}

.assistant-bubble {
  background: rgba(39, 37, 47, 0.8);
  border: 1px solid var(--border-default);
  border-bottom-left-radius: 4px;
}

.assistant-bubble .msg-label {
  color: var(--gold);
}

.msg-content {
  margin: 0;
  font-size: 14px;
  color: var(--text-primary);
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.7;
}

.tool-calls-section {
  margin-bottom: 8px;
}

.tool-bubble {
  background: rgba(212, 163, 106, 0.08);
  border: 1px solid rgba(212, 163, 106, 0.2);
  font-size: 12px;
}

.tool-bubble .msg-label {
  color: var(--gold);
}

.tool-result-text {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 11px;
  max-height: 150px;
  overflow-y: auto;
  color: var(--text-primary);
  font-family: var(--font-mono);
}

/* Thinking indicator */
.thinking-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0;
}

.thinking-indicator strong {
  font-size: 13px;
  color: var(--gold);
}

.thinking-indicator i {
  display: inline-block;
  width: 6px;
  height: 6px;
  background: var(--gold);
  border-radius: 50%;
  animation: bounce 1.4s infinite;
}

.thinking-indicator i:nth-child(2) { animation-delay: 0.2s; }
.thinking-indicator i:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 80%, 100% { transform: translateY(0); opacity: 0.4; }
  40% { transform: translateY(-6px); opacity: 1; }
}

/* Input area */
.agent-input-area {
  border-top: 1px solid var(--border-subtle);
  padding: 12px 20px;
  background: rgba(30, 29, 36, 0.5);
}

.agent-input-row {
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.agent-input {
  flex: 1;
  border: 1px solid var(--border-default);
  border-radius: var(--radius-md);
  padding: 10px 14px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  background: rgba(35, 34, 41, 0.7);
  color: var(--text-primary);
  transition: border-color var(--duration-fast);
}

.agent-input::placeholder {
  color: var(--text-muted);
}

.agent-input:focus {
  border-color: var(--gold-border);
  box-shadow: 0 0 0 3px rgba(212, 163, 106, 0.1);
}

.agent-input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-send {
  background: linear-gradient(135deg, var(--gold-deep), var(--gold));
  color: #f8f4ec;
  border: none;
  border-radius: var(--radius-md);
  padding: 10px 24px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity var(--duration-fast), transform var(--duration-fast);
}

.btn-send:hover:not(:disabled) {
  opacity: 0.9;
  transform: translateY(-1px);
}

.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
  transform: none;
}

.agent-error {
  margin: 6px 0 0;
  font-size: 12px;
  color: var(--danger);
}
</style>

<template>
  <section class="focus-panel">
    <div class="panel-topline">
      <span>Review</span>
      <strong>{{ runningState }}</strong>
    </div>

    <JobMatchPanel
      :jd-text="jdText"
      :jd-image-loading="jdImageLoading"
      :loading="jobMatchLoading"
      :result="jobMatchResult"
      :resume-ready="pdfReady"
      @analyze="$emit('analyze-job-match')"
      @jd-image-change="$emit('jd-image-change', $event)"
      @update:jd-text="$emit('update:jdText', $event)"
    />

    <section class="material-strip" aria-label="当前材料状态">
      <span>{{ pdfReady ? '简历已载入' : '等待简历' }}</span>
      <span>{{ jdText.trim() ? `JD ${jdText.trim().length} 字` : 'JD 待补充' }}</span>
      <span>{{ conversationMessages.length }} 条对话</span>
      <span>{{ profileOverview?.resumeCount || 0 }} 个版本</span>
      <button
        v-if="conversationMessages.length > 0"
        type="button"
        class="clear-chat-btn"
        @click="handleClearChat"
      >
        清空对话
      </button>
    </section>

    <section ref="conversationScroller" class="answer-stream">
      <article
        v-for="messageItem in conversationMessages"
        :key="messageItem.id"
        class="chat-message"
        :class="[messageItem.role, { pending: messageItem.pending }]"
      >
        <div class="chat-bubble">
          <span>{{ messageItem.label }}</span>
          <p v-if="messageItem.role === 'user'">{{ messageItem.content }}</p>
          <div v-else-if="messageItem.pending" class="thinking-indicator" aria-label="思考中">
            <strong>思考中</strong>
            <i></i>
            <i></i>
            <i></i>
          </div>
          <TypewriterText
            v-else
            :instant="!messageItem.typing"
            :realtime="messageItem.streamed"
            :text="messageItem.content"
          />
          <button
            v-if="messageItem.role !== 'user' && !messageItem.pending"
            class="copy-answer-button"
            type="button"
            @click="copyMessage(messageItem.content)"
          >
            复制回答
          </button>
        </div>
      </article>
      <article v-if="isEmpty" class="blank-state">
        <strong>从一份简历开始。</strong>
        <p>上传简历后，可以粘贴 JD 做岗位匹配，也可以直接询问简历修改、项目亮点和面试准备。</p>
        <div class="blank-prompts">
          <button
            v-for="prompt in quickPrompts"
            :key="prompt"
            type="button"
            :disabled="sending"
            @click="handleQuickAsk(prompt)"
          >
            {{ prompt }}
          </button>
        </div>
      </article>
    </section>

    <MessageComposer
      :file-input-ref="fileInputRef"
      :image-input-ref="imageInputRef"
      :qa-question="qaQuestion"
      :pdf-ready="pdfReady"
      :quick-actions="composerActions"
      :sending="sending"
      @update:qa-question="$emit('update:qaQuestion', $event)"
      @file-change="$emit('file-change', $event)"
      @image-change="$emit('image-change', $event)"
      @quick-action="handleComposerAction"
      @upload-document="$emit('upload-document')"
      @upload-image="$emit('upload-image')"
      @ask="$emit('ask')"
    />

    <p v-if="message.text" class="quiet-message" :class="message.type">{{ message.text }}</p>
  </section>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import JobMatchPanel from './JobMatchPanel.vue'
import MessageComposer from './MessageComposer.vue'
import TypewriterText from './TypewriterText.vue'

const props = defineProps({
  aiAnswer: {
    type: String,
    default: ''
  },
  aiSummary: {
    type: String,
    default: ''
  },
  fileInputRef: {
    type: Object,
    required: true
  },
  conversationMessages: {
    type: Array,
    default: () => []
  },
  imageInputRef: {
    type: Object,
    required: true
  },
  imageOcrText: {
    type: String,
    default: ''
  },
  jdText: {
    type: String,
    default: ''
  },
  jdImageLoading: {
    type: Boolean,
    default: false
  },
  jobMatchLoading: {
    type: Boolean,
    default: false
  },
  jobMatchResult: {
    type: Object,
    default: null
  },
  message: {
    type: Object,
    required: true
  },
  pdfReady: {
    type: Boolean,
    default: false
  },
  profileOverview: {
    type: Object,
    default: null
  },
  qaQuestion: {
    type: String,
    default: ''
  },
  ragAnswer: {
    type: String,
    default: ''
  },
  runningState: {
    type: String,
    required: true
  },
  sending: {
    type: Boolean,
    default: false
  },
  uploadedImageDescription: {
    type: String,
    default: ''
  }
})

const emit = defineEmits([
  'ask',
  'analyze-job-match',
  'clear-conversation',
  'file-change',
  'image-change',
  'jd-image-change',
  'quick-ask',
  'update:jdText',
  'update:qaQuestion',
  'upload-document',
  'upload-image'
])

const quickPrompts = [
  '简历哪里需要重写？',
  '我和这个岗位匹配吗？',
  '面试官会追问什么？'
]

const workflowTasks = [
  {
    title: '简历诊断',
    desc: '格式、内容、项目表达',
    prompt: '请从格式规范、内容完整度、项目表达、技能关键词四个角度诊断我的简历，并按优先级给出修改建议。'
  },
  {
    title: '项目追问',
    desc: '模拟面试官追问',
    prompt: '请根据我的简历项目经历，列出面试官最可能追问的 8 个问题，并告诉我每题应该怎么准备。'
  },
  {
    title: '亮点提炼',
    desc: '整理可讲的优势',
    prompt: '请帮我从简历中提炼适合面试表达的个人亮点、项目亮点和技术亮点，要求说法自然，不要像模板。'
  },
  {
    title: '补强清单',
    desc: '下一步怎么练',
    prompt: '请根据当前简历，给我一份面试前补强清单，按今天能做、本周能做、长期补强三个层级整理。'
  }
]

const composerActions = computed(() => {
  const actions = workflowTasks.map((task) => ({
    title: task.title,
    desc: task.desc,
    prompt: task.prompt,
    type: 'ask'
  }))
  if (props.conversationMessages.length) {
    actions.push({
      title: '清空对话',
      desc: '重新开始一轮分析',
      type: 'clear'
    })
  }
  return actions
})

const isEmpty = computed(() => {
  return props.conversationMessages.length === 0
})

function handleQuickAsk(prompt) {
  if (shouldRunJobMatch(prompt)) {
    emit('analyze-job-match')
    return
  }
  emit('quick-ask', prompt)
}

async function handleClearChat() {
  try {
    await ElMessageBox.confirm(
      '确定要清空所有对话记录吗？此操作不可撤销。',
      '清空对话',
      {
        confirmButtonText: '确定清空',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    emit('clear-conversation')
    ElMessage.success('对话已清空')
  } catch {
    // 用户取消
  }
}

function handleComposerAction(action) {
  if (action.type === 'clear') {
    emit('clear-conversation')
    return
  }
  if (action.prompt) {
    handleQuickAsk(action.prompt)
  }
}

function shouldRunJobMatch(prompt) {
  return props.pdfReady
    && props.jdText.trim()
    && /岗位|匹配|JD/i.test(prompt)
}

async function copyMessage(content) {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('回答已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择文本复制')
  }
}

const conversationScroller = ref(null)

onMounted(() => {
  scrollConversationToBottom()
})

watch(
  () => props.conversationMessages.length,
  () => scrollConversationToBottom()
)

async function scrollConversationToBottom() {
  await nextTick()
  if (conversationScroller.value) {
    conversationScroller.value.scrollTop = conversationScroller.value.scrollHeight
  }
}
</script>

<style scoped>
.clear-chat-btn {
  margin-left: auto;
  padding: 4px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-muted);
  background: transparent;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-full);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.clear-chat-btn:hover {
  color: var(--danger);
  border-color: var(--danger);
  background: var(--danger-surface);
}
</style>

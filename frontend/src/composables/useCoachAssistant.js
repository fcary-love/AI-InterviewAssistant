import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  askPdfRagQuestion,
  buildPdfRagIndex,
  inspectRedisCapability
} from '../api/pdf'
import { askDocumentQuestion, generateDocumentSummary } from '../api/documents'
import { shouldUseEvidenceSearch } from '../utils/interviewDeskUtils'
import { streamPost } from '../utils/sseClient'

export function useCoachAssistant(pdfData, conversation) {
  const summaryLoading = ref(false)
  const qaLoading = ref(false)
  const redisCheckLoading = ref(false)
  const ragIndexLoading = ref(false)
  const ragQaLoading = ref(false)
  const aiSummary = ref('')
  const qaQuestion = ref('')
  const aiAnswer = ref('')
  const redisCapability = ref('')
  const ragAnswer = ref('')
  const ragReferences = ref([])

  const isWorking = computed(() => {
    return qaLoading.value || ragQaLoading.value || summaryLoading.value
  })

  function clearCoachResults() {
    aiSummary.value = ''
    qaQuestion.value = ''
    aiAnswer.value = ''
    redisCapability.value = ''
    ragAnswer.value = ''
    ragReferences.value = []
  }

  async function handlePrimaryAsk() {
    const question = qaQuestion.value.trim()
    if (!question) {
      return
    }
    if (!pdfData.value?.fileId) {
      ElMessage.warning('请先上传文档')
      return
    }
    const history = buildRequestHistory()
    conversation?.addConversationMessage({
      role: 'user',
      label: '我',
      content: question,
      typing: false
    })
    const pendingMessage = conversation?.addConversationMessage({
      role: 'assistant',
      label: '助手',
      content: shouldUseEvidenceSearch(question)
        ? '正在从你的材料里找依据，稍等一下。'
        : '正在结合你的材料分析，马上回来。',
      typing: false,
      pending: true
    })
    qaQuestion.value = ''

    if (shouldUseEvidenceSearch(question)) {
      if (pdfData.value?.fileType === 'PDF') {
        await handleBuildRagIndex()
        await handleAskRagQuestion(question, history, pendingMessage?.id)
      } else {
        await handleAskQuestion(question, history, pendingMessage?.id)
      }
      return
    }
    await handleAskQuestion(question, history, pendingMessage?.id)
  }

  async function handleQuickAsk(question) {
    qaQuestion.value = question
    await handlePrimaryAsk()
  }

  async function handleGenerateSummary() {
    if (!pdfData.value?.fileId) {
      ElMessage.warning('请先上传文档')
      return
    }
    summaryLoading.value = true
    const pendingMessage = conversation?.addConversationMessage({
      role: 'assistant',
      label: '摘要',
      content: '正在整理这份材料的核心信息。',
      typing: false,
      pending: true,
      kind: 'summary'
    })
    try {
      const response = await generateDocumentSummary(pdfData.value.fileId)
      aiSummary.value = response.data?.summary || ''
      conversation?.updateConversationMessage(pendingMessage?.id, {
        content: aiSummary.value,
        typing: true,
        pending: false
      })
    } catch (error) {
      conversation?.updateConversationMessage(pendingMessage?.id, {
        content: error?.response?.data?.message || error.message || '摘要生成失败了，可以稍后再试一次。',
        typing: false,
        pending: false,
        kind: 'error'
      })
      ElMessage.error(error?.response?.data?.message || error.message || '摘要失败')
    } finally {
      summaryLoading.value = false
    }
  }

  async function handleAskQuestion(questionOverride = '', history = [], pendingMessageId = '') {
    if (!pdfData.value?.fileId) {
      ElMessage.warning('请先上传文档')
      return
    }
    const question = questionOverride || qaQuestion.value.trim()
    if (!question) {
      return
    }
    qaLoading.value = true
    let accumulated = ''
    let streamSucceeded = false
    try {
      await streamPost(
        `/api/documents/${pdfData.value.fileId}/qa/stream`,
        { question, history },
        {
          onToken(token) {
            accumulated += token
            streamSucceeded = true
            conversation?.updateConversationMessage(pendingMessageId, {
              content: accumulated,
              typing: false,
              pending: false,
              kind: 'qa',
              streamed: true
            })
          },
          onDone() {
            aiAnswer.value = accumulated
          },
          onError() {
            // fallback to non-streaming below
          }
        }
      )
      if (!streamSucceeded) {
        await handleAskQuestionFallback(question, history, pendingMessageId)
      }
    } catch {
      if (!streamSucceeded) {
        await handleAskQuestionFallback(question, history, pendingMessageId)
      }
    } finally {
      qaLoading.value = false
    }
  }

  async function handleAskQuestionFallback(question, history, pendingMessageId) {
    try {
      const response = await askDocumentQuestion(pdfData.value.fileId, question, history)
      aiAnswer.value = response.data?.answer || ''
      conversation?.updateConversationMessage(pendingMessageId, {
        content: aiAnswer.value,
        typing: true,
        pending: false,
        kind: 'qa'
      })
    } catch (error) {
      conversation?.updateConversationMessage(pendingMessageId, {
        content: formatAiError(error, '这次回答失败了，可以稍后再试一次。'),
        typing: false,
        pending: false,
        kind: 'error'
      })
      ElMessage.error(formatAiError(error, '问答失败'))
    }
  }

  async function handleInspectRedis() {
    redisCheckLoading.value = true
    try {
      const response = await inspectRedisCapability()
      redisCapability.value = response.data?.message || ''
      ElMessage.success(redisCapability.value || 'Redis 正常')
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error.message || 'Redis 检测失败')
    } finally {
      redisCheckLoading.value = false
    }
  }

  async function handleBuildRagIndex() {
    if (!pdfData.value?.fileId || pdfData.value?.fileType !== 'PDF') {
      ElMessage.warning('请先上传 PDF 文档')
      return
    }
    ragIndexLoading.value = true
    try {
      const response = await buildPdfRagIndex(pdfData.value.fileId)
      const count = response.data?.chunkCount ?? 0
      ElMessage.success(`已索引 ${count} 个片段`)
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error.message || '索引失败')
    } finally {
      ragIndexLoading.value = false
    }
  }

  async function handleAskRagQuestion(questionOverride = '', history = [], pendingMessageId = '') {
    if (!pdfData.value?.fileId) {
      ElMessage.warning('请先上传文档')
      return
    }
    const question = questionOverride || qaQuestion.value.trim()
    if (!question) {
      return
    }
    ragQaLoading.value = true
    try {
      const response = await askPdfRagQuestion(pdfData.value.fileId, question, history)
      ragAnswer.value = response.data?.answer || ''
      ragReferences.value = response.data?.references || []
      conversation?.updateConversationMessage(pendingMessageId, {
        label: '依据检索',
        content: ragAnswer.value,
        typing: true,
        pending: false,
        kind: 'rag'
      })
    } catch (error) {
      conversation?.updateConversationMessage(pendingMessageId, {
        content: formatAiError(error, '这次检索问答失败了，可以稍后再试一次。'),
        typing: false,
        pending: false,
        kind: 'error'
      })
      ElMessage.error(formatAiError(error, '检索问答失败'))
    } finally {
      ragQaLoading.value = false
    }
  }

  function buildRequestHistory() {
    return (conversation?.conversationMessages?.value || [])
      .slice(-8)
      .map((message) => ({
        role: message.role,
        content: message.content
      }))
      .filter((message) => message.content)
  }

  function formatAiError(error, fallback) {
    const message = error?.response?.data?.message || error?.message || fallback
    if (/timeout/i.test(message)) {
      return '这次 AI 思考时间太久，已经超时了。可以把问题问得更具体一点，或让它先给简版回答。'
    }
    return message
  }

  return {
    aiAnswer,
    aiSummary,
    clearCoachResults,
    handleGenerateSummary,
    handleInspectRedis,
    handlePrimaryAsk,
    handleQuickAsk,
    isWorking,
    qaLoading,
    qaQuestion,
    ragAnswer,
    ragQaLoading
  }
}

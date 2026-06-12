import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { analyzeJobMatch } from '../api/jobs'
import { runImageOcr, uploadImage } from '../api/pdf'

export function useJobMatch(pdfData, conversation) {
  const jdText = ref('')
  const jdImageLoading = ref(false)
  const jobMatchLoading = ref(false)
  const jobMatchResult = ref(null)

  async function handleAnalyzeJobMatch() {
    if (!pdfData.value?.fileId) {
      ElMessage.warning('请先上传一份简历')
      return
    }
    if (!jdText.value.trim()) {
      ElMessage.warning('请先粘贴岗位 JD')
      return
    }

    jobMatchLoading.value = true
    let pendingMessage = null
    try {
      conversation?.addConversationMessage({
        role: 'user',
        label: '我',
        content: '请根据我粘贴的岗位 JD，结合当前简历做岗位匹配分析。',
        typing: false,
        kind: 'job-match-request'
      })
      pendingMessage = conversation?.addConversationMessage({
        role: 'assistant',
        label: '岗位匹配',
        content: '正在阅读岗位 JD，并和你的简历逐项比对。',
        typing: false,
        pending: true,
        kind: 'job-match'
      })
      const response = await analyzeJobMatch(pdfData.value.fileId, jdText.value.trim())
      jobMatchResult.value = response.data
      conversation?.updateConversationMessage(pendingMessage?.id, {
        content: formatStructuredJobMatch(jobMatchResult.value),
        typing: true,
        pending: false,
        kind: 'job-match'
      })
      ElMessage.success('岗位匹配分析完成')
    } catch (error) {
      conversation?.updateConversationMessage(pendingMessage?.id, {
        content: formatJobMatchError(error),
        typing: false,
        pending: false,
        kind: 'error'
      })
      ElMessage.error(formatJobMatchError(error))
    } finally {
      jobMatchLoading.value = false
    }
  }

  function clearJobMatchResult() {
    jobMatchResult.value = null
  }

  function formatStructuredJobMatch(result) {
    if (!result) {
      return '岗位匹配分析完成，但没有返回有效结果。'
    }
    const sections = [
      `匹配分：${result.matchScore ?? '-'} 分`,
      formatListSection('岗位核心要求', result.coreRequirements),
      formatListSection('简历已覆盖关键词', result.matchedKeywords),
      formatListSection('需要补强的关键词', result.missingKeywords),
      formatListSection('简历修改方向', result.rewriteSuggestions),
      formatListSection('面试追问重点', result.interviewFocus),
      result.analysisContent || ''
    ].filter(Boolean)
    return sections.join('\n\n')
  }

  function formatListSection(title, items = []) {
    const cleanItems = items.filter(Boolean)
    if (!cleanItems.length) {
      return ''
    }
    return `${title}：\n${cleanItems.map((item, index) => `${index + 1}. ${item}`).join('\n')}`
  }

  async function handleJdImageChange(event) {
    const file = event.target.files?.[0]
    if (!file) {
      return
    }

    jdImageLoading.value = true
    try {
      const uploadRes = await uploadImage(file)
      const fileId = uploadRes?.data?.fileId
      if (!fileId) {
        throw new Error(uploadRes?.message || 'JD 图片上传失败')
      }

      const ocrRes = await runImageOcr(fileId)
      const recognizedText = ocrRes?.data?.fullText?.trim()
      if (!recognizedText) {
        throw new Error('没有从这张 JD 图片中识别到文字，请换一张更清晰的截图')
      }

      jdText.value = jdText.value.trim()
        ? `${jdText.value.trim()}\n\n${recognizedText}`
        : recognizedText
      ElMessage.success('JD 图片已识别并填入')
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error?.message || 'JD 图片识别失败')
    } finally {
      jdImageLoading.value = false
      event.target.value = ''
    }
  }

  function formatJobMatchError(error) {
    const message = error?.response?.data?.message || error?.message || '岗位匹配分析失败'
    if (/timeout/i.test(message)) {
      return '岗位匹配分析耗时太久，已经超时了。可以先缩短 JD 内容，或只保留岗位职责、任职要求和加分项。'
    }
    return message
  }

  return {
    clearJobMatchResult,
    handleAnalyzeJobMatch,
    handleJdImageChange,
    jdText,
    jdImageLoading,
    jobMatchLoading,
    jobMatchResult
  }
}

import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  difficultyOptions,
  directionLabels,
  focusOptions,
  interviewStyleLabels,
  questionModeLabels
} from '../constants/interviewOptions'
import { randomItem } from '../utils/interviewDeskUtils'
import {
  deleteInterviewReport,
  fetchInterviewReport,
  fetchInterviewReports,
  generateInterviewReport,
  refineInterviewReport,
  startInterview,
  streamInterviewAnswer,
  submitInterviewAnswer
} from '../api/interviews'

export function useInterviewConfig(pdfData, gamification) {
  const selectedDirection = ref('backend')
  const difficulty = ref('标准')
  const interviewFocus = ref('project')
  const interviewStyle = ref('strict')
  const questionMode = ref('improvised')
  const randomMixEnabled = ref(false)
  const jdText = ref('')
  const useStreaming = ref(true)
  const adaptiveDifficulty = ref(false)

  const interviewSession = ref(null)
  const interviewStarting = ref(false)
  const currentQuestion = ref('')
  const answerText = ref('')
  const answerSubmitting = ref(false)
  const questionStartedAt = ref(0)
  const lastTurn = ref(null)
  const interviewFinished = ref(false)
  const reportGenerating = ref(false)
  const reportRefining = ref(false)
  const reportLoading = ref(false)
  const reportOptimized = ref(false)
  const interviewReport = ref(null)
  const reportHistory = ref([])
  const selectedReport = ref(null)
  const interviewError = ref('')
  const answeredCount = ref(0)
  const streamingQuestion = ref(false)
  const streamingText = ref('')
  const currentScores = ref(null)
  const isFollowUp = ref(false)
  const currentDifficulty = ref('标准')
  let abortController = null

  const selectedDirectionLabel = computed(() => {
    return directionLabels[selectedDirection.value] || '后端开发'
  })

  const interviewFocusLabel = computed(() => {
    return focusOptions.find((item) => item.value === interviewFocus.value)?.label || '项目深挖'
  })

  const interviewStyleLabel = computed(() => {
    return interviewStyleLabels[interviewStyle.value] || '严格追问'
  })

  const questionModeLabel = computed(() => {
    return questionModeLabels[questionMode.value] || '即兴'
  })

  const sessionDraftText = computed(() => {
    if (randomMixEnabled.value) {
      return `${selectedDirectionLabel.value} / 随机搭配`
    }
    return `${selectedDirectionLabel.value} / ${interviewFocusLabel.value} / ${interviewStyleLabel.value} / ${questionModeLabel.value}`
  })

  function handleToggleRandomMix() {
    randomMixEnabled.value = !randomMixEnabled.value
    if (randomMixEnabled.value) {
      applyRandomInterviewOptions()
    }
  }

  function applyRandomInterviewOptions() {
    difficulty.value = randomItem(difficultyOptions)
    interviewFocus.value = randomItem(focusOptions).value
    interviewStyle.value = randomItem(['gentle', 'strict', 'pressure'])
    questionMode.value = randomItem(['improvised', 'online', 'custom'])
  }

  async function handleStartInterview() {
    interviewStarting.value = true
    interviewError.value = ''
    try {
      const response = await startInterview({
        resumeFileId: pdfData.value?.fileId || '',
        jdText: jdText.value || '',
        direction: selectedDirectionLabel.value,
        difficulty: difficulty.value,
        focus: interviewFocusLabel.value,
        style: interviewStyleLabel.value,
        questionMode: questionModeLabel.value,
        randomMix: randomMixEnabled.value,
        adaptiveDifficulty: adaptiveDifficulty.value
      })
      interviewSession.value = response.data
      currentQuestion.value = response.data?.firstQuestion || ''
      currentDifficulty.value = response.data?.currentDifficulty || difficulty.value
      answerText.value = ''
      lastTurn.value = null
      interviewReport.value = null
      selectedReport.value = null
      reportOptimized.value = false
      interviewFinished.value = false
      interviewError.value = ''
      answeredCount.value = 0
      currentScores.value = null
      isFollowUp.value = false
      questionStartedAt.value = Date.now()
    } catch (error) {
      interviewError.value = error?.response?.data?.message || error?.message || '面试启动失败，请确认后端服务已启动'
      ElMessage.error(interviewError.value)
    } finally {
      interviewStarting.value = false
    }
  }

  async function handleSubmitAnswer() {
    if (!interviewSession.value?.sessionId || !answerText.value.trim()) {
      return
    }

    if (useStreaming.value) {
      return handleSubmitAnswerStream()
    }

    answerSubmitting.value = true
    interviewError.value = ''
    try {
      const durationSeconds = Math.max(1, Math.round((Date.now() - questionStartedAt.value) / 1000))
      const response = await submitInterviewAnswer(interviewSession.value.sessionId, {
        answer: answerText.value,
        durationSeconds
      })
      applyTurnResult(response.data)
    } catch (error) {
      interviewError.value = error?.response?.data?.message || error?.message || '提交失败'
    } finally {
      answerSubmitting.value = false
    }
  }

  async function handleSubmitAnswerStream() {
    answerSubmitting.value = true
    streamingQuestion.value = true
    streamingText.value = ''
    interviewError.value = ''

    abortController = new AbortController()
    const durationSeconds = Math.max(1, Math.round((Date.now() - questionStartedAt.value) / 1000))

    let metaParsed = false
    let fullText = ''

    await streamInterviewAnswer(interviewSession.value.sessionId, {
      answer: answerText.value,
      durationSeconds
    }, {
      onToken(token) {
        if (token.startsWith('[TURN_META]')) {
          try {
            const meta = JSON.parse(token.slice(11))
            metaParsed = true
            applyTurnMeta(meta)
          } catch (e) {
            // ignore parse error
          }
          return
        }
        fullText += token
        streamingText.value = fullText
      },
      onDone() {
        if (!metaParsed) {
          // 如果没有收到元数据，用完整文本作为下一题
          currentQuestion.value = fullText.trim()
        }
        streamingQuestion.value = false
        answerSubmitting.value = false
        answerText.value = ''
        questionStartedAt.value = Date.now()
      },
      onError(err) {
        streamingQuestion.value = false
        answerSubmitting.value = false
        interviewError.value = err?.message || '流式回答失败，尝试回退到普通模式'
        // fallback to sync
        handleSubmitAnswerSync()
      },
      signal: abortController.signal
    })
  }

  async function handleSubmitAnswerSync() {
    answerSubmitting.value = true
    interviewError.value = ''
    try {
      const durationSeconds = Math.max(1, Math.round((Date.now() - questionStartedAt.value) / 1000))
      const response = await submitInterviewAnswer(interviewSession.value.sessionId, {
        answer: answerText.value,
        durationSeconds
      })
      applyTurnResult(response.data)
    } catch (error) {
      interviewError.value = error?.response?.data?.message || error?.message || '提交失败'
    } finally {
      answerSubmitting.value = false
    }
  }

  function applyTurnMeta(meta) {
    if (!meta) return
    lastTurn.value = {
      questionNo: meta.questionNo,
      score: meta.overall,
      scores: meta.scores,
      comment: meta.comment,
      followUp: meta.followUp,
      durationSeconds: Math.max(1, Math.round((Date.now() - questionStartedAt.value) / 1000))
    }
    answeredCount.value = meta.questionNo
    currentScores.value = meta.scores || null
    isFollowUp.value = !!meta.followUp
    interviewFinished.value = !!meta.finished

    // 面试结束时加载游戏化数据（后端会自动结算）
    if (meta.finished && gamification) {
      setTimeout(() => {
        gamification.loadGamification()
      }, 1000)
    }
  }

  function applyTurnResult(data) {
    lastTurn.value = data
    answeredCount.value = data?.questionNo || answeredCount.value + 1
    currentScores.value = data?.scores || null
    isFollowUp.value = !!data?.followUp
    interviewFinished.value = !!data?.finished
    answerText.value = ''
    if (data?.nextQuestion) {
      currentQuestion.value = data.nextQuestion
      questionStartedAt.value = Date.now()
    } else {
      currentQuestion.value = ''
    }
  }

  function handleResetInterview() {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    interviewSession.value = null
    currentQuestion.value = ''
    answerText.value = ''
    lastTurn.value = null
    interviewFinished.value = false
    interviewReport.value = null
    selectedReport.value = null
    interviewError.value = ''
    answeredCount.value = 0
    questionStartedAt.value = 0
    reportRefining.value = false
    reportOptimized.value = false
    streamingQuestion.value = false
    streamingText.value = ''
    currentScores.value = null
    isFollowUp.value = false
  }

  async function handleGenerateReport() {
    if (!interviewSession.value?.sessionId) {
      return
    }
    reportGenerating.value = true
    interviewError.value = ''
    try {
      const response = await generateInterviewReport(interviewSession.value.sessionId, {
        userReflection: ''
      })
      interviewReport.value = response.data
      selectedReport.value = null
      reportOptimized.value = false
      interviewFinished.value = true
    } catch (error) {
      if (error?.code === 'ECONNABORTED') {
        interviewError.value = '报告生成时间较长，请稍后再试。建议保持后端和网络稳定。'
      } else {
        interviewError.value = error?.response?.data?.message || error?.message || '报告生成失败'
      }
      ElMessage.error(interviewError.value)
    } finally {
      reportGenerating.value = false
    }
  }

  async function handleRefineReport() {
    if (!interviewSession.value?.sessionId || reportOptimized.value) {
      return
    }
    reportRefining.value = true
    interviewError.value = ''
    try {
      const response = await refineInterviewReport(interviewSession.value.sessionId, {
        userReflection: ''
      })
      interviewReport.value = response.data
      selectedReport.value = null
      interviewFinished.value = true
      reportOptimized.value = true
      await handleLoadReportHistory()
      ElMessage.success('优化报告已上传到历史报告')
    } catch (error) {
      if (error?.code === 'ECONNABORTED') {
        interviewError.value = 'AI 优化时间较长，请稍后再试。基础报告已经保存，不会丢失。'
      } else {
        interviewError.value = error?.response?.data?.message || error?.message || 'AI 优化失败'
      }
      ElMessage.error(interviewError.value)
    } finally {
      reportRefining.value = false
    }
  }

  async function handleLoadReportHistory() {
    reportLoading.value = true
    try {
      const response = await fetchInterviewReports()
      reportHistory.value = response.data || []
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error?.message || '历史报告加载失败')
    } finally {
      reportLoading.value = false
    }
  }

  async function handleViewReport(sessionId) {
    if (!sessionId) {
      return
    }
    reportLoading.value = true
    try {
      const response = await fetchInterviewReport(sessionId)
      selectedReport.value = response.data
      interviewReport.value = response.data
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error?.message || '报告详情加载失败')
    } finally {
      reportLoading.value = false
    }
  }

  async function handleDeleteReport(sessionId) {
    if (!sessionId) {
      return
    }
    try {
      await ElMessageBox.confirm('删除后历史报告列表中将不再显示这份报告，确定删除吗？', '删除报告', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await deleteInterviewReport(sessionId)
      if (selectedReport.value?.sessionId === sessionId) {
        selectedReport.value = null
      }
      if (interviewReport.value?.sessionId === sessionId) {
        interviewReport.value = null
        reportOptimized.value = false
      }
      await handleLoadReportHistory()
      ElMessage.success('报告已删除')
    } catch (error) {
      if (error === 'cancel' || error === 'close') {
        return
      }
      ElMessage.error(error?.response?.data?.message || error?.message || '报告删除失败')
    }
  }

  function handleBackReportList() {
    selectedReport.value = null
  }

  async function handleDownloadReport(report = selectedReport.value || interviewReport.value) {
    if (!report?.reportContent) {
      return
    }
    const container = document.createElement('div')
    container.className = 'pdf-report-template'
    container.innerHTML = `
      <div class="pdf-header">
        <h1>AI 面试助手 - 模拟面试报告</h1>
        <div class="pdf-meta">
          <span>综合评分：${report.totalScore ?? '-'} 分</span>
          <span>面试信息：${report.summary || '-'}</span>
          <span>更新时间：${report.updatedAt || '-'}</span>
        </div>
      </div>
      <div class="pdf-score-badge">${report.totalScore ?? '-'}</div>
      <div class="pdf-content">${report.reportContent.replace(/\n/g, '<br>')}</div>
      <div class="pdf-footer">由 AI 面试助手生成 · ${new Date().toLocaleDateString('zh-CN')}</div>
    `
    document.body.appendChild(container)

    try {
      const html2pdf = (await import('html2pdf.js')).default
      await html2pdf()
        .set({
          margin: [15, 15, 15, 15],
          filename: `面试报告-${report.sessionId || Date.now()}.pdf`,
          image: { type: 'jpeg', quality: 0.98 },
          html2canvas: { scale: 2, useCORS: true },
          jsPDF: { unit: 'mm', format: 'a4', orientation: 'portrait' }
        })
        .from(container)
        .save()
    } finally {
      document.body.removeChild(container)
    }
  }

  return {
    adaptiveDifficulty,
    answeredCount,
    answerSubmitting,
    answerText,
    currentDifficulty,
    currentQuestion,
    currentScores,
    difficulty,
    difficultyOptions,
    focusOptions,
    handleGenerateReport,
    handleBackReportList,
    handleDeleteReport,
    handleDownloadReport,
    handleLoadReportHistory,
    handleRefineReport,
    handleResetInterview,
    handleStartInterview,
    handleSubmitAnswer,
    handleToggleRandomMix,
    handleViewReport,
    interviewError,
    interviewFinished,
    interviewReport,
    interviewSession,
    interviewStarting,
    interviewFocus,
    interviewStyle,
    isFollowUp,
    jdText,
    lastTurn,
    questionMode,
    reportHistory,
    reportGenerating,
    reportLoading,
    reportOptimized,
    reportRefining,
    randomMixEnabled,
    selectedDirection,
    selectedReport,
    selectedDirectionLabel,
    sessionDraftText,
    streamingQuestion,
    streamingText,
    useStreaming
  }
}

<template>
  <section class="page-container">
    <div class="page-topline">
      <strong>求职助手</strong>
      <span>简历诊断 · 岗位匹配 · 项目优化</span>
    </div>

    <el-tabs v-model="activeTab" class="coach-tabs">
      <el-tab-pane label="AI 咨询" name="coach">
        <CoachPanel
          :ai-answer="desk.aiAnswer.value"
          :ai-summary="desk.aiSummary.value"
          :conversation-messages="desk.conversationMessages.value"
          :file-input-ref="desk.fileInputRef"
          :image-input-ref="desk.imageInputRef"
          :image-ocr-text="desk.imageOcrData.value?.fullText || ''"
          :jd-image-loading="desk.jdImageLoading.value"
          :jd-text="desk.jdText.value"
          :job-match-loading="desk.jobMatchLoading.value"
          :job-match-result="desk.jobMatchResult.value"
          :message="desk.message.value"
          :pdf-ready="!!desk.pdfData.value"
          :profile-overview="desk.profileOverview.value"
          :qa-question="desk.qaQuestion.value"
          :rag-answer="desk.ragAnswer.value"
          :running-state="desk.runningState.value"
          :sending="desk.qaLoading.value || desk.ragQaLoading.value"
          :uploaded-image-description="desk.uploadedImageDescription.value"
          @analyze-job-match="desk.handleAnalyzeJobMatch"
          @ask="desk.handlePrimaryAsk"
          @clear-conversation="desk.clearConversationMessages"
          @file-change="desk.handleFileChange"
          @image-change="desk.handleImageFileChange"
          @jd-image-change="desk.handleJdImageChange"
          @quick-ask="desk.handleQuickAsk"
          @update:jd-text="desk.jdText.value = $event"
          @update:qa-question="desk.qaQuestion.value = $event"
          @upload-document="desk.triggerUpload"
          @upload-image="desk.triggerImageUpload"
        />
      </el-tab-pane>
      <el-tab-pane label="岗位项目" name="matches">
        <JobProjectPanel
          @use-project="handleUseJobProject"
        />
      </el-tab-pane>
      <el-tab-pane label="匹配记录" name="matchHistory">
        <JobMatchHistoryPanel
          @open-coach="activeTab = 'coach'"
          @start-interview="handleStartInterviewFromMatch"
          @use-match="handleUseJobMatch"
        />
      </el-tab-pane>
    </el-tabs>

    <AttachmentTray
      :pdf-data="desk.pdfData.value"
      :image-data="desk.imageData.value"
      :pdf-preview-url="desk.pdfPreviewUrl.value"
      :image-preview-url="desk.imagePreviewUrl.value"
    />
  </section>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import CoachPanel from '../components/coach/CoachPanel.vue'
import JobProjectPanel from '../components/jobs/JobProjectPanel.vue'
import JobMatchHistoryPanel from '../components/jobs/JobMatchHistoryPanel.vue'
import AttachmentTray from '../components/documents/AttachmentTray.vue'
import { useInterviewDesk } from '../composables/useInterviewDesk'

const router = useRouter()
const desk = useInterviewDesk()
const activeTab = ref('coach')

function handleUseJobProject(project) {
  desk.jdText.value = project?.jdText || ''
  desk.jobMatchResult.value = project?.matchAnalysisId
    ? {
        id: project.matchAnalysisId,
        resumeFileId: project.resumeFileId,
        matchScore: project.matchScore,
        analysisContent: project.resumeSuggestions,
        createdAt: project.updatedAt,
        coreRequirements: [],
        matchedKeywords: project.resumeKeywords || [],
        missingKeywords: project.missingKeywords || [],
        rewriteSuggestions: project.resumeSuggestions ? [project.resumeSuggestions] : [],
        interviewFocus: []
      }
    : null
  activeTab.value = 'coach'
  desk.addConversationMessage({
    role: 'assistant',
    label: '岗位项目',
    content: `已把「${project.companyName} · ${project.jobTitle}」带回咨询区。你可以继续问岗位匹配、简历改写、投递版表达，或者进入模拟面试。`,
    typing: true,
    kind: 'job-project'
  })
}

function handleUseJobMatch(detail) {
  desk.jdText.value = detail?.jdText || ''
  desk.jobMatchResult.value = detail || null
  activeTab.value = 'coach'
  desk.addConversationMessage({
    role: 'assistant',
    label: '岗位记录',
    content: '已把这份历史 JD 放回咨询区。你可以继续问岗位匹配、简历改写、项目亮点，或者切到面试页开始针对这份岗位训练。',
    typing: true,
    kind: 'job-history'
  })
}

async function handleStartInterviewFromMatch(detail) {
  if (!detail?.resumeFileId) return
  desk.jdText.value = detail.jdText || ''
  desk.pdfData.value = {
    ...(desk.pdfData.value || {}),
    fileId: detail.resumeFileId,
    fileName: detail.resumeFileName || '历史简历材料',
    fileType: 'DOCUMENT',
    fullText: desk.pdfData.value?.fullText || ''
  }
  desk.selectedDirection.value = inferDirection(detail.jdText || '')
  desk.interviewFocus.value = 'project'
  desk.questionMode.value = 'improvised'
  router.push('/interview')
  await nextTick()
  desk.handleStartInterview()
}

function inferDirection(text) {
  const content = text.toLowerCase()
  if (content.includes('vue') || content.includes('前端') || content.includes('javascript')) return 'frontend'
  if (content.includes('测试') || content.includes('test') || content.includes('qa')) return 'qa'
  if (content.includes('运维') || content.includes('linux') || content.includes('kubernetes')) return 'ops'
  if (content.includes('软件') && !content.includes('java')) return 'software'
  return 'backend'
}
</script>

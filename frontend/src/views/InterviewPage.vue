<template>
  <section class="page-container">
    <div class="page-topline">
      <strong>模拟面试</strong>
      <span>配置 · 答题 · 评分 · 复盘</span>
    </div>

    <InterviewSetupPanel
      :adaptive-difficulty="desk.adaptiveDifficulty.value"
      :answered-count="desk.answeredCount.value"
      :answer-submitting="desk.answerSubmitting.value"
      :answer-text="desk.answerText.value"
      :current-difficulty="desk.currentDifficulty.value"
      :current-question="desk.currentQuestion.value"
      :current-scores="desk.currentScores.value"
      :difficulty="desk.difficulty.value"
      :difficulty-options="desk.difficultyOptions"
      :focus-options="desk.focusOptions"
      :interview-error="desk.interviewError.value"
      :interview-finished="desk.interviewFinished.value"
      :interview-focus="desk.interviewFocus.value"
      :interview-report="desk.interviewReport.value"
      :interview-session="desk.interviewSession.value"
      :interview-starting="desk.interviewStarting.value"
      :interview-style="desk.interviewStyle.value"
      :interviewers="desk.interviewers.value"
      :is-follow-up="desk.isFollowUp.value"
      :jd-text="desk.jdText.value"
      :last-turn="desk.lastTurn.value"
      :question-mode="desk.questionMode.value"
      :random-mix-enabled="desk.randomMixEnabled.value"
      :report-generating="desk.reportGenerating.value"
      :report-optimized="desk.reportOptimized.value"
      :report-refining="desk.reportRefining.value"
      :selected-direction="desk.selectedDirection.value"
      :selected-direction-label="desk.selectedDirectionLabel.value"
      :selected-interviewer-id="desk.selectedInterviewerId.value"
      :session-draft-text="desk.sessionDraftText.value"
      :streaming-question="desk.streamingQuestion.value"
      :streaming-text="desk.streamingText.value"
      :use-streaming="desk.useStreaming.value"
      @generate-report="desk.handleGenerateReport"
      @refine-report="desk.handleRefineReport"
      @reset="desk.handleResetInterview"
      @select-interviewer="desk.selectInterviewer"
      @start="desk.handleStartInterview"
      @submit-answer="desk.handleSubmitAnswer"
      @toggle-random-mix="desk.handleToggleRandomMix"
      @update:adaptive-difficulty="desk.adaptiveDifficulty.value = $event"
      @update:answer-text="desk.answerText.value = $event"
      @update:difficulty="desk.difficulty.value = $event"
      @update:interview-focus="desk.interviewFocus.value = $event"
      @update:interview-style="desk.interviewStyle.value = $event"
      @update:jd-text="desk.jdText.value = $event"
      @update:question-mode="desk.questionMode.value = $event"
      @update:selected-direction="desk.selectedDirection.value = $event"
      @update:use-streaming="desk.useStreaming.value = $event"
    />
  </section>
</template>

<script setup>
import InterviewSetupPanel from '../components/interview/InterviewSetupPanel.vue'
import { useInterviewDesk } from '../composables/useInterviewDesk'

const desk = useInterviewDesk()
</script>

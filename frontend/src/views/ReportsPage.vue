<template>
  <section class="page-container">
    <div class="page-topline">
      <strong>成长报告</strong>
      <span>数据看板 · 历史复盘</span>
    </div>

    <el-tabs v-model="activeTab" class="coach-tabs">
      <el-tab-pane label="成长看板" name="dashboard">
        <GrowthDashboardPanel
          @open-archive="activeTab = 'archive'"
          @open-coach="handleOpenCoach"
          @open-interview="handleOpenInterview"
          @open-report="activeTab = 'reports'"
        />
      </el-tab-pane>
      <el-tab-pane label="历史报告" name="reports">
        <InterviewReplayPanel
          v-if="replaySessionId"
          :session-id="replaySessionId"
          @close="replaySessionId = ''"
        />
        <ReportPanel
          v-else
          :interview-session="desk.interviewSession.value"
          :report-history="desk.reportHistory.value"
          :report-generating="desk.reportGenerating.value"
          :report-loading="desk.reportLoading.value"
          :selected-report="desk.selectedReport.value"
          @back-list="desk.handleBackReportList"
          @delete-report="desk.handleDeleteReport"
          @download-report="desk.handleDownloadReport"
          @generate-report="desk.handleGenerateReport"
          @view-report="desk.handleViewReport"
          @view-replay="replaySessionId = $event"
        />
      </el-tab-pane>
    </el-tabs>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import GrowthDashboardPanel from '../components/dashboard/GrowthDashboardPanel.vue'
import ReportPanel from '../components/report/ReportPanel.vue'
import InterviewReplayPanel from '../components/interview/InterviewReplayPanel.vue'
import { useInterviewDesk } from '../composables/useInterviewDesk'

const router = useRouter()
const desk = useInterviewDesk()
const activeTab = ref('dashboard')
const replaySessionId = ref('')

function handleOpenCoach() {
  router.push('/coach')
}

function handleOpenInterview() {
  router.push('/interview')
}
</script>

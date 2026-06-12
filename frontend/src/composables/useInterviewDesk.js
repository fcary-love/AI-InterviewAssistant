import { computed, onMounted, ref, watch } from 'vue'
import { modes } from '../constants/interviewOptions'
import { useCoachAssistant } from './useCoachAssistant'
import { useConversationHistory } from './useConversationHistory'
import { useDocumentMaterials } from './useDocumentMaterials'
import { useInterviewConfig } from './useInterviewConfig'
import { useJobMatch } from './useJobMatch'
import { useProfileOverview } from './useProfileOverview'
import { useGamification } from './useGamification'

let instance = null

export function useInterviewDesk() {
  if (instance) return instance

  const activeMode = ref('home')
  const conversation = useConversationHistory()
  const materials = useDocumentMaterials()
  const coach = useCoachAssistant(materials.pdfData, conversation)
  const gamification = useGamification()
  const interview = useInterviewConfig(materials.pdfData, gamification)
  const jobMatch = useJobMatch(materials.pdfData, conversation)
  const profile = useProfileOverview()

  const runningState = computed(() => {
    if (coach.isWorking.value) return '处理中'
    if (jobMatch.jobMatchLoading.value) return '匹配中'
    if (materials.pdfData.value || materials.imageData.value) return '就绪'
    return '空闲'
  })

  async function handleFileChange(event) {
    const uploaded = await materials.handleFileChange(event)
    if (uploaded) {
      coach.clearCoachResults()
      jobMatch.clearJobMatchResult()
      await profile.loadProfileOverview()
    }
  }

  async function handleImageFileChange(event) {
    await materials.handleImageFileChange(event)
  }

  watch(activeMode, (mode) => {
    if (mode === 'report') {
      interview.handleLoadReportHistory()
    }
  })

  onMounted(() => {
    profile.loadProfileOverview()
  })

  async function handleAnalyzeJobMatch() {
    await jobMatch.handleAnalyzeJobMatch()
    await profile.loadProfileOverview()
  }

  instance = {
    ...materials,
    ...conversation,
    ...coach,
    ...interview,
    ...jobMatch,
    ...profile,
    ...gamification,
    activeMode,
    handleAnalyzeJobMatch,
    handleFileChange,
    handleImageFileChange,
    modes,
    runningState
  }

  return instance
}

import { ref, computed } from 'vue'
import { fetchGamificationSummary, fetchAchievements, claimDailyTask } from '../api/gamification'
import { fetchInterviewers } from '../api/interviewers'

let instance = null

export function useGamification() {
  if (instance) return instance

  // 面试官列表
  const interviewers = ref([])
  const selectedInterviewerId = ref(null)

  // 游戏化数据
  const gamification = ref(null)
  const achievements = ref([])
  const dailyTasks = ref([])

  // 弹窗控制
  const showSettlePopup = ref(false)
  const settleResult = ref(null)
  const showAchievementPopup = ref(false)
  const newAchievement = ref(null)

  // 计算属性
  const currentInterviewer = computed(() => {
    return interviewers.value.find(i => i.id === selectedInterviewerId.value) || interviewers.value[0]
  })

  const levelInfo = computed(() => {
    if (!gamification.value) return { level: 1, title: '面试新手', exp: 0, progress: 0, needed: 100 }
    const g = gamification.value
    return {
      level: g.level,
      title: g.title,
      exp: g.expPoints,
      progress: g.expProgress,
      needed: g.expToNextLevel
    }
  })

  const streakInfo = computed(() => {
    if (!gamification.value) return { days: 0, active: false }
    return {
      days: gamification.value.streakDays,
      active: gamification.value.streakDays > 0
    }
  })

  // 加载面试官列表
  async function loadInterviewers() {
    try {
      const res = await fetchInterviewers()
      interviewers.value = res.data.data || []
      if (interviewers.value.length > 0 && !selectedInterviewerId.value) {
        const defaultOne = interviewers.value.find(i => i.isDefault) || interviewers.value[0]
        selectedInterviewerId.value = defaultOne.id
      }
    } catch {
      // 静默失败
    }
  }

  // 加载游戏化概览
  async function loadGamification() {
    try {
      const res = await fetchGamificationSummary()
      gamification.value = res.data.data || null
      dailyTasks.value = gamification.value?.dailyTasks || []
    } catch {
      // 静默失败
    }
  }

  // 加载成就列表
  async function loadAchievements() {
    try {
      const res = await fetchAchievements()
      achievements.value = res.data.data || []
    } catch {
      // 静默失败
    }
  }

  // 处理面试结算结果
  function handleSettleResult(result) {
    if (!result) return
    settleResult.value = result
    showSettlePopup.value = true

    // 更新本地数据
    if (gamification.value) {
      gamification.value = {
        ...gamification.value,
        expPoints: result.totalExp,
        level: result.level,
        title: result.title,
        streakDays: result.streakDays
      }
    }

    // 显示新解锁的成就
    if (result.unlockedAchievements?.length > 0) {
      setTimeout(() => {
        newAchievement.value = result.unlockedAchievements[0]
        showAchievementPopup.value = true
      }, 1500)
    }
  }

  // 领取每日任务奖励
  async function handleClaimTask(taskType) {
    try {
      const res = await claimDailyTask(taskType)
      const expGained = res.data.data?.expGained || 0
      if (expGained > 0) {
        await loadGamification()
      }
      return expGained
    } catch {
      return 0
    }
  }

  // 选择面试官
  function selectInterviewer(id) {
    selectedInterviewerId.value = id
  }

  // 初始化
  async function init() {
    await Promise.all([loadInterviewers(), loadGamification()])
  }

  instance = {
    // 状态
    interviewers,
    selectedInterviewerId,
    gamification,
    achievements,
    dailyTasks,
    showSettlePopup,
    settleResult,
    showAchievementPopup,
    newAchievement,

    // 计算属性
    currentInterviewer,
    levelInfo,
    streakInfo,

    // 方法
    loadInterviewers,
    loadGamification,
    loadAchievements,
    handleSettleResult,
    handleClaimTask,
    selectInterviewer,
    init,

    // 关闭弹窗
    closeSettlePopup: () => { showSettlePopup.value = false },
    closeAchievementPopup: () => { showAchievementPopup.value = false }
  }

  return instance
}

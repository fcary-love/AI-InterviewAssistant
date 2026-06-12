import { computed, ref } from 'vue'
import { fetchProfileOverview } from '../api/profile'

export function useProfileOverview() {
  const profileOverview = ref(null)
  const profileLoading = ref(false)

  const latestResume = computed(() => {
    return profileOverview.value?.resumeVersions?.[0] || null
  })

  const latestJobMatch = computed(() => {
    return profileOverview.value?.jobMatches?.[0] || null
  })

  async function loadProfileOverview() {
    profileLoading.value = true
    try {
      const response = await fetchProfileOverview()
      profileOverview.value = response.data || null
    } catch {
      profileOverview.value = profileOverview.value || null
    } finally {
      profileLoading.value = false
    }
  }

  return {
    latestJobMatch,
    latestResume,
    loadProfileOverview,
    profileLoading,
    profileOverview
  }
}

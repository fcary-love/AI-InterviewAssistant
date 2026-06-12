import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  compareResumeVersions,
  fetchResumeVersionDetail,
  fetchResumeVersions
} from '../api/profile'

export function useResumeArchive() {
  const resumeVersions = ref([])
  const selectedResume = ref(null)
  const compareResult = ref(null)
  const archiveLoading = ref(false)
  const detailLoading = ref(false)
  const compareLoading = ref(false)
  const leftVersionId = ref('')
  const rightVersionId = ref('')

  const hasEnoughVersions = computed(() => resumeVersions.value.length >= 2)

  async function loadResumeVersions() {
    archiveLoading.value = true
    try {
      const response = await fetchResumeVersions()
      resumeVersions.value = response.data || []
      if (resumeVersions.value.length) {
        leftVersionId.value = resumeVersions.value[1]?.id || resumeVersions.value[0].id
        rightVersionId.value = resumeVersions.value[0].id
        await viewResumeVersion(resumeVersions.value[0].id)
      } else {
        selectedResume.value = null
      }
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error.message || '读取简历档案失败')
    } finally {
      archiveLoading.value = false
    }
  }

  async function viewResumeVersion(id) {
    if (!id) {
      return
    }
    detailLoading.value = true
    try {
      const response = await fetchResumeVersionDetail(id)
      selectedResume.value = response.data || null
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error.message || '读取简历版本失败')
    } finally {
      detailLoading.value = false
    }
  }

  async function handleCompareResumeVersions() {
    if (!leftVersionId.value || !rightVersionId.value) {
      ElMessage.warning('请选择两个简历版本')
      return
    }
    if (leftVersionId.value === rightVersionId.value) {
      ElMessage.warning('请选择两个不同的版本')
      return
    }

    compareLoading.value = true
    try {
      const response = await compareResumeVersions(leftVersionId.value, rightVersionId.value)
      compareResult.value = response.data || null
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error.message || '简历版本对比失败')
    } finally {
      compareLoading.value = false
    }
  }

  return {
    archiveLoading,
    compareLoading,
    compareResult,
    detailLoading,
    handleCompareResumeVersions,
    hasEnoughVersions,
    leftVersionId,
    loadResumeVersions,
    resumeVersions,
    rightVersionId,
    selectedResume,
    viewResumeVersion
  }
}

import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  describeUploadedImage,
  describePdfImage,
  fetchImageContent,
  runImageOcr,
  runPdfOcr,
  uploadImage
} from '../api/pdf'
import { uploadDocument } from '../api/documents'
import { toAbsoluteUrl } from '../utils/interviewDeskUtils'

export function useDocumentMaterials() {
  const fileInputRef = ref(null)
  const imageInputRef = ref(null)
  const loading = ref(false)
  const imageLoading = ref(false)
  const ocrLoading = ref(false)
  const imageOcrLoading = ref(false)
  const imageDescribeLoading = ref('')
  const imageAiLoading = ref(false)
  const pdfData = ref(null)
  const imageData = ref(null)
  const ocrData = ref(null)
  const imageOcrData = ref(null)
  const imageDescription = ref('')
  const uploadedImageDescription = ref('')
  const message = ref({
    text: '',
    type: 'success'
  })

  const pdfPreviewUrl = computed(() => {
    if (!pdfData.value?.fileUrl) {
      return ''
    }
    return toAbsoluteUrl(pdfData.value.fileUrl)
  })

  const imagePreviewUrl = computed(() => {
    if (!imageData.value?.fileUrl) {
      return ''
    }
    return toAbsoluteUrl(imageData.value.fileUrl)
  })

  const activeDocumentName = computed(() => {
    if (pdfData.value?.fileName) {
      return pdfData.value.fileName
    }
    if (imageData.value?.fileName) {
      return imageData.value.fileName
    }
    return '未选择'
  })

  function triggerUpload() {
    fileInputRef.value?.click()
  }

  function triggerImageUpload() {
    imageInputRef.value?.click()
  }

  async function handleFileChange(event) {
    const file = event.target.files?.[0]
    if (!file) {
      return false
    }
    loading.value = true
    message.value = { text: '', type: 'success' }

    try {
      const uploadRes = await uploadDocument(file)
      const documentData = uploadRes?.data
      if (!documentData?.fileId) {
        throw new Error(uploadRes?.message || '上传失败')
      }

      if (documentData.fileType === 'IMAGE') {
        imageData.value = {
          fileId: documentData.fileId,
          fileName: documentData.fileName,
          fileUrl: documentData.fileUrl
        }
        imageOcrData.value = null
        uploadedImageDescription.value = ''
        await handleDescribeUploadedImage()
      } else {
        pdfData.value = {
          fileId: documentData.fileId,
          fileName: documentData.fileName,
          fileType: documentData.fileType,
          fileUrl: documentData.fileUrl,
          fullText: documentData.fullText || '',
          pageCount: documentData.fileType === 'PDF' ? undefined : 1,
          images: []
        }
        ocrData.value = null
        imageDescription.value = ''
      }
      message.value = {
        text: `${documentData.fileName} 已载入`,
        type: 'success'
      }
      return true
    } catch (error) {
      const errorMessage = error?.response?.data?.message || error.message || '上传失败'
      message.value = {
        text: errorMessage,
        type: 'error'
      }
      ElMessage.error(errorMessage)
      return false
    } finally {
      loading.value = false
      event.target.value = ''
    }
  }

  async function handleImageFileChange(event) {
    const file = event.target.files?.[0]
    if (!file) {
      return false
    }

    imageLoading.value = true
    message.value = { text: '', type: 'success' }
    try {
      const uploadRes = await uploadImage(file)
      const fileId = uploadRes?.data?.fileId
      if (!fileId) {
        throw new Error(uploadRes?.message || '图片上传失败')
      }
      const contentRes = await fetchImageContent(fileId)
      imageData.value = contentRes.data
      imageOcrData.value = null
      uploadedImageDescription.value = ''
      message.value = {
        text: `${imageData.value.fileName} 已载入`,
        type: 'success'
      }
      await handleDescribeUploadedImage()
      return true
    } catch (error) {
      const errorMessage = error?.response?.data?.message || error.message || '图片上传失败'
      message.value = { text: errorMessage, type: 'error' }
      ElMessage.error(errorMessage)
      return false
    } finally {
      imageLoading.value = false
      event.target.value = ''
    }
  }

  async function handleRunOcr() {
    if (!pdfData.value?.fileId) {
      ElMessage.warning('请先添加 PDF')
      return
    }

    ocrLoading.value = true
    try {
      const response = await runPdfOcr(pdfData.value.fileId)
      ocrData.value = response.data
      ElMessage.success('OCR 完成')
    } catch (error) {
      const errorMessage = error?.response?.data?.message || error.message || 'OCR 失败'
      ElMessage.error(errorMessage)
    } finally {
      ocrLoading.value = false
    }
  }

  async function handleDescribeImage(imageUrl) {
    if (!pdfData.value?.fileId) {
      ElMessage.warning('请先添加 PDF')
      return
    }
    imageDescribeLoading.value = imageUrl
    try {
      const response = await describePdfImage(pdfData.value.fileId, imageUrl)
      imageDescription.value = response.data?.description || ''
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error.message || '图片说明失败')
    } finally {
      imageDescribeLoading.value = ''
    }
  }

  async function handleRunImageOcr() {
    if (!imageData.value?.fileId) {
      ElMessage.warning('请先添加图片')
      return
    }
    imageOcrLoading.value = true
    try {
      const response = await runImageOcr(imageData.value.fileId)
      imageOcrData.value = response.data
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error.message || '图片 OCR 失败')
    } finally {
      imageOcrLoading.value = false
    }
  }

  async function handleDescribeUploadedImage() {
    if (!imageData.value?.fileId) {
      ElMessage.warning('请先添加图片')
      return
    }
    imageAiLoading.value = true
    try {
      const response = await describeUploadedImage(imageData.value.fileId)
      uploadedImageDescription.value = response.data?.description || ''
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || error.message || '图片说明失败')
    } finally {
      imageAiLoading.value = false
    }
  }

  return {
    activeDocumentName,
    fileInputRef,
    handleDescribeImage,
    handleDescribeUploadedImage,
    handleFileChange,
    handleImageFileChange,
    handleRunImageOcr,
    handleRunOcr,
    imageData,
    imageDescription,
    imageInputRef,
    imageLoading,
    imageOcrData,
    imagePreviewUrl,
    loading,
    message,
    ocrData,
    pdfData,
    pdfPreviewUrl,
    triggerImageUpload,
    triggerUpload,
    uploadedImageDescription
  }
}

<template>
  <div v-if="pdfData || imageData" class="attachment-tray">
    <a v-if="pdfData" :href="pdfPreviewUrl" target="_blank">
      <span>{{ pdfData.fileType || 'DOC' }}</span>
      {{ pdfData.fileName }}
    </a>
    <a v-if="imageData" :href="imagePreviewUrl" target="_blank">
      <span>IMG</span>
      {{ imageData.fileName }}
    </a>
  </div>
</template>

<script setup>
defineProps({
  pdfData: {
    type: Object,
    default: null
  },
  imageData: {
    type: Object,
    default: null
  },
  pdfPreviewUrl: {
    type: String,
    default: ''
  },
  imagePreviewUrl: {
    type: String,
    default: ''
  }
})
</script>

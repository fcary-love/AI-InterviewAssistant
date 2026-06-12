<template>
  <section class="composer">
    <input
      :ref="setFileInput"
      class="hidden-input"
      type="file"
      accept=".pdf,.docx,application/pdf,application/vnd.openxmlformats-officedocument.wordprocessingml.document,image/png,image/jpeg,image/jpg,image/bmp,image/webp"
      @change="$emit('file-change', $event)"
    />
    <input
      :ref="setImageInput"
      class="hidden-input"
      type="file"
      accept="image/png,image/jpeg,image/jpg,image/bmp,image/webp"
      @change="$emit('image-change', $event)"
    />

    <el-dropdown trigger="click" placement="top-start">
      <button class="icon-button" type="button" aria-label="添加附件">+</button>
      <template #dropdown>
        <el-dropdown-menu>
          <el-dropdown-item @click="$emit('upload-document')">
            <div class="upload-menu-item">
              <strong>上传文档</strong>
              <span>支持 PDF、DOCX、图片等</span>
            </div>
          </el-dropdown-item>
          <el-dropdown-item @click="$emit('upload-image')">
            <div class="upload-menu-item">
              <strong>上传图片</strong>
              <span>截图、表格、作品图</span>
            </div>
          </el-dropdown-item>
          <el-dropdown-item divided disabled>
            <div class="upload-menu-heading">快捷提问</div>
          </el-dropdown-item>
          <el-dropdown-item
            v-for="action in quickActions"
            :key="action.title"
            :disabled="sending"
            @click="$emit('quick-action', action)"
          >
            <div class="upload-menu-item">
              <strong>{{ action.title }}</strong>
              <span>{{ action.desc }}</span>
            </div>
          </el-dropdown-item>
        </el-dropdown-menu>
      </template>
    </el-dropdown>

    <el-input
      :model-value="qaQuestion"
      type="textarea"
      :autosize="{ minRows: 1, maxRows: 5 }"
      placeholder="问简历、岗位匹配、修改建议，或让它根据材料找依据"
      @update:model-value="$emit('update:qaQuestion', $event)"
      @keydown.enter.exact.prevent="$emit('ask')"
    />

    <button
      class="send-button"
      type="button"
      :disabled="!pdfReady || sending"
      @click="$emit('ask')"
    >
      发送
    </button>
  </section>
</template>

<script setup>
const props = defineProps({
  fileInputRef: {
    type: Object,
    required: true
  },
  imageInputRef: {
    type: Object,
    required: true
  },
  pdfReady: {
    type: Boolean,
    default: false
  },
  qaQuestion: {
    type: String,
    default: ''
  },
  quickActions: {
    type: Array,
    default: () => []
  },
  sending: {
    type: Boolean,
    default: false
  }
})

defineEmits([
  'ask',
  'file-change',
  'image-change',
  'quick-action',
  'update:qaQuestion',
  'upload-document',
  'upload-image'
])

function setFileInput(element) {
  props.fileInputRef.value = element
}

function setImageInput(element) {
  props.imageInputRef.value = element
}
</script>

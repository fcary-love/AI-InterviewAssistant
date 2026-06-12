<template>
  <div class="typewriter-text" :class="{ typing: isTyping }">
    <p v-for="(paragraph, index) in paragraphs" :key="index">{{ paragraph }}</p>
    <i v-if="isTyping" aria-hidden="true"></i>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { formatAssistantText } from '../../utils/answerFormatter'

const props = defineProps({
  text: {
    type: String,
    default: ''
  },
  speed: {
    type: Number,
    default: 28
  },
  instant: {
    type: Boolean,
    default: false
  },
  realtime: {
    type: Boolean,
    default: false
  }
})

const renderedText = ref('')
const isTyping = ref(false)
let timer = null

const cleanText = computed(() => formatAssistantText(props.text))

const paragraphs = computed(() => {
  if (!renderedText.value) {
    return []
  }
  return renderedText.value
    .split(/\n{1,2}/)
    .map((paragraph) => paragraph.trim())
    .filter(Boolean)
})

watch(
  cleanText,
  (value) => {
    if (props.realtime) {
      stop()
      renderedText.value = value
      return
    }
    play(value)
  },
  { immediate: true }
)

onBeforeUnmount(() => {
  stop()
})

function play(value) {
  stop()
  renderedText.value = ''
  if (!value) {
    return
  }
  if (props.instant) {
    renderedText.value = value
    return
  }

  let index = 0
  const step = value.length > 1200 ? 4 : value.length > 600 ? 3 : 2
  isTyping.value = true
  timer = window.setInterval(() => {
    index += step
    renderedText.value = value.slice(0, index)
    if (index >= value.length) {
      stop(false)
    }
  }, props.speed)
}

function stop(clearTyping = true) {
  if (timer) {
    window.clearInterval(timer)
    timer = null
  }
  isTyping.value = clearTyping ? false : false
}
</script>

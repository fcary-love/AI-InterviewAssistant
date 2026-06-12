import { ref, onUnmounted } from 'vue'

export function useSpeechRecognition() {
  const isListening = ref(false)
  const isSupported = ref(false)
  const transcript = ref('')
  const error = ref('')

  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  isSupported.value = !!SpeechRecognition

  let recognition = null

  function startListening(lang = 'zh-CN') {
    if (!SpeechRecognition) return
    stopListening()

    recognition = new SpeechRecognition()
    recognition.continuous = true
    recognition.interimResults = true
    recognition.lang = lang

    recognition.onresult = (event) => {
      let finalText = ''
      for (let i = event.resultIndex; i < event.results.length; i++) {
        if (event.results[i].isFinal) {
          finalText += event.results[i][0].transcript
        }
      }
      if (finalText) {
        transcript.value += finalText
      }
    }

    recognition.onerror = (event) => {
      error.value = event.error
      isListening.value = false
    }

    recognition.onend = () => {
      isListening.value = false
    }

    recognition.start()
    isListening.value = true
    error.value = ''
  }

  function stopListening() {
    if (recognition) {
      recognition.onend = null
      recognition.onerror = null
      recognition.onresult = null
      try { recognition.stop() } catch {}
      recognition = null
    }
    isListening.value = false
  }

  function toggleListening() {
    if (isListening.value) {
      stopListening()
    } else {
      transcript.value = ''
      startListening()
    }
  }

  function resetTranscript() {
    transcript.value = ''
  }

  onUnmounted(() => {
    stopListening()
  })

  return {
    isListening,
    isSupported,
    transcript,
    error,
    startListening,
    stopListening,
    toggleListening,
    resetTranscript
  }
}

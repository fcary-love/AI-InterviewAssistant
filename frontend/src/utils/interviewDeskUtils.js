export function shouldUseEvidenceSearch(question) {
  const evidenceWords = ['依据', '原文', '哪里', '哪段', '证明', '提到', '出处', '根据材料']
  return evidenceWords.some((word) => question.includes(word))
}

export function randomItem(items) {
  return items[Math.floor(Math.random() * items.length)]
}

export function toAbsoluteUrl(path) {
  if (!path) {
    return ''
  }
  return new URL(path, window.location.origin).toString()
}

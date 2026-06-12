const SYMBOL_LINES = /^[-*_]{3,}$/
const MARKDOWN_HEADING = /^#{1,6}\s*/
const UNORDERED_LIST = /^[-*+]\s+/
const ORDERED_LIST = /^\d+[.)]\s+/
const BLOCKQUOTE = /^>\s*/

export function formatAssistantText(text = '') {
  return text
    .replace(/\r\n/g, '\n')
    .replace(/\r/g, '\n')
    .replace(/```[\s\S]*?```/g, (match) => match.replace(/```[a-zA-Z]*\n?/g, '').replace(/```/g, ''))
    .split('\n')
    .map((line) => cleanLine(line))
    .filter((line) => line && !SYMBOL_LINES.test(line))
    .join('\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim()
}

function cleanLine(line) {
  return line
    .trim()
    .replace(MARKDOWN_HEADING, '')
    .replace(BLOCKQUOTE, '')
    .replace(UNORDERED_LIST, '')
    .replace(ORDERED_LIST, '')
    .replace(/\*\*(.*?)\*\*/g, '$1')
    .replace(/__(.*?)__/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\*/g, '')
    .replace(/_/g, '')
    .replace(/[✅⚠️❌]/g, '')
    .replace(/\s{2,}/g, ' ')
    .trim()
}

<template>
  <div ref="rootRef" class="interviewer-selector">
    <h4>选择面试官</h4>
    <div class="interviewer-grid">
      <div
        v-for="interviewer in interviewers"
        :key="interviewer.id"
        class="interviewer-card"
        :class="{ selected: interviewer.id === selectedId }"
        @click="$emit('select', interviewer.id)"
        @mouseenter="motion.hoverIn($event.currentTarget)"
        @mouseleave="motion.hoverOut($event.currentTarget)"
      >
        <div class="interviewer-avatar">
          <span>{{ avatarMap[interviewer.personality] || '👤' }}</span>
        </div>
        <span class="interviewer-name">{{ interviewer.name }}</span>
        <span class="interviewer-style">{{ styleMap[interviewer.personality] || interviewer.personality }}</span>
        <span v-if="interviewer.isDefault" class="default-tag">默认</span>
        <span v-if="interviewer.id === selectedId" class="selected-check">
          <svg viewBox="0 0 20 20" fill="currentColor" width="14" height="14">
            <path fill-rule="evenodd" d="M16.707 5.293a1 1 0 010 1.414l-8 8a1 1 0 01-1.414 0l-4-4a1 1 0 011.414-1.414L8 12.586l7.293-7.293a1 1 0 011.414 0z" clip-rule="evenodd"/>
          </svg>
        </span>
      </div>
    </div>
    <p v-if="currentInterviewer" class="interviewer-catchphrase">
      {{ currentInterviewer.catchphrase }}
    </p>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useGsapMotion } from '../../composables/useGsapMotion'

const props = defineProps({
  interviewers: { type: Array, default: () => [] },
  selectedId: { type: Number, default: null }
})

defineEmits(['select'])

const rootRef = ref(null)
const motion = useGsapMotion(rootRef)

const avatarMap = {
  strict: '👩‍💼',
  gentle: '👨‍💻',
  pressure: '👔',
  humorous: '😎'
}

const styleMap = {
  strict: '技术总监 · 严谨深入',
  gentle: 'HR 温和 · 引导表达',
  pressure: '高压面试 · 快节奏',
  humorous: '轻松幽默 · 专业深度'
}

const currentInterviewer = computed(() => {
  return props.interviewers.find(i => i.id === props.selectedId)
})
</script>

<style scoped>
.interviewer-selector {
  margin-bottom: 24px;
}

.interviewer-selector h4 {
  margin: 0 0 14px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: 0.02em;
}

.interviewer-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.interviewer-card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 12px;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
  will-change: transform;
  box-shadow: 0 8px 30px rgba(15, 23, 42, 0.02);
}

.interviewer-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 40px rgba(15, 23, 42, 0.06);
  border-color: var(--border-strong);
}

.interviewer-card.selected {
  border: 2px solid var(--primary);
  background: var(--primary-surface);
  box-shadow: 0 4px 16px var(--primary-glow);
}

.interviewer-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--bg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 26px;
}

.interviewer-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.interviewer-style {
  font-size: 11px;
  color: var(--text-muted);
  text-align: center;
}

.default-tag {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  font-size: 10px;
  font-weight: 600;
  color: var(--text-muted);
  background: var(--bg-surface);
  border-radius: 999px;
}

.selected-check {
  position: absolute;
  top: 8px;
  right: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--primary);
  color: var(--text-inverse);
}

.interviewer-catchphrase {
  margin: 14px 0 0;
  padding: 12px 16px;
  font-size: 13px;
  color: var(--text-secondary);
  font-style: italic;
  background: var(--bg-surface);
  border-radius: 12px;
  border-left: 3px solid var(--primary-border);
  line-height: 1.6;
}

@media (max-width: 640px) {
  .interviewer-grid {
    grid-template-columns: repeat(2, 1fr);
    gap: 8px;
  }

  .interviewer-card {
    padding: 12px 8px;
  }

  .interviewer-avatar {
    width: 40px;
    height: 40px;
    font-size: 22px;
  }
}
</style>

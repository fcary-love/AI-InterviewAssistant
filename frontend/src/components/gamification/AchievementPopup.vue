<template>
  <Teleport to="body">
    <Transition name="popup">
      <div v-if="visible && achievement" class="popup-overlay" @click.self="$emit('close')">
        <div class="popup-content">
          <div class="achievement-icon">{{ achievement.icon }}</div>
          <h3>成就解锁！</h3>
          <p class="achievement-name">{{ achievement.name }}</p>
          <p class="achievement-desc">{{ achievement.description }}</p>
          <div class="achievement-meta">
            <span class="rarity" :class="achievement.rarity">{{ rarityLabel }}</span>
            <span class="reward">+{{ achievement.expReward }} EXP</span>
          </div>
          <button class="popup-close-btn" @click="$emit('close')">太棒了！</button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  achievement: { type: Object, default: null }
})

defineEmits(['close'])

const rarityLabel = computed(() => {
  const map = { common: '普通', rare: '稀有', epic: '史诗', legendary: '传说' }
  return map[props.achievement?.rarity] || '普通'
})
</script>

<style scoped>
.popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
}

.popup-content {
  width: 90%;
  max-width: 320px;
  padding: 32px 24px;
  text-align: center;
  background: var(--bg-card);
  border: 1px solid var(--gold-border);
  border-radius: var(--radius-xl);
  box-shadow: 0 0 40px rgba(212, 163, 106, 0.15);
}

.achievement-icon {
  font-size: 56px;
  margin-bottom: 12px;
  animation: bounce 0.6s ease;
}

.popup-content h3 {
  margin: 0 0 12px;
  font-size: 16px;
  color: var(--gold);
  font-weight: 700;
}

.achievement-name {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.achievement-desc {
  margin: 0 0 16px;
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
}

.achievement-meta {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 20px;
}

.rarity {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}

.rarity.common {
  background: rgba(255, 255, 255, 0.08);
  color: var(--text-muted);
}

.rarity.rare {
  background: rgba(106, 135, 231, 0.15);
  color: #6a87e7;
}

.rarity.epic {
  background: rgba(166, 152, 192, 0.15);
  color: var(--violet);
}

.rarity.legendary {
  background: rgba(212, 163, 106, 0.15);
  color: var(--gold);
}

.reward {
  font-size: 12px;
  font-weight: 600;
  color: var(--gold);
}

.popup-close-btn {
  width: 100%;
  height: 40px;
  font-size: 14px;
  font-weight: 700;
  color: var(--text-inverse);
  background: linear-gradient(135deg, var(--gold-deep), var(--gold));
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
}

.popup-enter-active,
.popup-leave-active {
  transition: opacity 0.3s ease;
}

.popup-enter-from,
.popup-leave-to {
  opacity: 0;
}

@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
</style>

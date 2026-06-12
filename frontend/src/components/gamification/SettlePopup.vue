<template>
  <Teleport to="body">
    <Transition name="popup">
      <div v-if="visible" class="popup-overlay" @click.self="$emit('close')">
        <div class="popup-content">
          <div class="popup-header">
            <span class="popup-icon">🎉</span>
            <h3>面试结算</h3>
          </div>

          <div class="exp-breakdown">
            <div
              v-for="(item, index) in result?.expBreakdown || []"
              :key="index"
              class="exp-item"
              :style="{ animationDelay: index * 0.1 + 's' }"
            >
              <span class="exp-text">{{ item.split(' ')[0] }}</span>
              <span class="exp-value">{{ item.split(' ').slice(1).join(' ') }}</span>
            </div>
          </div>

          <div class="level-section">
            <div class="level-progress">
              <span class="level-label">Lv.{{ result?.previousLevel || 1 }}</span>
              <div class="progress-bar">
                <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
              </div>
              <span class="level-label">Lv.{{ result?.level || 1 }}</span>
            </div>
            <div v-if="result?.levelUp" class="level-up-badge">
              <span>🎊 升级了！</span>
            </div>
            <p class="exp-total">
              总计 <strong>{{ result?.totalExp || 0 }}</strong> EXP
              · 当前称号「{{ result?.title || '面试新手' }}」
            </p>
          </div>

          <div v-if="result?.unlockedAchievements?.length" class="achievements-section">
            <h4>🏆 解锁成就</h4>
            <div class="achievement-list">
              <div
                v-for="ach in result.unlockedAchievements"
                :key="ach.code"
                class="achievement-item"
              >
                <span class="ach-icon">{{ ach.icon }}</span>
                <div class="ach-info">
                  <span class="ach-name">{{ ach.name }}</span>
                  <span class="ach-desc">{{ ach.description }}</span>
                </div>
                <span class="ach-reward">+{{ ach.expReward }} EXP</span>
              </div>
            </div>
          </div>

          <div class="streak-info" v-if="result?.streakDays > 0">
            <span>🔥</span>
            <span>已连续练习 <strong>{{ result.streakDays }}</strong> 天</span>
          </div>

          <button class="popup-close-btn" @click="$emit('close')">继续练习</button>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  result: { type: Object, default: null }
})

defineEmits(['close'])

const progressPercent = computed(() => {
  if (!props.result) return 0
  const { previousLevel, level, totalExp } = props.result
  if (level > previousLevel) return 100
  const prevRequired = Math.pow(previousLevel - 1, 2) * 100
  const nextRequired = Math.pow(level, 2) * 100
  const range = nextRequired - prevRequired
  if (range <= 0) return 100
  return Math.min(100, Math.round(((totalExp - prevRequired) / range) * 100))
})
</script>

<style scoped>
.popup-overlay {
  position: fixed;
  inset: 0;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  backdrop-filter: blur(8px);
}

.popup-content {
  width: 90%;
  max-width: 420px;
  max-height: 80vh;
  overflow-y: auto;
  padding: 28px;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-elevated);
}

.popup-header {
  text-align: center;
  margin-bottom: 24px;
}

.popup-icon {
  font-size: 40px;
  display: block;
  margin-bottom: 8px;
}

.popup-header h3 {
  margin: 0;
  font-size: 20px;
  color: var(--text-primary);
  font-weight: 700;
}

.exp-breakdown {
  display: grid;
  gap: 8px;
  margin-bottom: 20px;
}

.exp-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: rgba(35, 34, 41, 0.5);
  border-radius: var(--radius-md);
  animation: fade-in-up 0.3s ease both;
}

.exp-text {
  font-size: 13px;
  color: var(--text-secondary);
}

.exp-value {
  font-size: 13px;
  font-weight: 600;
  color: var(--gold);
}

.level-section {
  margin-bottom: 20px;
}

.level-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.level-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--gold);
  flex-shrink: 0;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--gold-deep), var(--gold-bright));
  border-radius: 4px;
  transition: width 0.5s ease;
}

.level-up-badge {
  text-align: center;
  margin: 8px 0;
  padding: 6px 12px;
  background: linear-gradient(135deg, rgba(212, 163, 106, 0.15), rgba(136, 180, 152, 0.15));
  border-radius: var(--radius-full);
  display: inline-block;
  margin-left: 50%;
  transform: translateX(-50%);
}

.level-up-badge span {
  font-size: 14px;
  font-weight: 700;
  color: var(--gold);
}

.exp-total {
  text-align: center;
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--text-secondary);
}

.exp-total strong {
  color: var(--gold);
  font-weight: 700;
}

.achievements-section {
  margin-bottom: 16px;
}

.achievements-section h4 {
  margin: 0 0 10px;
  font-size: 14px;
  color: var(--gold);
  font-weight: 600;
}

.achievement-list {
  display: grid;
  gap: 8px;
}

.achievement-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: rgba(212, 163, 106, 0.08);
  border: 1px solid var(--gold-border);
  border-radius: var(--radius-md);
}

.ach-icon {
  font-size: 24px;
  flex-shrink: 0;
}

.ach-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.ach-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.ach-desc {
  font-size: 11px;
  color: var(--text-muted);
}

.ach-reward {
  font-size: 11px;
  font-weight: 600;
  color: var(--gold);
  flex-shrink: 0;
}

.streak-info {
  text-align: center;
  margin-bottom: 20px;
  font-size: 13px;
  color: var(--text-secondary);
}

.streak-info strong {
  color: var(--gold);
  font-weight: 700;
}

.popup-close-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 700;
  color: var(--text-inverse);
  background: linear-gradient(135deg, var(--gold-deep), var(--gold));
  border: none;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--duration-fast);
}

.popup-close-btn:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: 0 4px 16px rgba(212, 163, 106, 0.3);
}

/* 动画 */
.popup-enter-active,
.popup-leave-active {
  transition: opacity 0.3s ease;
}

.popup-enter-from,
.popup-leave-to {
  opacity: 0;
}

.popup-enter-active .popup-content,
.popup-leave-active .popup-content {
  transition: transform 0.3s ease;
}

.popup-enter-from .popup-content {
  transform: scale(0.95) translateY(10px);
}

.popup-leave-to .popup-content {
  transform: scale(0.95) translateY(10px);
}

@keyframes fade-in-up {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>

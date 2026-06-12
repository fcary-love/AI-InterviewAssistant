<template>
  <div class="score-trend-container">
    <h4>面试趋势</h4>
    <div v-if="!history.length" class="trend-empty">
      <p>完成多次面试后，这里会显示你的进步趋势</p>
    </div>
    <div v-else ref="chartRef" class="score-trend-chart"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  history: {
    type: Array,
    default: () => []
  }
})

const chartRef = ref(null)
let chart = null

function renderChart() {
  if (!chartRef.value || !props.history.length) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const sorted = [...props.history]
    .filter(r => r.totalScore != null)
    .sort((a, b) => new Date(a.updatedAt) - new Date(b.updatedAt))

  if (!sorted.length) return

  const dates = sorted.map(r => {
    const d = new Date(r.updatedAt)
    return `${d.getMonth() + 1}/${d.getDate()}`
  })
  const totalScores = sorted.map(r => r.totalScore)

  const technicalScores = sorted.map(r => {
    try {
      const s = typeof r.scores === 'string' ? JSON.parse(r.scores) : r.scores
      return s?.technical ?? null
    } catch { return null }
  })

  const expressionScores = sorted.map(r => {
    try {
      const s = typeof r.scores === 'string' ? JSON.parse(r.scores) : r.scores
      return s?.expression ?? null
    } catch { return null }
  })

  const matchScores = sorted.map(r => {
    try {
      const s = typeof r.scores === 'string' ? JSON.parse(r.scores) : r.scores
      return s?.match ?? null
    } catch { return null }
  })

  const problemSolvingScores = sorted.map(r => {
    try {
      const s = typeof r.scores === 'string' ? JSON.parse(r.scores) : r.scores
      return s?.problem_solving ?? null
    } catch { return null }
  })

  const followUpScores = sorted.map(r => {
    try {
      const s = typeof r.scores === 'string' ? JSON.parse(r.scores) : r.scores
      return s?.follow_up ?? null
    } catch { return null }
  })

  const stressScores = sorted.map(r => {
    try {
      const s = typeof r.scores === 'string' ? JSON.parse(r.scores) : r.scores
      return s?.stress_resistance ?? null
    } catch { return null }
  })

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(39, 37, 47, 0.95)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      textStyle: {
        color: '#f0ece3'
      }
    },
    legend: {
      data: ['综合', '技术深度', '表达逻辑', '岗位匹配', '问题解决', '追问表现', '抗压能力'],
      top: 0,
      textStyle: {
        color: '#aba79e',
        fontSize: 12
      },
      itemWidth: 16,
      itemHeight: 3
    },
    grid: {
      left: 40,
      right: 16,
      top: 40,
      bottom: 24
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.08)' } },
      axisLabel: { color: '#78736b', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      min: 40,
      max: 100,
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } },
      axisLabel: { color: '#78736b', fontSize: 11 },
      axisLine: { show: false }
    },
    series: [
      {
        name: '综合',
        type: 'line',
        data: totalScores,
        smooth: true,
        symbolSize: 6,
        lineStyle: { width: 3, color: '#d4a36a' },
        itemStyle: { color: '#d4a36a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(212, 163, 106, 0.25)' },
            { offset: 1, color: 'rgba(212, 163, 106, 0)' }
          ])
        }
      },
      {
        name: '技术深度',
        type: 'line',
        data: technicalScores,
        smooth: true,
        symbolSize: 4,
        lineStyle: { width: 1.5, color: '#88b498', type: 'dashed' },
        itemStyle: { color: '#88b498' }
      },
      {
        name: '表达逻辑',
        type: 'line',
        data: expressionScores,
        smooth: true,
        symbolSize: 4,
        lineStyle: { width: 1.5, color: '#a698c0', type: 'dashed' },
        itemStyle: { color: '#a698c0' }
      },
      {
        name: '岗位匹配',
        type: 'line',
        data: matchScores,
        smooth: true,
        symbolSize: 4,
        lineStyle: { width: 1.5, color: '#6db3b5', type: 'dashed' },
        itemStyle: { color: '#6db3b5' }
      },
      {
        name: '问题解决',
        type: 'line',
        data: problemSolvingScores,
        smooth: true,
        symbolSize: 4,
        lineStyle: { width: 1.5, color: '#e2b882', type: 'dashed' },
        itemStyle: { color: '#e2b882' }
      },
      {
        name: '追问表现',
        type: 'line',
        data: followUpScores,
        smooth: true,
        symbolSize: 4,
        lineStyle: { width: 1.5, color: '#c4a0d0', type: 'dashed' },
        itemStyle: { color: '#c4a0d0' }
      },
      {
        name: '抗压能力',
        type: 'line',
        data: stressScores,
        smooth: true,
        symbolSize: 4,
        lineStyle: { width: 1.5, color: '#d0a080', type: 'dashed' },
        itemStyle: { color: '#d0a080' }
      }
    ]
  }

  chart.setOption(option)
}

watch(() => props.history, () => {
  nextTick(renderChart)
}, { deep: true })

onMounted(() => {
  renderChart()
  window.addEventListener('resize', () => chart?.resize())
})
</script>

<style scoped>
.score-trend-container {
  margin: 20px 0;
  padding: 20px 24px;
  background: rgba(39, 37, 47, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
}

.score-trend-container h4 {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: var(--gold);
  font-weight: 600;
}

.trend-empty {
  text-align: center;
  padding: 40px 0;
}

.trend-empty p {
  margin: 0;
  color: var(--text-muted);
  font-size: 14px;
}

.score-trend-chart {
  width: 100%;
  height: 280px;
}

@media (max-width: 640px) {
  .score-trend-container {
    padding: 14px 16px;
    margin: 14px 0;
  }

  .score-trend-chart {
    height: 220px;
  }
}
</style>

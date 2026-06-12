<template>
  <div class="trajectory-container">
    <h4>难度变化轨迹</h4>
    <div v-if="!trajectories.length" class="trajectory-empty">
      <p>完成多次面试后，这里会显示你的能力成长轨迹</p>
    </div>
    <div v-else ref="chartRef" class="trajectory-chart"></div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  trajectories: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null

function renderChart() {
  if (!chartRef.value || !props.trajectories.length) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const sorted = [...props.trajectories].sort((a, b) => a.questionNo - b.questionNo)
  const questions = sorted.map(t => `第${t.questionNo}题`)
  const elos = sorted.map(t => t.userEloAfter)
  const scores = sorted.map(t => t.score)
  const labels = sorted.map(t => t.difficultyLabel)

  // 难度区间背景色
  const markAreaData = [
    [{ yAxis: 100, itemStyle: { color: 'rgba(136, 180, 136, 0.06)' } }, { yAxis: 1100 }],
    [{ yAxis: 1100, itemStyle: { color: 'rgba(212, 163, 106, 0.06)' } }, { yAxis: 1300 }],
    [{ yAxis: 1300, itemStyle: { color: 'rgba(201, 162, 85, 0.06)' } }, { yAxis: 1500 }],
    [{ yAxis: 1500, itemStyle: { color: 'rgba(200, 112, 96, 0.06)' } }, { yAxis: 2400 }]
  ]

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(39, 37, 47, 0.95)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      textStyle: { color: '#f0ece3' },
      formatter(params) {
        const idx = params[0].dataIndex
        return `${questions[idx]}<br/>` +
          `Elo: <strong>${elos[idx].toFixed(0)}</strong><br/>` +
          `得分: <strong>${scores[idx]}</strong><br/>` +
          `难度: <strong>${labels[idx]}</strong>`
      }
    },
    legend: {
      data: ['Elo评分', '得分'],
      top: 0,
      textStyle: { color: '#aba79e', fontSize: 12 }
    },
    grid: { left: 50, right: 20, top: 40, bottom: 30 },
    xAxis: {
      type: 'category',
      data: questions,
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.08)' } },
      axisLabel: { color: '#78736b', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: [
      {
        type: 'value',
        name: 'Elo',
        min: 800,
        max: 1800,
        splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } },
        axisLabel: { color: '#78736b', fontSize: 11 },
        axisLine: { show: false }
      },
      {
        type: 'value',
        name: '得分',
        min: 0,
        max: 100,
        splitLine: { show: false },
        axisLabel: { color: '#78736b', fontSize: 11 },
        axisLine: { show: false }
      }
    ],
    series: [
      {
        name: 'Elo评分',
        type: 'line',
        data: elos,
        smooth: true,
        symbolSize: 8,
        lineStyle: { width: 3, color: '#d4a36a' },
        itemStyle: { color: '#d4a36a' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(212, 163, 106, 0.2)' },
            { offset: 1, color: 'rgba(212, 163, 106, 0)' }
          ])
        },
        markArea: { silent: true, data: markAreaData }
      },
      {
        name: '得分',
        type: 'bar',
        yAxisIndex: 1,
        data: scores,
        barWidth: 20,
        itemStyle: {
          color: (params) => {
            const score = params.value
            if (score >= 75) return 'rgba(136, 180, 152, 0.6)'
            if (score >= 60) return 'rgba(212, 163, 106, 0.6)'
            return 'rgba(200, 112, 96, 0.6)'
          },
          borderRadius: [4, 4, 0, 0]
        }
      }
    ]
  }

  chart.setOption(option)
}

watch(() => props.trajectories, () => {
  nextTick(renderChart)
}, { deep: true })

onMounted(() => {
  renderChart()
  window.addEventListener('resize', () => chart?.resize())
})
</script>

<style scoped>
.trajectory-container {
  margin: 16px 0;
  padding: 20px 24px;
  background: rgba(39, 37, 47, 0.6);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
}

.trajectory-container h4 {
  margin: 0 0 16px 0;
  font-size: 14px;
  color: var(--gold);
  font-weight: 600;
}

.trajectory-empty {
  text-align: center;
  padding: 30px 0;
}

.trajectory-empty p {
  margin: 0;
  color: var(--text-muted);
  font-size: 14px;
}

.trajectory-chart {
  width: 100%;
  height: 280px;
}

@media (max-width: 640px) {
  .trajectory-container {
    padding: 14px 16px;
  }

  .trajectory-chart {
    height: 220px;
  }
}
</style>

<template>
  <div ref="chartRef" class="score-radar"></div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  scores: {
    type: Object,
    default: () => ({})
  }
})

const chartRef = ref(null)
let chart = null

const scoreLabels = {
  technical: '技术深度',
  expression: '表达逻辑',
  match: '岗位匹配',
  problem_solving: '问题解决',
  follow_up: '追问表现',
  stress_resistance: '抗压能力'
}

function getScoreColor(val) {
  if (val >= 75) return '#88b498'
  if (val >= 60) return '#d4a36a'
  return '#d07868'
}

function renderChart() {
  if (!chartRef.value || !props.scores) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const keys = Object.keys(props.scores)
  const values = Object.values(props.scores)
  const labels = keys.map(k => scoreLabels[k] || k)
  const maxVal = 100

  const avgScore = Math.round(values.reduce((a, b) => a + b, 0) / values.length)

  const option = {
    radar: {
      indicator: labels.map(l => ({ name: l, max: maxVal })),
      shape: 'polygon',
      splitNumber: 4,
      axisName: {
        color: '#aba79e',
        fontSize: 13,
        fontWeight: 600
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.06)'
        }
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(255, 255, 255, 0.02)', 'rgba(255, 255, 255, 0.04)']
        }
      },
      axisLine: {
        lineStyle: {
          color: 'rgba(255, 255, 255, 0.08)'
        }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '分项评分',
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(212, 163, 106, 0.35)' },
            { offset: 1, color: 'rgba(136, 180, 152, 0.15)' }
          ])
        },
        lineStyle: {
          color: '#d4a36a',
          width: 2
        },
        itemStyle: {
          color: '#d4a36a',
          borderColor: '#d4a36a',
          borderWidth: 2
        },
        symbol: 'circle',
        symbolSize: 8,
        label: {
          show: true,
          formatter: (params) => params.value,
          color: '#f0ece3',
          fontSize: 14,
          fontWeight: 700
        }
      }]
    }],
    graphic: [{
      type: 'text',
      left: 'center',
      top: 'center',
      style: {
        text: `${avgScore}`,
        fill: '#f0ece3',
        fontSize: 32,
        fontWeight: 800,
        fontFamily: 'Outfit, sans-serif'
      }
    }, {
      type: 'text',
      left: 'center',
      top: '55%',
      style: {
        text: '综合',
        fill: '#78736b',
        fontSize: 12,
        fontWeight: 600
      }
    }]
  }

  chart.setOption(option)
}

watch(() => props.scores, () => {
  nextTick(renderChart)
}, { deep: true })

onMounted(() => {
  renderChart()
  window.addEventListener('resize', () => chart?.resize())
})
</script>

<style scoped>
.score-radar {
  width: 100%;
  height: 320px;
}

@media (max-width: 640px) {
  .score-radar {
    height: 240px;
  }
}
</style>

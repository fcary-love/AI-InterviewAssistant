<template>
  <div class="time-distribution">
    <h4>时间分布</h4>
    <div class="time-summary" v-if="analysis">
      <div class="time-stat">
        <strong>{{ analysis.totalSeconds }}s</strong>
        <span>总用时</span>
      </div>
      <div class="time-stat">
        <strong>{{ analysis.avgSeconds }}s</strong>
        <span>平均</span>
      </div>
      <div class="time-stat">
        <strong>{{ analysis.minSeconds }}s</strong>
        <span>最短</span>
      </div>
      <div class="time-stat">
        <strong>{{ analysis.maxSeconds }}s</strong>
        <span>最长</span>
      </div>
    </div>
    <div ref="chartRef" class="time-chart"></div>
    <div v-if="analysis?.anomalies?.length" class="time-anomalies">
      <span class="anomaly-title">异常用时：</span>
      <span v-for="a in analysis.anomalies" :key="a.questionNo" class="anomaly-tag" :class="a.anomalyType === '过快' ? 'fast' : 'slow'">
        第{{ a.questionNo }}题 {{ a.durationSeconds }}s（{{ a.anomalyType }}）
      </span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'

const props = defineProps({
  analysis: { type: Object, default: null },
  turns: { type: Array, default: () => [] }
})

const chartRef = ref(null)
let chart = null

function renderChart() {
  if (!chartRef.value || !props.turns.length) return

  if (!chart) {
    chart = echarts.init(chartRef.value)
  }

  const questions = props.turns.map((t, i) => `第${i + 1}题`)
  const durations = props.turns.map(t => t.durationSeconds || 0)
  const avg = props.analysis?.avgSeconds || 0

  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(39, 37, 47, 0.95)',
      borderColor: 'rgba(255, 255, 255, 0.1)',
      textStyle: { color: '#f0ece3' }
    },
    grid: { left: 50, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: questions,
      axisLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.08)' } },
      axisLabel: { color: '#78736b', fontSize: 11 },
      axisTick: { show: false }
    },
    yAxis: {
      type: 'value',
      name: '秒',
      splitLine: { lineStyle: { color: 'rgba(255, 255, 255, 0.05)' } },
      axisLabel: { color: '#78736b', fontSize: 11 },
      axisLine: { show: false }
    },
    series: [{
      type: 'bar',
      data: durations.map(d => ({
        value: d,
        itemStyle: {
          color: d < 15 ? 'rgba(200, 112, 96, 0.7)' :
            d > 180 ? 'rgba(201, 162, 85, 0.7)' :
              'rgba(212, 163, 106, 0.6)',
          borderRadius: [4, 4, 0, 0]
        }
      })),
      barWidth: 30,
      markLine: {
        silent: true,
        data: [{
          yAxis: avg,
          lineStyle: { color: '#88b498', width: 2, type: 'dashed' },
          label: { formatter: '平均 {c}s', color: '#88b498', fontSize: 11 }
        }]
      }
    }]
  }

  chart.setOption(option)
}

watch(() => props.turns, () => nextTick(renderChart), { deep: true })

onMounted(() => {
  renderChart()
  window.addEventListener('resize', () => chart?.resize())
})
</script>

<style scoped>
.time-distribution {
  margin: 16px 0;
  padding: 20px 24px;
  background: rgba(39, 37, 47, 0.6);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
}

.time-distribution h4 {
  margin: 0 0 14px;
  font-size: 14px;
  color: var(--gold);
  font-weight: 600;
}

.time-summary {
  display: flex;
  gap: 20px;
  margin-bottom: 14px;
}

.time-stat {
  text-align: center;
}

.time-stat strong {
  display: block;
  font-size: 18px;
  color: var(--text-primary);
  font-weight: 700;
}

.time-stat span {
  font-size: 11px;
  color: var(--text-muted);
}

.time-chart {
  width: 100%;
  height: 200px;
}

.time-anomalies {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.anomaly-title {
  font-size: 12px;
  color: var(--text-muted);
}

.anomaly-tag {
  padding: 3px 10px;
  border-radius: var(--radius-full);
  font-size: 11px;
  font-weight: 600;
}

.anomaly-tag.fast {
  color: var(--danger);
  background: var(--danger-surface);
}

.anomaly-tag.slow {
  color: var(--warning);
  background: rgba(201, 162, 85, 0.1);
}
</style>

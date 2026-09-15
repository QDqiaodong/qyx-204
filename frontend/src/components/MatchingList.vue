<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { matchingApi, type UnitMatching } from '@/api'

const units = ref<UnitMatching[]>([])
const loading = ref(false)

const columns = [
  { prop: 'unitCode', label: '单元编号' },
  { prop: 'buildingName', label: '所属楼栋' },
  { prop: 'floor', label: '所在楼层' },
  { prop: 'roomCount', label: '房间数' },
  { prop: 'residentCount', label: '居住人数' },
  { prop: 'buildingResidentTotal', label: '楼栋合计人数' },
  { prop: 'shiftQuotaCapacity', label: '当班定额' },
  { prop: 'totalCapacity', label: '总容纳容量' },
  { prop: 'remainingCapacity', label: '剩余容量' },
  { prop: 'usageRate', label: '使用率' },
  { prop: 'matchingStatus', label: '匹配状态' }
]

const getStatusColor = (status: string) => {
  switch (status) {
    case '匹配': return 'success'
    case '预警': return 'warning'
    case '不匹配': return 'danger'
    case '超定额': return 'danger'
    default: return 'info'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case '匹配': return '匹配'
    case '预警': return '容量预警'
    case '不匹配': return '不匹配'
    case '超定额': return '超定额'
    default: return '未绑定'
  }
}

const loadData = async () => {
  loading.value = true
  units.value = await matchingApi.getAllUnitsMatching()
  loading.value = false
}

const rowClassName = ({ row }: { row: UnitMatching }) => {
  return row.quotaExceeded ? 'over-quota-row' : ''
}

const handleExport = () => {
  const headers = ['单元编号', '所属楼栋', '楼层', '房间数', '居住人数', '楼栋合计人数', '当班定额', '总容纳容量', '剩余容量', '使用率', '匹配状态']
  const rows = units.value.map(u => [
    u.unitCode,
    u.buildingName,
    u.floor,
    u.roomCount,
    u.residentCount,
    u.buildingResidentTotal ?? '',
    u.shiftQuotaCapacity ?? '未开单',
    u.totalCapacity,
    u.remainingCapacity,
    `${u.usageRate.toFixed(1)}%`,
    getStatusText(u.matchingStatus)
  ])
  
  const csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n')
  const blob = new Blob(['\uFEFF' + csvContent], { type: 'text/csv;charset=utf-8;' })
  const link = document.createElement('a')
  link.href = URL.createObjectURL(blob)
  link.download = `单元容量适配一览_${new Date().toLocaleDateString()}.csv`
  link.click()
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-button type="primary" @click="loadData">刷新数据</el-button>
      <el-button type="success" @click="handleExport">导出适配一览</el-button>
    </div>

    <el-table :data="units" border style="width: 100%" :loading="loading" :row-class-name="rowClassName">
      <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label">
        <template #default="scope">
          <template v-if="col.prop === 'usageRate'">
            <el-progress :percentage="scope.row.usageRate" :color="getStatusColor(scope.row.matchingStatus)" :stroke-width="10" />
          </template>
          <template v-else-if="col.prop === 'matchingStatus'">
            <el-tag :type="getStatusColor(scope.row.matchingStatus)">
              {{ getStatusText(scope.row.matchingStatus) }}
            </el-tag>
          </template>
          <template v-else-if="col.prop === 'remainingCapacity'">
            <span :class="scope.row.remainingCapacity < 0 ? 'negative' : ''">
              {{ scope.row.remainingCapacity }}人
            </span>
          </template>
          <template v-else-if="col.prop === 'buildingResidentTotal'">
            <span :class="scope.row.quotaExceeded ? 'negative' : ''">
              {{ scope.row.buildingResidentTotal != null ? scope.row.buildingResidentTotal + '人' : '—' }}
            </span>
          </template>
          <template v-else-if="col.prop === 'shiftQuotaCapacity'">
            <span :class="scope.row.quotaExceeded ? 'negative' : ''">
              {{ scope.row.shiftQuotaCapacity != null ? scope.row.shiftQuotaCapacity + '人' : '未开单' }}
            </span>
          </template>
          <template v-else>
            {{ scope.row[col.prop] }}
          </template>
        </template>
      </el-table-column>
      <el-table-column label="配套洗漱台">
        <template #default="scope">
          <div v-if="scope.row.washbasins.length === 0" class="empty-washbasin">无</div>
          <el-tag v-for="w in scope.row.washbasins" :key="w.id" size="small" style="margin: 2px">
            {{ w.washbasinCode }} ({{ w.capacity }}人)
          </el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div class="summary">
      <el-card title="统计汇总" size="small">
        <div class="summary-grid">
          <div class="summary-item">
            <span class="summary-label">总单元数：</span>
            <span class="summary-value">{{ units.length }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">已绑定单元：</span>
            <span class="summary-value">{{ units.filter(u => u.washbasins.length > 0).length }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">匹配正常：</span>
            <span class="summary-value success">{{ units.filter(u => u.matchingStatus === '匹配').length }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">容量预警：</span>
            <span class="summary-value warning">{{ units.filter(u => u.matchingStatus === '预警').length }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">不匹配：</span>
            <span class="summary-value danger">{{ units.filter(u => u.matchingStatus === '不匹配').length }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">超定额：</span>
            <span class="summary-value danger">{{ units.filter(u => u.matchingStatus === '超定额').length }}</span>
          </div>
          <div class="summary-item">
            <span class="summary-label">未绑定：</span>
            <span class="summary-value info">{{ units.filter(u => u.matchingStatus === '未绑定').length }}</span>
          </div>
        </div>
      </el-card>
    </div>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  justify-content: flex-end;
}

.empty-washbasin {
  color: #999;
}

.negative {
  color: #f56c6c;
  font-weight: 600;
}

:deep(.over-quota-row) {
  background-color: #fef0f0;
}

.summary {
  margin-top: 20px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

.summary-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.summary-label {
  font-weight: 600;
  color: #666;
}

.summary-value {
  font-weight: 600;
  font-size: 18px;
  color: #333;
}

.summary-value.success {
  color: #67c23a;
}

.summary-value.warning {
  color: #e6a23c;
}

.summary-value.danger {
  color: #f56c6c;
}

.summary-value.info {
  color: #909399;
}
</style>

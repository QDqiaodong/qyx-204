<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { buildingApi, livingUnitApi, washbasinApi, matchingApi, type Building, type LivingUnit, type Washbasin, type MatchingCheckResult } from '@/api'

const buildings = ref<Building[]>([])
const units = ref<LivingUnit[]>([])
const washbasins = ref<Washbasin[]>([])
const selectedBuildingId = ref<number | null>(null)
const selectedUnitId = ref<number | null>(null)
const selectedWashbasinIds = ref<number[]>([])

const checkResult = ref<MatchingCheckResult | null>(null)
const currentUnitInfo = ref<LivingUnit | null>(null)

const filteredUnits = computed(() => {
  if (!selectedBuildingId.value) return []
  return units.value.filter(u => u.buildingId === selectedBuildingId.value)
})

const filteredWashbasins = computed(() => {
  if (!selectedBuildingId.value) return []
  return washbasins.value.filter(w => w.buildingId === selectedBuildingId.value)
})

watch(selectedUnitId, async (newVal) => {
  if (newVal) {
    const res = await matchingApi.getUnitMatching(newVal)
    selectedWashbasinIds.value = res.washbasins.map(w => w.id)
    currentUnitInfo.value = units.value.find(u => u.id === newVal) || null
    await checkCapacity()
  } else {
    selectedWashbasinIds.value = []
    checkResult.value = null
    currentUnitInfo.value = null
  }
})

const loadData = async () => {
  const [buildingData, unitData, washbasinData] = await Promise.all([
    buildingApi.getAll(),
    livingUnitApi.getAll(),
    washbasinApi.getAll()
  ])
  buildings.value = buildingData
  units.value = unitData
  washbasins.value = washbasinData
}

const checkCapacity = async () => {
  if (!selectedUnitId.value) return
  checkResult.value = await matchingApi.check(selectedUnitId.value)
}

const handleBind = async () => {
  if (!selectedUnitId.value || selectedWashbasinIds.value.length === 0) {
    ElMessage.warning('请选择居住单元和要绑定的洗漱台')
    return
  }

  for (const washbasinId of selectedWashbasinIds.value) {
    const res = await matchingApi.bind({ unitId: selectedUnitId.value!, washbasinId })
    checkResult.value = res
    if (res.checkResult === 'FAIL') {
      ElMessage.error(res.checkMessage)
    } else if (res.checkResult === 'WARN') {
      ElMessage.warning(res.checkMessage)
    } else {
      ElMessage.success('绑定成功')
    }
  }
  await checkCapacity()
}

const handleUnbind = async (washbasinId: number) => {
  if (!selectedUnitId.value) return
  const res = await matchingApi.unbind(selectedUnitId.value, washbasinId)
  ElMessage.success('解绑成功')
  selectedWashbasinIds.value = selectedWashbasinIds.value.filter(id => id !== washbasinId)
  checkResult.value = res
}

const getStatusColor = (result: string) => {
  switch (result) {
    case 'PASS': return 'success'
    case 'WARN': return 'warning'
    case 'FAIL': return 'danger'
    default: return 'info'
  }
}

onMounted(() => {
  loadData()
})
</script>

<template>
  <el-card>
    <div class="form-row">
      <el-form-item label="选择楼栋" label-width="80px">
        <el-select v-model="selectedBuildingId" placeholder="请选择楼栋" style="width: 200px">
          <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="选择居住单元" label-width="100px">
        <el-select v-model="selectedUnitId" placeholder="请选择居住单元" style="width: 200px">
          <el-option v-for="u in filteredUnits" :key="u.id" :label="`${u.unitCode} (${u.residentCount}人)`" :value="u.id" />
        </el-select>
      </el-form-item>
    </div>

    <div v-if="currentUnitInfo" class="unit-info">
      <el-card title="单元信息" size="small">
        <div class="info-grid">
          <div><span class="label">单元编号：</span>{{ currentUnitInfo.unitCode }}</div>
          <div><span class="label">居住人数：</span>{{ currentUnitInfo.residentCount }}人</div>
          <div><span class="label">房间数：</span>{{ currentUnitInfo.roomCount }}间</div>
          <div><span class="label">所在楼层：</span>{{ currentUnitInfo.floor }}层</div>
        </div>
      </el-card>
    </div>

    <div class="matching-section">
      <el-card title="可选洗漱台（同一楼栋）" size="small">
        <el-checkbox-group v-model="selectedWashbasinIds">
          <el-checkbox v-for="w in filteredWashbasins" :key="w.id" :label="w.id" :disabled="selectedUnitId === null">
            {{ w.washbasinCode }} - {{ w.location }} (容量: {{ w.capacity }}人)
          </el-checkbox>
        </el-checkbox-group>
        <el-button type="primary" @click="handleBind" :disabled="!selectedUnitId || selectedWashbasinIds.length === 0" style="margin-top: 10px">
          绑定选中洗漱台
        </el-button>
      </el-card>

      <el-card title="已绑定洗漱台" size="small">
        <div v-if="selectedWashbasinIds.length === 0" class="empty-tip">暂无绑定的洗漱台</div>
        <el-tag v-for="w in filteredWashbasins.filter(w => selectedWashbasinIds.includes(w.id))" :key="w.id" closable @close="handleUnbind(w.id)">
          {{ w.washbasinCode }} - {{ w.location }} (容量: {{ w.capacity }}人)
        </el-tag>
      </el-card>
    </div>

    <div v-if="checkResult" class="check-result">
      <el-card :title="'容量匹配校验结果'" size="small" :class="`result-${getStatusColor(checkResult.checkResult)}`">
        <div class="result-content">
          <div class="result-status">
            <el-tag :type="getStatusColor(checkResult.checkResult)" size="large">
              {{ checkResult.checkResult === 'PASS' ? '校验通过' : checkResult.checkResult === 'WARN' ? '容量预警' : '校验失败' }}
            </el-tag>
          </div>
          <div class="result-message">{{ checkResult.checkMessage }}</div>
          <div class="result-details">
            <div class="detail-item">
              <span class="detail-label">单元居住人数：</span>
              <span class="detail-value">{{ checkResult.unitResidentCount }}人</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">总容纳容量：</span>
              <span class="detail-value">{{ checkResult.totalCapacity }}人</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">剩余容量：</span>
              <span :class="checkResult.remainingCapacity < 0 ? 'negative' : ''" class="detail-value">
                {{ checkResult.remainingCapacity }}人
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">使用率：</span>
              <el-progress :percentage="checkResult.usageRate" :color="getStatusColor(checkResult.checkResult)" :stroke-width="20" />
            </div>
          </div>
        </div>
      </el-card>
    </div>
  </el-card>
</template>

<style scoped>
.form-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.unit-info {
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.label {
  font-weight: 600;
  color: #666;
}

.matching-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 20px;
}

.empty-tip {
  color: #999;
  padding: 20px;
  text-align: center;
}

.check-result {
  margin-top: 20px;
}

.result-content {
  padding: 10px 0;
}

.result-status {
  margin-bottom: 10px;
}

.result-message {
  color: #666;
  margin-bottom: 15px;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 4px;
}

.result-details {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-label {
  font-weight: 600;
  width: 120px;
}

.detail-value {
  font-weight: 600;
  color: #333;
}

.detail-value.negative {
  color: #f56c6c;
}

.result-success :deep(.el-card__header) {
  border-left: 4px solid #67c23a;
}

.result-warning :deep(.el-card__header) {
  border-left: 4px solid #e6a23c;
}

.result-danger :deep(.el-card__header) {
  border-left: 4px solid #f56c6c;
}
</style>

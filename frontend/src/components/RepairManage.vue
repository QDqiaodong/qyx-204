<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  buildingApi,
  washbasinApi,
  repairOrderApi,
  type Building,
  type Washbasin,
  type RepairOrder
} from '@/api'

const buildings = ref<Building[]>([])
const washbasins = ref<Washbasin[]>([])
const orders = ref<RepairOrder[]>([])
const loading = ref(false)

const filterBuildingId = ref<number | null>(null)
const filterStatus = ref<string>('')
const filterWashbasinId = ref<number | null>(null)

const createVisible = ref(false)
const createForm = ref({
  washbasinId: null as number | null,
  damagePart: '',
  dutyPerson: ''
})

// 开单弹窗里每台洗漱台当前是否已有未结送检（用于禁用选择 + 提示上一张起始时间）
const currentOpenMap = ref<Record<number, RepairOrder>>({})

const completeVisible = ref(false)
const completeForm = ref({
  id: 0,
  washbasinCode: '',
  damagePart: '',
  dutyPerson: '',
  startedAt: '',
  repairNote: ''
})

const buildingName = (id: number) =>
  buildings.value.find(b => b.id === id)?.buildingName || String(id)

const washbasinOf = (id: number) => washbasins.value.find(w => w.id === id)

const washbasinCode = (id: number) => washbasinOf(id)?.washbasinCode || `#${id}`

const washbasinLocation = (id: number) => washbasinOf(id)?.location || '—'

// 按当前楼栋筛选可选洗漱台
const washbasinOptions = computed(() =>
  filterBuildingId.value
    ? washbasins.value.filter(w => w.buildingId === filterBuildingId.value)
    : washbasins.value
)

const formatTime = (t?: string) => (t ? new Date(t).toLocaleString() : '—')

const loadBuildings = async () => {
  buildings.value = await buildingApi.getAll()
}

const loadWashbasins = async () => {
  washbasins.value = await washbasinApi.getAll()
}

const loadOrders = async () => {
  loading.value = true
  try {
    const params: { buildingId?: number; status?: string; washbasinId?: number } = {}
    if (filterBuildingId.value) params.buildingId = filterBuildingId.value
    if (filterStatus.value) params.status = filterStatus.value
    if (filterWashbasinId.value) params.washbasinId = filterWashbasinId.value
    orders.value = await repairOrderApi.getAll(params)
  } finally {
    loading.value = false
  }
}

// 给每台洗漱台查当前未结单，用于开单弹窗禁用已在送检的台（一次批量取回）
const refreshCurrentOpenMap = async () => {
  const openOrders = await repairOrderApi.getOpen()
  const map: Record<number, RepairOrder> = {}
  for (const o of openOrders) map[o.washbasinId] = o
  currentOpenMap.value = map
}

const loadAll = async () => {
  await loadOrders()
  await refreshCurrentOpenMap()
}

const openCreate = () => {
  createForm.value = { washbasinId: null, damagePart: '', dutyPerson: '' }
  createVisible.value = true
}

const submitCreate = async () => {
  const f = createForm.value
  if (!f.washbasinId) {
    ElMessage.warning('请选择一台洗漱台')
    return
  }
  if (!f.damagePart.trim()) {
    ElMessage.warning('请写清损坏部位')
    return
  }
  if (!f.dutyPerson.trim()) {
    ElMessage.warning('请填写经办值班员')
    return
  }
  try {
    await repairOrderApi.create({
      washbasinId: f.washbasinId,
      damagePart: f.damagePart.trim(),
      dutyPerson: f.dutyPerson.trim(),
      operator: f.dutyPerson.trim()
    })
    ElMessage.success('送检单已开（待接单）')
    createVisible.value = false
    loadAll()
  } catch (e: any) {
    // 后端会带出上一张未结单的起始时间、单号、经办人
    ElMessage.error(e?.message || '开单失败')
  }
}

const openComplete = (row: RepairOrder) => {
  completeForm.value = {
    id: row.id,
    washbasinCode: washbasinCode(row.washbasinId),
    damagePart: row.damagePart,
    dutyPerson: row.dutyPerson,
    startedAt: formatTime(row.createdAt),
    repairNote: ''
  }
  completeVisible.value = true
}

const submitComplete = async () => {
  try {
    await repairOrderApi.complete(completeForm.value.id, {
      repairNote: completeForm.value.repairNote.trim() || undefined
    })
    ElMessage.success('已登记修复，该台重新计入配套')
    completeVisible.value = false
    loadAll()
  } catch (e: any) {
    ElMessage.error(e?.message || '登记修复失败')
  }
}

const statusTag = (status: string) =>
  status === 'PENDING' ? '待接单' : '已修复'

onMounted(async () => {
  await loadBuildings()
  await loadWashbasins()
  loadAll()
})
</script>

<template>
  <el-card>
    <el-alert
      type="warning"
      :closable="false"
      title="送检规则：同一台洗漱台只要还有未结送检（待接单），不能再开第二张；未结期间该台从匹配一览的配套洗漱台中剔除，绑定痕迹保留；登记修复后才能重新配套、再次送检。"
      style="margin-bottom: 16px"
    />

    <div class="toolbar">
      <el-select
        v-model="filterBuildingId"
        placeholder="按楼栋筛选"
        clearable
        style="width: 180px"
        @change="() => { filterWashbasinId = null; loadOrders() }"
      >
        <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
      </el-select>
      <el-select
        v-model="filterWashbasinId"
        placeholder="按洗漱台筛选"
        clearable
        style="width: 200px"
        @change="loadOrders"
      >
        <el-option
          v-for="w in washbasinOptions"
          :key="w.id"
          :label="`${w.washbasinCode}（${w.location}）`"
          :value="w.id"
        />
      </el-select>
      <el-select
        v-model="filterStatus"
        placeholder="按状态筛选"
        clearable
        style="width: 140px"
        @change="loadOrders"
      >
        <el-option label="待接单" value="PENDING" />
        <el-option label="已修复" value="REPAIRED" />
      </el-select>
      <el-button type="primary" @click="loadAll">刷新</el-button>
      <el-button type="success" @click="openCreate">开送检单</el-button>
    </div>

    <el-table :data="orders" border style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="单号" width="70" />
      <el-table-column label="洗漱台" min-width="170">
        <template #default="scope">
          <div>{{ washbasinCode(scope.row.washbasinId) }}</div>
          <div class="sub-text">
            {{ buildingName(washbasinOf(scope.row.washbasinId)?.buildingId || 0) }}
            · {{ washbasinLocation(scope.row.washbasinId) }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="damagePart" label="损坏部位" min-width="140" show-overflow-tooltip />
      <el-table-column prop="dutyPerson" label="经办值班员" width="110" />
      <el-table-column label="送检开始" width="170">
        <template #default="scope">{{ formatTime(scope.row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'PENDING' ? 'warning' : 'success'">
            {{ statusTag(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="修复备注 / 修复时间" min-width="180">
        <template #default="scope">
          <template v-if="scope.row.status === 'PENDING'">
            <span class="sub-text">尚未修复</span>
          </template>
          <template v-else>
            <div>{{ scope.row.repairNote || '—' }}</div>
            <div class="sub-text">{{ formatTime(scope.row.repairedAt) }}</div>
          </template>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="scope">
          <el-button
            v-if="scope.row.status === 'PENDING'"
            size="small"
            type="success"
            @click="openComplete(scope.row)"
          >登记修复</el-button>
          <span v-else class="sub-text">—</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="开洗漱台送检单" v-model="createVisible" width="520px">
      <el-form :model="createForm" label-width="110px">
        <el-form-item label="送检洗漱台" required>
          <el-select
            v-model="createForm.washbasinId"
            placeholder="请选择一台洗漱台"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="w in washbasinOptions"
              :key="w.id"
              :value="w.id"
              :disabled="currentOpenMap[w.id] != null"
            >
              <span>{{ w.washbasinCode }}（{{ buildingName(w.buildingId) }} · {{ w.location }}）</span>
              <span v-if="currentOpenMap[w.id]" class="opt-warn">
                送检中（{{ formatTime(currentOpenMap[w.id]?.createdAt) }} 起待接单）
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="损坏部位" required>
          <el-input
            v-model="createForm.damagePart"
            type="textarea"
            :rows="2"
            placeholder="例如：水龙头漏水 / 下水管堵塞 / 台面开裂"
          />
        </el-form-item>
        <el-form-item label="经办值班员" required>
          <el-input v-model="createForm.dutyPerson" placeholder="请输入经办值班员" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">开单（待接单）</el-button>
      </template>
    </el-dialog>

    <el-dialog title="登记修复" v-model="completeVisible" width="480px">
      <el-descriptions :column="1" border size="small" style="margin-bottom: 16px">
        <el-descriptions-item label="洗漱台">{{ completeForm.washbasinCode }}</el-descriptions-item>
        <el-descriptions-item label="损坏部位">{{ completeForm.damagePart }}</el-descriptions-item>
        <el-descriptions-item label="经办值班员">{{ completeForm.dutyPerson }}</el-descriptions-item>
        <el-descriptions-item label="待接单起始">{{ completeForm.startedAt }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="90px">
        <el-form-item label="修复备注">
          <el-input
            v-model="completeForm.repairNote"
            type="textarea"
            :rows="2"
            placeholder="修没修好、换了什么（可选）"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="completeVisible = false">取消</el-button>
        <el-button type="success" @click="submitComplete">确认已修复</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.sub-text {
  color: #909399;
  font-size: 12px;
}

.opt-warn {
  color: #e6a23c;
  font-size: 12px;
  margin-left: 8px;
}
</style>

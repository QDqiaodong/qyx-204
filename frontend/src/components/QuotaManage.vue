<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  buildingApi,
  quotaOrderApi,
  type Building,
  type ShiftQuotaOrder,
  type BuildingQuotaStatus
} from '@/api'

const buildings = ref<Building[]>([])
const orders = ref<ShiftQuotaOrder[]>([])
const statuses = ref<BuildingQuotaStatus[]>([])
const loading = ref(false)
const filterBuildingId = ref<number | null>(null)
const filterStatus = ref<string>('')

const createVisible = ref(false)
const createForm = ref({
  buildingId: null as number | null,
  quotaDate: '',
  dutyPerson: '',
  quotaCapacity: 0,
  shiftStart: '',
  shiftEnd: '',
  reopenReason: ''
})

const appendVisible = ref(false)
const appendForm = ref({
  id: 0,
  buildingName: '',
  currentCapacity: 0,
  additionalCapacity: 1,
  operator: ''
})

const formatDate = (d: Date) => {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

const formatDateTime = (d: Date) => {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${formatDate(d)}T${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const buildingName = (id: number) =>
  buildings.value.find(b => b.id === id)?.buildingName || String(id)

const loadBuildings = async () => {
  buildings.value = await buildingApi.getAll()
}

const loadOrders = async () => {
  loading.value = true
  try {
    const params: { buildingId?: number; status?: string } = {}
    if (filterBuildingId.value) params.buildingId = filterBuildingId.value
    if (filterStatus.value) params.status = filterStatus.value
    orders.value = await quotaOrderApi.getAll(params)
  } finally {
    loading.value = false
  }
}

const loadStatuses = async () => {
  statuses.value = await quotaOrderApi.getBuildingStatuses()
}

const loadAll = () => {
  loadOrders()
  loadStatuses()
}

const openCreate = () => {
  const now = new Date()
  const start = new Date(now)
  start.setHours(20, 0, 0, 0)
  const end = new Date(start)
  end.setDate(end.getDate() + 1)
  end.setHours(8, 0, 0, 0)
  createForm.value = {
    buildingId: null,
    quotaDate: formatDate(now),
    dutyPerson: '',
    quotaCapacity: 0,
    shiftStart: formatDateTime(start),
    shiftEnd: formatDateTime(end),
    reopenReason: ''
  }
  createVisible.value = true
}

const submitCreate = async () => {
  const f = createForm.value
  if (!f.buildingId || !f.dutyPerson.trim() || !f.shiftStart || !f.shiftEnd) {
    ElMessage.warning('请填写楼栋、值班人和当班起止时间')
    return
  }
  try {
    await quotaOrderApi.create({
      buildingId: f.buildingId,
      quotaDate: f.quotaDate,
      dutyPerson: f.dutyPerson.trim(),
      quotaCapacity: f.quotaCapacity,
      shiftStart: f.shiftStart,
      shiftEnd: f.shiftEnd,
      reopenReason: f.reopenReason.trim() || undefined,
      operator: f.dutyPerson.trim()
    })
    ElMessage.success('开单成功')
    createVisible.value = false
    loadAll()
  } catch (e: any) {
    ElMessage.error(e?.message || '开单失败')
  }
}

const openAppend = (row: ShiftQuotaOrder) => {
  appendForm.value = {
    id: row.id,
    buildingName: buildingName(row.buildingId),
    currentCapacity: row.quotaCapacity,
    additionalCapacity: 1,
    operator: row.dutyPerson
  }
  appendVisible.value = true
}

const submitAppend = async () => {
  const f = appendForm.value
  if (!f.additionalCapacity || f.additionalCapacity < 1) {
    ElMessage.warning('追加人数必须大于0')
    return
  }
  try {
    await quotaOrderApi.append(f.id, {
      additionalCapacity: f.additionalCapacity,
      operator: f.operator.trim() || undefined
    })
    ElMessage.success('定额追加成功')
    appendVisible.value = false
    loadAll()
  } catch (e: any) {
    ElMessage.error(e?.message || '追加失败')
  }
}

const handleClose = async (row: ShiftQuotaOrder) => {
  try {
    await ElMessageBox.confirm(
      `确定结案该定额单吗？（${buildingName(row.buildingId)}，定额${row.quotaCapacity}人，值班人${row.dutyPerson}）`,
      '结案确认',
      { type: 'warning' }
    )
  } catch {
    return
  }
  try {
    await quotaOrderApi.close(row.id, row.dutyPerson)
    ElMessage.success('结案成功')
    loadAll()
  } catch (e: any) {
    ElMessage.error(e?.message || '结案失败')
  }
}

const formatTime = (t?: string) => (t ? new Date(t).toLocaleString() : '')

onMounted(async () => {
  await loadBuildings()
  loadAll()
})
</script>

<template>
  <el-card>
    <div class="status-cards">
      <div
        v-for="s in statuses"
        :key="s.buildingId"
        class="status-card"
        :class="{ 'over-quota': s.overQuota }"
      >
        <div class="status-title">{{ s.buildingName }}</div>
        <div class="status-body">
          <span>居住合计：{{ s.residentTotal }}人</span>
          <span v-if="s.hasOpenOrder">当班定额：{{ s.quotaCapacity }}人（{{ s.dutyPerson }}）</span>
          <span v-else>当班定额：未开单</span>
        </div>
        <el-tag v-if="s.overQuota" type="danger" size="small">超定额</el-tag>
        <el-tag v-else-if="s.hasOpenOrder" type="success" size="small">定额内</el-tag>
        <el-tag v-else type="info" size="small">未开单</el-tag>
      </div>
    </div>

    <div class="toolbar">
      <el-select v-model="filterBuildingId" placeholder="按楼栋筛选" clearable @change="loadOrders">
        <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="按状态筛选" clearable @change="loadOrders">
        <el-option label="未结" value="OPEN" />
        <el-option label="已结" value="CLOSED" />
      </el-select>
      <el-button type="primary" @click="loadAll">刷新</el-button>
      <el-button type="success" @click="openCreate">开具当班定额单</el-button>
    </div>

    <el-table :data="orders" border style="width: 100%" :loading="loading">
      <el-table-column label="楼栋">
        <template #default="scope">{{ buildingName(scope.row.buildingId) }}</template>
      </el-table-column>
      <el-table-column prop="quotaDate" label="定额日期" />
      <el-table-column prop="dutyPerson" label="值班人" />
      <el-table-column label="定额可洗人数">
        <template #default="scope">{{ scope.row.quotaCapacity }}人</template>
      </el-table-column>
      <el-table-column label="当班起止">
        <template #default="scope">
          {{ formatTime(scope.row.shiftStart) }} ~ {{ formatTime(scope.row.shiftEnd) }}
        </template>
      </el-table-column>
      <el-table-column label="状态">
        <template #default="scope">
          <el-tag :type="scope.row.status === 'OPEN' ? 'warning' : 'info'">
            {{ scope.row.status === 'OPEN' ? '未结' : '已结' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="补开原因">
        <template #default="scope">{{ scope.row.reopenReason || '—' }}</template>
      </el-table-column>
      <el-table-column label="结案时间">
        <template #default="scope">{{ scope.row.closedAt ? formatTime(scope.row.closedAt) : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="scope">
          <template v-if="scope.row.status === 'OPEN'">
            <el-button size="small" type="primary" @click="openAppend(scope.row)">追加定额</el-button>
            <el-button size="small" type="danger" @click="handleClose(scope.row)">结案</el-button>
          </template>
          <span v-else>—</span>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog title="开具当班定额单" v-model="createVisible" width="520px">
      <el-alert
        type="info"
        :closable="false"
        title="每个楼栋每个自然日只允许一张未结定额单；同日补开需填写补开原因。"
        style="margin-bottom: 15px"
      />
      <el-form :model="createForm" label-width="110px">
        <el-form-item label="所属楼栋" required>
          <el-select v-model="createForm.buildingId" placeholder="请选择楼栋">
            <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="定额日期" required>
          <el-date-picker v-model="createForm.quotaDate" type="date" value-format="YYYY-MM-DD" placeholder="请选择定额日期" />
        </el-form-item>
        <el-form-item label="值班人" required>
          <el-input v-model="createForm.dutyPerson" placeholder="请输入值班人" />
        </el-form-item>
        <el-form-item label="定额可洗人数" required>
          <el-input-number v-model="createForm.quotaCapacity" :min="0" />
        </el-form-item>
        <el-form-item label="当班开始" required>
          <el-date-picker v-model="createForm.shiftStart" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="请选择当班开始时间" />
        </el-form-item>
        <el-form-item label="当班结束" required>
          <el-date-picker v-model="createForm.shiftEnd" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" placeholder="请选择当班结束时间" />
        </el-form-item>
        <el-form-item label="补开原因">
          <el-input v-model="createForm.reopenReason" type="textarea" placeholder="同日补开时必填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">开单</el-button>
      </template>
    </el-dialog>

    <el-dialog title="追加定额" v-model="appendVisible" width="480px">
      <el-form :model="appendForm" label-width="110px">
        <el-form-item label="所属楼栋">
          <span>{{ appendForm.buildingName }}</span>
        </el-form-item>
        <el-form-item label="当前定额">
          <span>{{ appendForm.currentCapacity }}人</span>
        </el-form-item>
        <el-form-item label="追加人数" required>
          <el-input-number v-model="appendForm.additionalCapacity" :min="1" />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="appendForm.operator" placeholder="请输入操作人" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="appendVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAppend">确认追加</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.status-cards {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.status-card {
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  padding: 12px 16px;
  min-width: 220px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: #fafafa;
}

.status-card.over-quota {
  border-color: #f56c6c;
  background: #fef0f0;
}

.status-title {
  font-weight: 600;
  color: #333;
}

.status-body {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  color: #666;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  justify-content: flex-end;
}
</style>

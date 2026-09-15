<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { buildingApi, livingUnitApi, type Building, type LivingUnit } from '@/api'

const buildings = ref<Building[]>([])
const units = ref<LivingUnit[]>([])
const form = ref<Partial<LivingUnit>>({})
const visible = ref(false)
const isEdit = ref(false)
const searchBuildingId = ref<number | null>(null)

const columns = [
  { prop: 'unitCode', label: '单元编号' },
  { prop: 'buildingName', label: '所属楼栋' },
  { prop: 'floor', label: '所在楼层' },
  { prop: 'roomCount', label: '房间数' },
  { prop: 'residentCount', label: '居住人数' },
  { prop: 'status', label: '状态', formatter: (row: LivingUnit & { buildingName?: string }) => row.status === 1 ? '正常' : '停用' },
  { prop: 'createdAt', label: '创建时间', formatter: (row: LivingUnit & { buildingName?: string }) => row.createdAt ? new Date(row.createdAt).toLocaleString() : '' }
]

const loadBuildings = async () => {
  buildings.value = await buildingApi.getAll()
}

const loadUnits = async () => {
  const data = await livingUnitApi.getAll()
  units.value = data.map(u => ({
    ...u,
    buildingName: buildings.value.find(b => b.id === u.buildingId)?.buildingName || ''
  }))
}

const handleAdd = () => {
  form.value = { status: 1, residentCount: 0 }
  isEdit.value = false
  visible.value = true
}

const handleEdit = (row: LivingUnit) => {
  form.value = { ...row }
  isEdit.value = true
  visible.value = true
}

const handleDelete = async (row: LivingUnit) => {
  await ElMessageBox.confirm('确定要删除该居住单元吗？', '提示', { type: 'warning' })
  await livingUnitApi.delete(row.id)
  ElMessage.success('删除成功')
  loadUnits()
}

const handleSubmit = async () => {
  if (!form.value.unitCode || !form.value.buildingId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    if (isEdit.value) {
      await livingUnitApi.update(form.value.id!, form.value)
      ElMessage.success('更新成功')
    } else {
      await livingUnitApi.create(form.value as Omit<LivingUnit, 'id'>)
      ElMessage.success('创建成功')
    }
    visible.value = false
    loadUnits()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const handleSearch = async () => {
  if (searchBuildingId.value) {
    const data = await livingUnitApi.getByBuildingId(searchBuildingId.value)
    units.value = data.map(u => ({
      ...u,
      buildingName: buildings.value.find(b => b.id === u.buildingId)?.buildingName || ''
    }))
  } else {
    loadUnits()
  }
}

onMounted(() => {
  loadBuildings()
  loadUnits()
})
</script>

<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="searchBuildingId" placeholder="按楼栋筛选" clearable @change="handleSearch">
        <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
      </el-select>
      <el-button type="primary" @click="handleAdd">新增居住单元</el-button>
    </div>
    <el-table :data="units" border style="width: 100%">
      <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label">
        <template #default="scope">
          {{ col.formatter ? col.formatter(scope.row) : scope.row[col.prop] }}
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="scope">
          <el-button size="small" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog :title="isEdit ? '编辑居住单元' : '新增居住单元'" v-model="visible">
      <el-form :model="form" label-width="100px">
        <el-form-item label="单元编号" required>
          <el-input v-model="form.unitCode" placeholder="请输入单元编号" />
        </el-form-item>
        <el-form-item label="所属楼栋" required>
          <el-select v-model="form.buildingId" placeholder="请选择楼栋">
            <el-option v-for="b in buildings" :key="b.id" :label="b.buildingName" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所在楼层">
          <el-input-number v-model="form.floor" :min="1" placeholder="请输入楼层" />
        </el-form-item>
        <el-form-item label="房间数">
          <el-input-number v-model="form.roomCount" :min="1" placeholder="请输入房间数" />
        </el-form-item>
        <el-form-item label="居住人数">
          <el-input-number v-model="form.residentCount" :min="0" placeholder="请输入居住人数" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option :label="1" :value="1">正常</el-option>
            <el-option :label="0" :value="0">停用</el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  justify-content: flex-end;
}
</style>

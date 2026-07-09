import axios, { type AxiosInstance, type AxiosRequestConfig } from 'axios'

const request: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 10000
})

request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  error => {
    console.error('API Error:', error)
    return Promise.reject(error)
  }
)

const get = async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  return request.get(url, config) as Promise<T>
}

const post = async <T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> => {
  return request.post(url, data, config) as Promise<T>
}

const put = async <T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> => {
  return request.put(url, data, config) as Promise<T>
}

const del = async <T>(url: string, config?: AxiosRequestConfig): Promise<T> => {
  return request.delete(url, config) as Promise<T>
}

export interface Building {
  id: number
  buildingCode: string
  buildingName: string
  floorCount: number
  createdAt?: string
}

export interface Washbasin {
  id: number
  washbasinCode: string
  capacity: number
  buildingId: number
  location: string
  status: number
  createdAt?: string
}

export interface LivingUnit {
  id: number
  unitCode: string
  buildingId: number
  floor: number
  roomCount: number
  residentCount: number
  status: number
  createdAt?: string
}

export interface MatchingCheckResult {
  success: boolean
  checkResult: string
  checkMessage: string
  unitResidentCount: number
  totalCapacity: number
  remainingCapacity: number
  usageRate: number
}

export interface UnitMatching {
  unitId: number
  unitCode: string
  buildingName: string
  floor: number
  roomCount: number
  residentCount: number
  washbasins: WashbasinInfo[]
  totalCapacity: number
  remainingCapacity: number
  usageRate: number
  matchingStatus: string
}

export interface WashbasinInfo {
  id: number
  washbasinCode: string
  capacity: number
  location: string
}

export interface MatchingCheckRecord {
  id: number
  unitId: number
  washbasinId: number
  checkType: string
  unitResidentCount: number
  totalCapacity: number
  checkResult: string
  checkMessage: string
  operator: string
  checkTime: string
}

export const buildingApi = {
  getAll: () => get<Building[]>('/buildings')
}

export const washbasinApi = {
  getAll: () => get<Washbasin[]>('/washbasins'),
  getByBuildingId: (buildingId: number) =>
    get<Washbasin[]>(`/washbasins/building/${buildingId}`),
  create: (data: Omit<Washbasin, 'id'>) =>
    post<Washbasin>('/washbasins', data),
  update: (id: number, data: Partial<Washbasin>) =>
    put<Washbasin>(`/washbasins/${id}`, data),
  delete: (id: number) => del<void>(`/washbasins/${id}`)
}

export const livingUnitApi = {
  getAll: () => get<LivingUnit[]>('/living-units'),
  getByBuildingId: (buildingId: number) =>
    get<LivingUnit[]>(`/living-units/building/${buildingId}`),
  create: (data: Omit<LivingUnit, 'id'>) =>
    post<LivingUnit>('/living-units', data),
  update: (id: number, data: Partial<LivingUnit>) =>
    put<LivingUnit>(`/living-units/${id}`, data),
  delete: (id: number) => del<void>(`/living-units/${id}`)
}

export const matchingApi = {
  bind: (data: { unitId: number; washbasinId: number }) =>
    post<MatchingCheckResult>('/matching/bind', data),
  unbind: (unitId: number, washbasinId: number) =>
    post<MatchingCheckResult>(
      `/matching/unbind?unitId=${unitId}&washbasinId=${washbasinId}`
    ),
  check: (unitId: number) =>
    get<MatchingCheckResult>(`/matching/check/${unitId}`),
  getUnitMatching: (unitId: number) =>
    get<UnitMatching>(`/matching/unit/${unitId}`),
  getAllUnitsMatching: () =>
    get<UnitMatching[]>('/matching/units'),
  getRecords: () =>
    get<MatchingCheckRecord[]>('/matching/records'),
  getRecordsByUnit: (unitId: number) =>
    get<MatchingCheckRecord[]>(`/matching/records/${unitId}`)
}

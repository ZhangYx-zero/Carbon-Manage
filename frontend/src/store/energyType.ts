import { defineStore } from 'pinia'
import { listEnergyTypes } from '@/api/energyType'

export interface EnergyType {
  id: number
  typeCode: string
  typeName: string
  unit: string
  ratioMin: number
  ratioMax: number
  status: number
  sortOrder: number
  remark?: string
}

let loading: Promise<void> | null = null

export const useEnergyTypeStore = defineStore('energyType', {
  state: () => ({
    types: [] as EnergyType[],
    loaded: false
  }),
  getters: {
    enabledTypes: (state): EnergyType[] =>
      [...state.types]
        .filter((t) => t.status === 1)
        .sort((a, b) => a.sortOrder - b.sortOrder || a.id - b.id)
  },
  actions: {
    /**
     * 单飞加载全量类型（含停用），用于历史数据的名称/单位映射；
     * 下拉选项使用 enabledTypes，历史停用类型仍能正确显示。
     */
    async ensureLoaded(force = false) {
      if (this.loaded && !force) return
      if (loading) return loading
      loading = listEnergyTypes()
        .then((res: any) => {
          this.types = res || []
          this.loaded = true
        })
        .catch(() => {
          this.types = []
          this.loaded = true
        })
        .finally(() => {
          loading = null
        })
      return loading
    },
    labelOf(code?: string) {
      if (!code) return ''
      const hit = this.types.find((t) => t.typeCode === code)
      return hit ? hit.typeName : code
    },
    unitOf(code?: string) {
      if (!code) return ''
      const hit = this.types.find((t) => t.typeCode === code)
      return hit ? hit.unit : ''
    }
  }
})

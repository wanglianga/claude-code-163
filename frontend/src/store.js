import { reactive } from 'vue'

export const store = reactive({
  user: JSON.parse(localStorage.getItem('user') || 'null'),
  enums: null,

  setUser(user, token) {
    this.user = user
    localStorage.setItem('user', JSON.stringify(user))
    localStorage.setItem('token', token)
  },

  logout() {
    this.user = null
    this.enums = null
    localStorage.removeItem('user')
    localStorage.removeItem('token')
  },

  /** 状态码 -> 中文标签 */
  label(group, value) {
    if (!value) return '-'
    const item = (this.enums?.[group] || []).find(i => i.value === value)
    return item ? item.label : value
  },

  roleLabel() {
    return this.label('roles', this.user?.role)
  }
})

export const STATUS_TAG = {
  REPORTED: 'info', INVESTIGATING: 'warning', OWNER_CONFIRMED: 'primary',
  OWNER_UNDETERMINED: 'danger', ORDER_ISSUED: 'primary', REPAIRING: 'warning',
  REPAIRED: 'warning', ACCEPTED: 'success', CLOSED: 'success',
  PENDING: 'danger', DISPATCHED: 'warning', SECURED: 'success',
  CONFIRMED: 'success', UNDETERMINED: 'danger',
  ISSUED: 'primary', ACCEPTED_WO: 'primary', FINISHED: 'warning', VERIFIED: 'success', RETURNED: 'danger',
  DECIDED: 'warning', PAID: 'success', REJECTED: 'info', ALLOCATED: 'success'
}

export const DANGER_TAG = { 高: 'danger', 中: 'warning', 低: 'success' }

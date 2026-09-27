import axios from 'axios'
import { ElMessage } from 'element-plus'

const api = axios.create({ baseURL: '/api', timeout: 20000 })

api.interceptors.request.use(cfg => {
  const token = localStorage.getItem('token')
  if (token) cfg.headers['X-Token'] = token
  return cfg
})

api.interceptors.response.use(
  res => {
    if (res.data && res.data.code !== 0) {
      ElMessage.error(res.data.msg || '操作失败')
      return Promise.reject(new Error(res.data.msg))
    }
    return res.data.data
  },
  err => {
    ElMessage.error(err.response?.data?.msg || err.message || '网络错误')
    return Promise.reject(err)
  }
)

export default api

import { createRouter, createWebHistory } from 'vue-router'
import { store } from './store'

const routes = [
  { path: '/login', component: () => import('./views/Login.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('./views/Layout.vue'),
    redirect: '/events',
    children: [
      { path: 'events', component: () => import('./views/EventList.vue'), meta: { title: '事件中心' } },
      { path: 'events/:id', component: () => import('./views/EventDetail.vue'), meta: { title: '事件详情' } },
      { path: 'report', component: () => import('./views/ReportEvent.vue'), meta: { title: '市民上报', roles: ['CITIZEN', 'ADMIN'] } },
      { path: 'work-orders', component: () => import('./views/WorkOrders.vue'), meta: { title: '维修工单', roles: ['REPAIR_TEAM', 'OWNER_UNIT', 'ADMIN', 'STREET_DUTY', 'INSPECTOR'] } },
      { path: 'compensations', component: () => import('./views/Compensations.vue'), meta: { title: '赔付案件', roles: ['COMPENSATION', 'ADMIN', 'CITIZEN'] } },
      { path: 'costs', component: () => import('./views/Costs.vue'), meta: { title: '费用复核', roles: ['ADMIN', 'STREET_DUTY'] } },
      { path: 'stats', component: () => import('./views/Stats.vue'), meta: { title: '统计分析' } },
      { path: 'admin', component: () => import('./views/Admin.vue'), meta: { title: '系统管理', roles: ['ADMIN'] } }
    ]
  }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach(to => {
  if (to.meta.public) return true
  if (!store.user) return '/login'
  return true
})

export default router

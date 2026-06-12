import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '../api/http'

const routes = [
  {
    path: '/',
    name: 'home',
    component: () => import('../views/HomePage.vue')
  },
  {
    path: '/coach',
    name: 'coach',
    component: () => import('../views/CoachPage.vue')
  },
  {
    path: '/interview',
    name: 'interview',
    component: () => import('../views/InterviewPage.vue')
  },
  {
    path: '/resources',
    name: 'resources',
    component: () => import('../views/ResourcesPage.vue')
  },
  {
    path: '/reports',
    name: 'reports',
    component: () => import('../views/ReportsPage.vue')
  },
  {
    path: '/agent',
    name: 'agent',
    component: () => import('../views/AgentPage.vue')
  },
  {
    path: '/skills',
    name: 'skills',
    component: () => import('../views/SkillsPage.vue')
  },
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/ProfilePage.vue')
  },
  {
    path: '/light',
    name: 'light',
    component: () => import('../views/LightWorkbench.vue')
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = getToken()
  if (to.path !== '/' && !token) {
    next({ path: '/' })
  } else {
    next()
  }
})

export default router

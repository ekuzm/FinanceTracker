import { createRouter, createWebHistory } from 'vue-router';
import DashboardPage from '@/pages/DashboardPage.vue';
import TransactionsPage from '@/pages/TransactionsPage.vue';
import AccountsPage from '@/pages/AccountsPage.vue';
import BudgetsPage from '@/pages/BudgetsPage.vue';
import UsersPage from '@/pages/UsersPage.vue';
import TagsPage from '@/pages/TagsPage.vue';

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: DashboardPage,
      meta: {
        title: 'Overview',
        description: 'Track liquidity, budget pressure, and recent activity in one place.',
      },
    },
    {
      path: '/transactions',
      name: 'transactions',
      component: TransactionsPage,
      meta: {
        title: 'Transactions',
        description: 'Filter, inspect, and maintain the ledger.',
      },
    },
    {
      path: '/accounts',
      name: 'accounts',
      component: AccountsPage,
      meta: {
        title: 'Accounts',
        description: 'Monitor balances and move money between owners.',
      },
    },
    {
      path: '/budgets',
      name: 'budgets',
      component: BudgetsPage,
      meta: {
        title: 'Budgets',
        description: 'Stay ahead of spend velocity and budget windows.',
      },
    },
    {
      path: '/users',
      name: 'users',
      component: UsersPage,
      meta: {
        title: 'Users',
        description: 'Manage the admin roster and ownership links.',
      },
    },
    {
      path: '/tags',
      name: 'tags',
      component: TagsPage,
      meta: {
        title: 'Tags',
        description: 'Keep categorization light, consistent, and easy to update.',
      },
    },
    {
      path: '/tasks',
      redirect: '/transactions',
    },
  ],
});

export default router;

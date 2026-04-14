<template>
  <div class="sidebar-card">
    <div class="brand-lockup">
      <div class="brand-mark">FT</div>
      <div>
        <h1>FINANCE-TRACKER</h1>
      </div>
    </div>

    <nav class="sidebar-nav">
      <RouterLink
        v-for="item in navigation"
        :key="item.to"
        :to="item.to"
        class="sidebar-link"
      >
        <span class="sidebar-link__label">{{ item.label }}</span>
        <span class="sidebar-link__caption">{{ item.caption }}</span>
      </RouterLink>
    </nav>

    <div class="sidebar-note">
      <p class="eyebrow">Project</p>
      <strong>Personal finance workspace</strong>
      <p>Track transactions, accounts, budgets, users, and tags in one place.</p>
    </div>
  </div>
</template>

<script setup lang="ts">
const navigation = [
  { to: '/', label: 'Overview', caption: 'Summary cards and hotspots' },
  { to: '/transactions', label: 'Transactions', caption: 'Filters, table, details' },
  { to: '/accounts', label: 'Accounts', caption: 'Balances, transfers, details' },
  { to: '/budgets', label: 'Budgets', caption: 'Progress and status' },
  { to: '/users', label: 'Users', caption: 'Ownership and search modes' },
  { to: '/tags', label: 'Tags', caption: 'Create, review, rename' },
];
</script>

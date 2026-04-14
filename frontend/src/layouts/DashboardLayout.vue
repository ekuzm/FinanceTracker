<template>
  <div class="app-shell">
    <img class="ambient-image" src="@/assets/ambient-fintech-bg.png" alt="" />
    <div class="ambient-scrim" />

    <aside class="shell-sidebar">
      <SidebarNav />
    </aside>

    <div class="shell-main">
      <TopBar />
      <main class="shell-content">
        <RouterView />
      </main>
    </div>

    <ConfirmDialogHost />
  </div>
</template>

<script setup lang="ts">
import ConfirmDialogHost from '@/components/ConfirmDialogHost.vue';
import SidebarNav from '@/widgets/SidebarNav.vue';
import TopBar from '@/widgets/TopBar.vue';
</script>

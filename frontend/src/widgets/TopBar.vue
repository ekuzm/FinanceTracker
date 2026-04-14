<template>
  <header class="topbar">
    <div>
      <p class="eyebrow">Finance-Tracker</p>
      <h2>{{ title }}</h2>
      <p class="topbar__description">{{ description }}</p>
    </div>

    <div v-if="lastUpdated" class="topbar__status">
      <span class="status-copy">Updated {{ formatDateTime(lastUpdated) }}</span>
    </div>
  </header>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useFinanceTracker } from '@/composables/useFinanceTracker';
import { formatDateTime } from '@/utils/format';

const route = useRoute();
const { lastUpdated } = useFinanceTracker();

const title = computed(() => String(route.meta.title ?? 'FINANCE-TRACKER'));
const description = computed(() => String(route.meta.description ?? ''));
</script>

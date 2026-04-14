<template>
  <section class="panel panel--toolbar">
    <div class="panel-header panel-header--compact">
      <div>
        <p class="eyebrow">Filters</p>
        <h3>{{ title }}</h3>
        <p v-if="description" class="panel-subtitle">{{ description }}</p>
      </div>
      <div v-if="$slots.actions" class="toolbar-actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="filters-grid">
      <slot />
    </div>
  </section>
</template>

<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string;
    description?: string;
  }>(),
  {
    description: '',
  },
);
</script>

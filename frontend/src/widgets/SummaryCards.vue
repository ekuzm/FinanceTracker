<template>
  <div class="summary-grid">
    <article v-for="card in items" :key="card.label" class="summary-card">
      <p class="summary-card__label">{{ card.label }}</p>
      <strong class="summary-card__value">{{ card.value }}</strong>
      <p class="summary-card__hint">{{ card.hint }}</p>
    </article>
  </div>
</template>

<script setup lang="ts">
defineProps<{
  items: Array<{
    label: string;
    value: string;
    hint: string;
  }>;
}>();
</script>

<template>
  <Teleport to="body">
    <div v-if="modelValue" class="overlay" @click.self="close">
      <div class="modal-shell" :style="{ maxWidth }">
        <div class="panel-header panel-header--compact">
          <div>
            <p class="eyebrow">Finance-Tracker</p>
            <h3>{{ title }}</h3>
            <p v-if="description" class="panel-subtitle">{{ description }}</p>
          </div>
          <button class="icon-button" type="button" @click="close">Close</button>
        </div>
        <div class="modal-body">
          <slot />
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, watch } from 'vue';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    title: string;
    description?: string;
    width?: string;
  }>(),
  {
    description: '',
    width: '760px',
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

const maxWidth = computed(() => props.width);

function close() {
  emit('update:modelValue', false);
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    close();
  }
}

watch(
  () => props.modelValue,
  (isOpen) => {
    if (isOpen) {
      window.addEventListener('keydown', onKeydown);
    } else {
      window.removeEventListener('keydown', onKeydown);
    }
  },
);

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onKeydown);
});
</script>

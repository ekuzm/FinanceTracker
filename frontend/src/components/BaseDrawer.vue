<template>
  <Teleport to="body">
    <div v-if="modelValue" class="overlay overlay--drawer" @click.self="close">
      <aside class="drawer-shell" :style="{ width }">
        <div class="panel-header panel-header--compact">
          <div>
            <p class="eyebrow">Details</p>
            <h3>{{ title }}</h3>
            <p v-if="description" class="panel-subtitle">{{ description }}</p>
          </div>
          <button class="icon-button" type="button" @click="close">Close</button>
        </div>
        <div class="drawer-body">
          <slot />
        </div>
      </aside>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue';

const props = withDefaults(
  defineProps<{
    modelValue: boolean;
    title: string;
    description?: string;
    width?: string;
  }>(),
  {
    description: '',
    width: '440px',
  },
);

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
}>();

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

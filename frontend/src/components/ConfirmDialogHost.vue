<template>
  <Teleport to="body">
    <div v-if="confirmState.open" class="overlay overlay--confirm" @click.self="rejectConfirm">
      <div class="modal-shell confirm-dialog-shell">
        <div class="confirm-dialog">
          <p class="eyebrow">Confirm action</p>
          <h3>{{ confirmState.title }}</h3>
          <p v-if="confirmState.description" class="panel-subtitle confirm-dialog__description">
            {{ confirmState.description }}
          </p>
        </div>

        <div class="confirm-dialog__actions">
          <button class="button button--ghost" type="button" @click="rejectConfirm">
            {{ confirmState.cancelLabel }}
          </button>
          <button
            class="button"
            :class="confirmState.tone === 'danger' ? 'button--danger' : 'button--primary'"
            type="button"
            @click="acceptConfirm"
          >
            {{ confirmState.confirmLabel }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { onBeforeUnmount, watch } from 'vue';
import { useConfirmDialog } from '@/composables/useConfirmDialog';

const { confirmState, acceptConfirm, rejectConfirm } = useConfirmDialog();

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    rejectConfirm();
  }
}

watch(
  () => confirmState.open,
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

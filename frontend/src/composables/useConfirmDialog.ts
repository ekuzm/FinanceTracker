import { reactive, readonly } from 'vue';

export type ConfirmDialogTone = 'danger' | 'primary';

export interface ConfirmDialogOptions {
  title: string;
  description?: string;
  confirmLabel?: string;
  cancelLabel?: string;
  tone?: ConfirmDialogTone;
}

const state = reactive({
  open: false,
  title: '',
  description: '',
  confirmLabel: 'Confirm',
  cancelLabel: 'Cancel',
  tone: 'danger' as ConfirmDialogTone,
});

let resolver: ((value: boolean) => void) | null = null;

function settle(value: boolean) {
  state.open = false;
  const activeResolver = resolver;
  resolver = null;
  activeResolver?.(value);
}

async function confirm(options: ConfirmDialogOptions): Promise<boolean> {
  if (resolver) {
    settle(false);
  }

  state.title = options.title;
  state.description = options.description ?? '';
  state.confirmLabel = options.confirmLabel ?? 'Confirm';
  state.cancelLabel = options.cancelLabel ?? 'Cancel';
  state.tone = options.tone ?? 'danger';
  state.open = true;

  return new Promise<boolean>((resolve) => {
    resolver = resolve;
  });
}

export function useConfirmDialog() {
  return {
    confirm,
    confirmState: readonly(state),
    acceptConfirm: () => settle(true),
    rejectConfirm: () => settle(false),
  };
}

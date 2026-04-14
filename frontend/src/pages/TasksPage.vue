<template>
  <section class="page-stack">
    <FiltersToolbar
      title="Tasks and diagnostics"
      description="Async transaction jobs are polled live, while the race-condition demo stays one click away."
    >
      <label class="input-shell input-shell--wide">
        <span>Search</span>
        <input v-model="filters.search" type="search" placeholder="Task ID or result" />
      </label>

      <label class="input-shell">
        <span>Status</span>
        <select v-model="filters.status">
          <option value="">All statuses</option>
          <option value="PENDING">Pending</option>
          <option value="IN_PROGRESS">In progress</option>
          <option value="COMPLETED">Completed</option>
          <option value="FAILED">Failed</option>
        </select>
      </label>

      <template #actions>
        <button class="button button--ghost" type="button" @click="refreshTasks">Refresh</button>
        <button class="button button--primary" type="button" @click="asyncModalOpen = true">New async import</button>
      </template>
    </FiltersToolbar>

    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Async transaction tasks</p>
            <h3>Queue state</h3>
          </div>
        </div>

        <div class="task-list">
          <article v-for="task in filteredTasks" :key="task.taskId" class="task-item">
            <div>
              <strong>{{ task.taskId }}</strong>
              <p>{{ task.result || 'Awaiting result message.' }}</p>
              <span class="muted-copy">{{ formatDateTime(task.startTime) }}</span>
            </div>
            <div class="task-item__meta">
              <span class="status-pill">{{ formatEnumLabel(task.status) }}</span>
              <strong>{{ task.progress ?? 0 }}%</strong>
            </div>
          </article>
          <p v-if="filteredTasks.length === 0" class="empty-copy">
            No async tasks match the current filters.
          </p>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Race condition</p>
            <h3>Concurrency demo</h3>
            <p class="panel-subtitle">Run the backend demo and compare unsafe vs atomic counters.</p>
          </div>
          <button class="button button--primary" type="button" @click="handleRunRaceDemo">
            Run demo
          </button>
        </div>

        <div v-if="raceDemo" class="race-grid">
          <article class="summary-card">
            <p class="summary-card__label">Expected value</p>
            <strong class="summary-card__value">{{ raceDemo.expectedValue }}</strong>
            <p class="summary-card__hint">{{ raceDemo.threadCount }} threads x {{ raceDemo.incrementsPerThread }}</p>
          </article>
          <article class="summary-card">
            <p class="summary-card__label">{{ raceDemo.unsafeCounter.name }}</p>
            <strong class="summary-card__value">{{ raceDemo.unsafeCounter.actualValue }}</strong>
            <p class="summary-card__hint">Lost updates {{ raceDemo.unsafeCounter.lostUpdates }}</p>
          </article>
          <article class="summary-card">
            <p class="summary-card__label">{{ raceDemo.atomicCounter.name }}</p>
            <strong class="summary-card__value">{{ raceDemo.atomicCounter.actualValue }}</strong>
            <p class="summary-card__hint">{{ raceDemo.atomicCounter.verdict }}</p>
          </article>
        </div>

        <p v-else class="empty-copy">
          Run the demo to load the latest race-condition summary.
        </p>
      </section>
    </div>
  </section>

  <BaseModal
    v-model="asyncModalOpen"
    title="Start async transaction import"
    description="A compact starter flow that posts two transactions into the async endpoint."
  >
    <form class="form-grid" @submit.prevent="submitAsyncImport">
      <label class="input-shell">
        <span>Account</span>
        <select v-model.number="asyncForm.accountId" required>
          <option v-for="account in accounts" :key="account.id" :value="account.id">
            {{ account.name }}
          </option>
        </select>
      </label>

      <label class="input-shell">
        <span>Tag</span>
        <select v-model="asyncForm.tagId">
          <option value="">No tag</option>
          <option v-for="tag in tags" :key="tag.id" :value="String(tag.id)">
            {{ tag.name }}
          </option>
        </select>
      </label>

      <label class="input-shell">
        <span>Income amount</span>
        <input v-model.number="asyncForm.incomeAmount" type="number" min="0.01" step="0.01" required />
      </label>

      <label class="input-shell">
        <span>Expense amount</span>
        <input v-model.number="asyncForm.expenseAmount" type="number" min="0.01" step="0.01" required />
      </label>

      <label class="toggle-shell">
        <input v-model="asyncForm.transactional" type="checkbox" />
        <span>Transactional</span>
      </label>

      <div class="form-actions">
        <button class="button button--ghost" type="button" @click="asyncModalOpen = false">Cancel</button>
        <button class="button button--primary" type="submit">Launch import</button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import BaseModal from '@/components/BaseModal.vue';
import { useFinanceTracker } from '@/composables/useFinanceTracker';
import { useRouteQueryState } from '@/composables/useRouteQueryState';
import type { TransactionRequest } from '@/types/api';
import { currentDateTimeInputValue, formatDateTime, formatEnumLabel } from '@/utils/format';
import FiltersToolbar from '@/widgets/FiltersToolbar.vue';

const {
  accounts,
  tags,
  asyncTasks,
  raceDemo,
  loadAccounts,
  loadTags,
  loadAsyncTasks,
  createTransactionsAsync,
  runRaceConditionDemo,
} = useFinanceTracker();

const filters = useRouteQueryState({
  search: '',
  status: '',
});

const asyncModalOpen = ref(false);
const asyncForm = reactive({
  accountId: 0,
  tagId: '',
  incomeAmount: 1000,
  expenseAmount: 25,
  transactional: true,
});

let pollingTimer: number | null = null;

onMounted(() => {
  void Promise.all([loadAccounts(), loadTags(), loadAsyncTasks()]);
  pollingTimer = window.setInterval(() => {
    if (Object.values(asyncTasks.value).some((task) => ['PENDING', 'IN_PROGRESS'].includes(task.status))) {
      void loadAsyncTasks();
    }
  }, 5000);
});

onBeforeUnmount(() => {
  if (pollingTimer != null) {
    window.clearInterval(pollingTimer);
  }
});

const filteredTasks = computed(() => {
  const search = filters.search.trim().toLowerCase();

  return Object.values(asyncTasks.value)
    .filter((task) => {
      const matchesStatus = !filters.status || task.status === filters.status;
      const matchesSearch =
        !search ||
        task.taskId.toLowerCase().includes(search) ||
        (task.result ?? '').toLowerCase().includes(search);
      return matchesStatus && matchesSearch;
    })
    .sort((left, right) =>
      new Date(right.startTime ?? 0).getTime() - new Date(left.startTime ?? 0).getTime(),
    );
});

async function refreshTasks() {
  await loadAsyncTasks();
}

async function handleRunRaceDemo() {
  await runRaceConditionDemo();
}

async function submitAsyncImport() {
  const timestamp = currentDateTimeInputValue();
  const parsedTagId = asyncForm.tagId ? Number(asyncForm.tagId) : null;
  const tagIds = parsedTagId == null ? [] : [parsedTagId];

  const payload: TransactionRequest[] = [
    {
      occurredAt: `${timestamp}:00`,
      amount: asyncForm.incomeAmount,
      description: 'Salary async batch',
      type: 'INCOME',
      accountId: asyncForm.accountId,
      tagIds,
    },
    {
      occurredAt: `${timestamp}:00`,
      amount: asyncForm.expenseAmount,
      description: 'Lunch async batch',
      type: 'EXPENSE',
      accountId: asyncForm.accountId,
      tagIds: [],
    },
  ];

  await createTransactionsAsync(payload, asyncForm.transactional);
  asyncModalOpen.value = false;
  await loadAsyncTasks();
}
</script>

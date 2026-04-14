<template>
  <section class="page-stack">
    <FiltersToolbar
      title="Budget filters"
      description="Pagination and sorting hit the backend, while user, status, and text narrowing stay fast in the page."
    >
      <label class="input-shell input-shell--wide">
        <span>Search</span>
        <input v-model="filters.search" type="search" placeholder="Budget name" />
      </label>

      <label class="input-shell">
        <span>User</span>
        <select v-model="filters.userId">
          <option value="">All users</option>
          <option v-for="user in users" :key="user.id" :value="String(user.id)">
            {{ user.username }}
          </option>
        </select>
      </label>

      <label class="input-shell">
        <span>Status</span>
        <select v-model="filters.status">
          <option value="">All statuses</option>
          <option value="active">Active</option>
          <option value="ending soon">Ending soon</option>
          <option value="exceeded">Exceeded</option>
          <option value="completed">Completed</option>
        </select>
      </label>

      <label class="input-shell">
        <span>Page size</span>
        <select v-model="filters.size">
          <option value="3">3</option>
          <option value="6">6</option>
          <option value="9">9</option>
        </select>
      </label>

      <label class="input-shell">
        <span>Sort by</span>
        <select v-model="filters.sortBy">
          <option value="id">Id</option>
          <option value="name">Name</option>
          <option value="limitAmount">Limit</option>
          <option value="startDate">Start date</option>
          <option value="endDate">End date</option>
        </select>
      </label>

      <label class="toggle-shell">
        <input v-model="filters.ascending" true-value="true" false-value="false" type="checkbox" />
        <span>Ascending</span>
      </label>

      <template #actions>
        <button class="button button--ghost" type="button" @click="resetFilters">Reset</button>
        <button class="button button--primary" type="button" @click="openCreateModal">Add budget</button>
      </template>
    </FiltersToolbar>

    <section class="panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">Progress cards</p>
          <h3>Budgets</h3>
          <p class="panel-subtitle">
            {{ budgetsPage?.totalElements ?? 0 }} budgets total, page {{ (budgetsPage?.number ?? 0) + 1 }} of {{ budgetsPage?.totalPages ?? 1 }}
          </p>
        </div>
        <div class="toolbar-actions">
          <button class="button button--ghost" type="button" :disabled="!budgetsPage || budgetsPage.first" @click="changePage(-1)">
            Previous
          </button>
          <button class="button button--ghost" type="button" :disabled="!budgetsPage || budgetsPage.last" @click="changePage(1)">
            Next
          </button>
        </div>
      </div>

        <div class="budget-grid">
        <article
          v-for="budget in enrichedBudgets"
          :key="budget.id"
          class="budget-card clickable-card"
          @click="selectBudget(budget)"
        >
          <div class="budget-card__topline">
            <div>
              <h3>{{ budget.name }}</h3>
              <span>{{ budget.owner }}</span>
            </div>
            <span class="status-pill">{{ budget.status }}</span>
          </div>

          <div class="budget-card__metrics">
            <span>Period {{ budget.startDate }} to {{ budget.endDate }}</span>
            <span>Limit {{ formatCurrency(budget.limitAmount) }}</span>
            <span>Spent {{ formatCurrency(budget.spent) }}</span>
            <span>Remaining {{ formatCurrency(budget.remaining) }}</span>
          </div>

          <div class="progress-track">
            <div class="progress-track__fill" :style="{ width: `${budget.progress}%` }" />
          </div>
        </article>

        <p v-if="enrichedBudgets.length === 0" class="empty-copy">
          No budgets match the current filters on this page.
        </p>
      </div>
    </section>
  </section>

  <BaseDrawer
    v-model="budgetDrawerOpen"
    :title="selectedBudget?.name ?? 'Budget'"
    description="Open any budget to review owner, progress, and available actions."
  >
    <div v-if="selectedBudget" class="detail-grid">
      <div class="detail-row">
        <span>Owner</span>
        <strong>{{ selectedBudget.owner }}</strong>
      </div>
      <div class="detail-row">
        <span>Status</span>
        <strong>{{ selectedBudget.status }}</strong>
      </div>
      <div class="detail-row">
        <span>Limit</span>
        <strong>{{ formatCurrency(selectedBudget.limitAmount) }}</strong>
      </div>
      <div class="detail-row">
        <span>Spent</span>
        <strong>{{ formatCurrency(selectedBudget.spent) }}</strong>
      </div>
      <div class="detail-row">
        <span>Remaining</span>
        <strong>{{ formatCurrency(selectedBudget.remaining) }}</strong>
      </div>
      <div class="detail-row">
        <span>Progress</span>
        <strong>{{ selectedBudget.progress }}%</strong>
      </div>
      <div class="detail-row">
        <span>Period</span>
        <strong>{{ selectedBudget.startDate }} to {{ selectedBudget.endDate }}</strong>
      </div>
      <div class="detail-row detail-row--stacked">
        <span>Actions</span>
        <div class="drawer-actions">
          <button class="button button--ghost" type="button" @click="handleEditFromDrawer">
            Edit
          </button>
          <button class="button button--danger" type="button" @click="handleDeleteFromDrawer">
            Delete
          </button>
        </div>
      </div>
    </div>
  </BaseDrawer>

  <BaseModal
    v-model="budgetModalOpen"
    :title="editingBudget ? 'Edit budget' : 'Add budget'"
    description="Track spending windows, limits, and ownership."
  >
    <form class="form-grid" @submit.prevent="submitBudget">
      <label class="input-shell input-shell--wide">
        <span>Name</span>
        <input v-model="budgetForm.name" type="text" minlength="3" maxlength="50" required />
      </label>

      <label class="input-shell">
        <span>Limit</span>
        <input v-model.number="budgetForm.limitAmount" type="number" min="0.01" step="0.01" required />
      </label>

      <label class="input-shell">
        <span>User</span>
        <select v-model.number="budgetForm.userId" required>
          <option v-for="user in users" :key="user.id" :value="user.id">
            {{ user.username }}
          </option>
        </select>
      </label>

      <label class="input-shell">
        <span>Start date</span>
        <input v-model="budgetForm.startDate" type="date" required />
      </label>

      <label class="input-shell">
        <span>End date</span>
        <input v-model="budgetForm.endDate" type="date" required />
      </label>

      <div class="form-actions">
        <button class="button button--ghost" type="button" @click="budgetModalOpen = false">Cancel</button>
        <button class="button button--primary" type="submit">
          {{ editingBudget ? 'Save budget' : 'Create budget' }}
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import BaseDrawer from '@/components/BaseDrawer.vue';
import BaseModal from '@/components/BaseModal.vue';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useFinanceTracker } from '@/composables/useFinanceTracker';
import { useRouteQueryState } from '@/composables/useRouteQueryState';
import type { BudgetRequest, BudgetResponse } from '@/types/api';
import { calculateBudgetProgress, calculateBudgetSpent, calculateBudgetStatus } from '@/utils/finance';
import { formatCurrency } from '@/utils/format';
import { parseBoolean, parseInteger } from '@/utils/query';
import FiltersToolbar from '@/widgets/FiltersToolbar.vue';

const {
  users,
  accounts,
  transactions,
  budgets,
  budgetsPage,
  loadUsers,
  loadAccounts,
  loadTransactions,
  loadBudgets,
  reloadBudgets,
  createBudget,
  updateBudget,
  deleteBudget,
  reloadUsers,
} = useFinanceTracker();
const { confirm } = useConfirmDialog();

const filters = useRouteQueryState({
  search: '',
  userId: '',
  status: '',
  page: '0',
  size: '6',
  sortBy: 'id',
  ascending: 'true',
});

type EnrichedBudget = BudgetResponse & {
  owner: string;
  spent: number;
  remaining: number;
  progress: number;
  status: string;
};

const budgetModalOpen = ref(false);
const budgetDrawerOpen = ref(false);
const editingBudget = ref<BudgetResponse | null>(null);
const selectedBudget = ref<EnrichedBudget | null>(null);

const budgetForm = reactive<BudgetRequest>({
  name: '',
  limitAmount: 500,
  startDate: '',
  endDate: '',
  userId: 0,
});

onMounted(() => {
  void Promise.all([loadUsers(), loadAccounts(), loadTransactions()]);
});

watch(
  () => [filters.page, filters.size, filters.sortBy, filters.ascending] as const,
  () => {
    void loadBudgets({
      page: parseInteger(filters.page, 0),
      size: parseInteger(filters.size, 6),
      sortBy: filters.sortBy,
      ascending: parseBoolean(filters.ascending, true),
    });
  },
  { immediate: true },
);

const enrichedBudgets = computed<EnrichedBudget[]>(() => {
  const search = filters.search.trim().toLowerCase();
  const userId = filters.userId ? Number(filters.userId) : null;

  return budgets.value
    .map((budget) => {
      const spent = calculateBudgetSpent(budget, accounts.value, transactions.value);
      const status = calculateBudgetStatus(budget, spent);
      return {
        ...budget,
        owner: users.value.find((user) => user.id === budget.userId)?.username ?? `User #${budget.userId}`,
        spent,
        remaining: budget.limitAmount - spent,
        progress: calculateBudgetProgress(budget.limitAmount, spent),
        status,
      };
    })
    .filter((budget) => {
      const matchesSearch = !search || budget.name.toLowerCase().includes(search);
      const matchesUser = userId == null || budget.userId === userId;
      const matchesStatus = !filters.status || budget.status === filters.status;
      return matchesSearch && matchesUser && matchesStatus;
    });
});

function resetBudgetForm() {
  Object.assign(budgetForm, {
    name: '',
    limitAmount: 500,
    startDate: '',
    endDate: '',
    userId: users.value[0]?.id ?? 0,
  });
}

function openCreateModal() {
  editingBudget.value = null;
  resetBudgetForm();
  budgetModalOpen.value = true;
}

function openEditModal(budget: BudgetResponse) {
  editingBudget.value = budget;
  Object.assign(budgetForm, {
    name: budget.name,
    limitAmount: budget.limitAmount,
    startDate: budget.startDate,
    endDate: budget.endDate,
    userId: budget.userId,
  });
  budgetModalOpen.value = true;
}

function selectBudget(budget: EnrichedBudget) {
  selectedBudget.value = budget;
  budgetDrawerOpen.value = true;
}

async function submitBudget() {
  if (editingBudget.value) {
    await updateBudget(editingBudget.value.id, { ...budgetForm });
  } else {
    await createBudget({ ...budgetForm });
  }

  budgetModalOpen.value = false;
  await Promise.all([reloadBudgets(), reloadUsers()]);
}

async function handleDelete(budget: BudgetResponse) {
  const confirmed = await confirm({
    title: `Delete budget "${budget.name}"?`,
    description: 'This budget will be removed from the tracker and its spending window will disappear.',
    confirmLabel: 'Delete',
  });

  if (!confirmed) {
    return;
  }

  await deleteBudget(budget.id);
  if (selectedBudget.value?.id === budget.id) {
    budgetDrawerOpen.value = false;
    selectedBudget.value = null;
  }
  await Promise.all([reloadBudgets(), reloadUsers()]);
}

function handleEditFromDrawer() {
  if (!selectedBudget.value) {
    return;
  }

  budgetDrawerOpen.value = false;
  openEditModal(selectedBudget.value);
}

async function handleDeleteFromDrawer() {
  if (!selectedBudget.value) {
    return;
  }

  budgetDrawerOpen.value = false;
  await handleDelete(selectedBudget.value);
}

function changePage(direction: number) {
  const nextPage = Math.max(0, parseInteger(filters.page, 0) + direction);
  filters.page = String(nextPage);
}

function resetFilters() {
  filters.search = '';
  filters.userId = '';
  filters.status = '';
  filters.page = '0';
  filters.size = '6';
  filters.sortBy = 'id';
  filters.ascending = 'true';
}
</script>

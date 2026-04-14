<template>
  <section class="page-stack">
    <FiltersToolbar
      title="Users filters"
      description="Switch between the full user list and backend JPQL/native search contracts."
    >
      <label class="input-shell input-shell--wide">
        <span>Search</span>
        <input v-model="filters.search" type="search" placeholder="Username or email" />
      </label>

      <label class="input-shell">
        <span>Mode</span>
        <select v-model="filters.mode">
          <option value="all">All users</option>
          <option value="jpql">JPQL search</option>
          <option value="native">Native search</option>
        </select>
      </label>

      <label class="input-shell">
        <span>Account type</span>
        <select v-model="filters.accountType">
          <option value="">Any</option>
          <option value="CHECKING">Checking</option>
          <option value="SAVINGS">Savings</option>
          <option value="CREDIT">Credit</option>
          <option value="INVESTMENT">Investment</option>
          <option value="CASH">Cash</option>
        </select>
      </label>

      <label class="input-shell">
        <span>Min budget</span>
        <input v-model="filters.minBudgetLimit" type="number" min="0" step="0.01" placeholder="100" />
      </label>

      <label class="input-shell">
        <span>Max budget</span>
        <input v-model="filters.maxBudgetLimit" type="number" min="0" step="0.01" placeholder="1000" />
      </label>

      <template #actions>
        <button class="button button--ghost" type="button" @click="resetFilters">Reset</button>
        <button class="button button--primary" type="button" @click="openCreateModal">Add user</button>
      </template>
    </FiltersToolbar>

    <section v-if="needsSearchParams" class="panel panel--notice">
      <p class="empty-copy">
        {{ searchParamsNotice }}
      </p>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">Admin table</p>
          <h3>Users</h3>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Username</th>
              <th>Email</th>
              <th class="table-amount">Accounts</th>
              <th class="table-amount">Budgets</th>
            </tr>
          </thead>
          <tbody v-if="displayedUsers.length">
            <tr
              v-for="user in displayedUsers"
              :key="user.id"
              class="clickable-row"
              @click="selectUser(user)"
            >
              <td>{{ user.username }}</td>
              <td>{{ user.email || 'No email' }}</td>
              <td class="table-amount">{{ user.accountIds.length }}</td>
              <td class="table-amount">{{ user.budgetIds.length }}</td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td colspan="4" class="table-empty">No users match the current search mode.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>

  <BaseDrawer
    v-model="userDrawerOpen"
    :title="selectedUser?.username ?? 'User'"
    description="Open any user to review linked accounts, budgets, and actions."
  >
    <div v-if="selectedUser" class="detail-grid">
      <div class="detail-row">
        <span>Email</span>
        <strong>{{ selectedUser.email || 'No email' }}</strong>
      </div>
      <div class="detail-row">
        <span>Accounts</span>
        <strong>{{ selectedUser.accountIds.length }}</strong>
      </div>
      <div class="detail-row">
        <span>Budgets</span>
        <strong>{{ selectedUser.budgetIds.length }}</strong>
      </div>
      <div class="detail-row detail-row--stacked">
        <span>Linked accounts</span>
        <div class="tag-stack">
          <span v-for="accountId in selectedUser.accountIds" :key="accountId" class="tag-chip">
            {{ accountName(accountId) }}
          </span>
          <span v-if="selectedUser.accountIds.length === 0" class="muted-copy">No linked accounts</span>
        </div>
      </div>
      <div class="detail-row detail-row--stacked">
        <span>Linked budgets</span>
        <div class="tag-stack">
          <span v-for="budgetId in selectedUser.budgetIds" :key="budgetId" class="tag-chip">
            {{ budgetName(budgetId) }}
          </span>
          <span v-if="selectedUser.budgetIds.length === 0" class="muted-copy">No linked budgets</span>
        </div>
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
    v-model="userModalOpen"
    :title="editingUser ? 'Edit user' : 'Add user'"
    description="Ownership links are handled directly in the same form."
  >
    <form class="form-grid" @submit.prevent="submitUser">
      <label class="input-shell">
        <span>Username</span>
        <input v-model="userForm.username" type="text" minlength="3" maxlength="50" required />
      </label>

      <label class="input-shell">
        <span>Email</span>
        <input v-model="userForm.email" type="email" placeholder="alex@example.com" />
      </label>

      <div class="input-shell input-shell--wide">
        <span>Accounts</span>
        <div class="checkbox-grid">
          <label
            v-for="account in accounts"
            :key="account.id"
            class="checkbox-pill"
            :class="{ 'checkbox-pill--active': userForm.accountIds.includes(account.id) }"
          >
            <input
              :checked="userForm.accountIds.includes(account.id)"
              type="checkbox"
              @change="toggleUserAccount(account.id)"
            />
            <span>{{ account.name }}</span>
          </label>
        </div>
      </div>

      <div class="input-shell input-shell--wide">
        <span>Budgets</span>
        <div class="checkbox-grid">
          <label
            v-for="budget in budgetOptions"
            :key="budget.id"
            class="checkbox-pill"
            :class="{ 'checkbox-pill--active': userForm.budgetIds.includes(budget.id) }"
          >
            <input
              :checked="userForm.budgetIds.includes(budget.id)"
              type="checkbox"
              @change="toggleUserBudget(budget.id)"
            />
            <span>{{ budget.name }}</span>
          </label>
        </div>
      </div>

      <div class="form-actions">
        <button class="button button--ghost" type="button" @click="userModalOpen = false">Cancel</button>
        <button class="button button--primary" type="submit">
          {{ editingUser ? 'Save user' : 'Create user' }}
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
import type { AccountType, BudgetResponse, UserRequest, UserResponse } from '@/types/api';
import FiltersToolbar from '@/widgets/FiltersToolbar.vue';

const budgetOptionsQuery = {
  page: 0,
  size: 1000,
  sortBy: 'id',
  ascending: true,
};

const {
  users,
  accounts,
  budgets,
  loadUsers,
  loadAccounts,
  loadBudgets,
  reloadUsers,
  createUser,
  updateUser,
  deleteUser,
} = useFinanceTracker();
const { confirm } = useConfirmDialog();

const filters = useRouteQueryState({
  search: '',
  mode: 'all',
  accountType: '',
  minBudgetLimit: '',
  maxBudgetLimit: '',
});

const userModalOpen = ref(false);
const userDrawerOpen = ref(false);
const editingUser = ref<UserResponse | null>(null);
const selectedUser = ref<UserResponse | null>(null);

const userForm = reactive<UserRequest>({
  username: '',
  email: '',
  accountIds: [],
  budgetIds: [],
});

onMounted(() => {
  void Promise.all([loadAccounts(), loadBudgets(budgetOptionsQuery)]);
});

const parsedMinBudgetLimit = computed<number | null>(() => {
  if (filters.minBudgetLimit === '') {
    return null;
  }

  const value = Number(filters.minBudgetLimit);
  return Number.isNaN(value) ? null : value;
});

const parsedMaxBudgetLimit = computed<number | null>(() => {
  if (filters.maxBudgetLimit === '') {
    return null;
  }

  const value = Number(filters.maxBudgetLimit);
  return Number.isNaN(value) ? null : value;
});

const budgetRangeInvalid = computed(
  () =>
    parsedMinBudgetLimit.value != null &&
    parsedMaxBudgetLimit.value != null &&
    parsedMinBudgetLimit.value > parsedMaxBudgetLimit.value,
);

const needsSearchParams = computed(
  () =>
    filters.mode !== 'all' &&
    (!filters.accountType || parsedMinBudgetLimit.value == null || parsedMaxBudgetLimit.value == null || budgetRangeInvalid.value),
);

const searchParamsNotice = computed(() => {
  if (budgetRangeInvalid.value) {
    return 'Min budget cannot be greater than max budget.';
  }

  if (!filters.accountType) {
    return 'Choose account type to run JPQL or native search.';
  }

  return 'Fill both budget limits to run JPQL or native search.';
});

const budgetOptions = computed<BudgetResponse[]>(() => budgets.value);

const budgetsByUserId = computed<Record<number, BudgetResponse[]>>(() =>
  budgetOptions.value.reduce<Record<number, BudgetResponse[]>>((accumulator, budget) => {
    if (!accumulator[budget.userId]) {
      accumulator[budget.userId] = [];
    }

    accumulator[budget.userId].push(budget);
    return accumulator;
  }, {}),
);

watch(
  () => [filters.mode, filters.accountType, filters.minBudgetLimit, filters.maxBudgetLimit] as const,
  ([mode], previousValue) => {
    if (mode === 'all') {
      if (previousValue?.[0] === 'all') {
        return;
      }

      void loadUsers({ mode: 'all' });
      return;
    }

    if (!needsSearchParams.value) {
      void loadUsers({
        mode: filters.mode as 'jpql' | 'native',
        accountType: filters.accountType as AccountType,
        minBudgetLimit: filters.minBudgetLimit,
        maxBudgetLimit: filters.maxBudgetLimit,
      });
    }
  },
  { immediate: true },
);

const displayedUsers = computed(() => {
  if (filters.mode !== 'all' && needsSearchParams.value) {
    return [];
  }

  if (budgetRangeInvalid.value) {
    return [];
  }

  const search = filters.search.trim().toLowerCase();
  return users.value.filter((user) => {
    const userBudgets = budgetsByUserId.value[user.id] ?? [];
    const matchesBudgetRange =
      (parsedMinBudgetLimit.value == null && parsedMaxBudgetLimit.value == null) ||
      userBudgets.some((budget) => {
        if (parsedMinBudgetLimit.value != null && budget.limitAmount < parsedMinBudgetLimit.value) {
          return false;
        }

        if (parsedMaxBudgetLimit.value != null && budget.limitAmount > parsedMaxBudgetLimit.value) {
          return false;
        }

        return true;
      });

    if (!search) {
      return matchesBudgetRange;
    }

    return (
      matchesBudgetRange &&
      (
        user.username.toLowerCase().includes(search) ||
        (user.email ?? '').toLowerCase().includes(search)
      )
    );
  });
});

function accountName(accountId: number): string {
  return accounts.value.find((account) => account.id === accountId)?.name ?? `Account #${accountId}`;
}

function budgetName(budgetId: number): string {
  return budgetOptions.value.find((budget) => budget.id === budgetId)?.name ?? `Budget #${budgetId}`;
}

function resetUserForm() {
  Object.assign(userForm, {
    username: '',
    email: '',
    accountIds: [],
    budgetIds: [],
  });
}

function openCreateModal() {
  editingUser.value = null;
  resetUserForm();
  userModalOpen.value = true;
}

function openEditModal(user: UserResponse) {
  editingUser.value = user;
  Object.assign(userForm, {
    username: user.username,
    email: user.email ?? '',
    accountIds: [...user.accountIds],
    budgetIds: [...user.budgetIds],
  });
  userModalOpen.value = true;
}

function selectUser(user: UserResponse) {
  selectedUser.value = user;
  userDrawerOpen.value = true;
}

function toggleUserAccount(accountId: number) {
  userForm.accountIds = userForm.accountIds.includes(accountId)
    ? userForm.accountIds.filter((currentId) => currentId !== accountId)
    : [...userForm.accountIds, accountId];
}

function toggleUserBudget(budgetId: number) {
  userForm.budgetIds = userForm.budgetIds.includes(budgetId)
    ? userForm.budgetIds.filter((currentId) => currentId !== budgetId)
    : [...userForm.budgetIds, budgetId];
}

async function submitUser() {
  const payload = {
    ...userForm,
    email: userForm.email || null,
  };

  if (editingUser.value) {
    await updateUser(editingUser.value.id, payload);
  } else {
    await createUser(payload);
  }

  userModalOpen.value = false;
  await Promise.all([reloadUsers(), loadAccounts(), loadBudgets(budgetOptionsQuery)]);
}

async function handleDelete(user: UserResponse) {
  const confirmed = await confirm({
    title: `Delete user "${user.username}"?`,
    description: 'This user will be removed from the workspace together with current ownership links.',
    confirmLabel: 'Delete',
  });

  if (!confirmed) {
    return;
  }

  await deleteUser(user.id);
  if (selectedUser.value?.id === user.id) {
    userDrawerOpen.value = false;
    selectedUser.value = null;
  }
  await Promise.all([reloadUsers(), loadAccounts(), loadBudgets(budgetOptionsQuery)]);
}

function handleEditFromDrawer() {
  if (!selectedUser.value) {
    return;
  }

  userDrawerOpen.value = false;
  openEditModal(selectedUser.value);
}

async function handleDeleteFromDrawer() {
  if (!selectedUser.value) {
    return;
  }

  userDrawerOpen.value = false;
  await handleDelete(selectedUser.value);
}

function resetFilters() {
  filters.search = '';
  filters.mode = 'all';
  filters.accountType = '';
  filters.minBudgetLimit = '';
  filters.maxBudgetLimit = '';
}
</script>

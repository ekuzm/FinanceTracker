<template>
  <section class="page-stack">
    <FiltersToolbar
      title="Accounts workspace"
      description="Overview cards stay visible while the management table keeps CRUD and transfers close."
    >
      <label class="input-shell input-shell--wide">
        <span>Search</span>
        <input v-model="filters.search" type="search" placeholder="Name or owner" />
      </label>

      <label class="input-shell">
        <span>Type</span>
        <select v-model="filters.type">
          <option value="">All types</option>
          <option value="CHECKING">Checking</option>
          <option value="SAVINGS">Savings</option>
          <option value="CREDIT">Credit</option>
          <option value="INVESTMENT">Investment</option>
          <option value="CASH">Cash</option>
        </select>
      </label>

      <label class="input-shell">
        <span>Owner</span>
        <select v-model="filters.ownerId">
          <option value="">All owners</option>
          <option v-for="user in users" :key="user.id" :value="String(user.id)">
            {{ user.username }}
          </option>
        </select>
      </label>

      <template #actions>
        <button class="button button--ghost" type="button" @click="resetFilters">Reset</button>
        <button class="button button--primary" type="button" @click="openCreateModal">Add account</button>
      </template>
    </FiltersToolbar>

    <section class="account-cards">
      <article
        v-for="account in filteredAccounts"
        :key="account.id"
        class="account-card clickable-card"
        @click="selectAccount(account)"
      >
        <div class="account-card__header">
          <div>
            <p class="eyebrow">{{ formatEnumLabel(account.type) }}</p>
            <h3>{{ account.name }}</h3>
          </div>
          <span class="status-pill">#{{ account.id }}</span>
        </div>

        <div class="account-card__balance">{{ formatCurrency(account.balance) }}</div>
        <div class="account-card__meta">
          <span>Owner {{ ownerName(account.userId) }}</span>
          <span>ID #{{ account.id }}</span>
        </div>
      </article>
    </section>

    <section class="panel">
      <div class="panel-header">
        <div>
          <p class="eyebrow">Management</p>
          <h3>Accounts table</h3>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Type</th>
              <th class="table-amount">Balance</th>
              <th>Owner</th>
            </tr>
          </thead>
          <tbody v-if="filteredAccounts.length">
            <tr
              v-for="account in filteredAccounts"
              :key="account.id"
              class="clickable-row"
              @click="selectAccount(account)"
            >
              <td>{{ account.name }}</td>
              <td>{{ formatEnumLabel(account.type) }}</td>
              <td class="table-amount">{{ formatCurrency(account.balance) }}</td>
              <td>{{ ownerName(account.userId) }}</td>
            </tr>
          </tbody>
          <tbody v-else>
            <tr>
              <td colspan="4" class="table-empty">No accounts match the active filters.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>
  </section>

  <BaseModal
    v-model="accountModalOpen"
    :title="editingAccount ? 'Edit account' : 'Add account'"
    description="Create new balance containers or update owner assignments."
  >
    <form class="form-grid" @submit.prevent="submitAccount">
      <label class="input-shell input-shell--wide">
        <span>Name</span>
        <input v-model="accountForm.name" type="text" minlength="3" maxlength="50" required />
      </label>

      <label class="input-shell">
        <span>Type</span>
        <select v-model="accountForm.type" required>
          <option value="CHECKING">Checking</option>
          <option value="SAVINGS">Savings</option>
          <option value="CREDIT">Credit</option>
          <option value="INVESTMENT">Investment</option>
          <option value="CASH">Cash</option>
        </select>
      </label>

      <label class="input-shell">
        <span>Balance</span>
        <input v-model.number="accountForm.balance" type="number" min="0" step="0.01" required />
      </label>

      <label class="input-shell input-shell--wide">
        <span>Owner</span>
        <select v-model.number="accountForm.userId" required>
          <option v-for="user in users" :key="user.id" :value="user.id">
            {{ user.username }}
          </option>
        </select>
      </label>

      <div class="form-actions">
        <button class="button button--ghost" type="button" @click="accountModalOpen = false">Cancel</button>
        <button class="button button--primary" type="submit">
          {{ editingAccount ? 'Save account' : 'Create account' }}
        </button>
      </div>
    </form>
  </BaseModal>

  <BaseDrawer
    v-model="accountDrawerOpen"
    :title="selectedAccount?.name ?? 'Account'"
    description="Open any account to review ownership, balance, and available actions."
  >
    <div v-if="selectedAccount" class="detail-grid">
      <div class="detail-row">
        <span>Type</span>
        <strong>{{ formatEnumLabel(selectedAccount.type) }}</strong>
      </div>
      <div class="detail-row">
        <span>Balance</span>
        <strong>{{ formatCurrency(selectedAccount.balance) }}</strong>
      </div>
      <div class="detail-row">
        <span>Owner</span>
        <strong>{{ ownerName(selectedAccount.userId) }}</strong>
      </div>
      <div class="detail-row">
        <span>Account ID</span>
        <strong>#{{ selectedAccount.id }}</strong>
      </div>
      <div class="detail-row detail-row--stacked">
        <span>Actions</span>
        <div class="drawer-actions">
          <button class="button button--ghost" type="button" @click="handleEditFromDrawer">
            Edit
          </button>
          <button class="button button--ghost" type="button" @click="handleTransferFromDrawer">
            Transfer
          </button>
          <button class="button button--danger" type="button" @click="handleDeleteFromDrawer">
            Delete
          </button>
        </div>
      </div>
    </div>
  </BaseDrawer>

  <BaseModal
    v-model="transferModalOpen"
    title="Transfer funds"
    description="Transfers create two mirrored transactions and respect transactional flags from the backend."
  >
    <form class="form-grid" @submit.prevent="submitTransfer">
      <label class="input-shell">
        <span>From account</span>
        <select v-model.number="transferForm.fromAccountId" required>
          <option v-for="account in accounts" :key="account.id" :value="account.id">
            {{ account.name }}
          </option>
        </select>
      </label>

      <label class="input-shell">
        <span>To account</span>
        <select v-model.number="transferForm.toAccountId" required>
          <option v-for="account in transferTargets" :key="account.id" :value="account.id">
            {{ account.name }}
          </option>
        </select>
      </label>

      <label class="input-shell">
        <span>Amount</span>
        <input v-model.number="transferForm.amount" type="number" min="0.01" step="0.01" required />
      </label>

      <label class="input-shell">
        <span>Occurred at</span>
        <input v-model="transferForm.occurredAt" type="datetime-local" />
      </label>

      <label class="input-shell input-shell--wide">
        <span>Note</span>
        <input v-model="transferForm.note" type="text" maxlength="255" placeholder="Card to savings" />
      </label>

      <label class="toggle-shell">
        <input v-model="transferTransactional" type="checkbox" />
        <span>Transactional</span>
      </label>

      <label class="toggle-shell">
        <input v-model="transferFailAfterDebit" type="checkbox" />
        <span>Fail after debit</span>
      </label>

      <div class="form-actions">
        <button class="button button--ghost" type="button" @click="transferModalOpen = false">Cancel</button>
        <button class="button button--primary" type="submit">Execute transfer</button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import BaseDrawer from '@/components/BaseDrawer.vue';
import BaseModal from '@/components/BaseModal.vue';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useFinanceTracker } from '@/composables/useFinanceTracker';
import { useRouteQueryState } from '@/composables/useRouteQueryState';
import type { AccountRequest, AccountResponse, AccountTransferRequest } from '@/types/api';
import { currentDateTimeInputValue, formatCurrency, formatEnumLabel } from '@/utils/format';
import FiltersToolbar from '@/widgets/FiltersToolbar.vue';

const {
  users,
  accounts,
  loadUsers,
  reloadUsers,
  loadAccounts,
  createAccount,
  updateAccount,
  deleteAccount,
  createTransfer,
  reloadTransactions,
} = useFinanceTracker();
const { confirm } = useConfirmDialog();

const filters = useRouteQueryState({
  search: '',
  type: '',
  ownerId: '',
});

const accountModalOpen = ref(false);
const accountDrawerOpen = ref(false);
const transferModalOpen = ref(false);
const editingAccount = ref<AccountResponse | null>(null);
const selectedAccount = ref<AccountResponse | null>(null);

const accountForm = reactive<AccountRequest>({
  name: '',
  type: 'CHECKING',
  balance: 0,
  userId: 0,
});

const transferForm = reactive({
  fromAccountId: 0,
  toAccountId: 0,
  amount: 150,
  occurredAt: currentDateTimeInputValue(),
  note: '',
});

const transferTransactional = ref(true);
const transferFailAfterDebit = ref(false);

onMounted(() => {
  void Promise.all([loadUsers(), loadAccounts()]);
});

const filteredAccounts = computed(() => {
  const search = filters.search.trim().toLowerCase();
  const ownerId = filters.ownerId ? Number(filters.ownerId) : null;

  return accounts.value.filter((account) => {
    const owner = ownerName(account.userId).toLowerCase();
    const matchesSearch =
      !search ||
      account.name.toLowerCase().includes(search) ||
      owner.includes(search);
    const matchesType = !filters.type || account.type === filters.type;
    const matchesOwner = ownerId == null || account.userId === ownerId;

    return matchesSearch && matchesType && matchesOwner;
  });
});

const transferTargets = computed(() => {
  const source = accounts.value.find((account) => account.id === transferForm.fromAccountId);
  if (!source) {
    return accounts.value;
  }

  return accounts.value.filter(
    (account) => account.id !== source.id && account.userId === source.userId,
  );
});

function ownerName(userId: number): string {
  return users.value.find((user) => user.id === userId)?.username ?? `User #${userId}`;
}

function resetAccountForm() {
  Object.assign(accountForm, {
    name: '',
    type: 'CHECKING',
    balance: 0,
    userId: users.value[0]?.id ?? 0,
  });
}

function openCreateModal() {
  editingAccount.value = null;
  resetAccountForm();
  accountModalOpen.value = true;
}

function openEditModal(account: AccountResponse) {
  editingAccount.value = account;
  Object.assign(accountForm, {
    name: account.name,
    type: account.type,
    balance: account.balance,
    userId: account.userId,
  });
  accountModalOpen.value = true;
}

function openTransferModal(account: AccountResponse) {
  transferForm.fromAccountId = account.id;
  transferForm.toAccountId =
    accounts.value.find((candidate) => candidate.id !== account.id && candidate.userId === account.userId)?.id ?? 0;
  transferForm.amount = 150;
  transferForm.occurredAt = currentDateTimeInputValue();
  transferForm.note = '';
  transferTransactional.value = true;
  transferFailAfterDebit.value = false;
  transferModalOpen.value = true;
}

function selectAccount(account: AccountResponse) {
  selectedAccount.value = account;
  accountDrawerOpen.value = true;
}

async function submitAccount() {
  if (editingAccount.value) {
    await updateAccount(editingAccount.value.id, { ...accountForm });
  } else {
    await createAccount({ ...accountForm });
  }

  accountModalOpen.value = false;
  await Promise.all([loadAccounts(), reloadUsers()]);
}

async function submitTransfer() {
  const payload: AccountTransferRequest = {
    fromAccountId: transferForm.fromAccountId,
    toAccountId: transferForm.toAccountId,
    amount: transferForm.amount,
    occurredAt: transferForm.occurredAt ? `${transferForm.occurredAt}:00` : null,
    note: transferForm.note || null,
  };

  await createTransfer(payload, transferTransactional.value, transferFailAfterDebit.value);
  transferModalOpen.value = false;
  await Promise.all([loadAccounts(), reloadTransactions()]);
}

async function handleDelete(account: AccountResponse) {
  const confirmed = await confirm({
    title: `Delete account "${account.name}"?`,
    description: 'This account will be removed and can no longer be used in transfers or transactions.',
    confirmLabel: 'Delete',
  });

  if (!confirmed) {
    return;
  }

  await deleteAccount(account.id);
  if (selectedAccount.value?.id === account.id) {
    accountDrawerOpen.value = false;
    selectedAccount.value = null;
  }
  await Promise.all([loadAccounts(), reloadUsers(), reloadTransactions()]);
}

function handleEditFromDrawer() {
  if (!selectedAccount.value) {
    return;
  }

  accountDrawerOpen.value = false;
  openEditModal(selectedAccount.value);
}

function handleTransferFromDrawer() {
  if (!selectedAccount.value) {
    return;
  }

  accountDrawerOpen.value = false;
  openTransferModal(selectedAccount.value);
}

async function handleDeleteFromDrawer() {
  if (!selectedAccount.value) {
    return;
  }

  accountDrawerOpen.value = false;
  await handleDelete(selectedAccount.value);
}

function resetFilters() {
  filters.search = '';
  filters.type = '';
  filters.ownerId = '';
}
</script>

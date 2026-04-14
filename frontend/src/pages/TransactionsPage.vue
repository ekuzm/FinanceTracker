<template>
  <section class="page-stack">
    <FiltersToolbar
      title="Ledger controls"
      description="Date range is pushed to the API, while account, tag, type, and search stay instant in the client."
    >
      <label class="input-shell">
        <span>Start date</span>
        <input v-model="filters.startDate" type="date" />
      </label>

      <label class="input-shell">
        <span>End date</span>
        <input v-model="filters.endDate" type="date" />
      </label>

      <label class="input-shell">
        <span>Type</span>
        <select v-model="filters.type">
          <option value="">All</option>
          <option value="INCOME">Income</option>
          <option value="EXPENSE">Expense</option>
        </select>
      </label>

      <label class="input-shell">
        <span>Account</span>
        <select v-model="filters.accountId">
          <option value="">All accounts</option>
          <option v-for="account in accounts" :key="account.id" :value="String(account.id)">
            {{ account.name }}
          </option>
        </select>
      </label>

      <label class="input-shell">
        <span>Tag</span>
        <select v-model="filters.tagId">
          <option value="">All tags</option>
          <option v-for="tag in tags" :key="tag.id" :value="String(tag.id)">
            {{ tag.name }}
          </option>
        </select>
      </label>

      <label class="input-shell input-shell--wide">
        <span>Search</span>
        <input v-model="filters.search" type="search" placeholder="Description or ID" />
      </label>

      <template #actions>
        <button class="button button--ghost" type="button" @click="resetFilters">Reset</button>
        <button class="button button--ghost" type="button" @click="refreshTransactions">Refresh</button>
      </template>
    </FiltersToolbar>

    <TransactionsTableBlock
      title="Transactions"
      description="Tap any row to open the side drawer with full context."
      :transactions="filteredTransactions"
      :accounts="accounts"
      :tags="tags"
      :users="users"
      @add="openCreateModal"
      @edit="openEditModal"
      @delete="handleDelete"
    />
  </section>

  <BaseModal
    v-model="transactionModalOpen"
    :title="editingTransaction ? 'Edit transaction' : 'Add transaction'"
    description="The same form handles both create and edit flows."
  >
    <form class="form-grid" @submit.prevent="submitTransaction">
      <label class="input-shell">
        <span>Date and time</span>
        <input v-model="transactionForm.occurredAt" type="datetime-local" required />
      </label>

      <label class="input-shell">
        <span>Amount</span>
        <input v-model.number="transactionForm.amount" type="number" min="0.01" step="0.01" required />
      </label>

      <label class="input-shell input-shell--wide">
        <span>Description</span>
        <input v-model="transactionForm.description" type="text" minlength="3" maxlength="255" required />
      </label>

      <label class="input-shell">
        <span>Type</span>
        <select v-model="transactionForm.type" required>
          <option value="EXPENSE">Expense</option>
          <option value="INCOME">Income</option>
        </select>
      </label>

      <label class="input-shell">
        <span>Account</span>
        <select v-model.number="transactionForm.accountId" required>
          <option v-for="account in accounts" :key="account.id" :value="account.id">
            {{ account.name }}
          </option>
        </select>
      </label>

      <div class="input-shell input-shell--wide">
        <span>Tags</span>
        <p class="input-shell__hint">Click one or more tags to select them.</p>
        <div class="checkbox-grid">
          <label
            v-for="tag in tags"
            :key="tag.id"
            class="checkbox-pill"
            :class="{ 'checkbox-pill--active': transactionForm.tagIds.includes(tag.id) }"
          >
            <input
              :checked="transactionForm.tagIds.includes(tag.id)"
              type="checkbox"
              @change="toggleTransactionTag(tag.id)"
            />
            <span>{{ tag.name }}</span>
          </label>
        </div>
      </div>

      <div class="form-actions">
        <button class="button button--ghost" type="button" @click="transactionModalOpen = false">Cancel</button>
        <button class="button button--primary" type="submit">
          {{ editingTransaction ? 'Save changes' : 'Create transaction' }}
        </button>
      </div>
    </form>
  </BaseModal>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import BaseModal from '@/components/BaseModal.vue';
import { useConfirmDialog } from '@/composables/useConfirmDialog';
import { useFinanceTracker } from '@/composables/useFinanceTracker';
import { useRouteQueryState } from '@/composables/useRouteQueryState';
import type { TransactionRequest, TransactionResponse } from '@/types/api';
import { currentDateTimeInputValue, startOfMonthInputValue, todayInputValue } from '@/utils/format';
import FiltersToolbar from '@/widgets/FiltersToolbar.vue';
import TransactionsTableBlock from '@/widgets/TransactionsTableBlock.vue';

const {
  accounts,
  users,
  tags,
  transactions,
  loadAccounts,
  loadUsers,
  loadTags,
  loadTransactions,
  reloadTransactions,
  createTransaction,
  updateTransaction,
  deleteTransaction,
  loadBudgets,
} = useFinanceTracker();
const { confirm } = useConfirmDialog();

const filters = useRouteQueryState({
  startDate: startOfMonthInputValue(),
  endDate: todayInputValue(),
  type: '',
  accountId: '',
  tagId: '',
  search: '',
});

const transactionModalOpen = ref(false);
const editingTransaction = ref<TransactionResponse | null>(null);

const transactionForm = reactive<TransactionRequest>({
  occurredAt: currentDateTimeInputValue(),
  amount: 25,
  description: '',
  type: 'EXPENSE',
  accountId: 0,
  tagIds: [],
});

onMounted(() => {
  void Promise.all([loadAccounts(), loadUsers(), loadTags()]);
});

watch(
  () => [filters.startDate, filters.endDate] as const,
  () => {
    void loadTransactions({
      startDate: filters.startDate,
      endDate: filters.endDate,
    });
  },
  { immediate: true },
);

const filteredTransactions = computed(() => {
  const search = filters.search.trim().toLowerCase();
  const accountId = filters.accountId ? Number(filters.accountId) : null;
  const tagId = filters.tagId ? Number(filters.tagId) : null;

  return transactions.value.filter((transaction) => {
    const matchesType = !filters.type || transaction.type === filters.type;
    const matchesAccount = accountId == null || transaction.accountId === accountId;
    const matchesTag = tagId == null || transaction.tagIds.includes(tagId);
    const matchesSearch =
      !search ||
      transaction.description.toLowerCase().includes(search) ||
      String(transaction.id).includes(search);

    return matchesType && matchesAccount && matchesTag && matchesSearch;
  });
});

function resetTransactionForm() {
  Object.assign(transactionForm, {
    occurredAt: currentDateTimeInputValue(),
    amount: 25,
    description: '',
    type: 'EXPENSE',
    accountId: accounts.value[0]?.id ?? 0,
    tagIds: [],
  });
}

function openCreateModal() {
  editingTransaction.value = null;
  resetTransactionForm();
  transactionModalOpen.value = true;
}

function openEditModal(transaction: TransactionResponse) {
  editingTransaction.value = transaction;
  Object.assign(transactionForm, {
    occurredAt: transaction.occurredAt.slice(0, 16),
    amount: transaction.amount,
    description: transaction.description,
    type: transaction.type,
    accountId: transaction.accountId,
    tagIds: [...transaction.tagIds],
  });
  transactionModalOpen.value = true;
}

function toggleTransactionTag(tagId: number) {
  if (transactionForm.tagIds.includes(tagId)) {
    transactionForm.tagIds = transactionForm.tagIds.filter((currentTagId) => currentTagId !== tagId);
    return;
  }

  transactionForm.tagIds = [...transactionForm.tagIds, tagId];
}

async function refreshTransactions() {
  await reloadTransactions();
}

async function submitTransaction() {
  const payload: TransactionRequest = {
    ...transactionForm,
    occurredAt:
      transactionForm.occurredAt.length === 16
        ? `${transactionForm.occurredAt}:00`
        : transactionForm.occurredAt,
  };

  if (editingTransaction.value) {
    await updateTransaction(editingTransaction.value.id, payload);
  } else {
    await createTransaction(payload);
  }

  transactionModalOpen.value = false;
  await Promise.all([reloadTransactions(), loadAccounts(), loadBudgets()]);
}

async function handleDelete(transaction: TransactionResponse) {
  const confirmed = await confirm({
    title: `Delete transaction "${transaction.description}"?`,
    description: 'This transaction will be removed from the ledger and related totals.',
    confirmLabel: 'Delete',
  });

  if (!confirmed) {
    return;
  }

  await deleteTransaction(transaction.id);
  await Promise.all([reloadTransactions(), loadAccounts(), loadBudgets()]);
}

function resetFilters() {
  filters.startDate = startOfMonthInputValue();
  filters.endDate = todayInputValue();
  filters.type = '';
  filters.accountId = '';
  filters.tagId = '';
  filters.search = '';
}
</script>

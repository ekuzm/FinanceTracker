<template>
  <section class="panel">
    <div class="panel-header">
      <div>
        <p class="eyebrow">Transactions</p>
        <h3>{{ title }}</h3>
        <p v-if="description" class="panel-subtitle">{{ description }}</p>
      </div>

      <div v-if="enableEditing" class="toolbar-actions">
        <button class="button button--ghost" type="button" :disabled="!selectedTransaction" @click="editSelected">
          Edit transaction
        </button>
        <button class="button button--primary" type="button" @click="$emit('add')">
          Add transaction
        </button>
      </div>
    </div>

    <div class="table-wrap">
      <table class="data-table transactions-table">
        <thead>
          <tr>
            <th>Date</th>
            <th>Description</th>
            <th>Type</th>
            <th class="table-amount">Amount</th>
            <th>Account</th>
            <th>Tags</th>
          </tr>
        </thead>
        <tbody v-if="sortedTransactions.length">
          <tr
            v-for="transaction in sortedTransactions"
            :key="transaction.id"
            class="clickable-row"
            @click="selectTransaction(transaction)"
          >
            <td>{{ formatDateTime(transaction.occurredAt) }}</td>
            <td>
              <div class="table-primary">
                <strong>{{ transaction.description }}</strong>
                <span>#{{ transaction.id }}</span>
              </div>
            </td>
            <td>
              <span class="status-pill" :class="transaction.type === 'INCOME' ? 'status-pill--positive' : 'status-pill--negative'">
                {{ formatEnumLabel(transaction.type) }}
              </span>
            </td>
            <td
              class="table-amount"
              :class="transaction.type === 'INCOME' ? 'metric-positive' : 'metric-negative'"
            >
              {{ transaction.type === 'INCOME' ? '+' : '-' }}{{ formatCurrency(transaction.amount) }}
            </td>
            <td>{{ accountName(transaction.accountId) }}</td>
            <td>
              <div class="tag-stack">
                <span
                  v-for="tagId in transaction.tagIds"
                  :key="tagId"
                  class="tag-chip"
                >
                  {{ tagName(tagId) }}
                </span>
                <span v-if="transaction.tagIds.length === 0" class="muted-copy">No tags</span>
              </div>
            </td>
          </tr>
        </tbody>
        <tbody v-else>
          <tr>
            <td colspan="6" class="table-empty">{{ emptyText }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>

  <BaseDrawer
    v-model="drawerOpen"
    :title="selectedTransaction?.description ?? 'Transaction'"
    description="Open the row to inspect linked account, tags, and timing."
  >
    <div v-if="selectedTransaction" class="detail-grid">
      <div class="detail-row">
        <span>Date</span>
        <strong>{{ formatDateTime(selectedTransaction.occurredAt) }}</strong>
      </div>
      <div class="detail-row">
        <span>Type</span>
        <strong>{{ formatEnumLabel(selectedTransaction.type) }}</strong>
      </div>
      <div class="detail-row">
        <span>Amount</span>
        <strong>{{ formatCurrency(selectedTransaction.amount) }}</strong>
      </div>
      <div class="detail-row">
        <span>Account</span>
        <strong>{{ accountName(selectedTransaction.accountId) }}</strong>
      </div>
      <div class="detail-row">
        <span>Owner</span>
        <strong>{{ accountOwner(selectedTransaction.accountId) }}</strong>
      </div>
      <div class="detail-row detail-row--stacked">
        <span>Tags</span>
        <div class="tag-stack">
          <span
            v-for="tagId in selectedTransaction.tagIds"
            :key="tagId"
            class="tag-chip"
          >
            {{ tagName(tagId) }}
          </span>
          <span v-if="selectedTransaction.tagIds.length === 0" class="muted-copy">No tags attached</span>
        </div>
      </div>
      <div class="detail-row detail-row--stacked">
        <span>Actions</span>
        <div class="drawer-actions">
          <button class="button button--ghost" type="button" @click="handleEdit(selectedTransaction)">
            Edit
          </button>
          <button class="button button--danger" type="button" @click="handleDelete(selectedTransaction)">
            Delete
          </button>
        </div>
      </div>
    </div>
  </BaseDrawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import BaseDrawer from '@/components/BaseDrawer.vue';
import type { AccountResponse, TagResponse, TransactionResponse, UserResponse } from '@/types/api';
import { formatCurrency, formatDateTime, formatEnumLabel } from '@/utils/format';

const props = withDefaults(
  defineProps<{
    title: string;
    description?: string;
    transactions: TransactionResponse[];
    accounts: AccountResponse[];
    tags: TagResponse[];
    users: UserResponse[];
    emptyText?: string;
    enableEditing?: boolean;
  }>(),
  {
    description: '',
    emptyText: 'No transactions for the current filter set.',
    enableEditing: true,
  },
);

const emit = defineEmits<{
  add: [];
  edit: [transaction: TransactionResponse];
  delete: [transaction: TransactionResponse];
}>();

const drawerOpen = ref(false);
const selectedTransaction = ref<TransactionResponse | null>(null);

const accountsById = computed<Record<number, AccountResponse>>(() =>
  Object.fromEntries(props.accounts.map((account) => [account.id, account])),
);

const usersById = computed<Record<number, UserResponse>>(() =>
  Object.fromEntries(props.users.map((user) => [user.id, user])),
);

const tagsById = computed<Record<number, TagResponse>>(() =>
  Object.fromEntries(props.tags.map((tag) => [tag.id, tag])),
);

const sortedTransactions = computed(() =>
  [...props.transactions].sort((left, right) =>
    new Date(right.occurredAt).getTime() - new Date(left.occurredAt).getTime(),
  ),
);

function accountName(accountId: number): string {
  return accountsById.value[accountId]?.name ?? `Account #${accountId}`;
}

function accountOwner(accountId: number): string {
  const account = accountsById.value[accountId];
  if (!account) {
    return 'Unknown owner';
  }

  return usersById.value[account.userId]?.username ?? `User #${account.userId}`;
}

function tagName(tagId: number): string {
  return tagsById.value[tagId]?.name ?? `Tag #${tagId}`;
}

function selectTransaction(transaction: TransactionResponse) {
  selectedTransaction.value = transaction;
  drawerOpen.value = true;
}

function editSelected() {
  if (!selectedTransaction.value) {
    return;
  }

  handleEdit(selectedTransaction.value);
}

function handleEdit(transaction: TransactionResponse) {
  drawerOpen.value = false;
  emit('edit', transaction);
}

function handleDelete(transaction: TransactionResponse) {
  drawerOpen.value = false;
  emit('delete', transaction);
}
</script>

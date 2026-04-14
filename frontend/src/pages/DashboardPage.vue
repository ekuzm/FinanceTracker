<template>
  <section class="page-stack">
    <SummaryCards :items="summaryItems" />

    <div class="dashboard-grid">
      <section class="panel panel--feature">
        <div class="panel-header">
          <div>
            <p class="eyebrow">Workspace pulse</p>
            <h3>Today at a glance</h3>
            <p class="panel-subtitle">
              Net position, spend tempo, and ownership stay in one surface.
            </p>
          </div>
          <div class="toolbar-actions">
            <RouterLink class="button button--primary" to="/transactions">Open ledger</RouterLink>
          </div>
        </div>

        <div class="feature-band">
          <div>
            <span class="feature-band__label">This month</span>
            <strong class="feature-band__value">{{ formatCurrency(monthlyNet) }}</strong>
          </div>
          <div>
            <span class="feature-band__label">Income</span>
            <strong>{{ formatCurrency(monthlyIncome) }}</strong>
          </div>
          <div>
            <span class="feature-band__label">Expense</span>
            <strong>{{ formatCurrency(monthlyExpense) }}</strong>
          </div>
          <div>
            <span class="feature-band__label">Accounts</span>
            <strong>{{ accounts.length }}</strong>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-header panel-header--compact">
          <div>
            <p class="eyebrow">Budgets</p>
            <h3>Closest to the line</h3>
          </div>
          <RouterLink class="button button--ghost" to="/budgets">See all budgets</RouterLink>
        </div>

        <div class="budget-stack">
          <article
            v-for="budget in budgetHighlights"
            :key="budget.id"
            class="budget-card budget-card--compact"
          >
            <div class="budget-card__topline">
              <div>
                <strong>{{ budget.name }}</strong>
                <span>{{ budget.owner }}</span>
              </div>
              <span class="status-pill">{{ budget.status }}</span>
            </div>
            <div class="budget-card__metrics">
              <span>Spent {{ formatCurrency(budget.spent) }}</span>
              <span>Remaining {{ formatCurrency(budget.remaining) }}</span>
            </div>
            <div class="progress-track">
              <div class="progress-track__fill" :style="{ width: `${budget.progress}%` }" />
            </div>
          </article>

          <p v-if="budgetHighlights.length === 0" class="empty-copy">
            Load budgets to see live pressure by period.
          </p>
        </div>
      </section>
    </div>

    <TransactionsTableBlock
      title="Recent ledger activity"
      description="Latest movements across all accounts."
      :transactions="recentTransactions"
      :accounts="accounts"
      :tags="tags"
      :users="users"
      :enable-editing="false"
      empty-text="No transaction activity yet."
      @add="goToTransactions"
      @edit="goToTransactions"
      @delete="goToTransactions"
    />

    <section class="panel">
      <div class="panel-header panel-header--compact">
        <div>
          <p class="eyebrow">Ownership</p>
          <h3>User coverage</h3>
        </div>
        <RouterLink class="button button--ghost" to="/users">Open users</RouterLink>
      </div>

      <div class="owner-list">
        <article v-for="user in userHighlights" :key="user.id" class="owner-card">
          <div>
            <strong>{{ user.username }}</strong>
            <span>{{ user.email || 'No email set' }}</span>
          </div>
          <div class="owner-card__meta">
            <span>{{ user.accountIds.length }} accounts</span>
            <span>{{ user.budgetIds.length }} budgets</span>
          </div>
        </article>
      </div>
    </section>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useFinanceTracker } from '@/composables/useFinanceTracker';
import SummaryCards from '@/widgets/SummaryCards.vue';
import TransactionsTableBlock from '@/widgets/TransactionsTableBlock.vue';
import { calculateBudgetProgress, calculateBudgetSpent, calculateBudgetStatus } from '@/utils/finance';
import {
  formatCompactCurrency,
  formatCurrency,
  formatPercent,
} from '@/utils/format';

const router = useRouter();
const {
  users,
  accounts,
  budgets,
  tags,
  transactions,
  loadOverview,
} = useFinanceTracker();

onMounted(() => {
  void loadOverview();
});

const currentMonth = new Date();
const monthStart = computed(() => new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1));
const nextMonthStart = computed(() => new Date(currentMonth.getFullYear(), currentMonth.getMonth() + 1, 1));

const monthlyTransactions = computed(() =>
  transactions.value.filter((transaction) => {
    const occurredAt = new Date(transaction.occurredAt);
    return occurredAt >= monthStart.value && occurredAt < nextMonthStart.value;
  }),
);

const monthlyIncome = computed(() =>
  monthlyTransactions.value
    .filter((transaction) => transaction.type === 'INCOME')
    .reduce((sum, transaction) => sum + transaction.amount, 0),
);

const monthlyExpense = computed(() =>
  monthlyTransactions.value
    .filter((transaction) => transaction.type === 'EXPENSE')
    .reduce((sum, transaction) => sum + transaction.amount, 0),
);

const monthlyNet = computed(() => monthlyIncome.value - monthlyExpense.value);
const netWorth = computed(() => accounts.value.reduce((sum, account) => sum + account.balance, 0));
const savingsRate = computed(() =>
  monthlyIncome.value > 0 ? monthlyNet.value / monthlyIncome.value : 0,
);

const summaryItems = computed(() => [
  {
    label: 'Net worth',
    value: formatCompactCurrency(netWorth.value),
    hint: `${accounts.value.length} funded accounts in play`,
  },
  {
    label: 'Income',
    value: formatCompactCurrency(monthlyIncome.value),
    hint: `Expense offset ${formatCompactCurrency(monthlyExpense.value)}`,
  },
  {
    label: 'Savings rate',
    value: formatPercent(savingsRate.value),
    hint: `Monthly net ${formatCompactCurrency(monthlyNet.value)}`,
  },
  {
    label: 'Active budgets',
    value: String(
      budgets.value.filter((budget) => calculateBudgetStatus(
        budget,
        calculateBudgetSpent(budget, accounts.value, transactions.value),
      ) !== 'completed').length,
    ),
    hint: `${users.value.length} owners with tracked budget windows`,
  },
]);

const recentTransactions = computed(() =>
  [...transactions.value]
    .sort((left, right) => new Date(right.occurredAt).getTime() - new Date(left.occurredAt).getTime())
    .slice(0, 6),
);

const budgetHighlights = computed(() =>
  budgets.value
    .map((budget) => {
      const spent = calculateBudgetSpent(budget, accounts.value, transactions.value);
      const progress = calculateBudgetProgress(budget.limitAmount, spent);
      const owner = users.value.find((user) => user.id === budget.userId)?.username ?? `User #${budget.userId}`;
      return {
        id: budget.id,
        name: budget.name,
        owner,
        spent,
        remaining: budget.limitAmount - spent,
        progress,
        status: calculateBudgetStatus(budget, spent),
      };
    })
    .sort((left, right) => right.progress - left.progress)
    .slice(0, 3),
);

const userHighlights = computed(() =>
  [...users.value]
    .sort((left, right) => right.accountIds.length - left.accountIds.length)
    .slice(0, 4),
);

function goToTransactions() {
  void router.push('/transactions');
}
</script>

import { computed, reactive, ref } from 'vue';
import { financeApi, getErrorMessage } from '@/services/api';
import type {
  AccountRequest,
  AccountResponse,
  AccountTransferRequest,
  AccountUpdateRequest,
  AsyncTaskMap,
  BudgetListQuery,
  BudgetPageResponse,
  BudgetRequest,
  BudgetResponse,
  BudgetUpdateRequest,
  FlashMessage,
  RaceConditionDemoResponse,
  TagRequest,
  TagResponse,
  TagUpdateRequest,
  TransactionListQuery,
  TransactionRequest,
  TransactionResponse,
  TransactionUpdateRequest,
  UserListQuery,
  UserRequest,
  UserResponse,
  UserUpdateRequest,
} from '@/types/api';

const BASE_URL_STORAGE_KEY = 'finance-tracker-base-url';

const baseUrl = ref(typeof window === 'undefined' ? '' : window.localStorage.getItem(BASE_URL_STORAGE_KEY) ?? '');
const flash = ref<FlashMessage | null>(null);
const lastUpdated = ref<string | null>(null);

const loading = reactive({
  users: false,
  accounts: false,
  budgets: false,
  tags: false,
  transactions: false,
  tasks: false,
  raceDemo: false,
  mutation: false,
});

const users = ref<UserResponse[]>([]);
const accounts = ref<AccountResponse[]>([]);
const budgetsPage = ref<BudgetPageResponse | null>(null);
const tags = ref<TagResponse[]>([]);
const transactions = ref<TransactionResponse[]>([]);
const asyncTasks = ref<AsyncTaskMap>({});
const raceDemo = ref<RaceConditionDemoResponse | null>(null);

const lastBudgetQuery = ref<BudgetListQuery>({
  page: 0,
  size: 6,
  sortBy: 'id',
  ascending: true,
});

const lastTransactionQuery = ref<TransactionListQuery>({
});

const lastUserQuery = ref<UserListQuery>({
  mode: 'all',
  accountType: '',
  minBudgetLimit: '',
  maxBudgetLimit: '',
});

function setFlash(tone: FlashMessage['tone'], text: string) {
  flash.value = { tone, text };
}

function markUpdated() {
  lastUpdated.value = new Date().toISOString();
}

function clearFlash() {
  flash.value = null;
}

function setBaseUrl(value: string) {
  const next = value.trim().replace(/\/$/, '');
  baseUrl.value = next;
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(BASE_URL_STORAGE_KEY, next);
  }
}

async function runLoader<T>(
  key: keyof typeof loading,
  operation: () => Promise<T>,
  onSuccess: (result: T) => void,
) {
  loading[key] = true;
  try {
    const result = await operation();
    onSuccess(result);
    markUpdated();
  } catch (error) {
    setFlash('error', getErrorMessage(error));
    throw error;
  } finally {
    loading[key] = false;
  }
}

async function runMutation<T>(
  successMessage: string,
  operation: () => Promise<T>,
): Promise<T> {
  loading.mutation = true;
  try {
    const result = await operation();
    setFlash('success', successMessage);
    markUpdated();
    return result;
  } catch (error) {
    setFlash('error', getErrorMessage(error));
    throw error;
  } finally {
    loading.mutation = false;
  }
}

async function loadUsers(query: UserListQuery = lastUserQuery.value) {
  lastUserQuery.value = { ...query };
  return runLoader(
    'users',
    () =>
      query.mode === 'all'
        ? financeApi.getUsers(baseUrl.value)
        : financeApi.searchUsers(baseUrl.value, query),
    (result) => {
      users.value = result;
    },
  );
}

async function reloadUsers() {
  return loadUsers(lastUserQuery.value);
}

async function loadAccounts() {
  return runLoader(
    'accounts',
    () => financeApi.getAccounts(baseUrl.value),
    (result) => {
      accounts.value = result;
    },
  );
}

async function loadBudgets(query: BudgetListQuery = lastBudgetQuery.value) {
  lastBudgetQuery.value = { ...query };
  return runLoader(
    'budgets',
    () => financeApi.getBudgets(baseUrl.value, query),
    (result) => {
      budgetsPage.value = result;
    },
  );
}

async function reloadBudgets() {
  return loadBudgets(lastBudgetQuery.value);
}

async function loadTags() {
  return runLoader(
    'tags',
    () => financeApi.getTags(baseUrl.value),
    (result) => {
      tags.value = result;
    },
  );
}

async function loadTransactions(query: TransactionListQuery = lastTransactionQuery.value) {
  lastTransactionQuery.value = { ...query };
  return runLoader(
    'transactions',
    () => financeApi.getTransactions(baseUrl.value, query),
    (result) => {
      transactions.value = result;
    },
  );
}

async function reloadTransactions() {
  return loadTransactions(lastTransactionQuery.value);
}

async function loadAsyncTasks() {
  return runLoader(
    'tasks',
    () => financeApi.getAsyncTasks(baseUrl.value),
    (result) => {
      asyncTasks.value = result;
    },
  );
}

async function loadAsyncTask(taskId: string) {
  return runLoader(
    'tasks',
    () => financeApi.getAsyncTask(baseUrl.value, taskId),
    (result) => {
      asyncTasks.value = {
        ...asyncTasks.value,
        [result.taskId]: result,
      };
    },
  );
}

async function runRaceConditionDemo() {
  return runLoader(
    'raceDemo',
    () => financeApi.runRaceConditionDemo(baseUrl.value),
    (result) => {
      raceDemo.value = result;
    },
  );
}

function loadOverview() {
  return Promise.all([
    loadUsers(),
    loadAccounts(),
    loadBudgets(),
    loadTags(),
    loadTransactions(),
  ]);
}

function createUser(payload: UserRequest) {
  return runMutation('User created', () => financeApi.createUser(baseUrl.value, payload));
}

function updateUser(userId: number, payload: UserUpdateRequest) {
  return runMutation('User updated', () => financeApi.updateUser(baseUrl.value, userId, payload));
}

function deleteUser(userId: number) {
  return runMutation('User deleted', () => financeApi.deleteUser(baseUrl.value, userId));
}

function createAccount(payload: AccountRequest) {
  return runMutation('Account created', () => financeApi.createAccount(baseUrl.value, payload));
}

function updateAccount(accountId: number, payload: AccountUpdateRequest) {
  return runMutation('Account updated', () => financeApi.updateAccount(baseUrl.value, accountId, payload));
}

function deleteAccount(accountId: number) {
  return runMutation('Account deleted', () => financeApi.deleteAccount(baseUrl.value, accountId));
}

function createTransfer(
  payload: AccountTransferRequest,
  transactional: boolean,
  failAfterDebit: boolean,
) {
  return runMutation('Transfer executed', () =>
    financeApi.createTransfer(baseUrl.value, payload, transactional, failAfterDebit),
  );
}

function createBudget(payload: BudgetRequest) {
  return runMutation('Budget created', () => financeApi.createBudget(baseUrl.value, payload));
}

function updateBudget(budgetId: number, payload: BudgetUpdateRequest) {
  return runMutation('Budget updated', () => financeApi.updateBudget(baseUrl.value, budgetId, payload));
}

function deleteBudget(budgetId: number) {
  return runMutation('Budget deleted', () => financeApi.deleteBudget(baseUrl.value, budgetId));
}

function createTag(payload: TagRequest) {
  return runMutation('Tag created', () => financeApi.createTag(baseUrl.value, payload));
}

function updateTag(tagId: number, payload: TagUpdateRequest) {
  return runMutation('Tag updated', () => financeApi.updateTag(baseUrl.value, tagId, payload));
}

function deleteTag(tagId: number) {
  return runMutation('Tag deleted', () => financeApi.deleteTag(baseUrl.value, tagId));
}

function createTransaction(payload: TransactionRequest) {
  return runMutation('Transaction created', () => financeApi.createTransaction(baseUrl.value, payload));
}

function updateTransaction(transactionId: number, payload: TransactionUpdateRequest) {
  return runMutation('Transaction updated', () =>
    financeApi.updateTransaction(baseUrl.value, transactionId, payload),
  );
}

function deleteTransaction(transactionId: number) {
  return runMutation('Transaction deleted', () => financeApi.deleteTransaction(baseUrl.value, transactionId));
}

function createTransactionsAsync(payload: TransactionRequest[], transactional: boolean) {
  return runMutation('Async import started', () =>
    financeApi.createTransactionsAsync(baseUrl.value, payload, transactional),
  );
}

const budgets = computed(() => budgetsPage.value?.content ?? []);
const isBusy = computed(() => Object.values(loading).some(Boolean));

export function useFinanceTracker() {
  return {
    baseUrl,
    flash,
    lastUpdated,
    loading,
    isBusy,
    users,
    accounts,
    budgetsPage,
    budgets,
    tags,
    transactions,
    asyncTasks,
    raceDemo,
    clearFlash,
    setBaseUrl,
    loadOverview,
    loadUsers,
    reloadUsers,
    loadAccounts,
    loadBudgets,
    reloadBudgets,
    loadTags,
    loadTransactions,
    reloadTransactions,
    loadAsyncTasks,
    loadAsyncTask,
    runRaceConditionDemo,
    createUser,
    updateUser,
    deleteUser,
    createAccount,
    updateAccount,
    deleteAccount,
    createTransfer,
    createBudget,
    updateBudget,
    deleteBudget,
    createTag,
    updateTag,
    deleteTag,
    createTransaction,
    updateTransaction,
    deleteTransaction,
    createTransactionsAsync,
  };
}

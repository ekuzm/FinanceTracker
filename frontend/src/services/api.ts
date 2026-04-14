import type {
  AccountRequest,
  AccountResponse,
  AccountTransferRequest,
  AccountUpdateRequest,
  AsyncTask,
  AsyncTaskMap,
  BudgetListQuery,
  BudgetPageResponse,
  BudgetRequest,
  BudgetResponse,
  BudgetUpdateRequest,
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

export class ApiError extends Error {
  status: number;
  payload: unknown;

  constructor(message: string, status: number, payload: unknown) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.payload = payload;
  }
}

function normalizeBaseUrl(baseUrl: string): string {
  return baseUrl.trim().replace(/\/$/, '');
}

function buildUrl(baseUrl: string, path: string): string {
  const normalized = normalizeBaseUrl(baseUrl);
  return normalized ? `${normalized}${path}` : path;
}

function buildQuery(params: Record<string, string | number | boolean | undefined>): string {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === '') {
      return;
    }

    searchParams.set(key, String(value));
  });

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
}

async function request<T>(
  baseUrl: string,
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const headers = new Headers(init.headers);

  if (!headers.has('Accept')) {
    headers.set('Accept', 'application/json');
  }

  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(buildUrl(baseUrl, path), {
    ...init,
    headers,
  });

  if (response.status === 204) {
    return undefined as T;
  }

  const contentType = response.headers.get('Content-Type') ?? '';
  const payload = contentType.includes('application/json')
    ? await response.json()
    : await response.text();

  if (!response.ok) {
    const message =
      typeof payload === 'object' && payload !== null && 'message' in payload
        ? String(payload.message)
        : response.statusText || 'Request failed';
    throw new ApiError(message, response.status, payload);
  }

  return payload as T;
}

export function getErrorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const payload = error.payload as { message?: string; errors?: Record<string, string> } | undefined;
    if (payload?.errors) {
      const details = Object.entries(payload.errors)
        .map(([field, issue]) => `${field}: ${issue}`)
        .join(', ');
      return `${payload.message ?? error.message}. ${details}`;
    }
    return payload?.message ?? error.message;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return 'Unexpected API error';
}

export const financeApi = {
  getUsers(baseUrl: string) {
    return request<UserResponse[]>(baseUrl, '/api/v1/users');
  },

  searchUsers(baseUrl: string, query: UserListQuery) {
    const path =
      query.mode === 'native'
        ? '/api/v1/users/search/account-type/native'
        : '/api/v1/users/search/account-type/jpql';

    return request<UserResponse[]>(
      baseUrl,
      `${path}${buildQuery({
        accountType: query.accountType,
        minBudgetLimit: query.minBudgetLimit,
        maxBudgetLimit: query.maxBudgetLimit,
      })}`,
    );
  },

  createUser(baseUrl: string, payload: UserRequest) {
    return request<UserResponse>(baseUrl, '/api/v1/users', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  updateUser(baseUrl: string, userId: number, payload: UserUpdateRequest) {
    return request<UserResponse>(baseUrl, `/api/v1/users/${userId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  deleteUser(baseUrl: string, userId: number) {
    return request<void>(baseUrl, `/api/v1/users/${userId}`, {
      method: 'DELETE',
    });
  },

  getAccounts(baseUrl: string) {
    return request<AccountResponse[]>(baseUrl, '/api/v1/accounts');
  },

  createAccount(baseUrl: string, payload: AccountRequest) {
    return request<AccountResponse>(baseUrl, '/api/v1/accounts', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  updateAccount(baseUrl: string, accountId: number, payload: AccountUpdateRequest) {
    return request<AccountResponse>(baseUrl, `/api/v1/accounts/${accountId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  deleteAccount(baseUrl: string, accountId: number) {
    return request<void>(baseUrl, `/api/v1/accounts/${accountId}`, {
      method: 'DELETE',
    });
  },

  createTransfer(
    baseUrl: string,
    payload: AccountTransferRequest,
    transactional: boolean,
    failAfterDebit: boolean,
  ) {
    return request<void>(
      baseUrl,
      `/api/v1/account/transfer${buildQuery({ transactional, failAfterDebit })}`,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
    );
  },

  getBudgets(baseUrl: string, query: BudgetListQuery) {
    return request<BudgetPageResponse>(
      baseUrl,
      `/api/v1/budgets${buildQuery({
        page: query.page,
        size: query.size,
        sortBy: query.sortBy,
        ascending: query.ascending,
      })}`,
    );
  },

  createBudget(baseUrl: string, payload: BudgetRequest) {
    return request<BudgetResponse>(baseUrl, '/api/v1/budgets', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  updateBudget(baseUrl: string, budgetId: number, payload: BudgetUpdateRequest) {
    return request<BudgetResponse>(baseUrl, `/api/v1/budgets/${budgetId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  deleteBudget(baseUrl: string, budgetId: number) {
    return request<void>(baseUrl, `/api/v1/budgets/${budgetId}`, {
      method: 'DELETE',
    });
  },

  getTags(baseUrl: string) {
    return request<TagResponse[]>(baseUrl, '/api/v1/tags');
  },

  createTag(baseUrl: string, payload: TagRequest) {
    return request<TagResponse>(baseUrl, '/api/v1/tags', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  updateTag(baseUrl: string, tagId: number, payload: TagUpdateRequest) {
    return request<TagResponse>(baseUrl, `/api/v1/tags/${tagId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  deleteTag(baseUrl: string, tagId: number) {
    return request<void>(baseUrl, `/api/v1/tags/${tagId}`, {
      method: 'DELETE',
    });
  },

  getTransactions(baseUrl: string, query: TransactionListQuery) {
    return request<TransactionResponse[]>(
      baseUrl,
      `/api/v1/transactions${buildQuery({
        startDate: query.startDate,
        endDate: query.endDate,
      })}`,
    );
  },

  createTransaction(baseUrl: string, payload: TransactionRequest) {
    return request<TransactionResponse>(baseUrl, '/api/v1/transactions', {
      method: 'POST',
      body: JSON.stringify(payload),
    });
  },

  updateTransaction(baseUrl: string, transactionId: number, payload: TransactionUpdateRequest) {
    return request<TransactionResponse>(baseUrl, `/api/v1/transactions/${transactionId}`, {
      method: 'PATCH',
      body: JSON.stringify(payload),
    });
  },

  deleteTransaction(baseUrl: string, transactionId: number) {
    return request<void>(baseUrl, `/api/v1/transactions/${transactionId}`, {
      method: 'DELETE',
    });
  },

  createTransactionsAsync(
    baseUrl: string,
    payload: TransactionRequest[],
    transactional: boolean,
  ) {
    return request<{ taskId: string }>(
      baseUrl,
      `/api/v1/transactions/async${buildQuery({ transactional })}`,
      {
        method: 'POST',
        body: JSON.stringify(payload),
      },
    );
  },

  getAsyncTask(baseUrl: string, taskId: string) {
    return request<AsyncTask>(baseUrl, `/api/v1/transactions/async/status/${taskId}`);
  },

  getAsyncTasks(baseUrl: string) {
    return request<AsyncTaskMap>(baseUrl, '/api/v1/transactions/async/tasks');
  },

  runRaceConditionDemo(baseUrl: string) {
    return request<RaceConditionDemoResponse>(baseUrl, '/api/v1/demo/race-condition/run');
  },
};

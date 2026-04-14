export type AccountType = 'CHECKING' | 'SAVINGS' | 'CREDIT' | 'INVESTMENT' | 'CASH';
export type TransactionType = 'INCOME' | 'EXPENSE';
export type AsyncTaskStatus = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'FAILED';
export type UserSearchMode = 'all' | 'jpql' | 'native';

export interface UserResponse {
  id: number;
  username: string;
  email: string | null;
  accountIds: number[];
  budgetIds: number[];
}

export interface AccountResponse {
  id: number;
  name: string;
  type: AccountType;
  balance: number;
  userId: number;
}

export interface BudgetResponse {
  id: number;
  name: string;
  limitAmount: number;
  startDate: string;
  endDate: string;
  userId: number;
}

export interface BudgetPageResponse {
  content: BudgetResponse[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface TagResponse {
  id: number;
  name: string;
}

export interface TransactionResponse {
  id: number;
  occurredAt: string;
  amount: number;
  description: string;
  type: TransactionType;
  accountId: number;
  tagIds: number[];
}

export interface AsyncTask {
  taskId: string;
  status: AsyncTaskStatus;
  startTime: string | null;
  endTime: string | null;
  progress: number | null;
  result: string | null;
}

export interface AsyncTaskMap {
  [taskId: string]: AsyncTask;
}

export interface RaceCounterResult {
  name: string;
  actualValue: number;
  lostUpdates: number;
  matchesExpected: boolean;
  verdict: string;
}

export interface RaceConditionDemoResponse {
  threadCount: number;
  incrementsPerThread: number;
  expectedValue: number;
  unsafeCounter: RaceCounterResult;
  atomicCounter: RaceCounterResult;
}

export interface UserRequest {
  username: string;
  email: string | null;
  accountIds: number[];
  budgetIds: number[];
}

export interface UserUpdateRequest extends UserRequest {}

export interface AccountRequest {
  name: string;
  type: AccountType;
  balance: number;
  userId: number;
}

export interface AccountUpdateRequest extends AccountRequest {}

export interface BudgetRequest {
  name: string;
  limitAmount: number;
  startDate: string;
  endDate: string;
  userId: number;
}

export interface BudgetUpdateRequest extends BudgetRequest {}

export interface TagRequest {
  name: string;
}

export interface TagUpdateRequest extends TagRequest {}

export interface TransactionRequest {
  occurredAt: string;
  amount: number;
  description: string;
  type: TransactionType;
  accountId: number;
  tagIds: number[];
}

export interface TransactionUpdateRequest extends TransactionRequest {}

export interface AccountTransferRequest {
  fromAccountId: number;
  toAccountId: number;
  amount: number;
  occurredAt: string | null;
  note: string | null;
}

export interface TransactionListQuery {
  startDate?: string;
  endDate?: string;
}

export interface BudgetListQuery {
  page: number;
  size: number;
  sortBy: string;
  ascending: boolean;
}

export interface UserListQuery {
  mode: UserSearchMode;
  accountType?: AccountType | '';
  minBudgetLimit?: string;
  maxBudgetLimit?: string;
}

export interface FlashMessage {
  tone: 'success' | 'error';
  text: string;
}

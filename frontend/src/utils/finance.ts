import type { AccountResponse, BudgetResponse, TransactionResponse } from '@/types/api';
import { clampPercentage, diffInDays, todayInputValue } from '@/utils/format';

export function buildLookup<T extends { id: number }>(items: T[]): Record<number, T> {
  return Object.fromEntries(items.map((item) => [item.id, item])) as Record<number, T>;
}

export function calculateBudgetSpent(
  budget: BudgetResponse,
  accounts: AccountResponse[],
  transactions: TransactionResponse[],
): number {
  const accountIds = new Set(
    accounts
      .filter((account) => account.userId === budget.userId)
      .map((account) => account.id),
  );

  return transactions
    .filter((transaction) => {
      const transactionDate = transaction.occurredAt.slice(0, 10);
      return (
        transaction.type === 'EXPENSE' &&
        accountIds.has(transaction.accountId) &&
        transactionDate >= budget.startDate &&
        transactionDate <= budget.endDate
      );
    })
    .reduce((sum, transaction) => sum + transaction.amount, 0);
}

export function calculateBudgetProgress(limitAmount: number, spent: number): number {
  if (limitAmount <= 0) {
    return 0;
  }

  return clampPercentage((spent / limitAmount) * 100);
}

export function calculateBudgetStatus(
  budget: BudgetResponse,
  spent: number,
): 'active' | 'ending soon' | 'exceeded' | 'completed' {
  if (spent > budget.limitAmount) {
    return 'exceeded';
  }

  const daysLeft = diffInDays(todayInputValue(), budget.endDate);
  if (daysLeft < 0) {
    return 'completed';
  }

  if (daysLeft <= 3) {
    return 'ending soon';
  }

  return 'active';
}

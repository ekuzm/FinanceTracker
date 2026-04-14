import type { LocationQuery, LocationQueryRaw, LocationQueryValue, LocationQueryValueRaw } from 'vue-router';

function normalizeQueryValue(
  value: LocationQueryValue | LocationQueryValue[] | LocationQueryValueRaw | LocationQueryValueRaw[] | undefined,
): string | undefined {
  const candidate = Array.isArray(value) ? value[0] : value;
  if (candidate == null || candidate === '') {
    return undefined;
  }

  return String(candidate);
}

export function cleanQuery(input: Record<string, string | undefined>): Record<string, string> {
  const result: Record<string, string> = {};

  Object.entries(input).forEach(([key, value]) => {
    const normalized = normalizeQueryValue(value);
    if (normalized !== undefined) {
      result[key] = normalized;
    }
  });

  return result;
}

export function sameQuery(
  left: LocationQuery,
  right: LocationQueryRaw,
): boolean {
  const normalizedLeft: Record<string, string> = {};
  const normalizedRight: Record<string, string> = {};

  Object.entries(left).forEach(([key, value]) => {
    const normalized = normalizeQueryValue(value);
    if (normalized !== undefined) {
      normalizedLeft[key] = normalized;
    }
  });

  Object.entries(right).forEach(([key, value]) => {
    const normalized = normalizeQueryValue(value);
    if (normalized !== undefined) {
      normalizedRight[key] = normalized;
    }
  });

  return JSON.stringify(normalizedLeft) === JSON.stringify(normalizedRight);
}

export function parseInteger(value: string | undefined, fallback: number): number {
  if (!value) {
    return fallback;
  }

  const parsed = Number.parseInt(value, 10);
  return Number.isNaN(parsed) ? fallback : parsed;
}

export function parseBoolean(value: string | undefined, fallback = false): boolean {
  if (value == null || value === '') {
    return fallback;
  }

  return value === 'true';
}

import { reactive, watch } from 'vue';
import { useRoute, useRouter, type LocationQuery } from 'vue-router';
import { cleanQuery, sameQuery } from '@/utils/query';

export function useRouteQueryState<T extends Record<string, string>>(defaults: T) {
  const route = useRoute();
  const router = useRouter();
  const state = reactive({ ...defaults }) as T;

  const syncFromRoute = (query: LocationQuery) => {
    Object.keys(defaults).forEach((key) => {
      const rawValue = query[key];
      const nextValue = Array.isArray(rawValue) ? rawValue[0] : rawValue;
      state[key as keyof T] = (typeof nextValue === 'string' ? nextValue : defaults[key as keyof T]) as T[keyof T];
    });
  };

  watch(
    () => route.query,
    (query) => {
      syncFromRoute(query);
    },
    { immediate: true },
  );

  watch(
    state,
    async (value) => {
      const nextQuery = cleanQuery(value);
      if (sameQuery(route.query, nextQuery)) {
        return;
      }

      await router.replace({
        query: nextQuery,
      });
    },
    { deep: true },
  );

  return state;
}

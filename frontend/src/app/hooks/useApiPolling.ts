import { useCallback, useEffect, useState } from "react";

interface UseApiPollingOptions<T> {
  intervalMs?: number;
  initialData: T;
}

export function useApiPolling<T>(
  fetcher: () => Promise<T>,
  options: UseApiPollingOptions<T>,
) {
  const { intervalMs = 15000, initialData } = options;
  const [data, setData] = useState<T>(initialData);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async (isManualRefresh = false) => {
    if (isManualRefresh) {
      setRefreshing(true);
    }

    try {
      const result = await fetcher();
      setData(result);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao carregar dados.");
    } finally {
      setLoading(false);
      if (isManualRefresh) {
        setRefreshing(false);
      }
    }
  }, [fetcher]);

  useEffect(() => {
    let mounted = true;
    const guardedLoad = async (isManualRefresh = false) => {
      await load(isManualRefresh);
      if (!mounted) {
        return;
      }
    };

    guardedLoad();
    const interval = window.setInterval(() => {
      guardedLoad();
    }, intervalMs);

    return () => {
      mounted = false;
      window.clearInterval(interval);
    };
  }, [intervalMs, load]);

  const refresh = useCallback(() => load(true), [load]);

  return { data, loading, refreshing, error, refresh };
}

import { useEffect, useState } from "react";

export function useAutoRefresh(intervalMs: number = 5000) {
  const [lastRefresh, setLastRefresh] = useState(new Date());

  useEffect(() => {
    const interval = setInterval(() => {
      setLastRefresh(new Date());
    }, intervalMs);

    return () => clearInterval(interval);
  }, [intervalMs]);

  return {
    lastRefresh,
    refresh: () => setLastRefresh(new Date()),
  };
}

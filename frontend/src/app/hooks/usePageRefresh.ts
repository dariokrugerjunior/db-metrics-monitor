import { useEffect } from "react";
import { useOutletContext } from "react-router";

export interface PageRefreshContextValue {
  setRefreshAction: (action: (() => void) | null) => void;
  refreshing: boolean;
}

export function usePageRefresh(action?: () => void) {
  const context = useOutletContext<PageRefreshContextValue>();

  useEffect(() => {
    if (!action) {
      return;
    }

    context.setRefreshAction(() => action);

    return () => {
      context.setRefreshAction(null);
    };
  }, [action, context]);

  return context;
}

import type {
  AiAnalysisHistoryResponse,
  AiAnalysisRequest,
  AiAnalysisResponse,
  ConnectionSummaryResponse,
  DashboardSummaryResponse,
  HistoricalIncidentResponse,
  HistoricalIncidentSummaryResponse,
  LockInfoResponse,
  QueryStatsResponse,
  RunningQueryResponse,
  SystemMetricsResponse,
  TerminateSessionResponse,
} from "./types";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "/api/v1";
const API_KEY_HEADER = import.meta.env.VITE_API_KEY_HEADER ?? "X-API-KEY";
const API_KEY = import.meta.env.VITE_API_KEY ?? "public-dev-key";

type RequestOptions = RequestInit & {
  params?: Record<string, string | number | boolean | undefined>;
};

async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { params, headers, ...init } = options;
  const url = new URL(`${API_BASE_URL}${path}`, window.location.origin);

  if (params) {
    Object.entries(params).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== "") {
        url.searchParams.set(key, String(value));
      }
    });
  }

  const response = await fetch(url.toString(), {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(API_KEY ? { [API_KEY_HEADER]: API_KEY } : {}),
      ...headers,
    },
  });

  if (!response.ok) {
    let message = `Request failed with status ${response.status}`;

    try {
      const error = await response.json();
      message = error.message ?? error.error ?? message;
    } catch {
      // Keep default message when the response body is not JSON.
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

export const api = {
  getDashboardSummary: () => request<DashboardSummaryResponse>("/dashboard/summary"),
  getConnectionSummary: () => request<ConnectionSummaryResponse>("/db/connections/summary"),
  getConnectionsByUser: () => request<ConnectionSummaryResponse["byUser"]>("/db/connections/by-user"),
  getConnectionsByApplication: () =>
    request<ConnectionSummaryResponse["byApplication"]>("/db/connections/by-application"),
  getLocks: () => request<LockInfoResponse[]>("/db/locks"),
  getBlockingLocks: () => request<LockInfoResponse[]>("/db/locks/blocking"),
  getBlockedLocks: () => request<LockInfoResponse[]>("/db/locks/blocked"),
  getTopQueries: (limit = 20) =>
    request<QueryStatsResponse>("/db/queries/top", { params: { limit } }),
  getSlowQueries: (limit = 20) =>
    request<QueryStatsResponse>("/db/queries/slow", { params: { limit } }),
  getRunningQueries: (minDurationSeconds = 30) =>
    request<RunningQueryResponse[]>("/db/queries/running", {
      params: { minDurationSeconds },
    }),
  getIdleInTransactionSessions: (minDurationSeconds = 60) =>
    request<unknown[]>("/db/sessions/idle-in-transaction", {
      params: { minDurationSeconds },
    }),
  getHistoryIncidents: (limit = 200) =>
    request<HistoricalIncidentResponse[]>("/history/incidents", { params: { limit } }),
  getHistorySummary: () => request<HistoricalIncidentSummaryResponse>("/history/summary"),
  analyzeWithAi: (payload: AiAnalysisRequest) =>
    request<AiAnalysisResponse>("/ai/analysis", {
      method: "POST",
      body: JSON.stringify(payload),
    }),
  getAiAnalysisHistory: (limit = 100) =>
    request<AiAnalysisHistoryResponse[]>("/ai/history", { params: { limit } }),
  getSystemMetrics: () => request<SystemMetricsResponse>("/system/metrics"),
  terminateSession: (pid: number, reason = "Terminated from frontend dashboard") =>
    request<TerminateSessionResponse>(`/db/sessions/${pid}/terminate`, {
      method: "POST",
      body: JSON.stringify({ reason, force: true }),
    }),
};

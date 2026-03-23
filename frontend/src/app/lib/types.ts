export interface GroupedConnectionResponse {
  name: string;
  connections: number;
}

export interface ConnectionSummaryResponse {
  totalConnections: number;
  activeConnections: number;
  idleConnections: number;
  idleInTransactionConnections: number;
  maxConnections: number;
  usagePercent: number;
  byUser: GroupedConnectionResponse[];
  byApplication: GroupedConnectionResponse[];
}

export interface TopQueryResponse {
  query: string;
  calls: number;
  totalExecTime: number;
  meanExecTime: number;
  rows: number;
  sharedBlksHit: number;
  sharedBlksRead: number;
  tempBlksWritten: number;
}

export interface RunningQueryResponse {
  pid: number;
  userName: string;
  database: string;
  duration: string;
  state: string;
  waitEvent: string | null;
  query: string;
}

export interface QueryStatsResponse {
  available: boolean;
  message: string | null;
  queries: TopQueryResponse[];
}

export interface LockInfoResponse {
  pid: number;
  userName: string;
  database: string;
  state: string;
  lockType: string;
  relation: string | null;
  query: string;
  queryStart: string | null;
  xactStart: string | null;
  queryDuration: string | null;
  transactionDuration: string | null;
  blockedByPid: number | null;
  blockedByQuery: string | null;
  applicationName: string | null;
  clientAddr: string | null;
  waitEventType: string | null;
  waitEvent: string | null;
}

export interface SystemMetricsResponse {
  memory: {
    heapUsedBytes: number;
    heapMaxBytes: number;
    nonHeapUsedBytes: number;
    nonHeapCommittedBytes: number;
    jvmTotalMemoryBytes: number;
    jvmFreeMemoryBytes: number;
  };
  cpu: {
    processCpuUsage: number;
    systemLoadAverage: number;
    availableProcessors: number;
  };
  threads: {
    liveThreads: number;
    daemonThreads: number;
    peakThreads: number;
  };
  uptime: string;
}

export interface DashboardSummaryResponse {
  database: {
    status: string;
    connections: ConnectionSummaryResponse;
    locks: {
      total: number;
      blocked: number;
      blocking: number;
    };
    runningQueries: {
      total: number;
      queries: RunningQueryResponse[];
    };
    topQueries: TopQueryResponse[];
    settings: DatabaseSettingResponse[];
  };
  application: {
    cpu: SystemMetricsResponse["cpu"];
    memory: SystemMetricsResponse["memory"];
    threads: SystemMetricsResponse["threads"];
    uptimeSeconds: number;
  };
  generatedAt: string;
}

export interface DatabaseSettingResponse {
  name: string;
  setting: string;
  unit: string | null;
}

export interface IdleSessionResponse {
  pid: number;
}

export interface TerminateSessionResponse {
  terminated: boolean;
  pid: number;
  message: string;
}

export interface HistoricalIncidentResponse {
  id: number;
  incidentType: "CPU_HIGH" | "MEMORY_HIGH" | "LOCK_BLOCKING" | string;
  severity: "warning" | "critical" | "info" | string;
  title: string;
  details: string | null;
  metricValue: number | null;
  metricUnit: string | null;
  source: string | null;
  referenceName: string | null;
  createdAt: string;
}

export interface HistoricalIncidentSummaryResponse {
  totalIncidents: number;
  cpuIncidents: number;
  memoryIncidents: number;
  lockIncidents: number;
}

export interface AiAnalysisRequest {
  prompt: string;
}

export interface AiAnalysisResponse {
  model: string;
  prompt: string;
  analysis: string;
  generatedAt: string;
}

export interface AiAnalysisHistoryResponse {
  id: number;
  dbUrlAdmin: string;
  model: string;
  userPrompt: string;
  finalPrompt: string;
  analysis: string;
  createdAt: string;
}

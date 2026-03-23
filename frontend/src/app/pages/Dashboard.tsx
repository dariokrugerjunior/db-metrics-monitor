import { useCallback, useMemo } from "react";
import { MetricCard } from "../components/MetricCard";
import { DataTable, Column } from "../components/DataTable";
import { StatusBadge } from "../components/StatusBadge";
import { TimeRangeFilter } from "../components/TimeRangeFilter";
import { LoadingChart, LoadingMetricCard, LoadingTable } from "../components/LoadingState";
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../lib/api";
import { buildFlatTrend, formatCompactNumber, formatDuration, formatPercent } from "../lib/formatters";
import type { DashboardSummaryResponse, RunningQueryResponse, TopQueryResponse } from "../lib/types";
import { useApiPolling } from "../hooks/useApiPolling";
import { usePageRefresh } from "../hooks/usePageRefresh";
import { Activity, Cable, Code2, Cpu, Lock, MemoryStick } from "lucide-react";
import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

const emptySummary: DashboardSummaryResponse = {
  database: {
    status: "UNKNOWN",
    connections: {
      totalConnections: 0,
      activeConnections: 0,
      idleConnections: 0,
      idleInTransactionConnections: 0,
      maxConnections: 0,
      usagePercent: 0,
      byUser: [],
      byApplication: [],
    },
    locks: {
      total: 0,
      blocked: 0,
      blocking: 0,
    },
    runningQueries: {
      total: 0,
      queries: [],
    },
    topQueries: [],
  },
  application: {
    cpu: {
      processCpuUsage: 0,
      systemLoadAverage: 0,
      availableProcessors: 0,
    },
    memory: {
      heapUsedBytes: 0,
      heapMaxBytes: 0,
      nonHeapUsedBytes: 0,
      nonHeapCommittedBytes: 0,
      jvmTotalMemoryBytes: 0,
      jvmFreeMemoryBytes: 0,
    },
    threads: {
      liveThreads: 0,
      daemonThreads: 0,
      peakThreads: 0,
    },
    uptimeSeconds: 0,
  },
  generatedAt: new Date(0).toISOString(),
};

type QueryRow = TopQueryResponse & { status: "healthy" | "warning" | "critical" };
type LockRow = RunningQueryResponse & { status: "blocked" | "blocking" };

function getQueryStatus(meanExecTime: number): QueryRow["status"] {
  if (meanExecTime >= 1000) return "critical";
  if (meanExecTime >= 250) return "warning";
  return "healthy";
}

function getLockStatus(index: number): LockRow["status"] {
  return index % 2 === 0 ? "blocking" : "blocked";
}

export function Dashboard() {
  const { data, loading, error, refresh } = useApiPolling(api.getDashboardSummary, {
    initialData: emptySummary,
    intervalMs: 15000,
  });
  usePageRefresh(useCallback(() => {
    void refresh();
  }, [refresh]));

  const cpuPercent = data.application.cpu.processCpuUsage;
  const memoryPercent =
    data.application.memory.jvmTotalMemoryBytes > 0
      ? (data.application.memory.heapUsedBytes / data.application.memory.jvmTotalMemoryBytes) * 100
      : 0;

  const cpuData = useMemo(() => buildFlatTrend(Number(cpuPercent.toFixed(1))), [cpuPercent]);
  const memoryData = useMemo(
    () => buildFlatTrend(Number(memoryPercent.toFixed(1))),
    [memoryPercent],
  );
  const connectionsData = useMemo(
    () => buildFlatTrend(data.database.connections.totalConnections),
    [data.database.connections.totalConnections],
  );

  const topQueries: QueryRow[] = data.database.topQueries.map((query) => ({
    ...query,
    status: getQueryStatus(query.meanExecTime),
  }));

  const recentLocks: LockRow[] = data.database.runningQueries.queries.slice(0, 5).map((query, index) => ({
    ...query,
    status: getLockStatus(index),
  }));

  const queryColumns: Column<QueryRow>[] = [
    {
      key: "query",
      header: "Query",
      render: (row) => <span className="font-mono text-xs text-[#a1a1aa]">{row.query}</span>,
    },
    {
      key: "calls",
      header: "Execuções",
      sortable: true,
      render: (row) => formatCompactNumber(row.calls),
    },
    {
      key: "meanExecTime",
      header: "Tempo Médio",
      sortable: true,
      render: (row) => `${row.meanExecTime.toFixed(2)}ms`,
    },
    {
      key: "status",
      header: "Status",
      render: (row) => <StatusBadge status={row.status} />,
    },
  ];

  const lockColumns: Column<LockRow>[] = [
    { key: "pid", header: "PID", sortable: true },
    { key: "database", header: "Database" },
    {
      key: "state",
      header: "State",
      render: (row) => <span className="capitalize">{row.state ?? "unknown"}</span>,
    },
    {
      key: "duration",
      header: "Duração",
      sortable: true,
      render: (row) => formatDuration(row.duration),
    },
    {
      key: "status",
      header: "Status",
      render: (row) => <StatusBadge status={row.status} />,
    },
  ];

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm text-[#71717a]">Snapshot operacional do backend Spring</p>
        </div>
        <TimeRangeFilter />
      </div>

      {error && (
        <StatusBanner
          status="error"
          title="Falha ao carregar o dashboard"
          description={error}
        />
      )}

      {loading ? (
        <>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }, (_, index) => (
              <LoadingMetricCard key={index} />
            ))}
          </div>
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
            {Array.from({ length: 3 }, (_, index) => (
              <LoadingChart key={index} />
            ))}
          </div>
          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <LoadingTable />
            <LoadingTable />
          </div>
        </>
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
            <MetricCard
              title="Total Connections"
              value={data.database.connections.totalConnections}
              change={`${formatPercent(data.database.connections.usagePercent)} da capacidade`}
              changeType="neutral"
              icon={Cable}
              status="healthy"
            />
            <MetricCard
              title="Active Connections"
              value={data.database.connections.activeConnections}
              change={`${data.database.connections.idleConnections} idle`}
              changeType="positive"
              icon={Activity}
              status="healthy"
            />
            <MetricCard
              title="Active Locks"
              value={data.database.locks.total}
              change={`${data.database.locks.blocked} blocked`}
              changeType={data.database.locks.blocked > 0 ? "negative" : "positive"}
              icon={Lock}
              status={data.database.locks.blocked > 0 ? "warning" : "healthy"}
            />
            <MetricCard
              title="Running Queries"
              value={data.database.runningQueries.total}
              change={`${recentLocks.length} visíveis no snapshot`}
              changeType="neutral"
              icon={Code2}
              status="healthy"
            />
            <MetricCard
              title="CPU Usage"
              value={formatPercent(cpuPercent)}
              change={`${data.application.cpu.availableProcessors} vCPUs`}
              changeType={cpuPercent >= 70 ? "negative" : "positive"}
              icon={Cpu}
              status={cpuPercent >= 80 ? "critical" : cpuPercent >= 60 ? "warning" : "healthy"}
            />
            <MetricCard
              title="Memory Usage"
              value={formatPercent(memoryPercent)}
              change={`${formatPercent(memoryPercent)} do heap JVM`}
              changeType={memoryPercent >= 75 ? "warning" : "positive"}
              icon={MemoryStick}
              status={memoryPercent >= 85 ? "critical" : memoryPercent >= 70 ? "warning" : "healthy"}
            />
          </div>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-3">
            <ChartCard title="CPU Usage %" data={cpuData} color="#3b82f6" />
            <ChartCard title="Heap Usage %" data={memoryData} color="#8b5cf6" />
            <ChartCard title="Connections" data={connectionsData} color="#10b981" />
          </div>

          <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
            <div>
              <h2 className="mb-3 text-lg font-semibold text-white">Top Queries</h2>
              <DataTable data={topQueries} columns={queryColumns} emptyMessage="Nenhuma query disponível" />
            </div>
            <div>
              <h2 className="mb-3 text-lg font-semibold text-white">Running Queries</h2>
              <DataTable data={recentLocks} columns={lockColumns} emptyMessage="Nenhuma query em execução" />
            </div>
          </div>
        </>
      )}
    </div>
  );
}

function ChartCard({
  title,
  data,
  color,
}: {
  title: string;
  data: Array<{ time: string; value: number }>;
  color: string;
}) {
  return (
    <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
      <h3 className="mb-4 text-sm font-medium text-[#a1a1aa]">{title}</h3>
      <ResponsiveContainer width="100%" height={200}>
        <LineChart data={data}>
          <CartesianGrid strokeDasharray="3 3" stroke="#27272a" />
          <XAxis dataKey="time" stroke="#71717a" style={{ fontSize: "12px" }} />
          <YAxis stroke="#71717a" style={{ fontSize: "12px" }} />
          <Tooltip
            contentStyle={{
              backgroundColor: "#111116",
              border: "1px solid #27272a",
              borderRadius: "8px",
              color: "#e4e4e7",
            }}
          />
          <Line type="monotone" dataKey="value" stroke={color} strokeWidth={2} dot={false} />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}

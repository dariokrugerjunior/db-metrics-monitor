import { useCallback, type ReactNode } from "react";
import { MetricCard } from "../components/MetricCard";
import { DataTable, Column } from "../components/DataTable";
import { StatusBadge } from "../components/StatusBadge";
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../lib/api";
import { formatDuration } from "../lib/formatters";
import type {
  DashboardSummaryResponse,
  DatabaseSettingResponse,
  RunningQueryResponse,
  TopQueryResponse,
} from "../lib/types";
import { useApiPolling } from "../hooks/useApiPolling";
import { usePageRefresh } from "../hooks/usePageRefresh";
import {
  Activity,
  AlertTriangle,
  Cable,
  CheckCircle,
  Code2,
  Info,
  Lock,
} from "lucide-react";

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
    settings: [],
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

type Alert = {
  id: number;
  severity: "critical" | "warning" | "info";
  message: string;
  timestamp: string;
  category: string;
};

function mapAlerts(data: DashboardSummaryResponse): Alert[] {
  const alerts: Alert[] = [];

  if (data.database.locks.blocked > 0) {
    alerts.push({
      id: 1,
      severity: "critical",
      message: `${data.database.locks.blocked} sessão(ões) bloqueadas no PostgreSQL`,
      timestamp: "agora",
      category: "Locks",
    });
  }

  if (data.database.runningQueries.total > 0) {
    alerts.push({
      id: 2,
      severity: "warning",
      message: `${data.database.runningQueries.total} query(s) em execução acima do threshold`,
      timestamp: "agora",
      category: "Queries",
    });
  }

  if (alerts.length === 0) {
    alerts.push({
      id: 3,
      severity: "info",
      message: "Nenhum alerta crítico no snapshot atual",
      timestamp: "agora",
      category: "Overview",
    });
  }

  return alerts;
}

export function Overview() {
  const { data, loading, error, refresh } = useApiPolling(api.getDashboardSummary, {
    initialData: emptySummary,
    intervalMs: 15000,
  });

  usePageRefresh(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );

  const activeAlerts = mapAlerts(data);
  const criticalLocks = data.database.runningQueries.queries.slice(0, 5);
  const slowQueries = data.database.topQueries.filter((query) => query.meanExecTime >= 250).slice(0, 5);
  const databaseSettings = data.database.settings;

  const alertColumns: Column<Alert>[] = [
    {
      key: "severity",
      header: "Severity",
      render: (row) => {
        const icons = {
          critical: <AlertTriangle className="h-4 w-4 text-[#ef4444]" />,
          warning: <AlertTriangle className="h-4 w-4 text-[#f59e0b]" />,
          info: <Info className="h-4 w-4 text-[#3b82f6]" />,
        };
        return icons[row.severity];
      },
      width: "60px",
    },
    {
      key: "message",
      header: "Message",
      render: (row) => (
        <div>
          <p className="text-sm text-white">{row.message}</p>
          <p className="mt-0.5 text-xs text-[#71717a]">{row.category}</p>
        </div>
      ),
    },
    { key: "timestamp", header: "Time", sortable: true },
  ];

  const lockColumns: Column<RunningQueryResponse>[] = [
    { key: "pid", header: "PID", sortable: true },
    { key: "database", header: "Database" },
    {
      key: "duration",
      header: "Duration",
      sortable: true,
      render: (row) => formatDuration(row.duration),
    },
    {
      key: "query",
      header: "Query",
      render: (row) => <span className="font-mono text-xs text-[#a1a1aa]">{row.query}</span>,
    },
    {
      key: "status",
      header: "Status",
      render: () => <StatusBadge status="warning" label="Running" />,
    },
  ];

  const queryColumns: Column<TopQueryResponse>[] = [
    {
      key: "query",
      header: "Query",
      render: (row) => <span className="font-mono text-xs text-[#a1a1aa]">{row.query}</span>,
    },
    {
      key: "meanExecTime",
      header: "Avg Time",
      sortable: true,
      render: (row) => `${row.meanExecTime.toFixed(2)}ms`,
    },
    { key: "calls", header: "Executions", sortable: true },
    {
      key: "status",
      header: "Status",
      render: (row) => <StatusBadge status={row.meanExecTime >= 1000 ? "critical" : "warning"} />,
    },
  ];

  const overallStatus = activeAlerts.some((alert) => alert.severity === "critical") ? "warning" : "success";

  return (
    <div className="space-y-6">
      {error && <StatusBanner status="error" title="Falha ao carregar overview" description={error} />}

      <StatusBanner
        status={overallStatus}
        title={overallStatus === "success" ? "System Status: Healthy" : "System Status: Warning"}
        description={
          loading
            ? "Carregando snapshot consolidado."
            : `${activeAlerts.length} alerta(s) ativos. Uptime atual: ${Math.floor(data.application.uptimeSeconds / 3600)}h.`
        }
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard title="Total Connections" value={data.database.connections.totalConnections} change="Snapshot atual" changeType="neutral" icon={Cable} status="healthy" />
        <MetricCard title="Active Connections" value={data.database.connections.activeConnections} change="Conexões utilizáveis" changeType="positive" icon={Activity} status="healthy" />
        <MetricCard title="Active Locks" value={data.database.locks.total} change={`${data.database.locks.blocked} bloqueadas`} changeType={data.database.locks.blocked > 0 ? "negative" : "positive"} icon={Lock} status={data.database.locks.blocked > 0 ? "warning" : "healthy"} />
        <MetricCard title="Running Queries" value={data.database.runningQueries.total} change="Threshold do backend" changeType="neutral" icon={Code2} status="healthy" />
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-white">Active Alerts</h2>
          <span className="text-xs text-[#71717a]">Atualizado continuamente</span>
        </div>
        <DataTable data={activeAlerts} columns={alertColumns} emptyMessage="Sem alertas ativos" />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-white">Critical Locks</h2>
            <StatusBadge status="warning" label={`${criticalLocks.length} Active`} />
          </div>
          <DataTable
            data={criticalLocks}
            columns={lockColumns}
            emptyMessage={loading ? "Carregando locks..." : "Nenhum lock crítico"}
          />
        </div>
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-white">Slow Queries</h2>
            <StatusBadge status={slowQueries.length > 0 ? "critical" : "healthy"} label={`${slowQueries.length} Queries`} />
          </div>
          <DataTable
            data={slowQueries}
            columns={queryColumns}
            emptyMessage={loading ? "Carregando queries..." : "Nenhuma slow query"}
          />
        </div>
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <h2 className="mb-4 text-lg font-semibold text-white">Application Status</h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <StatusItem label="Database" status={data.database.status} icon={<CheckCircle className="h-5 w-5 text-[#10b981]" />} />
          <StatusItem
            label="Connection Pool"
            status={`${data.database.connections.totalConnections}/${data.database.connections.maxConnections}`}
            icon={<CheckCircle className="h-5 w-5 text-[#10b981]" />}
          />
          <StatusItem
            label="Query Performance"
            status={slowQueries.length > 0 ? "Warning" : "Healthy"}
            icon={slowQueries.length > 0 ? <AlertTriangle className="h-5 w-5 text-[#f59e0b]" /> : <CheckCircle className="h-5 w-5 text-[#10b981]" />}
          />
          <StatusItem
            label="Running Queries"
            status={loading ? "Loading" : String(data.database.runningQueries.total)}
            icon={data.database.runningQueries.total > 0 ? <AlertTriangle className="h-5 w-5 text-[#f59e0b]" /> : <CheckCircle className="h-5 w-5 text-[#10b981]" />}
          />
        </div>
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <h2 className="mb-4 text-lg font-semibold text-white">Database Settings</h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3">
          {databaseSettings.map((setting) => (
            <DatabaseSettingItem key={setting.name} setting={setting} />
          ))}
        </div>
      </div>
    </div>
  );
}

function StatusItem({
  label,
  status,
  icon,
}: {
  label: string;
  status: string;
  icon: ReactNode;
}) {
  return (
    <div className="flex items-center space-x-3 rounded-lg border border-[#27272a] bg-[#0a0a0f] p-3">
      {icon}
      <div>
        <p className="text-xs text-[#71717a]">{label}</p>
        <p className="text-sm font-medium text-white">{status}</p>
      </div>
    </div>
  );
}

function DatabaseSettingItem({ setting }: { setting: DatabaseSettingResponse }) {
  const config = databaseSettingConfig[setting.name] ?? {
    label: setting.name,
    description: "Parâmetro retornado diretamente pelo pg_settings.",
  };

  return (
    <div className="rounded-xl border border-[#27272a] bg-gradient-to-br from-[#111116] to-[#0a0a0f] p-5">
      <p className="text-[11px] uppercase tracking-[0.18em] text-[#71717a]">{config.label}</p>
      <p className="mt-3 text-2xl font-semibold text-white">
        {formatDatabaseSettingValue(setting)}
      </p>
      <p className="mt-3 text-sm leading-6 text-[#a1a1aa]">{config.description}</p>
    </div>
  );
}

const databaseSettingConfig: Record<string, { label: string; description: string }> = {
  effective_cache_size: {
    label: "Effective Cache Size",
    description: "Estimativa de memória disponível para cache de dados usada pelo otimizador de consultas.",
  },
  maintenance_work_mem: {
    label: "Maintenance Work Mem",
    description: "Memória usada por operações de manutenção como VACUUM, CREATE INDEX e ALTER TABLE.",
  },
  max_connections: {
    label: "Max Connections",
    description: "Quantidade máxima de conexões simultâneas permitidas no PostgreSQL.",
  },
  shared_buffers: {
    label: "Shared Buffers",
    description: "Memória principal reservada pelo PostgreSQL para cache de páginas de dados.",
  },
  work_mem: {
    label: "Work Mem",
    description: "Memória disponível por operação para sorts, hashes e execuções intermediárias de query.",
  },
};

function formatDatabaseSettingValue(setting: DatabaseSettingResponse) {
  const numericValue = Number(setting.setting);
  if (Number.isNaN(numericValue)) {
    return [setting.setting, setting.unit].filter(Boolean).join(" ");
  }

  if (setting.unit?.toLowerCase() === "kb") {
    return `${(numericValue / 1024).toFixed(numericValue >= 1024 ? 0 : 2)} MB`;
  }

  if (setting.unit?.toLowerCase() === "8kb") {
    return `${((numericValue * 8) / 1024).toFixed(2)} MB`;
  }

  return [setting.setting, setting.unit].filter(Boolean).join(" ");
}

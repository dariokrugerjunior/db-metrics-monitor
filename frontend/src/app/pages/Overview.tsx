import { useCallback, type ReactNode } from "react";
import { useTranslation } from "react-i18next";
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

function useAlerts(data: DashboardSummaryResponse): Alert[] {
  const { t } = useTranslation();
  const alerts: Alert[] = [];

  if (data.database.locks.blocked > 0) {
    alerts.push({
      id: 1,
      severity: "critical",
      message: t("overview.alertBlocked", { count: data.database.locks.blocked }),
      timestamp: t("overview.agora"),
      category: "Locks",
    });
  }

  if (data.database.runningQueries.total > 0) {
    alerts.push({
      id: 2,
      severity: "warning",
      message: t("overview.alertRunningQueries", { count: data.database.runningQueries.total }),
      timestamp: t("overview.agora"),
      category: "Queries",
    });
  }

  if (alerts.length === 0) {
    alerts.push({
      id: 3,
      severity: "info",
      message: t("overview.noAlertsCritical"),
      timestamp: t("overview.agora"),
      category: "Overview",
    });
  }

  return alerts;
}

export function Overview() {
  const { t } = useTranslation();
  const { data, loading, error, refresh } = useApiPolling(api.getDashboardSummary, {
    initialData: emptySummary,
    intervalMs: 15000,
  });

  usePageRefresh(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );

  const activeAlerts = useAlerts(data);
  const criticalLocks = data.database.runningQueries.queries.slice(0, 5);
  const slowQueries = data.database.topQueries.filter((query) => query.meanExecTime >= 250).slice(0, 5);
  const databaseSettings = data.database.settings;

  const alertColumns: Column<Alert>[] = [
    {
      key: "severity",
      header: t("overview.alertSeverity"),
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
      header: t("overview.alertMessage"),
      render: (row) => (
        <div>
          <p className="text-sm text-white">{row.message}</p>
          <p className="mt-0.5 text-xs text-[#71717a]">{row.category}</p>
        </div>
      ),
    },
    { key: "timestamp", header: t("overview.alertTime"), sortable: true },
  ];

  const lockColumns: Column<RunningQueryResponse>[] = [
    { key: "pid", header: "PID", sortable: true },
    { key: "database", header: t("common.database") },
    {
      key: "duration",
      header: t("common.duration"),
      sortable: true,
      render: (row) => formatDuration(row.duration),
    },
    {
      key: "query",
      header: t("common.query"),
      render: (row) => <span className="font-mono text-xs text-[#a1a1aa]">{row.query}</span>,
    },
    {
      key: "status",
      header: t("common.status"),
      render: () => <StatusBadge status="warning" label="Running" />,
    },
  ];

  const queryColumns: Column<TopQueryResponse>[] = [
    {
      key: "query",
      header: t("common.query"),
      render: (row) => <span className="font-mono text-xs text-[#a1a1aa]">{row.query}</span>,
    },
    {
      key: "meanExecTime",
      header: t("dashboard.avgTime"),
      sortable: true,
      render: (row) => `${row.meanExecTime.toFixed(2)}ms`,
    },
    { key: "calls", header: t("dashboard.executions"), sortable: true },
    {
      key: "status",
      header: t("common.status"),
      render: (row) => <StatusBadge status={row.meanExecTime >= 1000 ? "critical" : "warning"} />,
    },
  ];

  const overallStatus = activeAlerts.some((alert) => alert.severity === "critical") ? "warning" : "success";

  return (
    <div className="space-y-6">
      {error && <StatusBanner status="error" title={t("overview.errorBanner")} description={error} />}

      <StatusBanner
        status={overallStatus}
        title={overallStatus === "success" ? t("overview.statusHealthy") : t("overview.statusWarning")}
        description={
          loading
            ? t("overview.loadingSnapshot")
            : t("overview.alertsSuffix", {
                count: activeAlerts.length,
                hours: Math.floor(data.application.uptimeSeconds / 3600),
              })
        }
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard title={t("overview.totalConnections")} value={data.database.connections.totalConnections} change={t("overview.currentSnapshot")} changeType="neutral" icon={Cable} status="healthy" />
        <MetricCard title={t("overview.activeConnections")} value={data.database.connections.activeConnections} change={t("overview.usableConnections")} changeType="positive" icon={Activity} status="healthy" />
        <MetricCard title={t("overview.activeLocks")} value={data.database.locks.total} change={t("overview.blockedCount", { count: data.database.locks.blocked })} changeType={data.database.locks.blocked > 0 ? "negative" : "positive"} icon={Lock} status={data.database.locks.blocked > 0 ? "warning" : "healthy"} />
        <MetricCard title={t("overview.runningQueries")} value={data.database.runningQueries.total} change={t("overview.backendThreshold")} changeType="neutral" icon={Code2} status="healthy" />
      </div>

      <div>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-white">{t("overview.activeAlerts")}</h2>
          <span className="text-xs text-[#71717a]">{t("overview.updatedContinuously")}</span>
        </div>
        <DataTable data={activeAlerts} columns={alertColumns} emptyMessage={t("overview.noAlerts")} />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-white">{t("overview.criticalLocks")}</h2>
            <StatusBadge status="warning" label={t("overview.activeLabel", { count: criticalLocks.length })} />
          </div>
          <DataTable
            data={criticalLocks}
            columns={lockColumns}
            emptyMessage={loading ? t("overview.loadingLocks") : t("overview.noCriticalLocks")}
          />
        </div>
        <div>
          <div className="mb-3 flex items-center justify-between">
            <h2 className="text-lg font-semibold text-white">{t("overview.slowQueries")}</h2>
            <StatusBadge status={slowQueries.length > 0 ? "critical" : "healthy"} label={t("overview.queriesLabel", { count: slowQueries.length })} />
          </div>
          <DataTable
            data={slowQueries}
            columns={queryColumns}
            emptyMessage={loading ? t("overview.loadingQueries") : t("overview.noSlowQueries")}
          />
        </div>
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <h2 className="mb-4 text-lg font-semibold text-white">{t("overview.applicationStatus")}</h2>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <StatusItem label={t("common.database")} status={data.database.status} icon={<CheckCircle className="h-5 w-5 text-[#10b981]" />} />
          <StatusItem
            label={t("overview.connectionPool")}
            status={`${data.database.connections.totalConnections}/${data.database.connections.maxConnections}`}
            icon={<CheckCircle className="h-5 w-5 text-[#10b981]" />}
          />
          <StatusItem
            label={t("overview.queryPerformance")}
            status={slowQueries.length > 0 ? t("common.warning") : t("common.healthy")}
            icon={slowQueries.length > 0 ? <AlertTriangle className="h-5 w-5 text-[#f59e0b]" /> : <CheckCircle className="h-5 w-5 text-[#10b981]" />}
          />
          <StatusItem
            label={t("overview.runningQueries")}
            status={loading ? t("overview.loading") : String(data.database.runningQueries.total)}
            icon={data.database.runningQueries.total > 0 ? <AlertTriangle className="h-5 w-5 text-[#f59e0b]" /> : <CheckCircle className="h-5 w-5 text-[#10b981]" />}
          />
        </div>
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <h2 className="mb-4 text-lg font-semibold text-white">{t("overview.databaseSettings")}</h2>
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
  const { t } = useTranslation();

  const settingConfigMap: Record<string, { labelKey: string; descKey: string }> = {
    effective_cache_size: { labelKey: "overview.effectiveCacheSize", descKey: "overview.effectiveCacheSizeDesc" },
    maintenance_work_mem: { labelKey: "overview.maintenanceWorkMem", descKey: "overview.maintenanceWorkMemDesc" },
    max_connections: { labelKey: "overview.maxConnections", descKey: "overview.maxConnectionsDesc" },
    shared_buffers: { labelKey: "overview.sharedBuffers", descKey: "overview.sharedBuffersDesc" },
    work_mem: { labelKey: "overview.workMem", descKey: "overview.workMemDesc" },
  };

  const keys = settingConfigMap[setting.name];
  const label = keys ? t(keys.labelKey) : setting.name;
  const description = keys ? t(keys.descKey) : t("overview.dbSettingDefaultDesc");

  return (
    <div className="rounded-xl border border-[#27272a] bg-gradient-to-br from-[#111116] to-[#0a0a0f] p-5">
      <p className="text-[11px] uppercase tracking-[0.18em] text-[#71717a]">{label}</p>
      <p className="mt-3 text-2xl font-semibold text-white">
        {formatDatabaseSettingValue(setting)}
      </p>
      <p className="mt-3 text-sm leading-6 text-[#a1a1aa]">{description}</p>
    </div>
  );
}

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

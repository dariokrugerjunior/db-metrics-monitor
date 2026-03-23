import { useCallback, useMemo } from "react";
import { AlertTriangle, Cpu, DatabaseZap, HardDrive } from "lucide-react";
import { DataTable, Column } from "../components/DataTable";
import { MetricCard } from "../components/MetricCard";
import { StatusBadge } from "../components/StatusBadge";
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../lib/api";
import { formatPercent, formatRelativeTimestamp } from "../lib/formatters";
import type { HistoricalIncidentResponse, HistoricalIncidentSummaryResponse } from "../lib/types";
import { useApiPolling } from "../hooks/useApiPolling";
import { usePageRefresh } from "../hooks/usePageRefresh";

async function fetchHistory() {
  const [summary, incidents] = await Promise.all([
    api.getHistorySummary(),
    api.getHistoryIncidents(200),
  ]);

  return { summary, incidents };
}

const emptySummary: HistoricalIncidentSummaryResponse = {
  totalIncidents: 0,
  cpuIncidents: 0,
  memoryIncidents: 0,
  lockIncidents: 0,
};

export function History() {
  const { data, loading, error, refresh } = useApiPolling(fetchHistory, {
    initialData: {
      summary: emptySummary,
      incidents: [],
    },
    intervalMs: 30000,
  });

  usePageRefresh(useCallback(() => {
    void refresh();
  }, [refresh]));

  const incidents = useMemo(() => data.incidents, [data.incidents]);

  const columns: Column<HistoricalIncidentResponse>[] = [
    {
      key: "createdAt",
      header: "Momento",
      sortable: true,
      render: (row) => formatRelativeTimestamp(row.createdAt),
    },
    {
      key: "incidentType",
      header: "Tipo",
      sortable: true,
      render: (row) => (
        <span className="font-medium text-white">{incidentTypeLabel(row.incidentType)}</span>
      ),
    },
    {
      key: "title",
      header: "Evento",
      render: (row) => (
        <div>
          <p className="text-sm text-white">{row.title}</p>
          <p className="mt-1 text-xs text-[#71717a]">{row.details ?? "Sem detalhes adicionais"}</p>
        </div>
      ),
    },
    {
      key: "metricValue",
      header: "Valor",
      sortable: true,
      render: (row) => {
        if (row.metricValue == null) {
          return "N/A";
        }

        if (row.metricUnit === "%") {
          return formatPercent(row.metricValue);
        }

        return `${row.metricValue.toFixed(2)} ${row.metricUnit ?? ""}`.trim();
      },
    },
    {
      key: "referenceName",
      header: "Referência",
      render: (row) => row.referenceName ?? "N/A",
    },
    {
      key: "severity",
      header: "Status",
      render: (row) => <StatusBadge status={mapSeverity(row.severity)} label={severityLabel(row.severity)} />,
    },
  ];

  return (
    <div className="space-y-6">
      {error && <StatusBanner status="error" title="Falha ao carregar histórico" description={error} />}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard title="Total Incidents" value={data.summary.totalIncidents} change="Persistidos localmente" changeType="neutral" icon={AlertTriangle} status="warning" />
        <MetricCard title="CPU > 60%" value={data.summary.cpuIncidents} change="Snapshots acima do threshold" changeType="warning" icon={Cpu} status="warning" />
        <MetricCard title="Memory > 60%" value={data.summary.memoryIncidents} change="Snapshots acima do threshold" changeType="warning" icon={HardDrive} status="warning" />
        <MetricCard title="Blocking Locks" value={data.summary.lockIncidents} change="Locks com tabela bloqueada" changeType="negative" icon={DatabaseZap} status="critical" />
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <div className="mb-4 flex items-center justify-between">
          <div>
            <h2 className="text-lg font-semibold text-white">Incidentes históricos</h2>
            <p className="text-sm text-[#71717a]">
              Registros capturados pelo scheduler quando CPU, memória ou locks críticos ultrapassam os limites.
            </p>
          </div>
          <StatusBadge
            status={incidents.length > 0 ? "warning" : "healthy"}
            label={loading ? "Carregando" : `${incidents.length} registros`}
          />
        </div>
        <DataTable
          data={incidents}
          columns={columns}
          emptyMessage={loading ? "Carregando histórico..." : "Nenhum incidente histórico registrado ainda"}
        />
      </div>
    </div>
  );
}

function incidentTypeLabel(value: string) {
  switch (value) {
    case "CPU_HIGH":
      return "CPU alta";
    case "MEMORY_HIGH":
      return "Memória alta";
    case "LOCK_BLOCKING":
      return "Lock bloqueante";
    default:
      return value;
  }
}

function mapSeverity(value: string): "healthy" | "warning" | "critical" | "info" {
  if (value === "critical") return "critical";
  if (value === "warning") return "warning";
  if (value === "info") return "info";
  return "healthy";
}

function severityLabel(value: string) {
  switch (value) {
    case "critical":
      return "Critical";
    case "warning":
      return "Warning";
    case "info":
      return "Info";
    default:
      return value;
  }
}

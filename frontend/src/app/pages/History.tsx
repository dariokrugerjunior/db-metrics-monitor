import { useCallback, useMemo, useState } from "react";
import { AlertTriangle, Cpu, DatabaseZap, HardDrive, Trash2 } from "lucide-react";
import { DataTable, Column } from "../components/DataTable";
import { MetricCard } from "../components/MetricCard";
import { StatusBadge } from "../components/StatusBadge";
import { StatusBanner } from "../components/StatusBanner";
import { Button } from "../components/ui/button";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "../components/ui/alert-dialog";
import {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "../components/ui/pagination";
import { api } from "../lib/api";
import { formatPercent, formatRelativeTimestamp } from "../lib/formatters";
import type {
  HistoricalIncidentPageResponse,
  HistoricalIncidentResponse,
  HistoricalIncidentSummaryResponse,
} from "../lib/types";
import { useApiPolling } from "../hooks/useApiPolling";
import { usePageRefresh } from "../hooks/usePageRefresh";

const PAGE_SIZE = 10;

const emptySummary: HistoricalIncidentSummaryResponse = {
  totalIncidents: 0,
  cpuIncidents: 0,
  memoryIncidents: 0,
  lockIncidents: 0,
};

const emptyPage: HistoricalIncidentPageResponse = {
  items: [],
  page: 1,
  size: PAGE_SIZE,
  totalItems: 0,
  totalPages: 1,
};

export function History() {
  const [page, setPage] = useState(1);
  const [clearDialogOpen, setClearDialogOpen] = useState(false);
  const [clearing, setClearing] = useState(false);

  const fetchHistory = useCallback(async () => {
    const [summary, incidentsPage] = await Promise.all([
      api.getHistorySummary(),
      api.getHistoryIncidentsPage(page, PAGE_SIZE),
    ]);

    return { summary, incidentsPage };
  }, [page]);

  const { data, loading, error, refresh } = useApiPolling(fetchHistory, {
    initialData: {
      summary: emptySummary,
      incidentsPage: emptyPage,
    },
    intervalMs: 30000,
  });

  usePageRefresh(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );

  const incidents = useMemo(() => data.incidentsPage.items, [data.incidentsPage.items]);

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
      render: (row) => <span className="font-medium text-white">{incidentTypeLabel(row.incidentType)}</span>,
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
      header: "Referencia",
      render: (row) => row.referenceName ?? "N/A",
    },
    {
      key: "severity",
      header: "Status",
      render: (row) => <StatusBadge status={mapSeverity(row.severity)} label={severityLabel(row.severity)} />,
    },
  ];

  const clearHistory = useCallback(async () => {
    setClearing(true);
    try {
      await api.clearHistoryIncidents();
      setClearDialogOpen(false);
      setPage(1);
      await refresh();
    } finally {
      setClearing(false);
    }
  }, [refresh]);

  const paginationItems = buildPagination(data.incidentsPage.page, data.incidentsPage.totalPages);

  return (
    <div className="space-y-6">
      {error && <StatusBanner status="error" title="Falha ao carregar historico" description={error} />}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard title="Total Incidents" value={data.summary.totalIncidents} change="Persistidos localmente" changeType="neutral" icon={AlertTriangle} status="warning" />
        <MetricCard title="CPU > 60%" value={data.summary.cpuIncidents} change="Snapshots acima do threshold" changeType="warning" icon={Cpu} status="warning" />
        <MetricCard title="Memory > 60%" value={data.summary.memoryIncidents} change="Snapshots acima do threshold" changeType="warning" icon={HardDrive} status="warning" />
        <MetricCard title="Blocking Locks" value={data.summary.lockIncidents} change="Locks com tabela bloqueada" changeType="negative" icon={DatabaseZap} status="critical" />
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <div className="mb-4 flex items-center justify-between gap-4">
          <div>
            <h2 className="text-lg font-semibold text-white">Incidentes historicos</h2>
            <p className="text-sm text-[#71717a]">
              Registros capturados pelo scheduler quando CPU, memoria ou locks criticos ultrapassam os limites.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <StatusBadge
              status={data.incidentsPage.totalItems > 0 ? "warning" : "healthy"}
              label={loading ? "Carregando" : `${data.incidentsPage.totalItems} registros`}
            />
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setClearDialogOpen(true)}
              disabled={data.incidentsPage.totalItems === 0 || clearing}
              className="border-[#ef4444]/30 text-[#fca5a5] hover:bg-[#ef4444]/10 hover:text-[#fecaca]"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              {clearing ? "Limpando..." : "Limpar historico"}
            </Button>
          </div>
        </div>

        <DataTable
          data={incidents}
          columns={columns}
          emptyMessage={loading ? "Carregando historico..." : "Nenhum incidente historico registrado ainda"}
        />

        <div className="mt-5 flex flex-col gap-3 border-t border-[#27272a] pt-4 md:flex-row md:items-center md:justify-between">
          <p className="text-sm text-[#71717a]">
            Pagina {data.incidentsPage.page} de {data.incidentsPage.totalPages} • {data.incidentsPage.totalItems} registros no total
          </p>

          <Pagination className="mx-0 w-auto justify-start md:justify-end">
            <PaginationContent>
              <PaginationItem>
                <PaginationPrevious
                  href="#"
                  onClick={(event) => {
                    event.preventDefault();
                    if (data.incidentsPage.page > 1) {
                      setPage(data.incidentsPage.page - 1);
                    }
                  }}
                  className={data.incidentsPage.page <= 1 ? "pointer-events-none opacity-50" : ""}
                />
              </PaginationItem>

              {paginationItems.map((item, index) => (
                <PaginationItem key={`${item}-${index}`}>
                  {item === "..." ? (
                    <PaginationEllipsis />
                  ) : (
                    <PaginationLink
                      href="#"
                      isActive={item === data.incidentsPage.page}
                      onClick={(event) => {
                        event.preventDefault();
                        setPage(item);
                      }}
                    >
                      {item}
                    </PaginationLink>
                  )}
                </PaginationItem>
              ))}

              <PaginationItem>
                <PaginationNext
                  href="#"
                  onClick={(event) => {
                    event.preventDefault();
                    if (data.incidentsPage.page < data.incidentsPage.totalPages) {
                      setPage(data.incidentsPage.page + 1);
                    }
                  }}
                  className={data.incidentsPage.page >= data.incidentsPage.totalPages ? "pointer-events-none opacity-50" : ""}
                />
              </PaginationItem>
            </PaginationContent>
          </Pagination>
        </div>
      </div>

      <AlertDialog open={clearDialogOpen} onOpenChange={setClearDialogOpen}>
        <AlertDialogContent className="border-[#27272a] bg-[#111116] text-white">
          <AlertDialogHeader>
            <AlertDialogTitle>Limpar historico operacional</AlertDialogTitle>
            <AlertDialogDescription className="text-[#a1a1aa]">
              Esta acao remove permanentemente todos os incidentes da tela History.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-[#27272a] bg-[#0a0a0f] text-white hover:bg-[#1f1f28]">
              Cancelar
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={() => void clearHistory()}
              className="bg-[#ef4444] text-white hover:bg-[#dc2626]"
            >
              Apagar historico
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

function buildPagination(currentPage: number, totalPages: number): Array<number | "..."> {
  if (totalPages <= 5) {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }

  if (currentPage <= 3) {
    return [1, 2, 3, 4, "...", totalPages];
  }

  if (currentPage >= totalPages - 2) {
    return [1, "...", totalPages - 3, totalPages - 2, totalPages - 1, totalPages];
  }

  return [1, "...", currentPage - 1, currentPage, currentPage + 1, "...", totalPages];
}

function incidentTypeLabel(value: string) {
  switch (value) {
    case "CPU_HIGH":
      return "CPU alta";
    case "MEMORY_HIGH":
      return "Memoria alta";
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

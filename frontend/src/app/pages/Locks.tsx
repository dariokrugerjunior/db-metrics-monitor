import { useCallback, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { DataTable, Column } from "../components/DataTable";
import { StatusBadge } from "../components/StatusBadge";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "../components/ui/dialog";
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
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../lib/api";
import { formatDuration, formatRelativeTimestamp } from "../lib/formatters";
import type { LockInfoResponse } from "../lib/types";
import { useApiPolling } from "../hooks/useApiPolling";
import { usePageRefresh } from "../hooks/usePageRefresh";
import { AlertTriangle, Search, XCircle } from "lucide-react";
import { toast } from "sonner";

type LockRow = LockInfoResponse & {
  status: "blocked" | "blocking";
  severity: "low" | "medium" | "high";
};

async function fetchLocks(): Promise<LockRow[]> {
  const [blocking, blocked] = await Promise.all([api.getBlockingLocks(), api.getBlockedLocks()]);

  return [
    ...blocking.map((lock) => ({ ...lock, status: "blocking" as const })),
    ...blocked.map((lock) => ({ ...lock, status: "blocked" as const })),
  ].map((lock) => ({
    ...lock,
    severity:
      lock.lockType?.includes("Exclusive") || lock.queryDuration?.includes("H")
        ? "high"
        : lock.status === "blocked"
          ? "medium"
          : "low",
  }));
}

export function Locks() {
  const { data, loading, error, refresh } = useApiPolling(fetchLocks, {
    initialData: [],
    intervalMs: 15000,
  });
  usePageRefresh(useCallback(() => {
    void refresh();
  }, [refresh]));
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedLock, setSelectedLock] = useState<LockRow | null>(null);
  const [lockToKill, setLockToKill] = useState<LockRow | null>(null);

  const filteredLocks = useMemo(
    () =>
      data.filter(
        (lock) =>
          lock.query?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          lock.userName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          lock.database?.toLowerCase().includes(searchQuery.toLowerCase()) ||
          lock.pid?.toString().includes(searchQuery),
      ),
    [data, searchQuery],
  );

  const handleKillSession = async (lock: LockRow) => {
    try {
      const response = await api.terminateSession(lock.pid);
      setLockToKill(null);
      setSelectedLock(null);
      toast.success(response.message || `Sessão ${lock.pid} finalizada.`);
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Falha ao encerrar a sessão.");
    }
  };

  const columns: Column<LockRow>[] = [
    {
      key: "pid",
      header: "PID",
      sortable: true,
      render: (row) => <span className="font-mono text-sm font-medium">{row.pid}</span>,
    },
    { key: "userName", header: "User", sortable: true },
    { key: "database", header: "Database", sortable: true },
    {
      key: "queryDuration",
      header: "Duração",
      sortable: true,
      render: (row) => formatDuration(row.queryDuration),
    },
    { key: "lockType", header: "Lock Type", sortable: true },
    {
      key: "query",
      header: "Query",
      render: (row) => (
        <span className="block max-w-md truncate font-mono text-xs text-[#a1a1aa]">
          {row.query || "Query não disponível"}
        </span>
      ),
    },
    {
      key: "status",
      header: "Status",
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      key: "actions",
      header: "Actions",
      render: (row) => (
        <Button
          variant="ghost"
          size="sm"
          onClick={(event) => {
            event.stopPropagation();
            setLockToKill(row);
          }}
          className="text-[#ef4444] hover:bg-[#ef4444]/10 hover:text-[#ef4444]"
        >
          <XCircle className="mr-1 h-4 w-4" />
          Kill
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {error && (
        <StatusBanner status="error" title="Falha ao carregar locks" description={error} />
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <StatCard title="Total Locks" value={data.length} color="bg-[#3b82f6]/10 text-[#3b82f6]" />
        <StatCard
          title="Blocked Sessions"
          value={data.filter((lock) => lock.status === "blocked").length}
          color="bg-[#ef4444]/10 text-[#ef4444]"
        />
        <StatCard
          title="Blocking Sessions"
          value={data.filter((lock) => lock.status === "blocking").length}
          color="bg-[#f59e0b]/10 text-[#f59e0b]"
        />
      </div>

      <div className="flex items-center space-x-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#71717a]" />
          <Input
            placeholder="Buscar por PID, usuário, database ou query..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            className="border-[#27272a] bg-[#111116] pl-10 text-white"
          />
        </div>
      </div>

      {data.some((lock) => lock.severity === "high") && (
        <div className="flex items-start space-x-3 rounded-lg border border-[#ef4444]/30 bg-[#ef4444]/10 p-4">
          <AlertTriangle className="mt-0.5 h-5 w-5 text-[#ef4444]" />
          <div>
            <h3 className="text-sm font-medium text-[#ef4444]">Locks críticos detectados</h3>
            <p className="mt-1 text-sm text-[#ef4444]/80">
              {data.filter((lock) => lock.severity === "high").length} lock(s) com alto impacto potencial.
            </p>
          </div>
        </div>
      )}

      <DataTable
        data={filteredLocks}
        columns={columns}
        onRowClick={setSelectedLock}
        emptyMessage={loading ? "Carregando locks..." : "Nenhum lock ativo encontrado"}
      />

      <Dialog open={!!selectedLock} onOpenChange={() => setSelectedLock(null)}>
        <DialogContent className="max-w-3xl border-[#27272a] bg-[#111116] text-white">
          <DialogHeader>
            <DialogTitle>Lock Details - PID {selectedLock?.pid}</DialogTitle>
            <DialogDescription className="text-[#a1a1aa]">
              Informações completas do lock reportado pelo backend.
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <DetailRow label="Process ID" value={selectedLock?.pid} />
            <DetailRow label="User" value={selectedLock?.userName} />
            <DetailRow label="Database" value={selectedLock?.database} />
            <DetailRow label="Duration" value={formatDuration(selectedLock?.queryDuration)} />
            <DetailRow label="Lock Type" value={selectedLock?.lockType} />
            <DetailRow label="State" value={selectedLock?.state} />
            <DetailRow label="Status" value={selectedLock && <StatusBadge status={selectedLock.status} />} />
            <DetailRow label="Application" value={selectedLock?.applicationName ?? "N/A"} />
            <DetailRow label="Relation" value={selectedLock?.relation ?? "N/A"} />
            <DetailRow label="Client" value={selectedLock?.clientAddr ?? "N/A"} />
            <DetailRow label="Started" value={formatRelativeTimestamp(selectedLock?.queryStart)} />
            <div className="space-y-2">
              <label className="text-sm font-medium text-[#a1a1aa]">Query</label>
              <div className="rounded-lg border border-[#27272a] bg-[#0a0a0f] p-4">
                <code className="whitespace-pre-wrap font-mono text-xs text-[#e4e4e7]">
                  {selectedLock?.query || "Query não disponível"}
                </code>
              </div>
            </div>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setSelectedLock(null)}
              className="border-[#27272a] text-white hover:bg-[#1f1f28]"
            >
              Close
            </Button>
            <Button
              variant="destructive"
              onClick={() => {
                if (selectedLock) {
                  setLockToKill(selectedLock);
                }
              }}
              className="bg-[#ef4444] text-white hover:bg-[#dc2626]"
            >
              <XCircle className="mr-2 h-4 w-4" />
              Kill Session
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={!!lockToKill} onOpenChange={() => setLockToKill(null)}>
        <AlertDialogContent className="border-[#27272a] bg-[#111116] text-white">
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center space-x-2">
              <AlertTriangle className="h-5 w-5 text-[#ef4444]" />
              <span>Confirmar encerramento</span>
            </AlertDialogTitle>
            <AlertDialogDescription className="text-[#a1a1aa]">
              Finalizar a sessão {lockToKill?.pid} no PostgreSQL.
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-[#27272a] text-white hover:bg-[#1f1f28]">
              Cancelar
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={() => lockToKill && handleKillSession(lockToKill)}
              className="bg-[#ef4444] text-white hover:bg-[#dc2626]"
            >
              Terminate Session
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

function StatCard({ title, value, color }: { title: string; value: number; color: string }) {
  return (
    <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
      <p className="mb-2 text-sm text-[#a1a1aa]">{title}</p>
      <p className={`text-3xl font-semibold ${color}`}>{value}</p>
    </div>
  );
}

function DetailRow({
  label,
  value,
}: {
  label: string;
  value: string | number | ReactNode | undefined;
}) {
  return (
    <div className="grid grid-cols-3 gap-4">
      <div className="text-sm font-medium text-[#a1a1aa]">{label}</div>
      <div className="col-span-2 text-sm text-white">{value}</div>
    </div>
  );
}

import { useCallback, useMemo, useState } from "react";
import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
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
  const { t } = useTranslation();
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
      toast.success(response.message || t("locks.sessionTerminated", { pid: lock.pid }));
    } catch (err) {
      toast.error(err instanceof Error ? err.message : t("locks.terminationFailed"));
    }
  };

  const columns: Column<LockRow>[] = [
    {
      key: "pid",
      header: "PID",
      sortable: true,
      render: (row) => <span className="font-mono text-sm font-medium">{row.pid}</span>,
    },
    { key: "userName", header: t("locks.user"), sortable: true },
    { key: "database", header: t("locks.database"), sortable: true },
    {
      key: "queryDuration",
      header: t("locks.duration"),
      sortable: true,
      render: (row) => formatDuration(row.queryDuration),
    },
    { key: "lockType", header: t("locks.lockType"), sortable: true },
    {
      key: "query",
      header: t("locks.query"),
      render: (row) => (
        <span className="block max-w-md truncate font-mono text-xs text-[#a1a1aa]">
          {row.query || t("locks.queryUnavailable")}
        </span>
      ),
    },
    {
      key: "status",
      header: t("locks.status"),
      render: (row) => <StatusBadge status={row.status} />,
    },
    {
      key: "actions",
      header: t("locks.actions"),
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
          {t("locks.kill")}
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      {error && (
        <StatusBanner status="error" title={t("locks.errorBanner")} description={error} />
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <StatCard title={t("locks.totalLocks")} value={data.length} color="bg-[#3b82f6]/10 text-[#3b82f6]" />
        <StatCard
          title={t("locks.blockedSessions")}
          value={data.filter((lock) => lock.status === "blocked").length}
          color="bg-[#ef4444]/10 text-[#ef4444]"
        />
        <StatCard
          title={t("locks.blockingSessions")}
          value={data.filter((lock) => lock.status === "blocking").length}
          color="bg-[#f59e0b]/10 text-[#f59e0b]"
        />
      </div>

      <div className="flex items-center space-x-4">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#71717a]" />
          <Input
            placeholder={t("locks.searchPlaceholder")}
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
            <h3 className="text-sm font-medium text-[#ef4444]">{t("locks.criticalDetected")}</h3>
            <p className="mt-1 text-sm text-[#ef4444]/80">
              {t("locks.criticalCount", { count: data.filter((lock) => lock.severity === "high").length })}
            </p>
          </div>
        </div>
      )}

      <DataTable
        data={filteredLocks}
        columns={columns}
        onRowClick={setSelectedLock}
        emptyMessage={loading ? t("locks.loadingLocks") : t("locks.noLocksFound")}
      />

      <Dialog open={!!selectedLock} onOpenChange={() => setSelectedLock(null)}>
        <DialogContent className="flex max-h-[85vh] max-w-3xl flex-col overflow-hidden border-[#27272a] bg-[#111116] text-white">
          <DialogHeader>
            <DialogTitle>{t("locks.lockDetailsTitle", { pid: selectedLock?.pid })}</DialogTitle>
            <DialogDescription className="text-[#a1a1aa]">
              {t("locks.lockDetailsDesc")}
            </DialogDescription>
          </DialogHeader>
          <div className="min-h-0 space-y-4 overflow-y-auto py-4 pr-2">
            <DetailRow label={t("locks.processId")} value={selectedLock?.pid} />
            <DetailRow label={t("locks.user")} value={selectedLock?.userName} />
            <DetailRow label={t("locks.database")} value={selectedLock?.database} />
            <DetailRow label={t("locks.duration")} value={formatDuration(selectedLock?.queryDuration)} />
            <DetailRow label={t("locks.lockType")} value={selectedLock?.lockType} />
            <DetailRow label={t("locks.state")} value={selectedLock?.state} />
            <DetailRow label={t("locks.status")} value={selectedLock && <StatusBadge status={selectedLock.status} />} />
            <DetailRow label={t("locks.application")} value={selectedLock?.applicationName ?? t("common.na")} />
            <DetailRow label={t("locks.relation")} value={selectedLock?.relation ?? t("common.na")} />
            <DetailRow label={t("locks.client")} value={selectedLock?.clientAddr ?? t("common.na")} />
            <DetailRow label={t("locks.started")} value={formatRelativeTimestamp(selectedLock?.queryStart)} />
            <div className="space-y-2">
              <label className="text-sm font-medium text-[#a1a1aa]">{t("locks.query")}</label>
              <div className="rounded-lg border border-[#27272a] bg-[#0a0a0f] p-4">
                <code className="whitespace-pre-wrap font-mono text-xs text-[#e4e4e7]">
                  {selectedLock?.query || t("locks.queryUnavailable")}
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
              {t("common.close")}
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
              {t("locks.killSession")}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      <AlertDialog open={!!lockToKill} onOpenChange={() => setLockToKill(null)}>
        <AlertDialogContent className="border-[#27272a] bg-[#111116] text-white">
          <AlertDialogHeader>
            <AlertDialogTitle className="flex items-center space-x-2">
              <AlertTriangle className="h-5 w-5 text-[#ef4444]" />
              <span>{t("locks.confirmTitle")}</span>
            </AlertDialogTitle>
            <AlertDialogDescription className="text-[#a1a1aa]">
              {t("locks.confirmDesc", { pid: lockToKill?.pid })}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-[#27272a] text-white hover:bg-[#1f1f28]">
              {t("common.cancel")}
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={() => lockToKill && handleKillSession(lockToKill)}
              className="bg-[#ef4444] text-white hover:bg-[#dc2626]"
            >
              {t("locks.terminateSession")}
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

import { useCallback, useMemo, useState } from "react";
import { useTranslation } from "react-i18next";
import { DataTable, Column } from "../components/DataTable";
import { StatusBadge } from "../components/StatusBadge";
import { StatusBanner } from "../components/StatusBanner";
import { Input } from "../components/ui/input";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "../components/ui/tabs";
import { api } from "../lib/api";
import { durationToMilliseconds, formatCompactNumber } from "../lib/formatters";
import type { QueryStatsResponse, RunningQueryResponse, TopQueryResponse } from "../lib/types";
import { useApiPolling } from "../hooks/useApiPolling";
import { usePageRefresh } from "../hooks/usePageRefresh";
import { Search } from "lucide-react";

type QueryTab = "top" | "slow" | "running";

type QueryRow = TopQueryResponse & {
  database: string;
  status: "healthy" | "warning" | "critical";
};

type RunningQueryRow = RunningQueryResponse & {
  executions: number;
  avgTime: number;
  totalTime: number;
  rows: number;
  status: "healthy" | "warning" | "critical";
};

const emptyQueryStats: QueryStatsResponse = {
  available: true,
  message: null,
  queries: [],
};

async function fetchQueryData() {
  const [top, slow, running] = await Promise.all([
    api.getTopQueries(20),
    api.getSlowQueries(20),
    api.getRunningQueries(30),
  ]);

  return { top, slow, running };
}

function mapQueryStatus(avgTime: number): QueryRow["status"] {
  if (avgTime >= 1000) return "critical";
  if (avgTime >= 250) return "warning";
  return "healthy";
}

function mapQueryRow(query: TopQueryResponse): QueryRow {
  return {
    ...query,
    database: "postgres",
    status: mapQueryStatus(query.meanExecTime),
  };
}

export function Queries() {
  const { t } = useTranslation();
  const { data, loading, error, refresh } = useApiPolling(fetchQueryData, {
    initialData: {
      top: emptyQueryStats,
      slow: emptyQueryStats,
      running: [],
    },
    intervalMs: 15000,
  });
  usePageRefresh(useCallback(() => {
    void refresh();
  }, [refresh]));
  const [searchQuery, setSearchQuery] = useState("");
  const [activeTab, setActiveTab] = useState<QueryTab>("top");

  const topQueries = useMemo(() => data.top.queries.map(mapQueryRow), [data.top.queries]);
  const slowQueries = useMemo(() => data.slow.queries.map(mapQueryRow), [data.slow.queries]);
  const runningQueries = useMemo<RunningQueryRow[]>(
    () =>
      data.running.map((query) => ({
        ...query,
        executions: 1,
        avgTime: durationToMilliseconds(query.duration),
        totalTime: durationToMilliseconds(query.duration),
        rows: 0,
        status: mapQueryStatus(durationToMilliseconds(query.duration)),
      })),
    [data.running],
  );

  const getFilteredData = <T extends { query: string; database: string }>(rows: T[]) =>
    rows.filter(
      (row) =>
        row.query.toLowerCase().includes(searchQuery.toLowerCase()) ||
        row.database.toLowerCase().includes(searchQuery.toLowerCase()),
    );

  const columns: Column<QueryRow | RunningQueryRow>[] = [
    {
      key: "query",
      header: t("queries.database") === "Database" ? "Query" : "Query",
      render: (row) => (
        <span className="block max-w-lg truncate font-mono text-xs text-[#a1a1aa]">{row.query}</span>
      ),
    },
    {
      key: "executions",
      header: t("queries.executions"),
      sortable: true,
      render: (row) => formatCompactNumber("calls" in row ? row.calls : row.executions),
    },
    {
      key: "avgTime",
      header: t("queries.avgTime"),
      sortable: true,
      render: (row) =>
        `${("meanExecTime" in row ? row.meanExecTime : row.avgTime).toFixed(2)}ms`,
    },
    {
      key: "totalTime",
      header: t("queries.totalTime"),
      sortable: true,
      render: (row) =>
        `${((("totalExecTime" in row ? row.totalExecTime : row.totalTime) ?? 0) / 1000).toFixed(2)}s`,
    },
    {
      key: "rows",
      header: t("queries.rows"),
      sortable: true,
      render: (row) => formatCompactNumber("rows" in row ? row.rows : 0),
    },
    {
      key: "database",
      header: t("queries.database"),
      sortable: true,
      render: (row) => ("database" in row ? row.database : "postgres"),
    },
    {
      key: "status",
      header: t("queries.status"),
      render: (row) => <StatusBadge status={row.status} />,
    },
  ];

  const currentAverage = topQueries.length
    ? topQueries.reduce((total, query) => total + query.meanExecTime, 0) / topQueries.length
    : 0;

  return (
    <div className="space-y-6">
      {error && (
        <StatusBanner status="error" title={t("queries.errorBanner")} description={error} />
      )}

      {!data.top.available && data.top.message && (
        <StatusBanner status="warning" title={t("queries.pgStatUnavailable")} description={data.top.message} />
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-4">
        <StatCard title={t("queries.totalQueries")} value={topQueries.length} subtitle={t("queries.topQueriesSubtitle")} />
        <StatCard
          title={t("queries.slowQueries")}
          value={slowQueries.length}
          subtitle={t("queries.slowQueriesSubtitle")}
          highlight="warning"
        />
        <StatCard
          title={t("queries.runningNow")}
          value={runningQueries.length}
          subtitle={t("queries.runningNowSubtitle")}
          highlight="info"
        />
        <StatCard
          title={t("queries.avgResponse")}
          value={`${currentAverage.toFixed(2)}ms`}
          subtitle={t("queries.avgResponseSubtitle")}
          highlight="healthy"
        />
      </div>

      <div className="relative">
        <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-[#71717a]" />
        <Input
          placeholder={t("queries.searchPlaceholder")}
          value={searchQuery}
          onChange={(event) => setSearchQuery(event.target.value)}
          className="border-[#27272a] bg-[#111116] pl-10 text-white"
        />
      </div>

      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as QueryTab)} className="w-full">
        <TabsList className="border border-[#27272a] bg-[#111116]">
          <TabsTrigger value="top" className="data-[state=active]:bg-[#1f1f28]">
            {t("queries.tabTop")}
          </TabsTrigger>
          <TabsTrigger value="slow" className="data-[state=active]:bg-[#1f1f28]">
            {t("queries.tabSlow")}
          </TabsTrigger>
          <TabsTrigger value="running" className="data-[state=active]:bg-[#1f1f28]">
            {t("queries.tabRunning")}
          </TabsTrigger>
        </TabsList>

        <TabsContent value="top" className="mt-4">
          <DataTable
            data={getFilteredData(topQueries)}
            columns={columns}
            emptyMessage={loading ? t("queries.loadingTop") : t("queries.noQueriesFound")}
          />
        </TabsContent>

        <TabsContent value="slow" className="mt-4">
          <div className="mb-4 rounded-lg border border-[#f59e0b]/30 bg-[#f59e0b]/10 p-4">
            <p className="text-sm text-[#f59e0b]">
              {t("queries.slowQueriesWarning")}
            </p>
          </div>
          <DataTable
            data={getFilteredData(slowQueries)}
            columns={columns}
            emptyMessage={loading ? t("queries.loadingSlow") : t("queries.noSlowFound")}
          />
        </TabsContent>

        <TabsContent value="running" className="mt-4">
          <DataTable
            data={getFilteredData(
              runningQueries.map((query) => ({
                ...query,
                database: query.database || "postgres",
              })),
            )}
            columns={columns}
            emptyMessage={loading ? t("queries.loadingRunning") : t("queries.noRunningFound")}
          />
        </TabsContent>
      </Tabs>
    </div>
  );
}

function StatCard({
  title,
  value,
  subtitle,
  highlight,
}: {
  title: string;
  value: string | number;
  subtitle: string;
  highlight?: "healthy" | "warning" | "info";
}) {
  const colors = {
    healthy: "text-[#10b981]",
    warning: "text-[#f59e0b]",
    info: "text-[#3b82f6]",
  };

  return (
    <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
      <p className="mb-2 text-sm text-[#a1a1aa]">{title}</p>
      <p className={`mb-1 text-3xl font-semibold ${highlight ? colors[highlight] : "text-white"}`}>{value}</p>
      <p className="text-xs text-[#71717a]">{subtitle}</p>
    </div>
  );
}

import { useCallback, useMemo } from "react";
import { MetricCard } from "../components/MetricCard";
import { DataTable, Column } from "../components/DataTable";
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../lib/api";
import { buildFlatTrend, formatPercent } from "../lib/formatters";
import type { ConnectionSummaryResponse } from "../lib/types";
import { useApiPolling } from "../hooks/useApiPolling";
import { usePageRefresh } from "../hooks/usePageRefresh";
import { Activity, Cable, Clock, Pause } from "lucide-react";
import {
  CartesianGrid,
  Cell,
  Legend,
  Line,
  LineChart,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

const COLORS = ["#3b82f6", "#8b5cf6", "#10b981", "#f59e0b", "#ef4444"];

const emptySummary: ConnectionSummaryResponse = {
  totalConnections: 0,
  activeConnections: 0,
  idleConnections: 0,
  idleInTransactionConnections: 0,
  maxConnections: 0,
  usagePercent: 0,
  byUser: [],
  byApplication: [],
};

export function Connections() {
  const { data, loading, error, refresh } = useApiPolling(api.getConnectionSummary, {
    initialData: emptySummary,
    intervalMs: 15000,
  });
  usePageRefresh(useCallback(() => {
    void refresh();
  }, [refresh]));

  const connectionsOverTime = useMemo(
    () =>
      buildFlatTrend(data.totalConnections).map((item) => ({
        time: item.time,
        total: data.totalConnections,
        active: data.activeConnections,
        idle: data.idleConnections,
      })),
    [data.activeConnections, data.idleConnections, data.totalConnections],
  );

  const connectionsByUser = data.byUser.map((item) => ({
    user: item.name,
    count: item.connections,
    active: item.connections,
    idle: 0,
  }));

  const connectionsByApp = data.byApplication.map((item) => ({
    application: item.name || "unknown",
    count: item.connections,
    percentage: data.totalConnections > 0 ? Math.round((item.connections / data.totalConnections) * 100) : 0,
  }));

  const userColumns: Column<(typeof connectionsByUser)[number]>[] = [
    { key: "user", header: "User", sortable: true },
    { key: "count", header: "Total", sortable: true },
    {
      key: "active",
      header: "Active",
      sortable: true,
      render: (row) => <span className="text-[#10b981]">{row.active}</span>,
    },
    {
      key: "idle",
      header: "Idle",
      sortable: true,
      render: (row) => <span className="text-[#71717a]">{row.idle}</span>,
    },
  ];

  const appColumns: Column<(typeof connectionsByApp)[number]>[] = [
    { key: "application", header: "Application", sortable: true },
    { key: "count", header: "Connections", sortable: true },
    {
      key: "percentage",
      header: "Percentage",
      sortable: true,
      render: (row) => `${row.percentage}%`,
    },
  ];

  return (
    <div className="space-y-6">
      {error && (
        <StatusBanner status="error" title="Falha ao carregar conexões" description={error} />
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          title="Total Connections"
          value={data.totalConnections}
          change={`${formatPercent(data.usagePercent)} da capacidade`}
          changeType="neutral"
          icon={Cable}
          status="healthy"
        />
        <MetricCard
          title="Active"
          value={data.activeConnections}
          change={`${data.byUser.length} usuários conectados`}
          changeType="positive"
          icon={Activity}
          status="healthy"
        />
        <MetricCard
          title="Idle"
          value={data.idleConnections}
          change="Snapshot atual"
          changeType="neutral"
          icon={Pause}
        />
        <MetricCard
          title="Idle in Transaction"
          value={data.idleInTransactionConnections}
          change="Monitorado pelo backend"
          changeType={data.idleInTransactionConnections > 0 ? "negative" : "positive"}
          icon={Clock}
          status={data.idleInTransactionConnections > 0 ? "warning" : "healthy"}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
          <h3 className="mb-4 text-sm font-medium text-[#a1a1aa]">Connections Snapshot</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={connectionsOverTime}>
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
              <Legend />
              <Line type="monotone" dataKey="total" stroke="#3b82f6" strokeWidth={2} name="Total" />
              <Line type="monotone" dataKey="active" stroke="#10b981" strokeWidth={2} name="Active" />
              <Line type="monotone" dataKey="idle" stroke="#71717a" strokeWidth={2} name="Idle" />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
          <h3 className="mb-4 text-sm font-medium text-[#a1a1aa]">Distribution by Application</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={connectionsByApp}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ application, percentage }) => `${application} (${percentage}%)`}
                outerRadius={100}
                dataKey="count"
              >
                {connectionsByApp.map((entry, index) => (
                  <Cell key={`${entry.application}-${index}`} fill={COLORS[index % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={{
                  backgroundColor: "#111116",
                  border: "1px solid #27272a",
                  borderRadius: "8px",
                  color: "#e4e4e7",
                }}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div>
          <h2 className="mb-3 text-lg font-semibold text-white">Connections by User</h2>
          <DataTable
            data={connectionsByUser}
            columns={userColumns}
            emptyMessage={loading ? "Carregando conexões por usuário..." : "Nenhuma conexão por usuário"}
          />
        </div>
        <div>
          <h2 className="mb-3 text-lg font-semibold text-white">Connections by Application</h2>
          <DataTable
            data={connectionsByApp}
            columns={appColumns}
            emptyMessage={loading ? "Carregando conexões por aplicação..." : "Nenhuma conexão por aplicação"}
          />
        </div>
      </div>
    </div>
  );
}

import { useCallback, useMemo } from "react";
import { MetricCard } from "../components/MetricCard";
import { ProgressBar } from "../components/ProgressBar";
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../lib/api";
import { buildFlatTrend, formatDuration, formatPercent } from "../lib/formatters";
import type { SystemMetricsResponse } from "../lib/types";
import { useApiPolling } from "../hooks/useApiPolling";
import { usePageRefresh } from "../hooks/usePageRefresh";
import { Clock, Cpu, HardDrive, MemoryStick } from "lucide-react";
import {
  Area,
  AreaChart,
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";

const emptyMetrics: SystemMetricsResponse = {
  memory: {
    heapUsedBytes: 0,
    heapMaxBytes: 0,
    nonHeapUsedBytes: 0,
    nonHeapCommittedBytes: 0,
    jvmTotalMemoryBytes: 0,
    jvmFreeMemoryBytes: 0,
  },
  cpu: {
    processCpuUsage: 0,
    systemLoadAverage: 0,
    availableProcessors: 0,
  },
  threads: {
    liveThreads: 0,
    daemonThreads: 0,
    peakThreads: 0,
  },
  uptime: "PT0S",
};

export function SystemMetrics() {
  const { data, loading, error, refresh } = useApiPolling(api.getSystemMetrics, {
    initialData: emptyMetrics,
    intervalMs: 15000,
  });
  usePageRefresh(useCallback(() => {
    void refresh();
  }, [refresh]));

  const cpuUsage = data.cpu.processCpuUsage;
  const memoryUsage = data.memory.jvmTotalMemoryBytes
    ? (data.memory.heapUsedBytes / data.memory.jvmTotalMemoryBytes) * 100
    : 0;

  const cpuData = useMemo(
    () =>
      buildFlatTrend(Number(cpuUsage.toFixed(1))).map((item) => ({
        time: item.time,
        usage: cpuUsage,
        system: Math.max(cpuUsage * 0.35, 0),
        user: Math.max(cpuUsage * 0.65, 0),
      })),
    [cpuUsage],
  );

  const memoryData = useMemo(
    () =>
      buildFlatTrend(Number(memoryUsage.toFixed(1))).map((item) => ({
        time: item.time,
        heap: memoryUsage,
        nonHeap:
          data.memory.nonHeapCommittedBytes > 0
            ? (data.memory.nonHeapUsedBytes / data.memory.nonHeapCommittedBytes) * 100
            : 0,
      })),
    [data.memory.heapUsedBytes, data.memory.jvmTotalMemoryBytes, data.memory.nonHeapUsedBytes, data.memory.nonHeapCommittedBytes, memoryUsage],
  );

  const threadsData = useMemo(
    () =>
      buildFlatTrend(data.threads.liveThreads).map((item) => ({
        time: item.time,
        active: data.threads.liveThreads,
        waiting: data.threads.daemonThreads,
        blocked: Math.max(data.threads.peakThreads - data.threads.liveThreads, 0),
      })),
    [data.threads.daemonThreads, data.threads.liveThreads, data.threads.peakThreads],
  );

  const diskData = useMemo(
    () =>
      buildFlatTrend(Math.round(data.cpu.systemLoadAverage || 0)).map((item) => ({
        time: item.time,
        read: data.cpu.systemLoadAverage || 0,
        write: (data.cpu.systemLoadAverage || 0) * 0.7,
      })),
    [data.cpu.systemLoadAverage],
  );

  return (
    <div className="space-y-6">
      {error && (
        <StatusBanner status="error" title="Falha ao carregar métricas do sistema" description={error} />
      )}

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
        <MetricCard
          title="CPU Usage"
          value={formatPercent(cpuUsage)}
          change={`${data.cpu.availableProcessors} processadores`}
          changeType={cpuUsage >= 70 ? "negative" : "positive"}
          icon={Cpu}
          status={cpuUsage >= 80 ? "critical" : cpuUsage >= 60 ? "warning" : "healthy"}
        />
        <MetricCard
          title="Memory Usage"
          value={formatPercent(memoryUsage)}
          change={`${formatPercent(memoryUsage)} do total JVM`}
          changeType={memoryUsage >= 75 ? "warning" : "positive"}
          icon={MemoryStick}
          status={memoryUsage >= 85 ? "critical" : memoryUsage >= 70 ? "warning" : "healthy"}
        />
        <MetricCard
          title="Active Threads"
          value={data.threads.liveThreads}
          change={`${data.threads.daemonThreads} daemon`}
          changeType="neutral"
          icon={HardDrive}
          status="healthy"
        />
        <MetricCard
          title="Uptime"
          value={formatDuration(data.uptime)}
          change="Desde o último restart"
          changeType="neutral"
          icon={Clock}
          status="healthy"
        />
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <h3 className="mb-4 text-sm font-medium text-[#a1a1aa]">Resource Utilization</h3>
        <div className="space-y-4">
          <ProgressBar value={cpuUsage} label="CPU Usage" />
          <ProgressBar value={memoryUsage} label="Memory Usage" />
          <ProgressBar value={Math.min((data.cpu.systemLoadAverage || 0) * 10, 100)} label="System Load" />
          <ProgressBar
            value={data.threads.peakThreads > 0 ? (data.threads.liveThreads / data.threads.peakThreads) * 100 : 0}
            label="Thread Utilization"
          />
        </div>
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-sm font-medium text-[#a1a1aa]">CPU Usage Snapshot</h3>
        </div>
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={cpuData}>
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
            <Area type="monotone" dataKey="usage" stroke="#3b82f6" fill="#3b82f6" fillOpacity={0.2} name="Total %" />
            <Area type="monotone" dataKey="user" stroke="#10b981" fill="#10b981" fillOpacity={0.1} name="User %" />
            <Area type="monotone" dataKey="system" stroke="#f59e0b" fill="#f59e0b" fillOpacity={0.1} name="System %" />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <h3 className="mb-4 text-sm font-medium text-[#a1a1aa]">Memory Usage Snapshot</h3>
        <ResponsiveContainer width="100%" height={300}>
          <AreaChart data={memoryData}>
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
            <Area type="monotone" dataKey="heap" stackId="1" stroke="#8b5cf6" fill="#8b5cf6" fillOpacity={0.6} name="Heap (%)" />
            <Area
              type="monotone"
              dataKey="nonHeap"
              stackId="1"
              stroke="#3b82f6"
              fill="#3b82f6"
              fillOpacity={0.6}
              name="Non-Heap (%)"
            />
          </AreaChart>
        </ResponsiveContainer>
      </div>

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
        <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
          <h3 className="mb-4 text-sm font-medium text-[#a1a1aa]">Thread Activity</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={threadsData}>
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
              <Line type="monotone" dataKey="active" stroke="#10b981" strokeWidth={2} name="Active" />
              <Line type="monotone" dataKey="waiting" stroke="#f59e0b" strokeWidth={2} name="Daemon" />
              <Line type="monotone" dataKey="blocked" stroke="#ef4444" strokeWidth={2} name="Headroom" />
            </LineChart>
          </ResponsiveContainer>
        </div>

        <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
          <h3 className="mb-4 text-sm font-medium text-[#a1a1aa]">System Load Proxy</h3>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={diskData}>
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
              <Line type="monotone" dataKey="read" stroke="#3b82f6" strokeWidth={2} name="Load" />
              <Line type="monotone" dataKey="write" stroke="#8b5cf6" strokeWidth={2} name="Scaled load" />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <h3 className="mb-4 text-sm font-medium text-[#a1a1aa]">System Information</h3>
        <div className="grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-4">
          <InfoItem label="Available CPUs" value={String(data.cpu.availableProcessors)} />
          <InfoItem label="System Load Avg" value={data.cpu.systemLoadAverage.toFixed(2)} />
          <InfoItem label="Peak Threads" value={String(data.threads.peakThreads)} />
          <InfoItem
            label="Heap Usage"
            value={formatPercent(memoryUsage)}
          />
          <InfoItem
            label="Non-Heap Usage"
            value={formatPercent(
              data.memory.nonHeapCommittedBytes > 0
                ? (data.memory.nonHeapUsedBytes / data.memory.nonHeapCommittedBytes) * 100
                : 0,
            )}
          />
          <InfoItem
            label="JVM Free"
            value={formatPercent(
              data.memory.jvmTotalMemoryBytes > 0
                ? (data.memory.jvmFreeMemoryBytes / data.memory.jvmTotalMemoryBytes) * 100
                : 0,
            )}
          />
          <InfoItem
            label="Heap Headroom"
            value={formatPercent(Math.max(100 - memoryUsage, 0))}
          />
          <InfoItem label="Snapshot" value={loading ? "Carregando..." : "Ativo"} />
        </div>
      </div>
    </div>
  );
}

function InfoItem({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="mb-1 text-xs text-[#71717a]">{label}</p>
      <p className="text-sm font-medium text-white">{value}</p>
    </div>
  );
}

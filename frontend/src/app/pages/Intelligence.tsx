import { useCallback } from "react";
import {
  AlertTriangle,
  BrainCircuit,
  Gauge,
  Radar,
  ShieldCheck,
  Sparkles,
} from "lucide-react";
import { MetricCard } from "../components/MetricCard";
import { HealthIndicator } from "../components/HealthIndicator";
import { StatusBadge } from "../components/StatusBadge";
import { StatusBanner } from "../components/StatusBanner";
import { api } from "../lib/api";
import type {
  AlertItemResponse,
  AnomalyItemResponse,
  DatabaseIntelligenceOverviewResponse,
  RecommendationItemResponse,
  ScoreBreakdownResponse,
} from "../lib/types";
import { formatNumber, formatPercent, formatRelativeTimestamp } from "../lib/formatters";
import { useApiPolling } from "../hooks/useApiPolling";
import { usePageRefresh } from "../hooks/usePageRefresh";

const emptyOverview: DatabaseIntelligenceOverviewResponse = {
  score: {
    score: 0,
    classification: "CRITICAL",
    penalties: [],
    breakdown: [],
    summary: "",
  },
  alerts: [],
  anomalies: [],
  recommendations: [],
  generatedAt: new Date(0).toISOString(),
  anomalyMessage: "Carregando baseline operacional.",
};

export function Intelligence() {
  const { data, loading, error, refresh } = useApiPolling(api.getDbIntelligenceOverview, {
    initialData: emptyOverview,
    intervalMs: 15000,
  });

  usePageRefresh(
    useCallback(() => {
      void refresh();
    }, [refresh]),
  );

  const healthStatus = mapHealthStatus(data.score.classification);
  const highestAlert = data.alerts[0];

  return (
    <div className="space-y-6">
      {error && <StatusBanner status="error" title="Falha ao carregar intelligence" description={error} />}

      <div className="rounded-2xl border border-[#27272a] bg-[radial-gradient(circle_at_top_left,_rgba(59,130,246,0.16),_transparent_35%),linear-gradient(135deg,#111116_0%,#0a0a0f_100%)] p-6">
        <div className="flex flex-col gap-6 lg:flex-row lg:items-center lg:justify-between">
          <div className="max-w-3xl">
            <div className="mb-4 inline-flex items-center gap-2 rounded-full border border-[#3b82f6]/20 bg-[#3b82f6]/10 px-3 py-1 text-xs font-medium uppercase tracking-[0.18em] text-[#93c5fd]">
              <BrainCircuit className="h-3.5 w-3.5" />
              Operational Intelligence
            </div>
            <h2 className="text-2xl font-semibold text-white">Visao analitica e acionavel do PostgreSQL</h2>
            <p className="mt-3 max-w-2xl text-sm leading-7 text-[#a1a1aa]">
              Motor deterministico baseado em score, correlacao de sinais, baseline historica e recomendacoes operacionais.
            </p>
            <p className="mt-3 text-xs uppercase tracking-[0.16em] text-[#71717a]">
              Gerado em {formatRelativeTimestamp(data.generatedAt)}
            </p>
          </div>

          <div className="flex flex-col items-start gap-3 rounded-2xl border border-[#27272a] bg-[#0a0a0f]/80 p-5">
            <HealthIndicator status={healthStatus} size="lg" />
            <div>
              <p className="text-xs uppercase tracking-[0.18em] text-[#71717a]">Health Score</p>
              <p className="mt-1 text-5xl font-semibold text-white">{formatNumber(data.score.score)}</p>
            </div>
            <p className="max-w-sm text-sm leading-6 text-[#a1a1aa]">{data.score.summary || "Aguardando consolidacao do snapshot."}</p>
          </div>
        </div>
      </div>

      <StatusBanner
        status={loading ? "info" : mapBannerStatus(data.score.classification)}
        title={loading ? "Calculando intelligence operacional" : bannerTitle(data)}
        description={
          loading
            ? "Carregando score, alertas, anomalias e recomendacoes do ambiente."
            : highestAlert
              ? `${highestAlert.title}: ${highestAlert.description}`
              : "Nenhum alerta critico ativo no snapshot atual."
        }
      />

      <div className="grid grid-cols-1 gap-4 md:grid-cols-2 xl:grid-cols-4">
        <MetricCard
          title="Score de Saude"
          value={data.score.score}
          change={data.score.classification}
          changeType={healthStatus === "healthy" ? "positive" : "negative"}
          icon={Gauge}
          status={healthStatus}
        />
        <MetricCard
          title="Alertas Ativos"
          value={data.alerts.length}
          change={summarizeAlertMix(data.alerts)}
          changeType={data.alerts.some((item) => item.severity === "CRITICAL") ? "negative" : "neutral"}
          icon={AlertTriangle}
          status={data.alerts.some((item) => item.severity === "CRITICAL") ? "critical" : data.alerts.length > 0 ? "warning" : "healthy"}
        />
        <MetricCard
          title="Anomalias"
          value={data.anomalies.length}
          change={data.anomalyMessage}
          changeType={data.anomalies.length > 0 ? "negative" : "neutral"}
          icon={Radar}
          status={data.anomalies.some((item) => item.severity === "CRITICAL") ? "critical" : data.anomalies.length > 0 ? "warning" : "healthy"}
        />
        <MetricCard
          title="Recomendacoes"
          value={data.recommendations.length}
          change={summarizePriorityMix(data.recommendations)}
          changeType={data.recommendations.some((item) => item.priority === "URGENT") ? "negative" : "neutral"}
          icon={Sparkles}
          status={data.recommendations.some((item) => item.priority === "URGENT") ? "critical" : data.recommendations.length > 0 ? "warning" : "healthy"}
        />
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-[1.35fr_0.95fr]">
        <section className="rounded-xl border border-[#27272a] bg-[#111116] p-5">
          <div className="mb-4 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-[#3b82f6]/10 p-2.5">
                <ShieldCheck className="h-5 w-5 text-[#3b82f6]" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Score Breakdown</h3>
                <p className="text-sm text-[#71717a]">Pontuacao por dominio operacional.</p>
              </div>
            </div>
            <StatusBadge status={healthStatus} label={data.score.classification} />
          </div>

          <div className="space-y-3">
            {data.score.breakdown.map((item) => (
              <BreakdownCard key={item.category} item={item} />
            ))}
          </div>
        </section>

        <section className="rounded-xl border border-[#27272a] bg-[#111116] p-5">
          <div className="mb-4 flex items-center justify-between">
            <div>
              <h3 className="text-lg font-semibold text-white">Penalidades Aplicadas</h3>
              <p className="text-sm text-[#71717a]">Sinais que derrubaram o score neste snapshot.</p>
            </div>
            <span className="text-xs uppercase tracking-[0.16em] text-[#71717a]">{data.score.penalties.length} itens</span>
          </div>

          {data.score.penalties.length === 0 ? (
            <EmptyBlock message="Nenhuma penalidade aplicada no momento." />
          ) : (
            <div className="space-y-3">
              {data.score.penalties.map((penalty) => (
                <div key={penalty.code} className="rounded-xl border border-[#27272a] bg-[#0a0a0f] p-4">
                  <div className="flex items-center justify-between gap-4">
                    <p className="text-sm font-medium text-white">{penalty.code}</p>
                    <span className="rounded-full bg-[#ef4444]/10 px-2.5 py-1 text-xs font-medium text-[#ef4444]">
                      -{penalty.points} pts
                    </span>
                  </div>
                  <p className="mt-2 text-sm leading-6 text-[#a1a1aa]">{penalty.message}</p>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>

      <div className="grid grid-cols-1 gap-4 xl:grid-cols-3">
        <Panel title="Alertas Ativos" subtitle="Eventos acionaveis classificados por severidade.">
          {data.alerts.length === 0 ? (
            <EmptyBlock message="Sem alertas ativos." />
          ) : (
            <div className="space-y-3">
              {data.alerts.map((alert) => (
                <AlertRow key={`${alert.code}-${alert.detectedAt}`} alert={alert} />
              ))}
            </div>
          )}
        </Panel>

        <Panel title="Anomalias" subtitle="Comparacao do snapshot atual com a baseline historica.">
          {!data.anomalies.length ? (
            <EmptyBlock message={data.anomalyMessage} />
          ) : (
            <div className="space-y-3">
              {data.anomalies.map((anomaly) => (
                <AnomalyRow key={anomaly.code} anomaly={anomaly} />
              ))}
            </div>
          )}
        </Panel>

        <Panel title="Recomendacoes" subtitle="Acoes sugeridas a partir da correlacao entre sinais.">
          {data.recommendations.length === 0 ? (
            <EmptyBlock message="Nenhuma recomendacao emitida neste snapshot." />
          ) : (
            <div className="space-y-3">
              {data.recommendations.map((recommendation) => (
                <RecommendationRow key={recommendation.code} recommendation={recommendation} />
              ))}
            </div>
          )}
        </Panel>
      </div>
    </div>
  );
}

function Panel({
  title,
  subtitle,
  children,
}: {
  title: string;
  subtitle: string;
  children: React.ReactNode;
}) {
  return (
    <section className="rounded-xl border border-[#27272a] bg-[#111116] p-5">
      <div className="mb-4">
        <h3 className="text-lg font-semibold text-white">{title}</h3>
        <p className="mt-1 text-sm text-[#71717a]">{subtitle}</p>
      </div>
      {children}
    </section>
  );
}

function BreakdownCard({ item }: { item: ScoreBreakdownResponse }) {
  const status = item.penaltyPoints === 0 ? "healthy" : item.penaltyPoints >= 20 ? "critical" : "warning";
  const progressWidth = `${Math.max(6, item.score)}%`;

  return (
    <div className="rounded-xl border border-[#27272a] bg-[#0a0a0f] p-4">
      <div className="flex items-center justify-between gap-4">
        <div>
          <p className="text-xs uppercase tracking-[0.18em] text-[#71717a]">{item.category}</p>
          <p className="mt-2 text-2xl font-semibold text-white">{item.score}</p>
        </div>
        <StatusBadge status={status} label={item.penaltyPoints === 0 ? "Estavel" : `-${item.penaltyPoints} pts`} />
      </div>
      <div className="mt-4 h-2 overflow-hidden rounded-full bg-[#1f1f28]">
        <div
          className={status === "healthy" ? "h-full rounded-full bg-[#10b981]" : status === "warning" ? "h-full rounded-full bg-[#f59e0b]" : "h-full rounded-full bg-[#ef4444]"}
          style={{ width: progressWidth }}
        />
      </div>
      <p className="mt-3 text-sm leading-6 text-[#71717a]">
        {item.penalties.length === 0
          ? "Nenhuma penalidade detectada nesta categoria."
          : item.penalties.map((penalty) => penalty.message).join(" ")}
      </p>
    </div>
  );
}

function AlertRow({ alert }: { alert: AlertItemResponse }) {
  const status = mapSeverityBadge(alert.severity);

  return (
    <div className="rounded-xl border border-[#27272a] bg-[#0a0a0f] p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-white">{alert.title}</p>
          <p className="mt-1 text-xs uppercase tracking-[0.16em] text-[#71717a]">{alert.code} · {alert.category}</p>
        </div>
        <StatusBadge status={status} label={alert.severity} />
      </div>
      <p className="mt-3 text-sm leading-6 text-[#a1a1aa]">{alert.description}</p>
      <div className="mt-3 rounded-lg border border-[#27272a] bg-[#111116] p-3">
        <p className="text-xs uppercase tracking-[0.16em] text-[#71717a]">Acao sugerida</p>
        <p className="mt-1 text-sm leading-6 text-[#e4e4e7]">{alert.suggestedAction}</p>
      </div>
    </div>
  );
}

function AnomalyRow({ anomaly }: { anomaly: AnomalyItemResponse }) {
  const status = mapSeverityBadge(anomaly.severity);

  return (
    <div className="rounded-xl border border-[#27272a] bg-[#0a0a0f] p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-white">{anomaly.code}</p>
          <p className="mt-1 text-xs uppercase tracking-[0.16em] text-[#71717a]">{anomaly.baseline.metric}</p>
        </div>
        <StatusBadge status={status} label={anomaly.severity} />
      </div>
      <p className="mt-3 text-sm leading-6 text-[#a1a1aa]">{anomaly.message}</p>
      <div className="mt-3 grid grid-cols-2 gap-3 text-sm">
        <MiniMetric label="Atual" value={formatNumber(anomaly.baseline.currentValue)} />
        <MiniMetric label="Media" value={formatNumber(Number(anomaly.baseline.average.toFixed(2)))} />
        <MiniMetric label="Mediana" value={formatNumber(Number(anomaly.baseline.median.toFixed(2)))} />
        <MiniMetric label="Desvio" value={formatPercent(anomaly.baseline.percentDeviation, 1)} />
      </div>
    </div>
  );
}

function RecommendationRow({ recommendation }: { recommendation: RecommendationItemResponse }) {
  const status = mapPriorityBadge(recommendation.priority);

  return (
    <div className="rounded-xl border border-[#27272a] bg-[#0a0a0f] p-4">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-sm font-semibold text-white">{recommendation.title}</p>
          <p className="mt-1 text-xs uppercase tracking-[0.16em] text-[#71717a]">{recommendation.code}</p>
        </div>
        <StatusBadge status={status} label={recommendation.priority} />
      </div>
      <p className="mt-3 text-sm leading-6 text-[#a1a1aa]">{recommendation.description}</p>
      <div className="mt-3 rounded-lg border border-[#27272a] bg-[#111116] p-3">
        <p className="text-xs uppercase tracking-[0.16em] text-[#71717a]">Rationale</p>
        <p className="mt-1 text-sm leading-6 text-[#e4e4e7]">{recommendation.rationale}</p>
      </div>
      <div className="mt-3 space-y-2">
        {recommendation.suggestedSteps.map((step, index) => (
          <div key={`${recommendation.code}-${index}`} className="flex items-start gap-3">
            <span className="mt-0.5 inline-flex h-5 w-5 items-center justify-center rounded-full bg-[#3b82f6]/10 text-[11px] font-semibold text-[#93c5fd]">
              {index + 1}
            </span>
            <p className="text-sm leading-6 text-[#a1a1aa]">{step}</p>
          </div>
        ))}
      </div>
      {recommendation.relatedSignals.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-2">
          {recommendation.relatedSignals.map((signal) => (
            <span key={signal} className="rounded-full border border-[#27272a] bg-[#111116] px-2.5 py-1 text-xs text-[#a1a1aa]">
              {signal}
            </span>
          ))}
        </div>
      )}
    </div>
  );
}

function MiniMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-[#27272a] bg-[#111116] p-3">
      <p className="text-[11px] uppercase tracking-[0.16em] text-[#71717a]">{label}</p>
      <p className="mt-1 text-sm font-medium text-white">{value}</p>
    </div>
  );
}

function EmptyBlock({ message }: { message: string }) {
  return (
    <div className="rounded-xl border border-dashed border-[#27272a] bg-[#0a0a0f] p-8 text-center text-sm leading-6 text-[#71717a]">
      {message}
    </div>
  );
}

function mapHealthStatus(classification: string): "healthy" | "warning" | "critical" {
  if (classification === "HEALTHY") return "healthy";
  if (classification === "WARNING") return "warning";
  return "critical";
}

function mapBannerStatus(classification: string): "success" | "warning" | "error" {
  if (classification === "HEALTHY") return "success";
  if (classification === "WARNING") return "warning";
  return "error";
}

function mapSeverityBadge(severity: string): "healthy" | "warning" | "critical" | "info" {
  if (severity === "CRITICAL") return "critical";
  if (severity === "WARNING") return "warning";
  return "info";
}

function mapPriorityBadge(priority: string): "healthy" | "warning" | "critical" | "info" {
  if (priority === "URGENT") return "critical";
  if (priority === "HIGH" || priority === "MEDIUM") return "warning";
  return "info";
}

function summarizeAlertMix(alerts: AlertItemResponse[]) {
  const critical = alerts.filter((item) => item.severity === "CRITICAL").length;
  const warning = alerts.filter((item) => item.severity === "WARNING").length;
  if (critical > 0) return `${critical} critico(s), ${warning} warning`;
  if (warning > 0) return `${warning} warning(s)`;
  return "Sem alertas relevantes";
}

function summarizePriorityMix(recommendations: RecommendationItemResponse[]) {
  const urgent = recommendations.filter((item) => item.priority === "URGENT").length;
  const high = recommendations.filter((item) => item.priority === "HIGH").length;
  if (urgent > 0) return `${urgent} urgente(s), ${high} high`;
  if (high > 0) return `${high} high priority`;
  return "Sem recomendacoes pendentes";
}

function bannerTitle(data: DatabaseIntelligenceOverviewResponse) {
  if (data.score.classification === "HEALTHY") {
    return "Ambiente estavel no snapshot atual";
  }
  if (data.score.classification === "WARNING") {
    return "Ambiente com sinais de atencao operacional";
  }
  return "Ambiente com risco operacional elevado";
}

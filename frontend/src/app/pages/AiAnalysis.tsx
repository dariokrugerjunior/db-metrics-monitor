import { useCallback, useEffect, useState } from "react";
import { useTranslation } from "react-i18next";
import { Bot, Eye, MessageSquareText, Send, ShieldAlert, Trash2 } from "lucide-react";
import { DataTable, Column } from "../components/DataTable";
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from "../components/ui/dialog";
import { Textarea } from "../components/ui/textarea";
import { api } from "../lib/api";
import type { AiAnalysisHistoryResponse, AiAnalysisResponse } from "../lib/types";
import { formatRelativeTimestamp } from "../lib/formatters";
import { usePageRefresh } from "../hooks/usePageRefresh";

export function AiAnalysis() {
  const { t } = useTranslation();
  const [prompt, setPrompt] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [analysis, setAnalysis] = useState<AiAnalysisResponse | null>(null);
  const [history, setHistory] = useState<AiAnalysisHistoryResponse[]>([]);
  const [selectedHistory, setSelectedHistory] = useState<AiAnalysisHistoryResponse | null>(null);
  const [clearDialogOpen, setClearDialogOpen] = useState(false);
  const [clearingHistory, setClearingHistory] = useState(false);

  const loadHistory = useCallback(async () => {
    try {
      const rows = await api.getAiAnalysisHistory(100);
      setHistory(rows);
    } catch (err) {
      setError(err instanceof Error ? err.message : t("aiAnalysis.loadHistoryError"));
    }
  }, [t]);

  const sendToAi = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.analyzeWithAi({ prompt });
      setAnalysis(response);
      await loadHistory();
    } catch (err) {
      setError(err instanceof Error ? err.message : t("aiAnalysis.analyzeError"));
    } finally {
      setLoading(false);
    }
  }, [loadHistory, prompt, t]);

  const clearHistory = useCallback(async () => {
    setClearingHistory(true);
    setError(null);
    try {
      await api.clearAiAnalysisHistory();
      setHistory([]);
      setSelectedHistory(null);
      setClearDialogOpen(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : t("aiAnalysis.clearHistoryError"));
    } finally {
      setClearingHistory(false);
    }
  }, [t]);

  usePageRefresh(
    useCallback(() => {
      void sendToAi();
    }, [sendToAi]),
  );

  useEffect(() => {
    void loadHistory();
  }, [loadHistory]);

  const historyColumns: Column<AiAnalysisHistoryResponse>[] = [
    {
      key: "createdAt",
      header: t("aiAnalysis.moment"),
      sortable: true,
      render: (row) => formatRelativeTimestamp(row.createdAt),
    },
    {
      key: "model",
      header: t("aiAnalysis.model"),
      sortable: true,
    },
    {
      key: "userPrompt",
      header: t("aiAnalysis.message"),
      render: (row) => (
        <span className="block max-w-md truncate text-sm text-[#a1a1aa]">
          {row.userPrompt || t("aiAnalysis.noPrompt")}
        </span>
      ),
    },
    {
      key: "actions",
      header: t("aiAnalysis.details"),
      render: (row) => (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setSelectedHistory(row)}
          className="text-[#3b82f6] hover:bg-[#3b82f6]/10 hover:text-[#3b82f6]"
        >
          <Eye className="mr-2 h-4 w-4" />
          {t("aiAnalysis.viewInfo")}
        </Button>
      ),
    },
  ];

  return (
    <div className="space-y-6">
      <div className="rounded-xl border border-[#27272a] bg-gradient-to-br from-[#111116] to-[#0a0a0f] p-6">
        <div className="flex items-start justify-between gap-6">
          <div>
            <div className="flex items-center gap-3">
              <div className="rounded-lg bg-[#3b82f6]/10 p-3">
                <Bot className="h-6 w-6 text-[#3b82f6]" />
              </div>
              <div>
                <h2 className="text-xl font-semibold text-white">{t("aiAnalysis.title")}</h2>
                <p className="text-sm text-[#a1a1aa]">
                  {t("aiAnalysis.description")}
                </p>
              </div>
            </div>
          </div>
          <div className="rounded-lg border border-[#f59e0b]/30 bg-[#f59e0b]/10 px-4 py-3 text-sm text-[#f59e0b]">
            {t("aiAnalysis.dbNotice")}
          </div>
        </div>
      </div>

      {loading && (
        <StatusBanner
          status="info"
          title={t("aiAnalysis.analyzingBanner")}
          description={t("aiAnalysis.analyzingDesc")}
        />
      )}

      {error && <StatusBanner status="error" title={t("aiAnalysis.errorBanner")} description={error} />}

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <div className="mb-4 flex items-center gap-3">
          <MessageSquareText className="h-5 w-5 text-[#3b82f6]" />
          <h3 className="text-lg font-semibold text-white">{t("aiAnalysis.promptLabel")}</h3>
        </div>
        <Textarea
          value={prompt}
          onChange={(event) => setPrompt(event.target.value)}
          placeholder={t("aiAnalysis.promptPlaceholder")}
          className="min-h-36 border-[#27272a] bg-[#0a0a0f] text-white"
        />
        <div className="mt-4 flex items-center justify-between">
          <p className="text-sm text-[#71717a]">
            {t("aiAnalysis.promptHint")}
          </p>
          <Button
            type="button"
            onClick={() => void sendToAi()}
            disabled={loading}
            className="bg-[#3b82f6] text-white hover:bg-[#2563eb]"
          >
            <Send className="mr-2 h-4 w-4" />
            {loading ? t("aiAnalysis.sending") : t("aiAnalysis.sendButton")}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <InsightCard
          title={t("aiAnalysis.sourceCard")}
          value={t("aiAnalysis.sourceValue")}
          description={t("aiAnalysis.sourceDesc")}
          icon={<Bot className="h-5 w-5 text-[#3b82f6]" />}
        />
        <InsightCard
          title={t("aiAnalysis.modelCard")}
          value={analysis?.model ?? t("aiAnalysis.modelPending")}
          description={t("aiAnalysis.modelDesc")}
          icon={<MessageSquareText className="h-5 w-5 text-[#10b981]" />}
        />
        <InsightCard
          title={t("aiAnalysis.lastAnalysisCard")}
          value={analysis ? formatRelativeTimestamp(analysis.generatedAt) : t("common.na")}
          description={t("aiAnalysis.lastAnalysisDesc")}
          icon={<ShieldAlert className="h-5 w-5 text-[#f59e0b]" />}
        />
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-white">{t("aiAnalysis.responseTitle")}</h3>
          <span className="text-xs text-[#71717a]">
            {error ? t("aiAnalysis.statusError") : analysis ? t("aiAnalysis.statusUpdated") : t("aiAnalysis.statusPending")}
          </span>
        </div>

        {error ? (
          <div className="rounded-lg border border-[#ef4444]/30 bg-[#ef4444]/10 p-4">
            <p className="text-sm font-medium text-[#ef4444]">{t("aiAnalysis.responseError")}</p>
            <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-[#f5b4b4]">{error}</p>
          </div>
        ) : analysis ? (
          <div className="space-y-4">
            <div className="rounded-lg border border-[#27272a] bg-[#0a0a0f] p-4">
              <p className="whitespace-pre-wrap text-sm leading-7 text-[#e4e4e7]">{analysis.analysis}</p>
            </div>
            {analysis.prompt && (
              <div>
                <p className="mb-2 text-xs uppercase tracking-wide text-[#71717a]">{t("aiAnalysis.promptSent")}</p>
                <div className="rounded-lg border border-[#27272a] bg-[#0a0a0f] p-4 text-sm text-[#a1a1aa]">
                  {analysis.prompt}
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="rounded-lg border border-dashed border-[#27272a] bg-[#0a0a0f] p-10 text-center text-[#71717a]">
            {t("aiAnalysis.clickToSend")}
          </div>
        )}
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-white">{t("aiAnalysis.historyTitle")}</h3>
          <div className="flex items-center gap-3">
            <span className="text-xs text-[#71717a]">{t("aiAnalysis.historySubtitle")}</span>
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setClearDialogOpen(true)}
              disabled={history.length === 0 || clearingHistory}
              className="border-[#ef4444]/30 text-[#fca5a5] hover:bg-[#ef4444]/10 hover:text-[#fecaca]"
            >
              <Trash2 className="mr-2 h-4 w-4" />
              {clearingHistory ? t("aiAnalysis.clearing") : t("aiAnalysis.clearHistory")}
            </Button>
          </div>
        </div>
        <DataTable
          data={history}
          columns={historyColumns}
          emptyMessage={t("aiAnalysis.noHistory")}
        />
      </div>

      <Dialog open={!!selectedHistory} onOpenChange={() => setSelectedHistory(null)}>
        <DialogContent className="flex max-h-[85vh] max-w-4xl flex-col overflow-hidden border-[#27272a] bg-[#111116] text-white">
          <DialogHeader>
            <DialogTitle>{t("aiAnalysis.detailsTitle")}</DialogTitle>
            <DialogDescription className="text-[#a1a1aa]">
              {t("aiAnalysis.detailsDesc", {
                timestamp: selectedHistory ? formatRelativeTimestamp(selectedHistory.createdAt) : "",
              })}
            </DialogDescription>
          </DialogHeader>

          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto py-4 pr-2">
            <DetailBlock title={t("aiAnalysis.dbUrlBlock")} content={selectedHistory?.dbUrlAdmin ?? ""} />
            <DetailBlock title={t("aiAnalysis.promptBlock")} content={selectedHistory?.userPrompt || t("aiAnalysis.noPrompt")} />
            <DetailBlock title={t("aiAnalysis.finalPromptBlock")} content={selectedHistory?.finalPrompt ?? ""} mono />
            <DetailBlock title={t("aiAnalysis.responseBlock")} content={selectedHistory?.analysis ?? ""} />
          </div>
        </DialogContent>
      </Dialog>

      <AlertDialog open={clearDialogOpen} onOpenChange={setClearDialogOpen}>
        <AlertDialogContent className="border-[#27272a] bg-[#111116] text-white">
          <AlertDialogHeader>
            <AlertDialogTitle>{t("aiAnalysis.clearDialogTitle")}</AlertDialogTitle>
            <AlertDialogDescription className="text-[#a1a1aa]">
              {t("aiAnalysis.clearDialogDesc")}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel className="border-[#27272a] bg-[#0a0a0f] text-white hover:bg-[#1f1f28]">
              {t("common.cancel")}
            </AlertDialogCancel>
            <AlertDialogAction
              onClick={() => void clearHistory()}
              className="bg-[#ef4444] text-white hover:bg-[#dc2626]"
            >
              {t("aiAnalysis.deleteHistory")}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  );
}

function InsightCard({
  title,
  value,
  description,
  icon,
}: {
  title: string;
  value: string;
  description: string;
  icon: React.ReactNode;
}) {
  return (
    <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
      <div className="mb-4 flex items-center justify-between">
        <p className="text-sm text-[#a1a1aa]">{title}</p>
        {icon}
      </div>
      <p className="text-xl font-semibold text-white">{value}</p>
      <p className="mt-2 text-sm leading-6 text-[#71717a]">{description}</p>
    </div>
  );
}

function DetailBlock({
  title,
  content,
  mono = false,
}: {
  title: string;
  content: string;
  mono?: boolean;
}) {
  return (
    <div>
      <p className="mb-2 text-xs uppercase tracking-wide text-[#71717a]">{title}</p>
      <div className="rounded-lg border border-[#27272a] bg-[#0a0a0f] p-4">
        <p className={`whitespace-pre-wrap text-sm leading-7 text-[#e4e4e7] ${mono ? "font-mono text-xs" : ""}`}>
          {content}
        </p>
      </div>
    </div>
  );
}

import { useCallback, useEffect, useState } from "react";
import { Bot, Eye, MessageSquareText, Send, ShieldAlert } from "lucide-react";
import { DataTable, Column } from "../components/DataTable";
import { StatusBanner } from "../components/StatusBanner";
import { Button } from "../components/ui/button";
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
  const [prompt, setPrompt] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [analysis, setAnalysis] = useState<AiAnalysisResponse | null>(null);
  const [history, setHistory] = useState<AiAnalysisHistoryResponse[]>([]);
  const [selectedHistory, setSelectedHistory] = useState<AiAnalysisHistoryResponse | null>(null);

  const loadHistory = useCallback(async () => {
    try {
      const rows = await api.getAiAnalysisHistory(100);
      setHistory(rows);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao carregar histórico de IA.");
    }
  }, []);

  const sendToAi = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.analyzeWithAi({ prompt });
      setAnalysis(response);
      await loadHistory();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Falha ao analisar com IA.");
    } finally {
      setLoading(false);
    }
  }, [loadHistory, prompt]);

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
      header: "Momento",
      sortable: true,
      render: (row) => formatRelativeTimestamp(row.createdAt),
    },
    {
      key: "model",
      header: "Modelo",
      sortable: true,
    },
    {
      key: "userPrompt",
      header: "Mensagem",
      render: (row) => (
        <span className="block max-w-md truncate text-sm text-[#a1a1aa]">
          {row.userPrompt || "Sem prompt complementar"}
        </span>
      ),
    },
    {
      key: "actions",
      header: "Detalhes",
      render: (row) => (
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={() => setSelectedHistory(row)}
          className="text-[#3b82f6] hover:bg-[#3b82f6]/10 hover:text-[#3b82f6]"
        >
          <Eye className="mr-2 h-4 w-4" />
          Ver informações
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
                <h2 className="text-xl font-semibold text-white">Análise IA</h2>
                <p className="text-sm text-[#a1a1aa]">
                  Envia o snapshot atual do banco para a OpenAI e retorna um parecer operacional em português.
                </p>
              </div>
            </div>
          </div>
          <div className="rounded-lg border border-[#f59e0b]/30 bg-[#f59e0b]/10 px-4 py-3 text-sm text-[#f59e0b]">
            O histórico exibido abaixo é filtrado pelo DB_URL_ADMIN atual.
          </div>
        </div>
      </div>

      {loading && (
        <StatusBanner
          status="info"
          title="Análise em andamento"
          description="Se já existir outra análise rodando para este banco, esta requisição vai aguardar a anterior terminar antes de iniciar."
        />
      )}

      {error && <StatusBanner status="error" title="Falha ao consultar a IA" description={error} />}

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <div className="mb-4 flex items-center gap-3">
          <MessageSquareText className="h-5 w-5 text-[#3b82f6]" />
          <h3 className="text-lg font-semibold text-white">Prompt complementar</h3>
        </div>
        <Textarea
          value={prompt}
          onChange={(event) => setPrompt(event.target.value)}
          placeholder="Ex.: verifique se existe risco de saturação por conexões, locks ou queries lentas."
          className="min-h-36 border-[#27272a] bg-[#0a0a0f] text-white"
        />
        <div className="mt-4 flex items-center justify-between">
          <p className="text-sm text-[#71717a]">
            Se você deixar vazio, a IA usa apenas o contexto operacional coletado pelo backend.
          </p>
          <Button
            type="button"
            onClick={() => void sendToAi()}
            disabled={loading}
            className="bg-[#3b82f6] text-white hover:bg-[#2563eb]"
          >
            <Send className="mr-2 h-4 w-4" />
            {loading ? "Enviando..." : "Enviar para IA"}
          </Button>
        </div>
      </div>

      <div className="grid grid-cols-1 gap-4 md:grid-cols-3">
        <InsightCard
          title="Fonte"
          value="OpenAI API"
          description="Análise baseada no snapshot atual do banco e histórico local."
          icon={<Bot className="h-5 w-5 text-[#3b82f6]" />}
        />
        <InsightCard
          title="Modelo"
          value={analysis?.model ?? "Aguardando"}
          description="Modelo configurado no backend via APP_OPENAI_MODEL."
          icon={<MessageSquareText className="h-5 w-5 text-[#10b981]" />}
        />
        <InsightCard
          title="Última análise"
          value={analysis ? formatRelativeTimestamp(analysis.generatedAt) : "N/A"}
          description="Resultado mais recente gerado pela integração."
          icon={<ShieldAlert className="h-5 w-5 text-[#f59e0b]" />}
        />
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-white">Resposta da IA</h3>
          <span className="text-xs text-[#71717a]">
            {error ? "Erro" : analysis ? "Atualizado" : "Ainda não executado"}
          </span>
        </div>

        {error ? (
          <div className="rounded-lg border border-[#ef4444]/30 bg-[#ef4444]/10 p-4">
            <p className="text-sm font-medium text-[#ef4444]">Não foi possível obter a resposta da IA.</p>
            <p className="mt-2 whitespace-pre-wrap text-sm leading-7 text-[#f5b4b4]">{error}</p>
          </div>
        ) : analysis ? (
          <div className="space-y-4">
            <div className="rounded-lg border border-[#27272a] bg-[#0a0a0f] p-4">
              <p className="whitespace-pre-wrap text-sm leading-7 text-[#e4e4e7]">{analysis.analysis}</p>
            </div>
            {analysis.prompt && (
              <div>
                <p className="mb-2 text-xs uppercase tracking-wide text-[#71717a]">Prompt complementar enviado</p>
                <div className="rounded-lg border border-[#27272a] bg-[#0a0a0f] p-4 text-sm text-[#a1a1aa]">
                  {analysis.prompt}
                </div>
              </div>
            )}
          </div>
        ) : (
          <div className="rounded-lg border border-dashed border-[#27272a] bg-[#0a0a0f] p-10 text-center text-[#71717a]">
            Clique em <span className="text-white">Enviar para IA</span> para receber um parecer do estado atual do banco.
          </div>
        )}
      </div>

      <div className="rounded-lg border border-[#27272a] bg-[#111116] p-5">
        <div className="mb-4 flex items-center justify-between">
          <h3 className="text-lg font-semibold text-white">Histórico de chats IA</h3>
          <span className="text-xs text-[#71717a]">Apenas do DB_URL_ADMIN atual</span>
        </div>
        <DataTable
          data={history}
          columns={historyColumns}
          emptyMessage="Nenhum chat IA salvo para este DB_URL_ADMIN"
        />
      </div>

      <Dialog open={!!selectedHistory} onOpenChange={() => setSelectedHistory(null)}>
        <DialogContent className="flex max-h-[85vh] max-w-4xl flex-col overflow-hidden border-[#27272a] bg-[#111116] text-white">
          <DialogHeader>
            <DialogTitle>Detalhes do chat IA</DialogTitle>
            <DialogDescription className="text-[#a1a1aa]">
              Histórico salvo para o DB_URL_ADMIN atual em {selectedHistory ? formatRelativeTimestamp(selectedHistory.createdAt) : ""}
            </DialogDescription>
          </DialogHeader>

          <div className="min-h-0 flex-1 space-y-4 overflow-y-auto py-4 pr-2">
            <DetailBlock title="DB_URL_ADMIN" content={selectedHistory?.dbUrlAdmin ?? ""} />
            <DetailBlock title="Prompt complementar enviado" content={selectedHistory?.userPrompt || "Sem prompt complementar"} />
            <DetailBlock title="Prompt final enviado para a OpenAI" content={selectedHistory?.finalPrompt ?? ""} mono />
            <DetailBlock title="Resposta da IA" content={selectedHistory?.analysis ?? ""} />
          </div>
        </DialogContent>
      </Dialog>
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

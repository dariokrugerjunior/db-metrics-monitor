import { useEffect, useMemo, useState } from "react";
import {
  CheckCircle2,
  Database,
  Eye,
  EyeOff,
  KeyRound,
  RotateCcw,
  Save,
  ServerCog,
  ShieldCheck,
} from "lucide-react";
import { toast } from "sonner";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { api } from "../lib/api";
import type { AppConfigurationUpdateRequest, DatabaseConnectionTestResponse } from "../lib/types";

type SettingsForm = {
  dbUrl: string;
  dbUser: string;
  dbPassword: string;
  openAiApiKey: string;
  openAiMaxOutputTokens: string;
};

const defaultValues: SettingsForm = {
  dbUrl: "jdbc:postgresql://localhost:5432/observability",
  dbUser: "postgres",
  dbPassword: "",
  openAiApiKey: "",
  openAiMaxOutputTokens: "900",
};

export function Settings() {
  const [form, setForm] = useState<SettingsForm>(defaultValues);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [activeDatasourceUrl, setActiveDatasourceUrl] = useState("");
  const [restartRequired, setRestartRequired] = useState(false);
  const [showDbPassword, setShowDbPassword] = useState(false);
  const [showApiKey, setShowApiKey] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [testing, setTesting] = useState(false);
  const [connectionTest, setConnectionTest] = useState<DatabaseConnectionTestResponse | null>(null);

  useEffect(() => {
    const loadConfiguration = async () => {
      try {
        const response = await api.getConfiguration();
        setForm({
          dbUrl: response.dbUrl,
          dbUser: response.dbUser,
          dbPassword: response.dbPassword,
          openAiApiKey: response.appOpenAiApiKey,
          openAiMaxOutputTokens: String(response.appOpenAiMaxOutputTokens),
        });
        setSavedAt(response.savedAt);
        setActiveDatasourceUrl(response.activeDatasourceUrl);
        setRestartRequired(response.restartRequired);
      } catch (error) {
        toast.error(error instanceof Error ? error.message : "Falha ao carregar configuracao.");
      } finally {
        setLoading(false);
      }
    };

    void loadConfiguration();
  }, []);

  const completion = useMemo(() => {
    const requiredFields = [
      form.dbUrl,
      form.dbUser,
      form.dbPassword,
      form.openAiApiKey,
      form.openAiMaxOutputTokens,
    ];
    const filled = requiredFields.filter((value) => value.trim().length > 0).length;
    return Math.round((filled / requiredFields.length) * 100);
  }, [form]);

  const tokenCount = Number(form.openAiMaxOutputTokens);
  const tokenFieldInvalid =
    form.openAiMaxOutputTokens.trim().length > 0 &&
    (!Number.isInteger(tokenCount) || tokenCount <= 0);

  const handleChange = (field: keyof SettingsForm, value: string) => {
    setForm((current) => ({
      ...current,
      [field]: value,
    }));
  };

  const handleSave = () => {
    if (loading || saving) {
      return;
    }

    if (tokenFieldInvalid) {
      toast.error("APP_OPENAI_MAX_OUTPUT_TOKENS deve ser um inteiro positivo.");
      return;
    }

    const payload: AppConfigurationUpdateRequest = {
      dbUrl: form.dbUrl.trim(),
      dbUser: form.dbUser.trim(),
      dbPassword: form.dbPassword,
      appOpenAiApiKey: form.openAiApiKey.trim(),
      appOpenAiMaxOutputTokens: Number(form.openAiMaxOutputTokens),
    };

    setSaving(true);
    void api
      .updateConfiguration(payload)
      .then((response) => {
        setSavedAt(response.savedAt);
        setActiveDatasourceUrl(response.activeDatasourceUrl);
        setRestartRequired(response.restartRequired);
        toast.success(
          response.restartRequired
            ? "Configuracao salva. Reinicie o backend para aplicar o novo datasource."
            : "Configuracao salva e aplicada ao runtime atual.",
        );
      })
      .catch((error) => {
        toast.error(error instanceof Error ? error.message : "Falha ao salvar configuracao.");
      })
      .finally(() => {
        setSaving(false);
      });
  };

  const handleReset = () => {
    setForm(defaultValues);
    setSavedAt(null);
    setConnectionTest(null);
    setRestartRequired(false);
    toast.success("Formulario resetado. Use Salvar para persistir no backend.");
  };

  const handleTestConnection = () => {
    if (testing || loading) {
      return;
    }

    setTesting(true);
    setConnectionTest(null);
    void api
      .testDatabaseConnection({
        dbUrl: form.dbUrl.trim(),
        dbUser: form.dbUser.trim(),
        dbPassword: form.dbPassword,
      })
      .then((response) => {
        setConnectionTest(response);
        if (response.success) {
          toast.success("Conexao validada com sucesso.");
          return;
        }
        toast.error(response.message);
      })
      .catch((error) => {
        toast.error(error instanceof Error ? error.message : "Falha ao testar conexao.");
      })
      .finally(() => {
        setTesting(false);
      });
  };

  const environmentPreview = [
    `DB_URL_ADMIN=${form.dbUrl || "<defina o DB URL>"}`,
    `DB_USER=${form.dbUser || "<defina o DB user>"}`,
    `DB_PASSWORD=${form.dbPassword ? "********" : "<defina o DB password>"}`,
    `APP_OPENAI_API_KEY=${form.openAiApiKey ? "********" : "<defina a API key>"}`,
    `APP_OPENAI_MAX_OUTPUT_TOKENS=${form.openAiMaxOutputTokens || "<defina os tokens>"}`,
  ].join("\n");

  return (
    <div className="space-y-6">
      <section className="overflow-hidden rounded-2xl border border-[#27272a] bg-[radial-gradient(circle_at_top_left,_rgba(59,130,246,0.24),_transparent_35%),linear-gradient(135deg,#111116_0%,#0a0a0f_60%,#101826_100%)]">
        <div className="grid gap-6 px-6 py-6 lg:grid-cols-[minmax(0,1.6fr)_320px] lg:px-8">
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <div className="rounded-xl border border-[#3b82f6]/20 bg-[#3b82f6]/10 p-3">
                <ServerCog className="h-6 w-6 text-[#93c5fd]" />
              </div>
              <div>
                <h2 className="text-2xl font-semibold text-white">Configuracao do ambiente</h2>
                <p className="text-sm text-[#cbd5e1]">
                  Centralize os parametros do banco e da integracao OpenAI antes de subir o backend.
                </p>
              </div>
            </div>

            <div className="grid gap-3 text-sm text-[#a1a1aa] md:grid-cols-3">
              <Highlight icon={<Database className="h-4 w-4 text-[#60a5fa]" />} label="Banco" value="DB_URL_ADMIN, DB_USER e DB_PASSWORD" />
              <Highlight icon={<KeyRound className="h-4 w-4 text-[#34d399]" />} label="IA" value="APP_OPENAI_API_KEY e limite de tokens" />
              <Highlight icon={<ShieldCheck className="h-4 w-4 text-[#fbbf24]" />} label="Persistencia" value="Salva localmente no navegador por enquanto" />
            </div>
          </div>

          <div className="rounded-2xl border border-white/10 bg-black/20 p-5 backdrop-blur-sm">
            <div className="flex items-center justify-between">
              <p className="text-xs uppercase tracking-[0.18em] text-[#71717a]">Prontidao</p>
              <span className="text-sm font-semibold text-white">{completion}%</span>
            </div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/10">
              <div className="h-full rounded-full bg-gradient-to-r from-[#3b82f6] via-[#22c55e] to-[#f59e0b]" style={{ width: `${completion}%` }} />
            </div>
            <div className="mt-5 space-y-3 text-sm">
              <StatusLine label="DB URL" ready={Boolean(form.dbUrl.trim())} />
              <StatusLine label="DB user" ready={Boolean(form.dbUser.trim())} />
              <StatusLine label="DB password" ready={Boolean(form.dbPassword.trim())} />
              <StatusLine label="OpenAI API key" ready={Boolean(form.openAiApiKey.trim())} />
              <StatusLine
                label="Max output tokens"
                ready={Boolean(form.openAiMaxOutputTokens.trim()) && !tokenFieldInvalid}
              />
            </div>
            <p className="mt-5 text-xs leading-5 text-[#71717a]">
              {loading ? "Carregando configuracao do backend..." : `Ultimo salvamento: ${savedAt ? formatSavedAt(savedAt) : "ainda nao salvo"}`}
            </p>
          </div>
        </div>
      </section>

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.6fr)_minmax(320px,0.9fr)]">
        <section className="rounded-2xl border border-[#27272a] bg-[#111116] p-6">
          <div className="mb-6 flex items-start justify-between gap-4">
            <div>
              <h3 className="text-lg font-semibold text-white">Variaveis principais</h3>
              <p className="mt-1 text-sm text-[#71717a]">
                Use os mesmos nomes esperados no backend Spring Boot.
              </p>
            </div>

            <div className="flex flex-wrap gap-3">
              <Button type="button" variant="outline" onClick={handleReset}>
                <RotateCcw className="h-4 w-4" />
                Limpar
              </Button>
              <Button type="button" variant="outline" onClick={handleTestConnection} disabled={testing || loading}>
                <Database className="h-4 w-4" />
                {testing ? "Testando..." : "Testar conexao"}
              </Button>
              <Button type="button" onClick={handleSave} className="bg-[#3b82f6] text-white hover:bg-[#2563eb]">
                <Save className="h-4 w-4" />
                {saving ? "Salvando..." : "Salvar"}
              </Button>
            </div>
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <FieldShell
              id="db-url"
              label="db-url"
              envName="DB_URL_ADMIN"
              hint="Ex.: jdbc:postgresql://host:5432/observability"
            >
              <Input
                id="db-url"
                value={form.dbUrl}
                onChange={(event) => handleChange("dbUrl", event.target.value)}
                placeholder="jdbc:postgresql://localhost:5432/observability"
                className="h-11 border-[#27272a] bg-[#0a0a0f] text-white"
              />
            </FieldShell>

            <FieldShell
              id="db-user"
              label="DB_USER"
              envName="DB_USER"
              hint="Usuario usado pelo datasource principal."
            >
              <Input
                id="db-user"
                value={form.dbUser}
                onChange={(event) => handleChange("dbUser", event.target.value)}
                placeholder="postgres"
                className="h-11 border-[#27272a] bg-[#0a0a0f] text-white"
              />
            </FieldShell>

            <FieldShell
              id="db-password"
              label="db-password"
              envName="DB_PASSWORD"
              hint="Mantenha protegido; a tela mascara o valor."
            >
              <div className="relative">
                <Input
                  id="db-password"
                  type={showDbPassword ? "text" : "password"}
                  value={form.dbPassword}
                  onChange={(event) => handleChange("dbPassword", event.target.value)}
                  placeholder="••••••••"
                  className="h-11 border-[#27272a] bg-[#0a0a0f] pr-11 text-white"
                />
                <button
                  type="button"
                  onClick={() => setShowDbPassword((current) => !current)}
                  className="absolute inset-y-0 right-0 flex items-center px-3 text-[#71717a] transition hover:text-white"
                  aria-label={showDbPassword ? "Ocultar senha" : "Mostrar senha"}
                >
                  {showDbPassword ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </FieldShell>

            <FieldShell
              id="openai-api-key"
              label="APP_OPENAI_API_KEY"
              envName="APP_OPENAI_API_KEY"
              hint="Chave usada pela pagina de Analise IA."
            >
              <div className="relative">
                <Input
                  id="openai-api-key"
                  type={showApiKey ? "text" : "password"}
                  value={form.openAiApiKey}
                  onChange={(event) => handleChange("openAiApiKey", event.target.value)}
                  placeholder="sk-proj-..."
                  className="h-11 border-[#27272a] bg-[#0a0a0f] pr-11 text-white"
                />
                <button
                  type="button"
                  onClick={() => setShowApiKey((current) => !current)}
                  className="absolute inset-y-0 right-0 flex items-center px-3 text-[#71717a] transition hover:text-white"
                  aria-label={showApiKey ? "Ocultar API key" : "Mostrar API key"}
                >
                  {showApiKey ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                </button>
              </div>
            </FieldShell>

            <FieldShell
              id="openai-max-output-tokens"
              label="APP_OPENAI_MAX_OUTPUT_TOKENS"
              envName="APP_OPENAI_MAX_OUTPUT_TOKENS"
              hint="Inteiro positivo. O backend usa 900 por padrao."
              className="md:col-span-2"
            >
              <Input
                id="openai-max-output-tokens"
                inputMode="numeric"
                value={form.openAiMaxOutputTokens}
                onChange={(event) => handleChange("openAiMaxOutputTokens", event.target.value)}
                placeholder="900"
                aria-invalid={tokenFieldInvalid}
                className="h-11 border-[#27272a] bg-[#0a0a0f] text-white"
              />
              <p className={`mt-2 text-xs ${tokenFieldInvalid ? "text-[#ef4444]" : "text-[#71717a]"}`}>
                {tokenFieldInvalid
                  ? "Informe um numero inteiro maior que zero."
                  : "Defina um teto conservador para evitar respostas excessivamente longas."}
              </p>
            </FieldShell>
          </div>
        </section>

        <section className="space-y-6">
          <div className="rounded-2xl border border-[#27272a] bg-[#111116] p-6">
            <div className="mb-4 flex items-center gap-3">
              <div className="rounded-xl bg-[#3b82f6]/10 p-3">
                <CheckCircle2 className="h-5 w-5 text-[#60a5fa]" />
              </div>
              <div>
                <h3 className="text-lg font-semibold text-white">Preview de ambiente</h3>
                <p className="text-sm text-[#71717a]">
                  Referencia rapida para replicar em `.env`, Docker ou variaveis do host.
                </p>
              </div>
            </div>
            <pre className="overflow-x-auto rounded-xl border border-[#27272a] bg-[#0a0a0f] p-4 text-xs leading-6 text-[#cbd5e1]">
              <code>{environmentPreview}</code>
            </pre>
          </div>

          <div className="rounded-2xl border border-[#27272a] bg-[#111116] p-6">
            <h3 className="text-lg font-semibold text-white">Estado de aplicacao</h3>
            <div className="mt-4 space-y-3 text-sm text-[#a1a1aa]">
              <StatusLine label="OpenAI em runtime" ready={Boolean(form.openAiApiKey.trim()) && !loading} />
              <StatusLine label="Datasource atual" ready={!restartRequired} />
            </div>
            <div className="mt-4 rounded-xl border border-[#27272a] bg-[#0a0a0f] p-4">
              <p className="text-xs uppercase tracking-[0.18em] text-[#71717a]">Datasource ativo</p>
              <p className="mt-2 break-all text-sm text-[#e4e4e7]">{activeDatasourceUrl || "Nao carregado"}</p>
              <p className={`mt-3 text-xs leading-5 ${restartRequired ? "text-[#f59e0b]" : "text-[#10b981]"}`}>
                {restartRequired
                  ? "Existe diferenca entre a configuracao salva e o datasource ativo. Reinicie o backend para aplicar o novo banco."
                  : "O datasource salvo ja coincide com o runtime atual."}
              </p>
            </div>
          </div>

          {connectionTest && (
            <div className="rounded-2xl border border-[#27272a] bg-[#111116] p-6">
              <h3 className="text-lg font-semibold text-white">Resultado do teste</h3>
              <div
                className={`mt-4 rounded-xl border p-4 ${
                  connectionTest.success
                    ? "border-[#10b981]/30 bg-[#10b981]/10"
                    : "border-[#ef4444]/30 bg-[#ef4444]/10"
                }`}
              >
                <p className={`text-sm font-medium ${connectionTest.success ? "text-[#34d399]" : "text-[#fca5a5]"}`}>
                  {connectionTest.message}
                </p>
                <p className="mt-2 text-sm text-[#cbd5e1]">Tempo de resposta: {connectionTest.responseTimeMs} ms</p>
                {connectionTest.success && (
                  <>
                    <p className="mt-2 text-sm text-[#cbd5e1]">Banco atual: {connectionTest.currentDatabase}</p>
                    <p className="mt-1 text-sm text-[#cbd5e1]">
                      Motor: {connectionTest.databaseProductName} {connectionTest.databaseVersion}
                    </p>
                  </>
                )}
              </div>
            </div>
          )}

          <div className="rounded-2xl border border-[#27272a] bg-[#111116] p-6">
            <h3 className="text-lg font-semibold text-white">Observacoes</h3>
            <ul className="mt-4 space-y-3 text-sm leading-6 text-[#a1a1aa]">
              <li>Agora a configuracao e persistida no backend em arquivo JSON dentro de `data/`.</li>
              <li>`APP_OPENAI_API_KEY` e `APP_OPENAI_MAX_OUTPUT_TOKENS` passam a valer imediatamente para a integracao de IA.</li>
              <li>As configuracoes de banco ficam salvas e podem ser testadas agora, mas o datasource principal do Spring ainda exige reinicio para trocar de conexao.</li>
            </ul>
          </div>
        </section>
      </div>
    </div>
  );
}

function Highlight({
  icon,
  label,
  value,
}: {
  icon: React.ReactNode;
  label: string;
  value: string;
}) {
  return (
    <div className="rounded-xl border border-white/10 bg-black/20 p-4">
      <div className="mb-3 inline-flex rounded-lg border border-white/10 bg-white/5 p-2">
        {icon}
      </div>
      <p className="text-xs uppercase tracking-[0.18em] text-[#71717a]">{label}</p>
      <p className="mt-2 text-sm text-[#e4e4e7]">{value}</p>
    </div>
  );
}

function StatusLine({ label, ready }: { label: string; ready: boolean }) {
  return (
    <div className="flex items-center justify-between rounded-xl border border-white/10 bg-white/5 px-3 py-2">
      <span className="text-[#cbd5e1]">{label}</span>
      <span
        className={`rounded-full px-2.5 py-1 text-xs font-medium ${
          ready ? "bg-[#10b981]/15 text-[#34d399]" : "bg-[#27272a] text-[#a1a1aa]"
        }`}
      >
        {ready ? "OK" : "Pendente"}
      </span>
    </div>
  );
}

function FieldShell({
  id,
  label,
  envName,
  hint,
  className,
  children,
}: {
  id: string;
  label: string;
  envName: string;
  hint: string;
  className?: string;
  children: React.ReactNode;
}) {
  return (
    <div className={`rounded-2xl border border-[#27272a] bg-[#0a0a0f] p-4 ${className ?? ""}`}>
      <div className="mb-3 flex items-center justify-between gap-3">
        <Label htmlFor={id} className="text-sm font-medium text-white">
          {label}
        </Label>
        <span className="rounded-full border border-[#27272a] bg-[#111116] px-2.5 py-1 text-[11px] uppercase tracking-[0.18em] text-[#71717a]">
          {envName}
        </span>
      </div>
      {children}
      <p className="mt-3 text-xs leading-5 text-[#71717a]">{hint}</p>
    </div>
  );
}

function formatSavedAt(savedAt: string) {
  return new Intl.DateTimeFormat("pt-BR", {
    dateStyle: "short",
    timeStyle: "short",
  }).format(new Date(savedAt));
}

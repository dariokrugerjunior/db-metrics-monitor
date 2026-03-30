import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Database, Eye, EyeOff, Loader2 } from "lucide-react";
import { Button } from "../components/ui/button";
import { Input } from "../components/ui/input";
import { Label } from "../components/ui/label";
import { api } from "../lib/api";
import { useAuth } from "../context/AuthContext";
import { setLanguage } from "../../i18n";

export function ConnectPage() {
  const { t, i18n } = useTranslation();
  const { connect } = useAuth();

  const [dbUrl, setDbUrl] = useState("");
  const [dbUser, setDbUser] = useState("");
  const [dbPassword, setDbPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const currentLang = i18n.language as "pt-BR" | "en-US";

  const toggleLanguage = () => {
    setLanguage(currentLang === "pt-BR" ? "en-US" : "pt-BR");
  };

  useEffect(() => {
    api.getConfiguration().then((config) => {
      if (config.dbUrl) setDbUrl(config.dbUrl);
      if (config.dbUser) setDbUser(config.dbUser);
    }).catch(() => {
      // Best-effort pre-fill; ignore errors
    });
  }, []);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!dbUrl.trim() || !dbUser.trim() || !dbPassword) {
      setError(t("connect.validationRequired"));
      return;
    }

    setLoading(true);
    try {
      const result = await api.connectDatabase({
        dbUrl: dbUrl.trim(),
        dbUser: dbUser.trim(),
        dbPassword,
      });

      if (result.connected) {
        connect(result.dbUrl);
      } else {
        setError(result.message || t("connect.errorFailed"));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : t("connect.errorFailed"));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="dark min-h-screen bg-[#0a0a0f] flex flex-col">
      {/* Top bar */}
      <div className="flex justify-end px-6 py-4">
        <button
          type="button"
          onClick={toggleLanguage}
          className="flex items-center gap-1.5 rounded-lg border border-[#27272a] bg-[#111116] px-3 py-1.5 text-xs font-medium text-[#a1a1aa] transition-colors hover:border-[#3b82f6]/40 hover:text-white"
          title={t("layout.languageLabel")}
        >
          <span className={currentLang === "pt-BR" ? "text-white" : "text-[#71717a]"}>PT</span>
          <span className="text-[#27272a]">|</span>
          <span className={currentLang === "en-US" ? "text-white" : "text-[#71717a]"}>EN</span>
        </button>
      </div>

      {/* Center content */}
      <div className="flex flex-1 items-center justify-center px-4">
        <div className="w-full max-w-md">
          {/* Logo */}
          <div className="mb-8 flex flex-col items-center gap-3">
            <div className="flex items-center justify-center w-14 h-14 rounded-xl bg-[#3b82f6]/10 border border-[#3b82f6]/20">
              <Database className="w-7 h-7 text-[#3b82f6]" />
            </div>
            <div className="text-center">
              <h1 className="text-2xl font-semibold text-white">{t("connect.title")}</h1>
              <p className="mt-1 text-sm text-[#71717a]">{t("connect.subtitle")}</p>
            </div>
          </div>

          {/* Form */}
          <form
            onSubmit={(e) => { void handleSubmit(e); }}
            className="rounded-lg border border-[#27272a] bg-[#111116] p-6 space-y-5"
          >
            {error && (
              <div className="rounded-md border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-400">
                {error}
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="dbUrl" className="text-sm text-[#a1a1aa]">
                {t("connect.dbUrlLabel")}
              </Label>
              <Input
                id="dbUrl"
                type="text"
                value={dbUrl}
                onChange={(e) => { setDbUrl(e.target.value); }}
                placeholder={t("connect.dbUrlPlaceholder")}
                disabled={loading}
                className="bg-[#0f0f14] border-[#27272a] text-white placeholder:text-[#52525b] focus:border-[#3b82f6]/50"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="dbUser" className="text-sm text-[#a1a1aa]">
                {t("connect.dbUserLabel")}
              </Label>
              <Input
                id="dbUser"
                type="text"
                value={dbUser}
                onChange={(e) => { setDbUser(e.target.value); }}
                placeholder={t("connect.dbUserPlaceholder")}
                disabled={loading}
                className="bg-[#0f0f14] border-[#27272a] text-white placeholder:text-[#52525b] focus:border-[#3b82f6]/50"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="dbPassword" className="text-sm text-[#a1a1aa]">
                {t("connect.dbPasswordLabel")}
              </Label>
              <div className="relative">
                <Input
                  id="dbPassword"
                  type={showPassword ? "text" : "password"}
                  value={dbPassword}
                  onChange={(e) => { setDbPassword(e.target.value); }}
                  placeholder={t("connect.dbPasswordPlaceholder")}
                  disabled={loading}
                  className="bg-[#0f0f14] border-[#27272a] text-white placeholder:text-[#52525b] focus:border-[#3b82f6]/50 pr-10"
                />
                <button
                  type="button"
                  onClick={() => { setShowPassword(!showPassword); }}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-[#71717a] hover:text-[#a1a1aa]"
                  tabIndex={-1}
                >
                  {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                </button>
              </div>
            </div>

            <Button
              type="submit"
              disabled={loading}
              className="w-full bg-[#3b82f6] hover:bg-[#2563eb] text-white font-medium"
            >
              {loading ? (
                <>
                  <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                  {t("connect.connecting")}
                </>
              ) : (
                t("connect.connectButton")
              )}
            </Button>
          </form>

          <p className="mt-4 text-center text-xs text-[#52525b]">
            {t("connect.securityNote")}
          </p>
        </div>
      </div>
    </div>
  );
}

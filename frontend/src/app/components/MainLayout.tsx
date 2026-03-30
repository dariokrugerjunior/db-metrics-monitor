import { Outlet, Link, useLocation } from "react-router";
import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
  Lock,
  Code2,
  Cable,
  BarChart3,
  BrainCircuit,
  Bot,
  History as HistoryIcon,
  Database,
  Circle,
  LogOut,
  Settings as SettingsIcon,
} from "lucide-react";
import { cn } from "./ui/utils";
import { LiveActivityIndicator } from "./LiveActivityIndicator";
import { RefreshButton } from "./RefreshButton";
import { setLanguage } from "../../i18n";
import { useAuth } from "../context/AuthContext";

export function MainLayout() {
  const location = useLocation();
  const { t, i18n } = useTranslation();
  const { disconnect } = useAuth();
  const [refreshAction, setRefreshAction] = useState<(() => void) | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const navigation = [
    { name: t("nav.overview"), href: "/overview", icon: BarChart3 },
    { name: t("nav.intelligence"), href: "/intelligence", icon: BrainCircuit },
    { name: t("nav.locks"), href: "/locks", icon: Lock },
    { name: t("nav.queries"), href: "/queries", icon: Code2 },
    { name: t("nav.connections"), href: "/connections", icon: Cable },
    { name: t("nav.history"), href: "/history", icon: HistoryIcon },
    { name: t("nav.aiAnalysis"), href: "/ai-analysis", icon: Bot },
    { name: t("nav.settings"), href: "/settings", icon: SettingsIcon },
  ];

  const handleRefresh = async () => {
    if (!refreshAction) {
      return;
    }

    setRefreshing(true);
    try {
      await Promise.resolve(refreshAction());
    } finally {
      setRefreshing(false);
    }
  };

  const currentLang = i18n.language as "pt-BR" | "en-US";

  const toggleLanguage = () => {
    setLanguage(currentLang === "pt-BR" ? "en-US" : "pt-BR");
  };

  return (
    <div className="dark min-h-screen flex">
      {/* Sidebar */}
      <aside className="w-64 bg-[#0f0f14] border-r border-[#27272a] flex flex-col">
        {/* Logo */}
        <div className="h-16 flex items-center px-6 border-b border-[#27272a]">
          <Database className="w-6 h-6 text-[#3b82f6]" />
          <span className="ml-3 text-lg font-semibold text-white">
            {t("layout.title")}
          </span>
        </div>

        {/* Navigation */}
        <nav className="flex-1 px-3 py-4 space-y-1">
          {navigation.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.href;

            return (
              <Link
                key={item.href}
                to={item.href}
                className={cn(
                  "flex items-center px-3 py-2.5 rounded-lg transition-colors",
                  isActive
                    ? "bg-[#1f1f28] text-white"
                    : "text-[#a1a1aa] hover:bg-[#1f1f28] hover:text-white",
                )}
              >
                <Icon className="w-5 h-5" />
                <span className="ml-3 text-sm font-medium">{item.name}</span>
              </Link>
            );
          })}
        </nav>

        {/* Footer */}
        <div className="p-4 border-t border-[#27272a]">
          <div className="text-xs text-[#71717a]">
            PostgreSQL 15.2
            <br />
            Uptime: 45d 12h 34m
          </div>
        </div>
      </aside>

      {/* Main Content */}
      <div className="flex-1 flex flex-col">
        {/* Header */}
        <header className="h-16 bg-[#0a0a0f] border-b border-[#27272a] flex items-center justify-between px-6">
          <div className="flex items-center space-x-4">
            <h1 className="text-xl font-semibold text-white">
              {navigation.find((item) => item.href === location.pathname)
                ?.name || t("nav.overview")}
            </h1>
            <LiveActivityIndicator />
          </div>

          <div className="flex items-center space-x-3">
            <button
              type="button"
              onClick={toggleLanguage}
              className="flex items-center gap-1.5 rounded-lg border border-[#27272a] bg-[#111116] px-3 py-1.5 text-xs font-medium text-[#a1a1aa] transition-colors hover:border-[#3b82f6]/40 hover:text-white"
              title={t("layout.languageLabel")}
            >
              <span className={currentLang === "pt-BR" ? "text-white" : "text-[#71717a]"}>
                PT
              </span>
              <span className="text-[#27272a]">|</span>
              <span className={currentLang === "en-US" ? "text-white" : "text-[#71717a]"}>
                EN
              </span>
            </button>
            <RefreshButton onClick={handleRefresh} refreshing={refreshing} />
            <button
              type="button"
              onClick={disconnect}
              className="flex items-center gap-1.5 rounded-lg border border-[#27272a] bg-[#111116] px-3 py-1.5 text-xs font-medium text-[#a1a1aa] transition-colors hover:border-red-500/40 hover:text-red-400"
              title={t("connect.disconnect")}
            >
              <LogOut className="w-3.5 h-3.5" />
              {t("connect.disconnect")}
            </button>
            <SystemStatus />
          </div>
        </header>

        {/* Page Content */}
        <main className="flex-1 overflow-auto bg-[#0a0a0f] p-6">
          <Outlet context={{ setRefreshAction, refreshing }} />
        </main>
      </div>
    </div>
  );
}

function SystemStatus() {
  const { t } = useTranslation();
  const status = "healthy"; // healthy, warning, critical

  const statusConfig = {
    healthy: {
      color: "text-[#10b981]",
      bg: "bg-[#10b981]/10",
      text: t("layout.systemHealthy"),
    },
    warning: {
      color: "text-[#f59e0b]",
      bg: "bg-[#f59e0b]/10",
      text: t("layout.systemWarning"),
    },
    critical: {
      color: "text-[#ef4444]",
      bg: "bg-[#ef4444]/10",
      text: t("layout.systemCritical"),
    },
  };

  const config = statusConfig[status as keyof typeof statusConfig];

  return (
    <div className={cn("flex items-center px-3 py-1.5 rounded-lg", config.bg)}>
      <Circle className={cn("w-2 h-2 fill-current", config.color)} />
      <span className={cn("ml-2 text-sm font-medium", config.color)}>
        {config.text}
      </span>
    </div>
  );
}

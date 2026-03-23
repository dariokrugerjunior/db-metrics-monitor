import { LucideIcon } from "lucide-react";
import { cn } from "./ui/utils";

interface MetricCardProps {
  title: string;
  value: string | number;
  change?: string;
  changeType?: "positive" | "negative" | "neutral";
  icon?: LucideIcon;
  trend?: "up" | "down";
  status?: "healthy" | "warning" | "critical";
}

export function MetricCard({
  title,
  value,
  change,
  changeType = "neutral",
  icon: Icon,
  status,
}: MetricCardProps) {
  const statusColors = {
    healthy: "border-l-[#10b981]",
    warning: "border-l-[#f59e0b]",
    critical: "border-l-[#ef4444]",
  };

  const changeColors = {
    positive: "text-[#10b981]",
    negative: "text-[#ef4444]",
    neutral: "text-[#a1a1aa]",
  };

  return (
    <div
      className={cn(
        "bg-[#111116] rounded-lg p-5 border border-[#27272a] border-l-2 transition-all hover:border-[#3b82f6]/30",
        status && statusColors[status]
      )}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm text-[#a1a1aa] mb-1">{title}</p>
          <p className="text-2xl font-semibold text-white mb-1">{value}</p>
          {change && (
            <p className={cn("text-xs font-medium", changeColors[changeType])}>
              {change}
            </p>
          )}
        </div>
        {Icon && (
          <div className="p-2.5 bg-[#1f1f28] rounded-lg">
            <Icon className="w-5 h-5 text-[#3b82f6]" />
          </div>
        )}
      </div>
    </div>
  );
}

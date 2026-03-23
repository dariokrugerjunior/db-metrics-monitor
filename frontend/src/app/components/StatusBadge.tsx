import { cn } from "./ui/utils";

interface StatusBadgeProps {
  status: "healthy" | "warning" | "critical" | "info" | "blocked" | "blocking" | "running" | "idle";
  label?: string;
}

export function StatusBadge({ status, label }: StatusBadgeProps) {
  const statusConfig = {
    healthy: {
      bg: "bg-[#10b981]/10",
      text: "text-[#10b981]",
      label: label || "Healthy",
    },
    warning: {
      bg: "bg-[#f59e0b]/10",
      text: "text-[#f59e0b]",
      label: label || "Warning",
    },
    critical: {
      bg: "bg-[#ef4444]/10",
      text: "text-[#ef4444]",
      label: label || "Critical",
    },
    info: {
      bg: "bg-[#3b82f6]/10",
      text: "text-[#3b82f6]",
      label: label || "Info",
    },
    blocked: {
      bg: "bg-[#ef4444]/10",
      text: "text-[#ef4444]",
      label: label || "Blocked",
    },
    blocking: {
      bg: "bg-[#f59e0b]/10",
      text: "text-[#f59e0b]",
      label: label || "Blocking",
    },
    running: {
      bg: "bg-[#3b82f6]/10",
      text: "text-[#3b82f6]",
      label: label || "Running",
    },
    idle: {
      bg: "bg-[#71717a]/10",
      text: "text-[#71717a]",
      label: label || "Idle",
    },
  };

  const config = statusConfig[status];

  return (
    <span
      className={cn(
        "inline-flex items-center px-2.5 py-1 rounded-md text-xs font-medium",
        config.bg,
        config.text
      )}
    >
      {config.label}
    </span>
  );
}

import { motion } from "motion/react";
import { AlertTriangle, CheckCircle, XCircle, Info } from "lucide-react";

interface StatusBannerProps {
  status: "success" | "warning" | "error" | "info";
  title: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
}

export function StatusBanner({
  status,
  title,
  description,
  actionLabel,
  onAction,
}: StatusBannerProps) {
  const config = {
    success: {
      icon: CheckCircle,
      color: "#10b981",
      bg: "bg-[#10b981]/10",
      border: "border-[#10b981]/30",
    },
    warning: {
      icon: AlertTriangle,
      color: "#f59e0b",
      bg: "bg-[#f59e0b]/10",
      border: "border-[#f59e0b]/30",
    },
    error: {
      icon: XCircle,
      color: "#ef4444",
      bg: "bg-[#ef4444]/10",
      border: "border-[#ef4444]/30",
    },
    info: {
      icon: Info,
      color: "#3b82f6",
      bg: "bg-[#3b82f6]/10",
      border: "border-[#3b82f6]/30",
    },
  };

  const Icon = config[status].icon;

  return (
    <motion.div
      initial={{ opacity: 0, y: -20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
      className={`${config[status].bg} border ${config[status].border} rounded-lg p-4 flex items-start justify-between`}
    >
      <div className="flex items-start space-x-3">
        <div
          className={`p-2 ${config[status].bg} rounded-lg`}
          style={{ backgroundColor: `${config[status].color}20` }}
        >
          <Icon className="w-5 h-5" style={{ color: config[status].color }} />
        </div>
        <div>
          <h3
            className="text-sm font-medium mb-1"
            style={{ color: config[status].color }}
          >
            {title}
          </h3>
          {description && (
            <p className="text-sm" style={{ color: `${config[status].color}CC` }}>
              {description}
            </p>
          )}
        </div>
      </div>
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="px-3 py-1.5 text-xs font-medium rounded-md transition-colors"
          style={{
            color: config[status].color,
            backgroundColor: `${config[status].color}20`,
          }}
        >
          {actionLabel}
        </button>
      )}
    </motion.div>
  );
}

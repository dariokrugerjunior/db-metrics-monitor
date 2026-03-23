import { motion } from "motion/react";
import { LucideIcon } from "lucide-react";
import { cn } from "./ui/utils";

interface AnimatedMetricProps {
  title: string;
  value: string | number;
  change?: string;
  changeType?: "positive" | "negative" | "neutral";
  icon?: LucideIcon;
  status?: "healthy" | "warning" | "critical";
  delay?: number;
}

export function AnimatedMetric({
  title,
  value,
  change,
  changeType = "neutral",
  icon: Icon,
  status,
  delay = 0,
}: AnimatedMetricProps) {
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
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, delay }}
      className={cn(
        "bg-[#111116] rounded-lg p-5 border border-[#27272a] border-l-2 transition-all hover:border-[#3b82f6]/30 hover:shadow-lg hover:shadow-[#3b82f6]/5",
        status && statusColors[status]
      )}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="text-sm text-[#a1a1aa] mb-1">{title}</p>
          <motion.p
            initial={{ scale: 0.8 }}
            animate={{ scale: 1 }}
            transition={{ duration: 0.3, delay: delay + 0.1 }}
            className="text-2xl font-semibold text-white mb-1"
          >
            {value}
          </motion.p>
          {change && (
            <p className={cn("text-xs font-medium", changeColors[changeType])}>
              {change}
            </p>
          )}
        </div>
        {Icon && (
          <motion.div
            initial={{ rotate: -180, opacity: 0 }}
            animate={{ rotate: 0, opacity: 1 }}
            transition={{ duration: 0.5, delay: delay + 0.2 }}
            className="p-2.5 bg-[#1f1f28] rounded-lg"
          >
            <Icon className="w-5 h-5 text-[#3b82f6]" />
          </motion.div>
        )}
      </div>
    </motion.div>
  );
}

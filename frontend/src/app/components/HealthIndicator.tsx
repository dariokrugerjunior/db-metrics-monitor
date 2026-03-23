import { motion } from "motion/react";
import { CheckCircle, AlertTriangle, XCircle } from "lucide-react";

interface HealthIndicatorProps {
  status: "healthy" | "warning" | "critical";
  size?: "sm" | "md" | "lg";
}

export function HealthIndicator({ status, size = "md" }: HealthIndicatorProps) {
  const config = {
    healthy: {
      icon: CheckCircle,
      color: "#10b981",
      label: "Healthy",
      bgColor: "bg-[#10b981]/10",
    },
    warning: {
      icon: AlertTriangle,
      color: "#f59e0b",
      label: "Warning",
      bgColor: "bg-[#f59e0b]/10",
    },
    critical: {
      icon: XCircle,
      color: "#ef4444",
      label: "Critical",
      bgColor: "bg-[#ef4444]/10",
    },
  };

  const sizes = {
    sm: { icon: 16, padding: "p-2", text: "text-xs" },
    md: { icon: 20, padding: "p-3", text: "text-sm" },
    lg: { icon: 24, padding: "p-4", text: "text-base" },
  };

  const Icon = config[status].icon;
  const sizeConfig = sizes[size];

  return (
    <motion.div
      initial={{ scale: 0 }}
      animate={{ scale: 1 }}
      transition={{ type: "spring", stiffness: 260, damping: 20 }}
      className={`inline-flex items-center space-x-2 ${sizeConfig.padding} rounded-lg ${config[status].bgColor}`}
    >
      <motion.div
        animate={{
          scale: [1, 1.2, 1],
        }}
        transition={{
          duration: 2,
          repeat: Infinity,
          repeatType: "reverse",
        }}
      >
        <Icon
          size={sizeConfig.icon}
          style={{ color: config[status].color }}
        />
      </motion.div>
      <span
        className={`${sizeConfig.text} font-medium`}
        style={{ color: config[status].color }}
      >
        {config[status].label}
      </span>
    </motion.div>
  );
}

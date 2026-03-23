import { motion } from "motion/react";

interface PerformanceGaugeProps {
  value: number; // 0-100
  label: string;
  status?: "healthy" | "warning" | "critical";
}

export function PerformanceGauge({ value, label, status }: PerformanceGaugeProps) {
  const getColor = () => {
    if (status === "critical") return "#ef4444";
    if (status === "warning") return "#f59e0b";
    return "#10b981";
  };

  const getStatusColor = () => {
    if (value >= 80) return "#ef4444";
    if (value >= 60) return "#f59e0b";
    return "#10b981";
  };

  const color = status ? getColor() : getStatusColor();
  const circumference = 2 * Math.PI * 40;
  const strokeDashoffset = circumference - (value / 100) * circumference;

  return (
    <div className="flex flex-col items-center">
      <div className="relative w-28 h-28">
        <svg className="transform -rotate-90" width="112" height="112">
          {/* Background circle */}
          <circle
            cx="56"
            cy="56"
            r="40"
            stroke="#27272a"
            strokeWidth="8"
            fill="none"
          />
          {/* Progress circle */}
          <motion.circle
            cx="56"
            cy="56"
            r="40"
            stroke={color}
            strokeWidth="8"
            fill="none"
            strokeDasharray={circumference}
            initial={{ strokeDashoffset: circumference }}
            animate={{ strokeDashoffset }}
            transition={{ duration: 1, ease: "easeOut" }}
            strokeLinecap="round"
          />
        </svg>
        <div className="absolute inset-0 flex items-center justify-center">
          <motion.span
            initial={{ opacity: 0, scale: 0.5 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ duration: 0.5 }}
            className="text-2xl font-semibold"
            style={{ color }}
          >
            {value}%
          </motion.span>
        </div>
      </div>
      <p className="mt-2 text-sm text-[#a1a1aa] text-center">{label}</p>
    </div>
  );
}

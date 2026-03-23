import { motion } from "motion/react";

interface ProgressBarProps {
  value: number; // 0-100
  label?: string;
  showValue?: boolean;
  color?: string;
  height?: string;
}

export function ProgressBar({
  value,
  label,
  showValue = true,
  color,
  height = "h-2",
}: ProgressBarProps) {
  const getColor = () => {
    if (color) return color;
    if (value >= 80) return "#ef4444";
    if (value >= 60) return "#f59e0b";
    return "#10b981";
  };

  const progressColor = getColor();

  return (
    <div className="w-full">
      {(label || showValue) && (
        <div className="flex items-center justify-between mb-2">
          {label && <span className="text-xs text-[#a1a1aa]">{label}</span>}
          {showValue && (
            <span className="text-xs font-medium text-white">{value}%</span>
          )}
        </div>
      )}
      <div className={`w-full ${height} bg-[#27272a] rounded-full overflow-hidden`}>
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${value}%` }}
          transition={{ duration: 0.8, ease: "easeOut" }}
          className={`${height} rounded-full`}
          style={{ backgroundColor: progressColor }}
        />
      </div>
    </div>
  );
}

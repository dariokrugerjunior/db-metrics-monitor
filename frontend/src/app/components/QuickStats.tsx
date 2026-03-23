import { motion } from "motion/react";
import { TrendingUp, TrendingDown, Minus } from "lucide-react";

interface QuickStatProps {
  label: string;
  value: string | number;
  trend?: "up" | "down" | "neutral";
  trendValue?: string;
  color?: string;
}

export function QuickStat({
  label,
  value,
  trend = "neutral",
  trendValue,
  color = "#3b82f6",
}: QuickStatProps) {
  const getTrendIcon = () => {
    switch (trend) {
      case "up":
        return <TrendingUp className="w-3 h-3" />;
      case "down":
        return <TrendingDown className="w-3 h-3" />;
      case "neutral":
        return <Minus className="w-3 h-3" />;
    }
  };

  const getTrendColor = () => {
    switch (trend) {
      case "up":
        return "text-[#10b981]";
      case "down":
        return "text-[#ef4444]";
      case "neutral":
        return "text-[#71717a]";
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.3 }}
      className="flex items-center justify-between p-3 bg-[#0a0a0f] rounded-lg border border-[#27272a]"
    >
      <div>
        <p className="text-xs text-[#71717a] mb-0.5">{label}</p>
        <p className="text-lg font-semibold" style={{ color }}>
          {value}
        </p>
      </div>
      {trendValue && (
        <div className={`flex items-center space-x-1 text-xs ${getTrendColor()}`}>
          {getTrendIcon()}
          <span>{trendValue}</span>
        </div>
      )}
    </motion.div>
  );
}

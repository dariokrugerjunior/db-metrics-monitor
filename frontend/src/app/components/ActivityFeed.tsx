import { motion } from "motion/react";
import { Clock, Database, AlertTriangle, CheckCircle, Info } from "lucide-react";

interface Activity {
  id: number;
  type: "success" | "warning" | "error" | "info";
  message: string;
  timestamp: string;
}

const activities: Activity[] = [
  {
    id: 1,
    type: "success",
    message: "Database backup completed successfully",
    timestamp: "2 minutes ago",
  },
  {
    id: 2,
    type: "warning",
    message: "Slow query detected in production database",
    timestamp: "5 minutes ago",
  },
  {
    id: 3,
    type: "error",
    message: "Connection limit reached for user 'app_user'",
    timestamp: "10 minutes ago",
  },
  {
    id: 4,
    type: "info",
    message: "New connection established from 192.168.1.100",
    timestamp: "15 minutes ago",
  },
];

export function ActivityFeed() {
  return (
    <div className="bg-[#111116] rounded-lg border border-[#27272a] p-5">
      <div className="flex items-center justify-between mb-4">
        <h3 className="text-sm font-medium text-white">Recent Activity</h3>
        <Clock className="w-4 h-4 text-[#71717a]" />
      </div>
      <div className="space-y-3">
        {activities.map((activity, index) => (
          <ActivityItem key={activity.id} activity={activity} delay={index * 0.1} />
        ))}
      </div>
    </div>
  );
}

function ActivityItem({ activity, delay }: { activity: Activity; delay: number }) {
  const getIcon = () => {
    switch (activity.type) {
      case "success":
        return <CheckCircle className="w-4 h-4 text-[#10b981]" />;
      case "warning":
        return <AlertTriangle className="w-4 h-4 text-[#f59e0b]" />;
      case "error":
        return <AlertTriangle className="w-4 h-4 text-[#ef4444]" />;
      case "info":
        return <Info className="w-4 h-4 text-[#3b82f6]" />;
    }
  };

  const getBorderColor = () => {
    switch (activity.type) {
      case "success":
        return "border-l-[#10b981]";
      case "warning":
        return "border-l-[#f59e0b]";
      case "error":
        return "border-l-[#ef4444]";
      case "info":
        return "border-l-[#3b82f6]";
    }
  };

  return (
    <motion.div
      initial={{ opacity: 0, x: -20 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ duration: 0.3, delay }}
      className={`flex items-start space-x-3 p-3 bg-[#0a0a0f] rounded-lg border-l-2 ${getBorderColor()}`}
    >
      <div className="mt-0.5">{getIcon()}</div>
      <div className="flex-1 min-w-0">
        <p className="text-sm text-white">{activity.message}</p>
        <p className="text-xs text-[#71717a] mt-1">{activity.timestamp}</p>
      </div>
    </motion.div>
  );
}

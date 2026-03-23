import { motion } from "motion/react";
import { Activity } from "lucide-react";

export function LiveActivityIndicator() {
  return (
    <div className="flex items-center space-x-2">
      <motion.div
        animate={{
          scale: [1, 1.2, 1],
          opacity: [1, 0.5, 1],
        }}
        transition={{
          duration: 2,
          repeat: Infinity,
          ease: "easeInOut",
        }}
        className="relative"
      >
        <div className="w-2 h-2 bg-[#10b981] rounded-full"></div>
        <motion.div
          animate={{
            scale: [1, 2, 1],
            opacity: [0.5, 0, 0.5],
          }}
          transition={{
            duration: 2,
            repeat: Infinity,
            ease: "easeInOut",
          }}
          className="absolute inset-0 w-2 h-2 bg-[#10b981] rounded-full"
        ></motion.div>
      </motion.div>
      <span className="text-xs text-[#71717a]">Live</span>
      <Activity className="w-3 h-3 text-[#71717a]" />
    </div>
  );
}

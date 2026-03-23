import { RefreshCw } from "lucide-react";
import { Button } from "./ui/button";

interface RefreshButtonProps {
  onClick: () => void;
  refreshing?: boolean;
}

export function RefreshButton({ onClick, refreshing = false }: RefreshButtonProps) {
  return (
    <Button
      type="button"
      variant="outline"
      onClick={onClick}
      disabled={refreshing}
      className="border-[#27272a] bg-[#111116] text-white hover:bg-[#1f1f28]"
    >
      <RefreshCw className={`mr-2 h-4 w-4 ${refreshing ? "animate-spin" : ""}`} />
      Refresh
    </Button>
  );
}

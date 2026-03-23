import { useState } from "react";
import { Calendar, ChevronDown } from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "./ui/dropdown-menu";
import { Button } from "./ui/button";

const timeRanges = [
  { label: "Last 15 minutes", value: "15m" },
  { label: "Last 1 hour", value: "1h" },
  { label: "Last 6 hours", value: "6h" },
  { label: "Last 24 hours", value: "24h" },
  { label: "Last 7 days", value: "7d" },
  { label: "Last 30 days", value: "30d" },
];

export function TimeRangeFilter() {
  const [selectedRange, setSelectedRange] = useState(timeRanges[3]);

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button
          variant="outline"
          className="bg-[#111116] border-[#27272a] text-white hover:bg-[#1f1f28] hover:text-white"
        >
          <Calendar className="w-4 h-4 mr-2" />
          {selectedRange.label}
          <ChevronDown className="w-4 h-4 ml-2" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent
        align="end"
        className="bg-[#111116] border-[#27272a] text-white"
      >
        {timeRanges.map((range) => (
          <DropdownMenuItem
            key={range.value}
            onClick={() => setSelectedRange(range)}
            className="hover:bg-[#1f1f28] cursor-pointer"
          >
            {range.label}
          </DropdownMenuItem>
        ))}
      </DropdownMenuContent>
    </DropdownMenu>
  );
}

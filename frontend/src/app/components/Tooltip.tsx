import {
  Tooltip as TooltipPrimitive,
  TooltipContent,
  TooltipProvider,
  TooltipTrigger,
} from "./ui/tooltip";

interface TooltipProps {
  content: string;
  children: React.ReactNode;
}

export function Tooltip({ content, children }: TooltipProps) {
  return (
    <TooltipProvider>
      <TooltipPrimitive>
        <TooltipTrigger asChild>{children}</TooltipTrigger>
        <TooltipContent
          side="right"
          className="bg-[#111116] border-[#27272a] text-white"
        >
          <p className="text-xs">{content}</p>
        </TooltipContent>
      </TooltipPrimitive>
    </TooltipProvider>
  );
}

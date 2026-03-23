import { Skeleton } from "./ui/skeleton";

export function LoadingMetricCard() {
  return (
    <div className="bg-[#111116] rounded-lg p-5 border border-[#27272a]">
      <Skeleton className="h-4 w-24 mb-2 bg-[#1f1f28]" />
      <Skeleton className="h-8 w-16 mb-1 bg-[#1f1f28]" />
      <Skeleton className="h-3 w-32 bg-[#1f1f28]" />
    </div>
  );
}

export function LoadingTable() {
  return (
    <div className="bg-[#111116] rounded-lg border border-[#27272a] p-5 space-y-3">
      {[...Array(5)].map((_, i) => (
        <Skeleton key={i} className="h-12 w-full bg-[#1f1f28]" />
      ))}
    </div>
  );
}

export function LoadingChart() {
  return (
    <div className="bg-[#111116] rounded-lg border border-[#27272a] p-5">
      <Skeleton className="h-4 w-32 mb-4 bg-[#1f1f28]" />
      <Skeleton className="h-64 w-full bg-[#1f1f28]" />
    </div>
  );
}

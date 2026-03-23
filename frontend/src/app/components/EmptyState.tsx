import { LucideIcon } from "lucide-react";

interface EmptyStateProps {
  icon?: LucideIcon;
  title: string;
  description?: string;
}

export function EmptyState({ icon: Icon, title, description }: EmptyStateProps) {
  return (
    <div className="bg-[#111116] rounded-lg border border-[#27272a] p-12 text-center">
      {Icon && (
        <div className="flex justify-center mb-4">
          <div className="p-3 bg-[#1f1f28] rounded-lg">
            <Icon className="w-8 h-8 text-[#71717a]" />
          </div>
        </div>
      )}
      <h3 className="text-base font-medium text-white mb-1">{title}</h3>
      {description && (
        <p className="text-sm text-[#71717a]">{description}</p>
      )}
    </div>
  );
}

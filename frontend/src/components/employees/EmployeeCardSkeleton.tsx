import { Skeleton } from "../ui/Skeleton";

export function EmployeeCardSkeleton() {
  return (
    <div className="flex flex-col gap-4 rounded-2xl border border-brand-900/8 bg-white p-5">
      <div className="flex items-center gap-3">
        <Skeleton className="h-11 w-11 rounded-full" />
        <div className="flex flex-1 flex-col gap-2">
          <Skeleton className="h-4 w-32" />
          <Skeleton className="h-3 w-20" />
        </div>
      </div>
      <Skeleton className="h-5 w-24 rounded-full" />
      <Skeleton className="h-3 w-16" />
    </div>
  );
}

import { useEffect, useMemo, useState } from "react";
import { motion, MotionConfig } from "framer-motion";
import { getAllEmployees, type Employee } from "../lib/employeeApi";
import { getErrorMessage } from "../lib/apiClient";
import { useInfiniteBatch } from "../hooks/useInfiniteBatch";
import { PageHeader } from "../components/ui/PageHeader";
import { EmptyState } from "../components/ui/EmptyState";
import { EmployeeCard } from "../components/employees/EmployeeCard";
import { EmployeeCardSkeleton } from "../components/employees/EmployeeCardSkeleton";

const BATCH_SIZE = 24;
// Every 5th visible card is "featured" only if it's genuinely High risk -
// the size variation in the grid tracks a real signal, not a fixed pattern.
const FEATURE_EVERY = 5;

const gridVariants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.03 } },
};

export function EmployeeList() {
  const [employees, setEmployees] = useState<Employee[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [department, setDepartment] = useState("All Departments");

  useEffect(() => {
    getAllEmployees()
      .then(setEmployees)
      .catch((err) => setLoadError(getErrorMessage(err, "Couldn't load employees.")));
  }, []);

  const departments = useMemo(
    () => ["All Departments", ...Array.from(new Set((employees ?? []).map((e) => e.department)))],
    [employees]
  );

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (employees ?? []).filter((e) => {
      const matchesQuery =
        q.length === 0 ||
        `${e.firstName} ${e.lastName}`.toLowerCase().includes(q) ||
        e.employeeId.toLowerCase().includes(q) ||
        e.jobRole.toLowerCase().includes(q);
      const matchesDepartment = department === "All Departments" || e.department === department;
      return matchesQuery && matchesDepartment;
    });
  }, [employees, query, department]);

  const { visibleItems, hasMore, loadingMore, sentinelRef } = useInfiniteBatch(filtered, BATCH_SIZE);

  if (loadError) {
    return (
      <div className="mx-auto max-w-7xl">
        <PageHeader eyebrow="Directory" title="Employees" />
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{loadError}</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        eyebrow="Directory"
        title="Employees"
        description={
          employees
            ? `${filtered.length} of ${employees.length} employees across ${departments.length - 1} departments`
            : undefined
        }
      />

      <div className="mb-6 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <SearchIcon className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-400" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by name, ID, or job role..."
            disabled={!employees}
            className="w-full rounded-full border border-brand-900/12 bg-white py-2.5 pl-11 pr-4 text-sm outline-none transition-colors placeholder:text-neutral-400 focus:border-brand-900/40 disabled:bg-brand-50/50"
          />
        </div>
        <select
          value={department}
          onChange={(e) => setDepartment(e.target.value)}
          disabled={!employees}
          className="rounded-full border border-brand-900/12 bg-white px-4 py-2.5 text-sm font-medium outline-none transition-colors focus:border-brand-900/40 disabled:bg-brand-50/50"
        >
          {departments.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
      </div>

      {!employees ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <EmployeeCardSkeleton key={i} />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No employees match your search"
          description="Try a different name, employee ID, job role, or department filter."
        />
      ) : (
        <>
          <MotionConfig reducedMotion="user">
            <motion.div
              key={`${query}-${department}`}
              variants={gridVariants}
              initial="hidden"
              animate="visible"
              className="grid grid-cols-1 gap-4 sm:grid-cols-3"
            >
              {visibleItems.map((employee, i) => (
                <EmployeeCard
                  key={employee.id}
                  employee={employee}
                  featured={i % FEATURE_EVERY === 0 && employee.attrition === "Yes"}
                />
              ))}
            </motion.div>
          </MotionConfig>

          {hasMore && (
            <div ref={sentinelRef} className="mt-4">
              {loadingMore && (
                <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
                  {Array.from({ length: 3 }, (_, i) => (
                    <EmployeeCardSkeleton key={i} />
                  ))}
                </div>
              )}
            </div>
          )}
        </>
      )}
    </div>
  );
}

function SearchIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <circle cx="11" cy="11" r="7" />
      <path d="m21 21-4.3-4.3" strokeLinecap="round" />
    </svg>
  );
}

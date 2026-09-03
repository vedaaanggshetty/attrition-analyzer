import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { getAllEmployees, type Employee } from "../lib/employeeApi";
import { attritionRisk, avatarColorFor, departmentChipStyleFor } from "../lib/employeeDisplay";
import { ApiError } from "../lib/apiClient";
import { cx, formatCurrency } from "../lib/utils";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Avatar } from "../components/ui/Avatar";
import { RiskBadge } from "../components/ui/Badge";
import { EmptyState } from "../components/ui/EmptyState";
import { Skeleton } from "../components/ui/Skeleton";

export function EmployeeList() {
  const [employees, setEmployees] = useState<Employee[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [department, setDepartment] = useState("All Departments");

  useEffect(() => {
    getAllEmployees()
      .then(setEmployees)
      .catch((err) => setLoadError(err instanceof ApiError ? err.message : "Couldn't load employees."));
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
          employees ? `${employees.length} employees across ${departments.length - 1} departments` : undefined
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
        <Card className="divide-y divide-brand-900/8 overflow-hidden">
          {Array.from({ length: 6 }, (_, i) => (
            <div key={i} className="flex items-center gap-3 px-6 py-4">
              <Skeleton className="h-11 w-11 rounded-full" />
              <div className="flex flex-1 flex-col gap-2">
                <Skeleton className="h-4 w-40" />
                <Skeleton className="h-3 w-24" />
              </div>
            </div>
          ))}
        </Card>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No employees match your search"
          description="Try a different name, employee ID, job role, or department filter."
        />
      ) : (
        <Card className="overflow-hidden !border-brand-900/8 bg-white/70 backdrop-blur-xl">
          <div className="hidden grid-cols-[2fr_1fr_1fr_1fr_auto_1.5rem] gap-4 px-4 py-3 text-xs font-semibold uppercase tracking-wider text-neutral-400 sm:grid">
            <span>Employee</span>
            <span>Department</span>
            <span>Job Role</span>
            <span className="text-right">Compensation</span>
            <span className="text-right">Risk</span>
            <span />
          </div>
          <div className="flex flex-col gap-1 p-2">
            {filtered.map((employee) => (
              <Link
                key={employee.id}
                to={`/employees/${employee.id}`}
                className="group grid grid-cols-2 items-center gap-4 rounded-xl px-4 py-3.5 transition-colors hover:bg-brand-50/60 sm:grid-cols-[2fr_1fr_1fr_1fr_auto_1.5rem]"
              >
                <div className="col-span-2 flex items-center gap-3 sm:col-span-1">
                  <Avatar
                    firstName={employee.firstName}
                    lastName={employee.lastName}
                    color={avatarColorFor(employee.id)}
                    size="sm"
                  />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-brand-900">
                      {employee.firstName} {employee.lastName}
                    </p>
                    <p className="truncate text-xs text-neutral-400">{employee.employeeId}</p>
                  </div>
                </div>
                <span>
                  <span
                    className={cx(
                      "inline-flex max-w-full truncate rounded-full px-2.5 py-1 text-xs font-semibold",
                      departmentChipStyleFor(employee.department)
                    )}
                  >
                    {employee.department}
                  </span>
                </span>
                <span className="truncate text-sm text-neutral-600">{employee.jobRole}</span>
                <span className="text-right font-display text-sm font-medium tabular-nums text-brand-900">
                  {formatCurrency(employee.salary)}
                </span>
                <div className="flex justify-start sm:justify-end">
                  <RiskBadge risk={attritionRisk(employee)} />
                </div>
                <ChevronIcon className="hidden h-4 w-4 shrink-0 text-neutral-300 transition-all group-hover:translate-x-0.5 group-hover:text-brand-900 sm:block" />
              </Link>
            ))}
          </div>
        </Card>
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
function ChevronIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <path d="m9 6 6 6-6 6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

import { useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { employees } from "../data/mockData";
import { formatCurrency } from "../lib/utils";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Avatar } from "../components/ui/Avatar";
import { RiskBadge } from "../components/ui/Badge";
import { EmptyState } from "../components/ui/EmptyState";

const DEPARTMENTS = ["All Departments", ...Array.from(new Set(employees.map((e) => e.department)))];

export function EmployeeList() {
  const [query, setQuery] = useState("");
  const [department, setDepartment] = useState("All Departments");

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return employees.filter((e) => {
      const matchesQuery =
        q.length === 0 ||
        `${e.firstName} ${e.lastName}`.toLowerCase().includes(q) ||
        e.employeeId.toLowerCase().includes(q) ||
        e.jobRole.toLowerCase().includes(q);
      const matchesDepartment = department === "All Departments" || e.department === department;
      return matchesQuery && matchesDepartment;
    });
  }, [query, department]);

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        eyebrow="Directory"
        title="Employees"
        description={`${employees.length} employees across ${DEPARTMENTS.length - 1} departments`}
      />

      <div className="mb-6 flex flex-col gap-3 sm:flex-row">
        <div className="relative flex-1">
          <SearchIcon className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-400" />
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search by name, ID, or job role..."
            className="w-full rounded-full border border-brand-900/12 bg-white py-2.5 pl-11 pr-4 text-sm outline-none transition-colors placeholder:text-neutral-400 focus:border-brand-900/40"
          />
        </div>
        <select
          value={department}
          onChange={(e) => setDepartment(e.target.value)}
          className="rounded-full border border-brand-900/12 bg-white px-4 py-2.5 text-sm font-medium outline-none transition-colors focus:border-brand-900/40"
        >
          {DEPARTMENTS.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
      </div>

      {filtered.length === 0 ? (
        <EmptyState
          title="No employees match your search"
          description="Try a different name, employee ID, job role, or department filter."
        />
      ) : (
        <Card className="overflow-hidden">
          <div className="hidden grid-cols-[2fr_1fr_1fr_1fr_auto] gap-4 border-b border-brand-900/10 bg-brand-50/50 px-6 py-3 text-xs font-semibold uppercase tracking-wider text-neutral-400 sm:grid">
            <span>Employee</span>
            <span>Department</span>
            <span>Job Role</span>
            <span>Compensation</span>
            <span className="text-right">Risk</span>
          </div>
          <div className="divide-y divide-brand-900/8">
            {filtered.map((employee) => (
              <Link
                key={employee.id}
                to={`/employees/${employee.id}`}
                className="grid grid-cols-2 items-center gap-4 px-6 py-4 transition-colors hover:bg-brand-50/40 sm:grid-cols-[2fr_1fr_1fr_1fr_auto]"
              >
                <div className="col-span-2 flex items-center gap-3 sm:col-span-1">
                  <Avatar firstName={employee.firstName} lastName={employee.lastName} color={employee.avatarColor} size="sm" />
                  <div className="min-w-0">
                    <p className="truncate text-sm font-semibold text-brand-900">
                      {employee.firstName} {employee.lastName}
                    </p>
                    <p className="truncate text-xs text-neutral-400">{employee.employeeId}</p>
                  </div>
                </div>
                <span className="text-sm text-neutral-600">{employee.department}</span>
                <span className="text-sm text-neutral-600">{employee.jobRole}</span>
                <span className="text-sm text-neutral-600">{formatCurrency(employee.salary)}</span>
                <div className="flex justify-start sm:justify-end">
                  <RiskBadge risk={employee.attritionRisk} />
                </div>
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

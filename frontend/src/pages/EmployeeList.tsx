import { useEffect, useMemo, useState } from "react";
import { motion, MotionConfig } from "framer-motion";
import { useLocation } from "react-router-dom";
import { getAllEmployees, type Employee } from "../lib/employeeApi";
import { getErrorMessage } from "../lib/apiClient";
import { salaryBandLabel, PROMOTION_BANDS, promotionBandLabel } from "../lib/employeeDisplay";
import { useInfiniteBatch } from "../hooks/useInfiniteBatch";
import { PageHeader } from "../components/ui/PageHeader";
import { EmptyState } from "../components/ui/EmptyState";
import { EmployeeCard } from "../components/employees/EmployeeCard";
import { EmployeeCardSkeleton } from "../components/employees/EmployeeCardSkeleton";

const BATCH_SIZE = 24;
const ALL = "All";

const gridVariants = {
  hidden: {},
  visible: { transition: { staggerChildren: 0.03 } },
};

interface Filters {
  attrition: "All" | "Yes" | "No";
  department: string;
  jobRole: string;
  compensationBand: string;
  gender: string;
  overTime: "All" | "Yes" | "No";
  promotionBand: string;
}

const DEFAULT_FILTERS: Filters = {
  attrition: "All",
  department: ALL,
  jobRole: ALL,
  compensationBand: ALL,
  gender: ALL,
  overTime: "All",
  promotionBand: ALL,
};

export function EmployeeList() {
  const [employees, setEmployees] = useState<Employee[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [query, setQuery] = useState("");
  const [filters, setFilters] = useState<Filters>(DEFAULT_FILTERS);

  const location = useLocation();

  useEffect(() => {
    getAllEmployees()
      .then(setEmployees)
      .catch((err) => setLoadError(getErrorMessage(err, "Couldn't load employees.")));
  }, []);

  // Employee detail's "View <dimension> attrition" links land here with
  // e.g. ?department=Sales - pre-apply that single filter on arrival.
  useEffect(() => {
    if (!employees) return;
    const params = new URLSearchParams(location.search);
    const preset: Partial<Filters> = {};
    if (params.get("department")) preset.department = params.get("department")!;
    if (params.get("jobRole")) preset.jobRole = params.get("jobRole")!;
    if (params.get("compensationBand")) preset.compensationBand = params.get("compensationBand")!;
    if (params.get("gender")) preset.gender = params.get("gender")!;
    if (params.get("overTime")) preset.overTime = params.get("overTime") as "Yes" | "No";
    if (params.get("promotionBand")) preset.promotionBand = params.get("promotionBand")!;
    if (params.get("attrition")) preset.attrition = params.get("attrition") as "Yes" | "No";
    if (Object.keys(preset).length > 0) setFilters((f) => ({ ...f, ...preset }));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [employees, location.search]);

  const options = useMemo(() => {
    const list = employees ?? [];
    return {
      departments: Array.from(new Set(list.map((e) => e.department))).sort(),
      jobRoles: Array.from(new Set(list.map((e) => e.jobRole))).sort(),
      compensationBands: Array.from(new Set(list.map((e) => salaryBandLabel(e.salary)))).sort(
        (a, b) => Number(a.replace(/\D/g, "")) - Number(b.replace(/\D/g, ""))
      ),
      genders: Array.from(new Set(list.map((e) => e.gender))).sort(),
    };
  }, [employees]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return (employees ?? []).filter((e) => {
      const matchesQuery =
        q.length === 0 ||
        `${e.firstName} ${e.lastName}`.toLowerCase().includes(q) ||
        e.employeeId.toLowerCase().includes(q) ||
        e.jobRole.toLowerCase().includes(q);
      const matchesAttrition = filters.attrition === "All" || e.attrition === filters.attrition;
      const matchesDepartment = filters.department === ALL || e.department === filters.department;
      const matchesJobRole = filters.jobRole === ALL || e.jobRole === filters.jobRole;
      const matchesCompensation =
        filters.compensationBand === ALL || salaryBandLabel(e.salary) === filters.compensationBand;
      const matchesGender = filters.gender === ALL || e.gender === filters.gender;
      const matchesOverTime = filters.overTime === "All" || e.overTime === filters.overTime;
      const matchesPromotion =
        filters.promotionBand === ALL || promotionBandLabel(e.yearsSinceLastPromotion) === filters.promotionBand;
      return (
        matchesQuery &&
        matchesAttrition &&
        matchesDepartment &&
        matchesJobRole &&
        matchesCompensation &&
        matchesGender &&
        matchesOverTime &&
        matchesPromotion
      );
    });
  }, [employees, query, filters]);

  const { visibleItems, hasMore, loadingMore, sentinelRef } = useInfiniteBatch(filtered, BATCH_SIZE);

  const activeFilterCount = Object.entries(filters).filter(([key, v]) =>
    key === "attrition" || key === "overTime" ? v !== "All" : v !== ALL
  ).length;

  if (loadError) {
    return (
      <div className="mx-auto max-w-7xl">
        <PageHeader eyebrow="Attrition Explorer" title="Employees" />
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{loadError}</p>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        eyebrow="US-11 – US-16 · Attrition Explorer"
        title="Employees"
        description={
          employees
            ? `${filtered.length} of ${employees.length} employees match${activeFilterCount ? ` · ${activeFilterCount} filter${activeFilterCount > 1 ? "s" : ""} active` : ""}`
            : undefined
        }
      />

      <div className="mb-4 relative">
        <SearchIcon className="pointer-events-none absolute left-4 top-1/2 h-4 w-4 -translate-y-1/2 text-neutral-400" />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Search by name, ID, or job role..."
          disabled={!employees}
          className="w-full rounded-full border border-brand-900/12 bg-white py-2.5 pl-11 pr-4 text-sm outline-none transition-colors placeholder:text-neutral-400 focus:border-brand-900/40 disabled:bg-brand-50/50"
        />
      </div>

      <div className="mb-8 flex flex-col gap-3 border-y border-brand-900/8 py-4">
        <div className="flex flex-wrap items-center gap-2">
          <FilterSelect
            label="Attrition"
            value={filters.attrition}
            options={["All", "Yes", "No"]}
            disabled={!employees}
            onChange={(v) => setFilters((f) => ({ ...f, attrition: v as Filters["attrition"] }))}
          />
          <FilterSelect
            label="Department"
            value={filters.department}
            options={[ALL, ...options.departments]}
            disabled={!employees}
            onChange={(v) => setFilters((f) => ({ ...f, department: v }))}
          />
          <FilterSelect
            label="Job Role"
            value={filters.jobRole}
            options={[ALL, ...options.jobRoles]}
            disabled={!employees}
            onChange={(v) => setFilters((f) => ({ ...f, jobRole: v }))}
          />
          <FilterSelect
            label="Compensation"
            value={filters.compensationBand}
            options={[ALL, ...options.compensationBands]}
            disabled={!employees}
            onChange={(v) => setFilters((f) => ({ ...f, compensationBand: v }))}
          />
          <FilterSelect
            label="Demographics"
            value={filters.gender}
            options={[ALL, ...options.genders]}
            disabled={!employees}
            onChange={(v) => setFilters((f) => ({ ...f, gender: v }))}
          />
          <FilterSelect
            label="Work-Life Balance"
            value={filters.overTime}
            options={["All", "Yes", "No"]}
            disabled={!employees}
            onChange={(v) => setFilters((f) => ({ ...f, overTime: v as Filters["overTime"] }))}
            renderLabel={(v) => (v === "All" ? "All" : `Overtime: ${v}`)}
          />
          <FilterSelect
            label="Career Progression"
            value={filters.promotionBand}
            options={[ALL, ...PROMOTION_BANDS]}
            disabled={!employees}
            onChange={(v) => setFilters((f) => ({ ...f, promotionBand: v }))}
          />
          {activeFilterCount > 0 && (
            <button
              type="button"
              onClick={() => setFilters(DEFAULT_FILTERS)}
              className="ml-1 text-xs font-semibold text-neutral-400 underline underline-offset-4 hover:text-brand-900 transition-colors"
            >
              Clear filters
            </button>
          )}
        </div>
      </div>

      {!employees ? (
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          {Array.from({ length: 6 }, (_, i) => (
            <EmployeeCardSkeleton key={i} />
          ))}
        </div>
      ) : filtered.length === 0 ? (
        <EmptyState
          title="No employees match these filters"
          description="Try loosening a filter or clearing the search."
        />
      ) : (
        <>
          <MotionConfig reducedMotion="user">
            <motion.div
              key={`${query}-${JSON.stringify(filters)}`}
              variants={gridVariants}
              initial="hidden"
              animate="visible"
              className="grid grid-cols-1 gap-4 sm:grid-cols-3"
            >
              {visibleItems.map((employee) => (
                <EmployeeCard key={employee.id} employee={employee} />
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

function FilterSelect({
  label,
  value,
  options,
  disabled,
  onChange,
  renderLabel,
}: {
  label: string;
  value: string;
  options: readonly string[];
  disabled?: boolean;
  onChange: (value: string) => void;
  renderLabel?: (value: string) => string;
}) {
  return (
    <label className="inline-flex items-center gap-1.5 rounded-full border border-brand-900/12 bg-white pl-3 pr-2 py-1.5 text-xs">
      <span className="font-semibold uppercase tracking-wide text-neutral-400">{label}</span>
      <select
        value={value}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value)}
        className="bg-transparent text-xs font-medium text-brand-900 outline-none disabled:text-neutral-400"
      >
        {options.map((o) => (
          <option key={o} value={o}>
            {renderLabel ? renderLabel(o) : o}
          </option>
        ))}
      </select>
    </label>
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

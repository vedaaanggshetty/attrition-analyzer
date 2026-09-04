import { Link } from "react-router-dom";
import { motion } from "framer-motion";
import type { Employee } from "../../lib/employeeApi";
import { avatarColorFor, departmentChipStyleFor } from "../../lib/employeeDisplay";
import { formatCurrency } from "../../lib/utils";
import { cx } from "../../lib/utils";
import { Avatar } from "../ui/Avatar";
import { AttritionBadge } from "../ui/Badge";

const cardVariants = {
  hidden: { opacity: 0, y: 10 },
  visible: { opacity: 1, y: 0 },
};

/**
 * One employee, as a bento tile. `featured` employees (currently: their real
 * `attrition` field is "Yes" - not a computed score) render larger, so the
 * grid's size variation tracks an actual fact about the record.
 */
export function EmployeeCard({ employee, featured = false }: { employee: Employee; featured?: boolean }) {
  return (
    <motion.div variants={cardVariants} className={featured ? "sm:col-span-2 sm:row-span-2" : undefined}>
      <Link
        to={`/employees/${employee.id}`}
        className="group relative flex h-full flex-col justify-between overflow-hidden rounded-xl bg-neutral-50 p-6 transition-all duration-300 hover:bg-neutral-100 hover:shadow-sm border border-brand-900/5"
      >
        <div className="flex flex-col gap-5">
          <div className="flex items-start justify-between gap-3">
            <Avatar
              firstName={employee.firstName}
              lastName={employee.lastName}
              color={avatarColorFor(employee.id)}
              size={featured ? "lg" : "md"}
            />
            <AttritionBadge attrition={employee.attrition} />
          </div>

          <div className="min-w-0">
            <p className={cx("truncate font-semibold text-brand-900 mb-1 tracking-tight", featured ? "text-2xl" : "text-lg")}>
              {employee.firstName} {employee.lastName}
            </p>
            <p className="truncate text-sm font-medium text-neutral-500 mb-4">{employee.jobRole}</p>

            <div className="flex flex-wrap items-center gap-2">
              <span
                className={cx(
                  "inline-flex rounded-md px-2.5 py-1 text-xs font-semibold",
                  departmentChipStyleFor(employee.department)
                )}
              >
                {employee.department}
              </span>
              <span className="truncate text-xs font-mono text-neutral-400 border border-brand-900/10 rounded-md px-2 py-0.5">{employee.employeeId}</span>
            </div>
          </div>

          {featured && (
            <div className="mt-2 border-l-2 border-red-500 pl-3">
              <p className="text-sm font-medium text-red-700 leading-snug">
                Overtime: {employee.overTime} &middot; {employee.yearsSinceLastPromotion} yrs since promotion
              </p>
            </div>
          )}
        </div>

        <div className="mt-8 flex items-end justify-between pt-4 border-t border-brand-900/5">
          <div className="flex flex-col">
            <span className="text-[10px] font-semibold uppercase tracking-wider text-neutral-400 mb-1">Base Salary</span>
            <span className="font-display text-lg font-medium tracking-tight tabular-nums text-brand-900">
              {formatCurrency(employee.salary)}
            </span>
          </div>
          <div className="flex h-8 w-8 items-center justify-center rounded-full bg-white text-neutral-400 shadow-sm transition-transform duration-300 group-hover:bg-brand-900 group-hover:text-white group-hover:scale-110">
            <ChevronIcon className="h-4 w-4 shrink-0" />
          </div>
        </div>
      </Link>
    </motion.div>
  );
}

function ChevronIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <path d="m9 6 6 6-6 6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

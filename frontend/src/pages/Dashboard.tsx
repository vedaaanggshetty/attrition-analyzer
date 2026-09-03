import { Link } from "react-router-dom";
import {
  attritionByCareerProgression,
  attritionByCompensation,
  attritionByDemographics,
  attritionByDepartment,
  attritionByJobRole,
  attritionByWorkLifeBalance,
  employees,
  kpiSummary,
  notifications,
} from "../data/mockData";
import { formatRelativeTime } from "../lib/utils";
import { useParallax } from "../hooks/useParallax";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { BarList } from "../components/ui/BarList";
import { Avatar } from "../components/ui/Avatar";
import { RiskBadge } from "../components/ui/Badge";
import { EmptyState } from "../components/ui/EmptyState";

const highRiskEmployees = employees.filter((e) => e.attritionRisk === "High").slice(0, 5);

export function Dashboard() {
  // Large decorative numeral drifts extremely slowly; the two list cards get
  // only a hint of independent vertical movement for depth.
  const { ref: numeralRef, offset: numeralOffset } = useParallax<HTMLSpanElement>(0.025);
  const { ref: flaggedRef, offset: flaggedOffset } = useParallax<HTMLDivElement>(0.015);
  const { ref: notificationsRef, offset: notificationsOffset } = useParallax<HTMLDivElement>(0.015);

  return (
    <div className="mx-auto max-w-7xl">
      <PageHeader
        eyebrow="Overview"
        title="Analytics"
        description="A live snapshot of attrition risk across your organization."
      />

      <div className="grid gap-4 lg:grid-cols-3">
        {/* The one stat that actually drives HR decisions gets the editorial
            treatment instead of sitting in a row of identical cards. */}
        <div className="relative overflow-hidden rounded-2xl border border-brand-900/10 bg-brand-900 p-7 text-white lg:col-span-2">
          <span
            ref={numeralRef}
            aria-hidden="true"
            className="pointer-events-none absolute -bottom-10 -right-4 select-none font-display font-bold text-white/[0.06]"
            style={{ fontSize: "clamp(6rem, 22vw, 14rem)", lineHeight: 1, transform: `translateY(${numeralOffset}px)` }}
          >
            {kpiSummary.attritionRate}
          </span>
          <div className="relative">
            <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-white/50">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand-300 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-brand-300" />
              </span>
              Attrition Rate
            </p>
            <p className="mt-3 font-display text-7xl font-semibold tracking-tight sm:text-8xl">
              {kpiSummary.attritionRate}
              <span className="font-serif text-4xl italic text-white/50 sm:text-5xl">%</span>
            </p>
            <p className="mt-4 max-w-xs text-sm leading-relaxed text-white/50">
              Up 1.2 points from last quarter &mdash; concentrated in Sales and Technology. Worth a closer look.
            </p>
          </div>
        </div>

        <div className="flex flex-col divide-y divide-brand-900/8 rounded-2xl border border-brand-900/10 bg-white">
          <CompactStat label="Total Employees" value={kpiSummary.totalEmployees.toString()} icon={<IconUsers />} />
          <CompactStat
            label="High-Risk Employees"
            value={kpiSummary.highRiskEmployees.toString()}
            hint={`${Math.round((kpiSummary.highRiskEmployees / kpiSummary.totalEmployees) * 100)}% of workforce`}
            icon={<IconAlert />}
          />
          <CompactStat label="Departments" value={kpiSummary.departments.toString()} icon={<IconBuilding />} />
        </div>
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-5">
        <Card className="p-6 lg:col-span-3">
          <div className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-1">
            <SectionTitle title="Attrition by Department" />
            {/* Editorial annotation: the single number that matters, set apart from the list */}
            <p className="text-right text-xs leading-tight text-neutral-400">
              <span className="font-serif text-lg italic text-brand-900">{attritionByDepartment[0]?.groupLabel}</span>
              <br />
              is the highest-risk department
            </p>
          </div>
          <div className="mt-5">
            <BarList data={attritionByDepartment} />
          </div>
        </Card>
        <Card className="p-6 lg:col-span-2">
          <SectionTitle title="Attrition by Job Role" />
          <div className="mt-5">
            <BarList data={attritionByJobRole.slice(0, 6)} />
          </div>
        </Card>
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-3">
        <Card className="p-6">
          <SectionTitle title="Compensation Analysis" subtitle="Attrition by salary band" />
          <div className="mt-5">
            <BarList data={attritionByCompensation} />
          </div>
        </Card>
        <Card className="p-6">
          <SectionTitle title="Demographic Insights" subtitle="Attrition by gender" />
          <div className="mt-5">
            <BarList data={attritionByDemographics} />
          </div>
        </Card>
        <Card className="p-6">
          <SectionTitle title="Work-Life Balance" subtitle="Attrition by overtime status" />
          <div className="mt-5">
            <BarList data={attritionByWorkLifeBalance} />
          </div>
        </Card>
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-2">
        <Card className="p-6">
          <SectionTitle title="Career Progression" subtitle="Attrition by years since last promotion" />
          <div className="mt-5">
            <BarList data={attritionByCareerProgression} />
          </div>
        </Card>

        <Card ref={flaggedRef} className="p-6" style={{ transform: `translateY(${flaggedOffset}px)` }}>
          <SectionTitle title="Recent Flagged Employees" />
          <div className="mt-5 flex flex-col divide-y divide-brand-900/8">
            {highRiskEmployees.length === 0 ? (
              <EmptyState title="No flagged employees" description="Flagged, high-risk employees will show up here." />
            ) : (
              highRiskEmployees.map((employee) => (
                <Link
                  key={employee.id}
                  to={`/employees/${employee.id}`}
                  className="flex items-center gap-3 py-3.5 transition-colors hover:opacity-70"
                >
                  <Avatar firstName={employee.firstName} lastName={employee.lastName} color={employee.avatarColor} size="sm" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-brand-900">
                      {employee.firstName} {employee.lastName}
                    </p>
                    <p className="truncate text-xs text-neutral-500">
                      {employee.jobRole} &middot; {employee.department}
                    </p>
                  </div>
                  <RiskBadge risk={employee.attritionRisk} />
                </Link>
              ))
            )}
          </div>
        </Card>
      </div>

      <Card ref={notificationsRef} className="mt-5 p-6" style={{ transform: `translateY(${notificationsOffset}px)` }}>
        <SectionTitle title="Notifications" subtitle="Comments and flags from your HR team" />
        <div className="mt-5 flex flex-col divide-y divide-brand-900/8">
          {notifications.length === 0 ? (
            <EmptyState title="No notifications yet" description="Flag an employee to start a thread here." />
          ) : (
            notifications.map((note) => (
              <div key={note.id} className="flex items-start gap-3 py-4">
                <Avatar
                  firstName={note.employeeName.split(" ")[0]}
                  lastName={note.employeeName.split(" ")[1] ?? ""}
                  size="sm"
                />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
                    <p className="text-sm font-semibold text-brand-900">{note.employeeName}</p>
                    <span className="text-xs text-neutral-400">{formatRelativeTime(note.createdAt)}</span>
                  </div>
                  <p className="mt-1 text-sm text-neutral-600">{note.comment}</p>
                </div>
              </div>
            ))
          )}
        </div>
      </Card>
    </div>
  );
}

function CompactStat({
  label,
  value,
  hint,
  icon,
}: {
  label: string;
  value: string;
  hint?: string;
  icon: React.ReactNode;
}) {
  return (
    <div className="flex items-center gap-3 p-5">
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-brand-50 text-brand-900">
        {icon}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium text-neutral-500">{label}</p>
        <div className="flex items-baseline gap-2">
          <p className="font-display text-2xl font-semibold tracking-tight text-brand-900">{value}</p>
          {hint && <p className="truncate text-xs text-neutral-400">{hint}</p>}
        </div>
      </div>
    </div>
  );
}

function SectionTitle({ title, subtitle }: { title: string; subtitle?: string }) {
  return (
    <div>
      <h2 className="font-display text-base font-semibold text-brand-900">{title}</h2>
      {subtitle && <p className="mt-0.5 text-xs text-neutral-500">{subtitle}</p>}
    </div>
  );
}

function IconUsers() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" className="h-4 w-4">
      <circle cx="9" cy="8" r="3.25" />
      <path d="M3 20c0-3.5 2.7-6 6-6s6 2.5 6 6" />
      <path d="M16 8.5a3 3 0 1 1 3.2 3" />
      <path d="M21 20c0-2.8-1.8-5-4.2-5.7" />
    </svg>
  );
}
function IconAlert() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" className="h-4 w-4">
      <path d="M12 3 2 20h20L12 3Z" strokeLinejoin="round" />
      <path d="M12 10v4" strokeLinecap="round" />
      <circle cx="12" cy="17" r="0.5" fill="currentColor" />
    </svg>
  );
}
function IconBuilding() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" className="h-4 w-4">
      <rect x="4" y="3" width="16" height="18" rx="1.5" />
      <path d="M9 8h1M14 8h1M9 12h1M14 12h1M9 16h1M14 16h1" strokeLinecap="round" />
    </svg>
  );
}

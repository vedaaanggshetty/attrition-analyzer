import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  getAllEmployees,
  getAttritionByCareerProgression,
  getAttritionByCompensation,
  getAttritionByDemographics,
  getAttritionByDepartment,
  getAttritionByJobRole,
  getAttritionByWorkLifeBalance,
  type AttritionAnalysis,
  type Employee,
} from "../lib/employeeApi";
import { deleteNotification, getMyNotifications, type Notification } from "../lib/notificationApi";
import { attritionRisk, avatarColorFor } from "../lib/employeeDisplay";
import { ApiError } from "../lib/apiClient";
import { cx, formatRelativeTime } from "../lib/utils";
import { useAuth } from "../context/AuthContext";
import { useParallax } from "../hooks/useParallax";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { BarList } from "../components/ui/BarList";
import { Avatar } from "../components/ui/Avatar";
import { RiskBadge } from "../components/ui/Badge";
import { EmptyState } from "../components/ui/EmptyState";
import { Skeleton } from "../components/ui/Skeleton";

interface Analysis {
  department: AttritionAnalysis[];
  jobRole: AttritionAnalysis[];
  compensation: AttritionAnalysis[];
  demographics: AttritionAnalysis[];
  workLifeBalance: AttritionAnalysis[];
  careerProgression: AttritionAnalysis[];
}

export function Dashboard() {
  const { user } = useAuth();
  const greetingName = user?.fullName?.split(" ")[0] ?? user?.email.split("@")[0] ?? "there";
  const today = new Intl.DateTimeFormat("en-US", { day: "numeric", month: "long", year: "numeric" }).format(
    new Date()
  );

  const { ref: numeralRef, offset: numeralOffset } = useParallax<HTMLSpanElement>(0.025);
  const { ref: flaggedRef, offset: flaggedOffset } = useParallax<HTMLDivElement>(0.015);
  const { ref: notificationsRef, offset: notificationsOffset } = useParallax<HTMLDivElement>(0.015);

  const [employees, setEmployees] = useState<Employee[] | null>(null);
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [notifications, setNotifications] = useState<Notification[] | null>(null);
  const [notifError, setNotifError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  useEffect(() => {
    Promise.all([
      getAllEmployees(),
      getAttritionByDepartment(),
      getAttritionByJobRole(),
      getAttritionByCompensation(),
      getAttritionByDemographics(),
      getAttritionByWorkLifeBalance(),
      getAttritionByCareerProgression(),
    ])
      .then(([employeeList, department, jobRole, compensation, demographics, workLifeBalance, careerProgression]) => {
        setEmployees(employeeList);
        setAnalysis({ department, jobRole, compensation, demographics, workLifeBalance, careerProgression });
      })
      .catch((err) => setLoadError(err instanceof ApiError ? err.message : "Couldn't load analytics."));

    getMyNotifications()
      .then(setNotifications)
      .catch((err) => setNotifError(err instanceof ApiError ? err.message : "Couldn't load notifications."));
  }, []);

  const kpis = useMemo(() => {
    if (!employees) return null;
    const attritionCount = employees.filter((e) => e.attrition === "Yes").length;
    const highRisk = employees.filter((e) => attritionRisk(e) === "High");
    return {
      total: employees.length,
      attritionRate: employees.length ? Math.round((attritionCount / employees.length) * 1000) / 10 : 0,
      departments: new Set(employees.map((e) => e.department)).size,
      highRiskCount: highRisk.length,
      highRiskEmployees: highRisk.slice(0, 5),
    };
  }, [employees]);

  async function handleDeleteNotification(id: number) {
    setDeletingId(id);
    try {
      await deleteNotification(id);
      setNotifications((prev) => (prev ? prev.filter((n) => n.id !== id) : prev));
    } catch (err) {
      setNotifError(err instanceof ApiError ? err.message : "Couldn't delete that notification.");
    } finally {
      setDeletingId(null);
    }
  }

  if (loadError) {
    return (
      <div className="mx-auto max-w-7xl">
        <PageHeader eyebrow="Overview" title="Analytics" />
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{loadError}</p>
      </div>
    );
  }

  return (
    <div className="relative mx-auto max-w-7xl">
      {/* Ambient wash so the glassmorphic stat panel below has something to
          actually blur, instead of sitting on flat white. */}
      <div
        className="animate-drift-slow pointer-events-none absolute -right-10 -top-16 -z-10 h-72 w-72 rounded-full bg-brand-300/25 blur-3xl"
        aria-hidden="true"
      />
      <div
        className="animate-drift pointer-events-none absolute -left-10 top-24 -z-10 h-56 w-56 rounded-full bg-violet-300/15 blur-3xl"
        aria-hidden="true"
      />

      <div className="mb-8 flex flex-col justify-between gap-4 sm:flex-row sm:items-start">
        <div>
          <h1 className="font-display text-3xl font-semibold capitalize tracking-tight text-brand-900 sm:text-4xl">
            Hello, {greetingName}
          </h1>
          <p className="mt-2 max-w-2xl text-sm text-neutral-500">
            A live snapshot of attrition risk across your organization.
          </p>
        </div>
        <span className="inline-flex shrink-0 items-center gap-2 rounded-full border border-brand-900/12 bg-white px-4 py-2 text-sm font-medium text-brand-900">
          <CalendarIcon className="h-4 w-4 text-neutral-400" />
          {today}
        </span>
      </div>

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
            {kpis ? kpis.attritionRate : "--"}
          </span>
          <div className="relative">
            <p className="flex items-center gap-2 text-xs font-semibold uppercase tracking-wider text-white/50">
              <span className="relative flex h-2 w-2">
                <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-brand-300 opacity-75" />
                <span className="relative inline-flex h-2 w-2 rounded-full bg-brand-300" />
              </span>
              Attrition Rate
            </p>
            {kpis ? (
              <p className="mt-3 font-display text-7xl font-semibold tracking-tight sm:text-8xl">
                {kpis.attritionRate}
                <span className="font-serif text-4xl italic text-white/50 sm:text-5xl">%</span>
              </p>
            ) : (
              <Skeleton className="mt-3 h-16 w-32 bg-white/10" />
            )}
            <p className="mt-4 max-w-xs text-sm leading-relaxed text-white/50">
              Share of employees marked as attrition across your full workforce.
            </p>
          </div>
        </div>

        <div className="flex flex-col divide-y divide-brand-900/8 rounded-2xl border border-white/60 bg-white/70 shadow-[0_8px_30px_-12px_rgba(13,71,161,0.15)] backdrop-blur-xl">
          <CompactStat label="Total Employees" value={kpis?.total.toString()} icon={<IconUsers />} tone="blue" />
          <CompactStat
            label="High-Risk Employees"
            value={kpis?.highRiskCount.toString()}
            hint={kpis && kpis.total ? `${Math.round((kpis.highRiskCount / kpis.total) * 100)}% of workforce` : undefined}
            icon={<IconAlert />}
            tone="amber"
          />
          <CompactStat label="Departments" value={kpis?.departments.toString()} icon={<IconBuilding />} tone="violet" />
        </div>
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-5">
        <Card className="p-6 lg:col-span-3">
          <div className="flex flex-wrap items-baseline justify-between gap-x-6 gap-y-1">
            <SectionTitle title="Attrition by Department" />
            {analysis && (
              <p className="text-right text-xs leading-tight text-neutral-400">
                <span className="font-serif text-lg italic text-brand-900">{analysis.department[0]?.groupLabel}</span>
                <br />
                is the highest-risk department
              </p>
            )}
          </div>
          <div className="mt-5">{analysis ? <BarList data={analysis.department} /> : <BarListSkeleton />}</div>
        </Card>
        <Card className="p-6 lg:col-span-2">
          <SectionTitle title="Attrition by Job Role" />
          <div className="mt-5">
            {analysis ? <BarList data={analysis.jobRole.slice(0, 6)} /> : <BarListSkeleton />}
          </div>
        </Card>
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-3">
        <Card className="p-6">
          <SectionTitle title="Compensation Analysis" subtitle="Attrition by salary band" />
          <div className="mt-5">{analysis ? <BarList data={analysis.compensation} /> : <BarListSkeleton />}</div>
        </Card>
        <Card className="p-6">
          <SectionTitle title="Demographic Insights" subtitle="Attrition by gender" />
          <div className="mt-5">{analysis ? <BarList data={analysis.demographics} /> : <BarListSkeleton />}</div>
        </Card>
        <Card className="p-6">
          <SectionTitle title="Work-Life Balance" subtitle="Attrition by overtime status" />
          <div className="mt-5">
            {analysis ? <BarList data={analysis.workLifeBalance} /> : <BarListSkeleton />}
          </div>
        </Card>
      </div>

      <div className="mt-5 grid gap-5 lg:grid-cols-2">
        <Card className="p-6">
          <SectionTitle title="Career Progression" subtitle="Attrition by years since last promotion" />
          <div className="mt-5">
            {analysis ? <BarList data={analysis.careerProgression} /> : <BarListSkeleton />}
          </div>
        </Card>

        <Card ref={flaggedRef} className="p-6" style={{ transform: `translateY(${flaggedOffset}px)` }}>
          <SectionTitle title="Recent Flagged Employees" />
          <div className="mt-5 flex flex-col divide-y divide-brand-900/8">
            {!kpis ? (
              <div className="flex flex-col gap-3 py-1">
                {Array.from({ length: 3 }, (_, i) => (
                  <Skeleton key={i} className="h-11 w-full" />
                ))}
              </div>
            ) : kpis.highRiskEmployees.length === 0 ? (
              <EmptyState title="No flagged employees" description="Flagged, high-risk employees will show up here." />
            ) : (
              kpis.highRiskEmployees.map((employee) => (
                <Link
                  key={employee.id}
                  to={`/employees/${employee.id}`}
                  className="flex items-center gap-3 py-3.5 transition-colors hover:opacity-70"
                >
                  <Avatar firstName={employee.firstName} lastName={employee.lastName} color={avatarColorFor(employee.id)} size="sm" />
                  <div className="min-w-0 flex-1">
                    <p className="truncate text-sm font-semibold text-brand-900">
                      {employee.firstName} {employee.lastName}
                    </p>
                    <p className="truncate text-xs text-neutral-500">
                      {employee.jobRole} &middot; {employee.department}
                    </p>
                  </div>
                  <RiskBadge risk={attritionRisk(employee)} />
                </Link>
              ))
            )}
          </div>
        </Card>
      </div>

      <Card ref={notificationsRef} className="mt-5 p-6" style={{ transform: `translateY(${notificationsOffset}px)` }}>
        <SectionTitle title="Notifications" subtitle="Comments and flags from your HR team" />
        {notifError && <p className="mt-3 text-sm font-medium text-red-600">{notifError}</p>}
        <div className="mt-5 flex flex-col divide-y divide-brand-900/8">
          {!notifications ? (
            <div className="flex flex-col gap-3 py-1">
              {Array.from({ length: 3 }, (_, i) => (
                <Skeleton key={i} className="h-14 w-full" />
              ))}
            </div>
          ) : notifications.length === 0 ? (
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
                <button
                  type="button"
                  onClick={() => handleDeleteNotification(note.id)}
                  disabled={deletingId === note.id}
                  aria-label="Delete notification"
                  className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-neutral-400 transition-colors hover:bg-red-50 hover:text-red-600 disabled:opacity-50"
                >
                  <TrashIcon className="h-4 w-4" />
                </button>
              </div>
            ))
          )}
        </div>
      </Card>
    </div>
  );
}

function BarListSkeleton() {
  return (
    <div className="flex flex-col gap-4">
      {Array.from({ length: 5 }, (_, i) => (
        <Skeleton key={i} className="h-6 w-full" />
      ))}
    </div>
  );
}

const STAT_TONES = {
  blue: "bg-brand-50 text-brand-900",
  amber: "bg-amber-50 text-amber-600",
  violet: "bg-violet-50 text-violet-600",
} as const;

function CompactStat({
  label,
  value,
  hint,
  icon,
  tone = "blue",
}: {
  label: string;
  value: string | undefined;
  hint?: string;
  icon: React.ReactNode;
  tone?: keyof typeof STAT_TONES;
}) {
  return (
    <div className="flex items-center gap-3 p-5">
      <div className={cx("flex h-9 w-9 shrink-0 items-center justify-center rounded-full", STAT_TONES[tone])}>
        {icon}
      </div>
      <div className="min-w-0 flex-1">
        <p className="text-xs font-medium text-neutral-500">{label}</p>
        <div className="flex items-baseline gap-2">
          {value !== undefined ? (
            <p className="font-display text-2xl font-semibold tracking-tight text-brand-900">{value}</p>
          ) : (
            <Skeleton className="h-6 w-10" />
          )}
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

function CalendarIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M8 3v4M16 3v4M3 10h18" strokeLinecap="round" />
    </svg>
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
function TrashIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <path d="M4 7h16M9 7V4h6v3M6 7l1 13a1 1 0 0 0 1 1h8a1 1 0 0 0 1-1l1-13" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

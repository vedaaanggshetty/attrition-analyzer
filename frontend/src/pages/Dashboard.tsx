import { useEffect, useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
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
import { getErrorMessage } from "../lib/apiClient";
import { cx, formatRelativeTime } from "../lib/utils";
import { useAuth } from "../context/AuthContext";
import { Avatar } from "../components/ui/Avatar";
import { Skeleton } from "../components/ui/Skeleton";

interface Analysis {
  department: AttritionAnalysis[];
  jobRole: AttritionAnalysis[];
  compensation: AttritionAnalysis[];
  demographics: AttritionAnalysis[];
  workLifeBalance: AttritionAnalysis[];
  careerProgression: AttritionAnalysis[];
}

// Round to 1 decimal, return as string "19.0"
function fmt(n: number): string {
  return (Math.round(n * 10) / 10).toFixed(1);
}

// Bar/number color reflects the real attrition rate on that row - not a
// separate score. Neutral brand color under 15%, amber 15-30%, red above -
// the only place red/orange appear on this page.
function severity(rate: number): "neutral" | "amber" | "red" {
  if (rate >= 30) return "red";
  if (rate >= 15) return "amber";
  return "neutral";
}

const SEVERITY_BAR: Record<ReturnType<typeof severity>, string> = {
  neutral: "bg-brand-500",
  amber: "bg-amber-500",
  red: "bg-red-500",
};

const SEVERITY_TEXT: Record<ReturnType<typeof severity>, string> = {
  neutral: "text-brand-700",
  amber: "text-amber-600",
  red: "text-red-600",
};

export function Dashboard() {
  const { user } = useAuth();
  const greetingName = user?.fullName?.split(" ")[0] ?? user?.email?.split("@")[0] ?? "there";

  const [employees, setEmployees] = useState<Employee[] | null>(null);
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [notifications, setNotifications] = useState<Notification[] | null>(null);
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
      .catch((err) => setLoadError(getErrorMessage(err, "Couldn't load analytics.")));

    getMyNotifications()
      .then(setNotifications)
      .catch(() => setNotifications([]));
  }, []);

  // Employee detail links here as e.g. /dashboard#department - React Router
  // doesn't scroll to a #hash on client-side navigation the way a full page
  // load would.
  const location = useLocation();
  useEffect(() => {
    if (!location.hash) return;
    const el = document.querySelector(location.hash);
    el?.scrollIntoView({ behavior: "smooth", block: "start" });
  }, [location.hash, analysis]);

  const kpis = useMemo(() => {
    if (!employees) return null;
    const attritionCount = employees.filter((e) => e.attrition === "Yes").length;
    return {
      total: employees.length,
      attritionCount,
      attritionRate: employees.length ? (attritionCount / employees.length) * 100 : 0,
      departments: new Set(employees.map((e) => e.department)).size,
      overtimeCount: employees.filter((e) => e.overTime === "Yes").length,
    };
  }, [employees]);

  // The single highest-attrition group in each of the six US-11–US-16
  // dimensions - all straight from the backend's own ranked aggregation,
  // nothing computed or scored on the frontend.
  const topGroups = useMemo(() => {
    if (!analysis) return null;
    return [
      { label: "Department", anchor: "department", queryKey: "department", row: analysis.department[0] },
      { label: "Job Role", anchor: "job-role", queryKey: "jobRole", row: analysis.jobRole[0] },
      { label: "Compensation", anchor: "compensation", queryKey: "compensationBand", row: analysis.compensation[0] },
      { label: "Demographics", anchor: "demographics", queryKey: "gender", row: analysis.demographics[0] },
      {
        label: "Work-Life Balance",
        anchor: "work-life-balance",
        queryKey: "overTime",
        row: analysis.workLifeBalance[0],
      },
      {
        label: "Career Progression",
        anchor: "career-progression",
        queryKey: "promotionBand",
        row: analysis.careerProgression[0],
      },
    ].filter(
      (g): g is { label: string; anchor: string; queryKey: string; row: AttritionAnalysis } => g.row !== undefined
    );
  }, [analysis]);

  async function handleDeleteNotification(id: number) {
    setDeletingId(id);
    try {
      await deleteNotification(id);
      setNotifications((prev) => prev?.filter((n) => n.id !== id) ?? prev);
    } catch {
      // silently ignore — don't block the page
    } finally {
      setDeletingId(null);
    }
  }

  if (loadError) {
    return (
      <div>
        <PageTitle name={greetingName} />
        <div className="mt-6 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {loadError}
        </div>
      </div>
    );
  }

  const topDept = topGroups?.find((g) => g.anchor === "department")?.row;

  return (
    <div className="flex flex-col gap-5">
      <PageTitle name={greetingName} />

      {/* ── KPI cards ─────────────────────────────────────────────── */}
      <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 xl:grid-cols-4">
        <KpiCard
          icon={<UsersIcon className="h-5 w-5" />}
          label="Total Employees"
          value={kpis ? kpis.total.toLocaleString() : null}
          sub={kpis ? `across ${kpis.departments} departments` : null}
        />
        <KpiCard
          icon={<PulseIcon className="h-5 w-5" />}
          label="Overall Attrition Rate"
          value={kpis ? `${fmt(kpis.attritionRate)}%` : null}
          sub={kpis ? `${kpis.attritionCount} of ${kpis.total} employees` : null}
          tone={kpis ? severity(kpis.attritionRate) : "neutral"}
          emphasis
        />
        <KpiCard
          icon={<FlagIcon className="h-5 w-5" />}
          label="Top Attrition Department"
          value={topDept ? topDept.groupLabel : null}
          sub={topDept ? `${fmt(topDept.attritionRate)}% attrition` : null}
          tone={topDept ? severity(topDept.attritionRate) : "neutral"}
        />
        <KpiCard
          icon={<ClockIcon className="h-5 w-5" />}
          label="Working Overtime"
          value={kpis ? kpis.overtimeCount.toLocaleString() : null}
          sub={kpis ? `${fmt((kpis.overtimeCount / Math.max(kpis.total, 1)) * 100)}% of workforce` : null}
        />
      </div>

      {/* ── US-11 Department + US-12 Job Role ─────────────────────── */}
      <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-2">
        <div id="department" className="scroll-mt-24">
          <AnalysisCard
            eyebrow="US-11"
            title="Attrition by Department"
            data={analysis?.department.slice(0, 7) ?? null}
            queryKey="department"
          />
        </div>
        <div id="job-role" className="scroll-mt-24">
          <AnalysisCard
            eyebrow="US-12"
            title="Attrition by Job Role"
            data={analysis?.jobRole.slice(0, 7) ?? null}
            queryKey="jobRole"
          />
        </div>
      </div>

      {/* ── US-13–US-16: Compensation, Demographics, Work-Life, Career ─ */}
      <div className="grid grid-cols-1 gap-3.5 sm:grid-cols-2 xl:grid-cols-4">
        <div id="compensation" className="scroll-mt-24">
          <AnalysisCard
            eyebrow="US-13"
            title="Compensation"
            data={analysis?.compensation ?? null}
            queryKey="compensationBand"
            compact
          />
        </div>
        <div id="demographics" className="scroll-mt-24">
          <AnalysisCard
            eyebrow="US-14"
            title="Demographics"
            data={analysis?.demographics ?? null}
            queryKey="gender"
            compact
          />
        </div>
        <div id="work-life-balance" className="scroll-mt-24">
          <AnalysisCard
            eyebrow="US-15"
            title="Work-Life Balance"
            data={analysis?.workLifeBalance ?? null}
            queryKey="overTime"
            compact
          />
        </div>
        <div id="career-progression" className="scroll-mt-24">
          <AnalysisCard
            eyebrow="US-16"
            title="Career Progression"
            data={analysis?.careerProgression ?? null}
            queryKey="promotionBand"
            compact
          />
        </div>
      </div>

      {/* ── Top Attrition Groups + Notifications ─────────────────── */}
      <div className="grid grid-cols-1 gap-3.5 lg:grid-cols-5">
        {/* Top Attrition Groups - the single highest-rate group per US-11–US-16
            dimension, straight from the backend's own ranking. No score, no
            per-employee risk - just where the six analyses currently peak. */}
        <Card className="lg:col-span-3">
          <div className="mb-1 flex items-baseline justify-between">
            <h2 className="font-serif text-lg font-semibold italic text-brand-900">Top Attrition Groups</h2>
          </div>
          <p className="mb-4 text-sm text-neutral-500">Highest-attrition group in each of the six analyses</p>
          {!topGroups ? (
            <WatchlistSkeleton />
          ) : topGroups.length === 0 ? (
            <p className="text-sm text-neutral-500">No analysis data available.</p>
          ) : (
            <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2">
              {topGroups.map((g) => {
                const s = severity(g.row.attritionRate);
                return (
                  <Link
                    key={g.anchor}
                    to={`/employees?${g.queryKey}=${encodeURIComponent(g.row.groupLabel)}`}
                    className="group flex items-center justify-between gap-3 rounded-2xl border border-neutral-200/70 bg-neutral-50/60 px-4 py-3 transition-all duration-300 hover:-translate-y-0.5 hover:border-brand-900/15 hover:bg-white hover:shadow-[0_8px_20px_-12px_rgba(13,71,161,0.25)]"
                  >
                    <div className="min-w-0">
                      <p className="text-[10px] font-semibold uppercase tracking-wider text-neutral-400">{g.label}</p>
                      <p className="truncate text-sm font-semibold text-brand-900">{g.row.groupLabel}</p>
                    </div>
                    <span className={cx("num shrink-0 text-sm font-bold", SEVERITY_TEXT[s])}>
                      {fmt(g.row.attritionRate)}%
                    </span>
                  </Link>
                );
              })}
            </div>
          )}
        </Card>

        {/* Notifications */}
        <Card className="glass-card-dark !border-0 lg:col-span-2 text-white">
          <div className="mb-1 flex items-center justify-between">
            <h2 className="font-serif text-lg font-semibold italic text-white">Recent Notifications</h2>
            <Link to="/notifications" className="text-xs font-semibold text-white/50 hover:text-white transition-colors">
              View all →
            </Link>
          </div>
          <p className="mb-4 text-sm text-white/40">Notes sent to the HR team</p>
          {!notifications ? (
            <ActivitySkeleton />
          ) : notifications.length === 0 ? (
            <p className="text-sm text-white/40">No notifications yet. Send one from an employee's detail page.</p>
          ) : (
            <div className="flex flex-col gap-1">
              {notifications.slice(0, 5).map((note) => (
                <div
                  key={note.id}
                  className="group flex items-start gap-3 rounded-xl px-2 py-2.5 transition-colors hover:bg-white/5"
                >
                  <Avatar
                    firstName={note.employeeName.split(" ")[0]}
                    lastName={note.employeeName.split(" ")[1] ?? ""}
                    size="sm"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex flex-wrap items-baseline gap-2">
                      <span className="text-sm font-semibold text-white">{note.employeeName}</span>
                      <span className="text-xs text-white/35">{formatRelativeTime(note.createdAt)}</span>
                    </div>
                    <p className="mt-0.5 line-clamp-2 text-sm leading-snug text-white/55">{note.comment}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleDeleteNotification(note.id)}
                    disabled={deletingId === note.id}
                    aria-label="Dismiss"
                    className="flex h-6 w-6 shrink-0 items-center justify-center rounded text-white/30 opacity-0 transition-all hover:bg-white/10 hover:text-white group-hover:opacity-100 disabled:opacity-30"
                  >
                    <XIcon className="h-3 w-3" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function Card({ className, children }: { className?: string; children: React.ReactNode }) {
  return (
    <div
      className={cx(
        "glass-card rounded-3xl p-5 transition-shadow duration-300 hover:shadow-[0_1px_2px_rgba(15,23,42,0.04),0_20px_40px_-16px_rgba(13,71,161,0.18)]",
        className
      )}
    >
      {children}
    </div>
  );
}

function PageTitle({ name }: { name: string }) {
  const now = new Date();
  const dateStr = now.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" });
  return (
    <div className="flex items-end justify-between gap-4">
      <div>
        <h1 className="font-serif text-3xl font-semibold italic tracking-tight text-brand-900 sm:text-4xl">
          Good morning, {name}
        </h1>
        <p className="mt-1.5 text-sm text-neutral-500">{dateStr}</p>
      </div>
      <Link
        to="/employees"
        className="group hidden shrink-0 items-center gap-2 rounded-full bg-brand-900 py-2.5 pl-4 pr-3 text-xs font-semibold text-white shadow-[0_8px_20px_-8px_rgba(13,71,161,0.6)] transition-all duration-300 hover:bg-brand-800 hover:pr-4 hover:shadow-[0_10px_24px_-8px_rgba(13,71,161,0.7)] sm:flex"
      >
        Open Attrition Explorer
        <ChevronIcon className="h-3 w-3 transition-transform duration-300 group-hover:translate-x-0.5" />
      </Link>
    </div>
  );
}

function KpiCard({
  icon,
  label,
  value,
  sub,
  tone = "neutral",
  emphasis = false,
}: {
  icon: React.ReactNode;
  label: string;
  value: string | null;
  sub: string | null;
  tone?: "neutral" | "amber" | "red";
  emphasis?: boolean;
}) {
  const iconTone =
    tone === "red"
      ? "bg-gradient-to-br from-red-50 to-red-100/60 text-red-600"
      : tone === "amber"
        ? "bg-gradient-to-br from-amber-50 to-amber-100/60 text-amber-600"
        : "bg-gradient-to-br from-brand-50 to-brand-100/60 text-brand-700";
  return (
    <Card className="group flex flex-col justify-between hover:-translate-y-0.5">
      <div className="flex items-center justify-between">
        <span className="text-[11px] font-semibold uppercase tracking-wider text-neutral-400">{label}</span>
        <span
          className={cx(
            "flex h-10 w-10 shrink-0 items-center justify-center rounded-2xl transition-transform duration-300 group-hover:scale-105",
            iconTone
          )}
        >
          {icon}
        </span>
      </div>
      <div className="mt-5">
        {value ? (
          <p
            className={cx(
              "num truncate font-display font-semibold tracking-tight text-brand-900",
              emphasis ? "text-4xl" : "text-3xl",
              tone === "red" && "text-red-600",
              tone === "amber" && "text-amber-600"
            )}
          >
            {value}
          </p>
        ) : (
          <Skeleton className="h-9 w-20 rounded-md" />
        )}
        {sub ? (
          <p className="mt-1.5 text-xs text-neutral-500">{sub}</p>
        ) : (
          <Skeleton className="mt-2 h-3 w-28 rounded" />
        )}
      </div>
    </Card>
  );
}

function AnalysisCard({
  eyebrow,
  title,
  data,
  queryKey,
  compact = false,
}: {
  eyebrow: string;
  title: string;
  data: AttritionAnalysis[] | null;
  queryKey: string;
  compact?: boolean;
}) {
  const top = data?.[0];
  return (
    <Card className="h-full">
      <div className="mb-4 flex items-center justify-between">
        <div>
          <p className="text-[10px] font-semibold uppercase tracking-wider text-brand-300">{eyebrow}</p>
          <h3 className="font-serif text-base font-semibold italic text-brand-900">{title}</h3>
        </div>
        {!compact && top ? <MiniSparkline data={data.slice(0, 7)} /> : null}
      </div>
      {compact && top ? (
        <div className="mb-4 flex items-baseline justify-between rounded-2xl bg-brand-50/50 px-3.5 py-3">
          <div className="min-w-0">
            <p className="truncate text-xs font-semibold text-brand-900">{top.groupLabel}</p>
            <p className="text-[10px] text-neutral-400">Highest attrition group</p>
          </div>
          <span className={cx("num shrink-0 text-lg font-bold font-display", SEVERITY_TEXT[severity(top.attritionRate)])}>
            {fmt(top.attritionRate)}%
          </span>
        </div>
      ) : null}
      {data === null ? (
        <AnalysisSkeleton rows={compact ? 4 : 6} />
      ) : (
        <RankedBarList data={compact ? data.slice(0, 5) : data} queryKey={queryKey} compact={compact} />
      )}
    </Card>
  );
}

function MiniSparkline({ data }: { data: AttritionAnalysis[] }) {
  const max = Math.max(...data.map((d) => d.attritionRate), 1);
  return (
    <div className="flex h-9 items-end gap-1">
      {data.map((row) => {
        const s = severity(row.attritionRate);
        const h = Math.max((row.attritionRate / max) * 100, 10);
        return (
          <div
            key={row.groupLabel}
            title={`${row.groupLabel}: ${fmt(row.attritionRate)}%`}
            className={cx("w-2 rounded-full transition-all duration-500", SEVERITY_BAR[s])}
            style={{ height: `${h}%`, opacity: 0.85 }}
          />
        );
      })}
    </div>
  );
}

function RankedBarList({
  data,
  queryKey,
  compact = false,
}: {
  data: AttritionAnalysis[];
  queryKey: string;
  compact?: boolean;
}) {
  if (data.length === 0) return <p className="text-sm text-neutral-500">No data.</p>;
  const max = Math.max(...data.map((d) => d.attritionRate), 1);
  return (
    <ul className={cx("flex flex-col", compact ? "gap-3" : "gap-3.5")}>
      {data.map((row) => {
        const pct = fmt(row.attritionRate);
        const barWidth = Math.max((row.attritionRate / max) * 100, 3);
        const s = severity(row.attritionRate);
        return (
          <li key={row.groupLabel}>
            <Link
              to={`/employees?${queryKey}=${encodeURIComponent(row.groupLabel)}`}
              className="group block"
            >
              <div className="mb-2 flex items-baseline justify-between gap-3">
                <span
                  className={cx(
                    "truncate font-medium text-brand-900 group-hover:text-brand-700",
                    compact ? "text-xs" : "text-sm"
                  )}
                >
                  {row.groupLabel}
                </span>
                <span className={cx("num shrink-0", compact ? "text-xs" : "text-sm")}>
                  <span className={cx("font-bold", SEVERITY_TEXT[s])}>{pct}%</span>{" "}
                  <span className="text-neutral-400">
                    ({row.attritionCount}/{row.totalEmployees})
                  </span>
                </span>
              </div>
              <div
                className={cx(
                  "w-full overflow-hidden rounded-full bg-neutral-100 transition-colors group-hover:bg-neutral-200/70",
                  compact ? "h-2.5" : "h-3"
                )}
              >
                <div
                  className={cx(
                    "h-full rounded-full shadow-[inset_0_1px_1px_rgba(255,255,255,0.25)] transition-[width] duration-700 ease-out",
                    SEVERITY_BAR[s]
                  )}
                  style={{ width: `${barWidth}%` }}
                />
              </div>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

function AnalysisSkeleton({ rows, compact = false }: { rows: number; compact?: boolean }) {
  return (
    <ul className={`flex flex-col ${compact ? "gap-3" : "gap-3.5"} animate-pulse`}>
      {Array.from({ length: rows }, (_, i) => (
        <li key={i}>
          <div className="mb-2 flex justify-between">
            <div className="h-3 rounded bg-neutral-100" style={{ width: `${40 + i * 7}%` }} />
            <div className="h-3 w-10 rounded bg-neutral-100" />
          </div>
          <div className="h-2 w-full overflow-hidden rounded-full bg-neutral-100">
            <div className="h-full rounded-full bg-neutral-200" style={{ width: `${70 - i * 8}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}

function WatchlistSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-2 sm:grid-cols-2 animate-pulse">
      {Array.from({ length: 6 }, (_, i) => (
        <div key={i} className="rounded-xl border border-neutral-200/70 bg-neutral-50/60 px-4 py-3">
          <div className="h-2.5 w-16 rounded bg-neutral-200" />
          <div className="mt-2 h-3.5 w-24 rounded bg-neutral-200" />
        </div>
      ))}
    </div>
  );
}

function ActivitySkeleton() {
  return (
    <div className="flex flex-col gap-5 animate-pulse">
      {Array.from({ length: 3 }, (_, i) => (
        <div key={i} className="flex items-start gap-3">
          <div className="h-8 w-8 shrink-0 rounded-full bg-white/10" />
          <div className="flex-1 space-y-1.5">
            <div className="h-3 w-24 rounded bg-white/10" />
            <div className="h-2.5 w-full rounded bg-white/10" />
            <div className="h-2.5 w-3/4 rounded bg-white/10" />
          </div>
        </div>
      ))}
    </div>
  );
}

function ChevronIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="m9 6 6 6-6 6" />
    </svg>
  );
}
function XIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" {...props}>
      <path d="M18 6L6 18M6 6l12 12" />
    </svg>
  );
}
function UsersIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <circle cx="9" cy="8" r="3.25" />
      <path d="M3 20c0-3.5 2.7-6 6-6s6 2.5 6 6" />
      <path d="M16 8.5a3 3 0 1 1 3.2 3" />
      <path d="M21 20c0-2.8-1.8-5-4.2-5.7" />
    </svg>
  );
}
function PulseIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <path d="M3 12h4l2-7 4 14 2-7h6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
function FlagIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <path d="M5 21V4" strokeLinecap="round" />
      <path d="M5 4h13l-3 4.5L18 13H5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
function ClockIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3.5 2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

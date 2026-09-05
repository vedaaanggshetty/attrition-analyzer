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

function fmt(n: number): string {
  return (Math.round(n * 10) / 10).toFixed(1);
}

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

// ─── Age-group buckets (frontend-only, derived from Employee.age) ──────────
const AGE_BUCKETS = [
  { label: "< 25",  min: 0,  max: 24 },
  { label: "25–34", min: 25, max: 34 },
  { label: "35–44", min: 35, max: 44 },
  { label: "45–54", min: 45, max: 54 },
  { label: "55+",   min: 55, max: 999 },
];

function computeAgeGroups(employees: Employee[]) {
  return AGE_BUCKETS.map(({ label, min, max }) => {
    const group = employees.filter((e) => e.age >= min && e.age <= max);
    const attrited = group.filter((e) => e.attrition === "Yes").length;
    const rate = group.length > 0 ? (attrited / group.length) * 100 : 0;
    return { label, total: group.length, attrited, rate };
  });
}

export function Dashboard() {
  const { user: _user } = useAuth();

  const [employees, setEmployees] = useState<Employee[] | null>(null);
  const [analysis, setAnalysis] = useState<Analysis | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [loaderLeaving, setLoaderLeaving] = useState(false);

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
        setLoaderLeaving(true);
        window.setTimeout(() => setLoading(false), 480);
      })
      .catch((err) => {
        setLoadError(getErrorMessage(err, "Couldn't load analytics."));
        setLoaderLeaving(true);
        window.setTimeout(() => setLoading(false), 480);
      });

    getMyNotifications()
      .then(setNotifications)
      .catch(() => setNotifications([]));
  }, []);

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

  const ageGroups = useMemo(() => {
    if (!employees) return null;
    return computeAgeGroups(employees);
  }, [employees]);

  const topGroups = useMemo(() => {
    if (!analysis) return null;
    return [
      { label: "Department", anchor: "department", queryKey: "department", row: analysis.department[0] },
      { label: "Job Role", anchor: "job-role", queryKey: "jobRole", row: analysis.jobRole[0] },
      { label: "Compensation", anchor: "compensation", queryKey: "compensationBand", row: analysis.compensation[0] },
      { label: "Demographics", anchor: "demographics", queryKey: "gender", row: analysis.demographics[0] },
      { label: "Work-Life Balance", anchor: "work-life-balance", queryKey: "overTime", row: analysis.workLifeBalance[0] },
      { label: "Career Progression", anchor: "career-progression", queryKey: "promotionBand", row: analysis.careerProgression[0] },
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
      // silently ignore
    } finally {
      setDeletingId(null);
    }
  }

  // ── Dashboard-level splash loader ─────────────────────────────────────────
  if (loading) {
    return (
      <div
        className={cx(
          "fixed inset-0 z-50 flex flex-col items-center justify-center bg-brand-900 text-white transition-transform duration-[480ms] ease-[cubic-bezier(0.65,0,0.35,1)]",
          loaderLeaving && "-translate-y-full"
        )}
      >
        <p className="mb-5 text-[10px] font-semibold uppercase tracking-[0.3em] text-white/40">
          Loading analytics
        </p>
        <div className="flex items-end gap-1">
          {[0.6, 1, 0.75, 0.45, 0.85].map((h, i) => (
            <div
              key={i}
              className="w-1.5 rounded-full bg-white/30"
              style={{
                height: `${h * 32}px`,
                animation: `loaderBar 1.1s ease-in-out ${i * 0.12}s infinite alternate`,
              }}
            />
          ))}
        </div>
        <style>{`
          @keyframes loaderBar {
            from { opacity: 0.25; transform: scaleY(0.4); }
            to   { opacity: 1;    transform: scaleY(1);   }
          }
        `}</style>
      </div>
    );
  }

  if (loadError) {
    return (
      <div>
        <DashboardHeader />
        <div className="mt-4 rounded border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {loadError}
        </div>
      </div>
    );
  }

  const topDept = topGroups?.find((g) => g.anchor === "department")?.row;

  return (
    <div className="flex flex-col gap-0">
      <style>{`
        @keyframes slideUpFade {
          from { opacity: 0; transform: translateY(16px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .animate-section {
          animation: slideUpFade 0.7s cubic-bezier(0.16, 1, 0.3, 1) both;
        }
      `}</style>
      
      {/* ── Header ──────────────────────────────────────────────────── */}
      <div className="animate-section pb-5" style={{ animationDelay: "0ms" }}>
        <DashboardHeader />
      </div>

      <hr className="border-neutral-100" />

      {/* ── KPI Strip ───────────────────────────────────────────────── */}
      <div className="animate-section grid grid-cols-2 divide-x divide-y divide-neutral-100 border-b border-neutral-100 bg-white xl:grid-cols-4 xl:divide-y-0" style={{ animationDelay: "100ms" }}>
        <div className="px-5 py-4">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-neutral-400">Total Employees</p>
          <div className="mt-2">
            {kpis ? <span className="num text-2xl font-bold text-ink-900">{kpis.total.toLocaleString()}</span> : <Skeleton className="h-7 w-16 rounded" />}
          </div>
          {kpis ? <p className="mt-1 text-xs text-neutral-400">across {kpis.departments} departments</p> : <Skeleton className="mt-1.5 h-3 w-24 rounded" />}
        </div>

        <div className="px-5 py-4">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-neutral-400">Overall Attrition Rate</p>
          <div className="mt-2">
            {kpis ? (
              <span className={cx("num text-2xl font-bold",
                severity(kpis.attritionRate) === "red" ? "text-red-600"
                : severity(kpis.attritionRate) === "amber" ? "text-amber-600"
                : "text-ink-900")}>
                {fmt(kpis.attritionRate)}%
              </span>
            ) : <Skeleton className="h-7 w-16 rounded" />}
          </div>
          {kpis ? <p className="mt-1 text-xs text-neutral-400">{kpis.attritionCount} of {kpis.total} employees</p> : <Skeleton className="mt-1.5 h-3 w-28 rounded" />}
        </div>

        <div className="px-5 py-4">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-neutral-400">Top Attrition Dept.</p>
          <div className="mt-2">
            {topDept ? <span className="block truncate text-xl font-bold leading-tight text-ink-900">{topDept.groupLabel}</span> : <Skeleton className="h-7 w-28 rounded" />}
          </div>
          {topDept ? <p className="mt-1 text-xs text-neutral-400">{fmt(topDept.attritionRate)}% attrition</p> : <Skeleton className="mt-1.5 h-3 w-20 rounded" />}
        </div>

        <div className="px-5 py-4">
          <p className="text-[10px] font-semibold uppercase tracking-widest text-neutral-400">Working Overtime</p>
          <div className="mt-2">
            {kpis ? <span className="num text-2xl font-bold text-ink-900">{kpis.overtimeCount.toLocaleString()}</span> : <Skeleton className="h-7 w-16 rounded" />}
          </div>
          {kpis ? <p className="mt-1 text-xs text-neutral-400">{fmt((kpis.overtimeCount / Math.max(kpis.total, 1)) * 100)}% of workforce</p> : <Skeleton className="mt-1.5 h-3 w-24 rounded" />}
        </div>
      </div>

      {/* ── US-11 + US-12 ───────────────────────────────────────────── */}
      <div className="animate-section grid grid-cols-1 items-start gap-0 border-b border-neutral-100 lg:grid-cols-2 lg:divide-x lg:divide-neutral-100" style={{ animationDelay: "200ms" }}>
        <div id="department" className="scroll-mt-24 py-6 lg:pr-8">

          <h2 className="font-serif text-lg font-semibold italic text-ink-900">Attrition by Department</h2>
          <p className="mb-4 mt-0.5 text-xs text-neutral-400">Comparison of attrition rates across departments</p>
          {analysis === null ? (
            <AnalysisSkeleton rows={3} />
          ) : (
            <ul className="flex h-full flex-col justify-between gap-[22px] pb-2">
              {analysis.department.slice(0, 7).map((row) => {
                const max = Math.max(...analysis.department.map((d) => d.attritionRate), 1);
                const barW = Math.max((row.attritionRate / max) * 100, 3);
                const s = severity(row.attritionRate);
                return (
                  <li key={row.groupLabel}>
                    <Link to={`/employees?department=${encodeURIComponent(row.groupLabel)}`} className="group block">
                      <div className="mb-1.5 flex items-baseline justify-between gap-3">
                        <span className="text-sm font-medium text-ink-900 transition-colors group-hover:text-brand-700">{row.groupLabel}</span>
                        <span className="num shrink-0 text-sm">
                          <span className={cx("font-bold", SEVERITY_TEXT[s])}>{fmt(row.attritionRate)}%</span>
                          {" "}<span className="text-neutral-400">({row.attritionCount}/{row.totalEmployees})</span>
                        </span>
                      </div>
                      <div className="h-3 w-full overflow-hidden rounded-full bg-neutral-100">
                        <div className={cx("h-full rounded-full transition-[width] duration-700 ease-out", SEVERITY_BAR[s])} style={{ width: `${barW}%` }} />
                      </div>
                    </Link>
                  </li>
                );
              })}
            </ul>
          )}
        </div>

        <div id="job-role" className="scroll-mt-24 border-t border-neutral-100 py-6 lg:border-0 lg:pl-8">

          <h2 className="font-serif text-lg font-semibold italic text-ink-900">Attrition by Job Role</h2>
          <p className="mb-4 mt-0.5 text-xs text-neutral-400">Attrition rates across key job roles</p>
          {analysis === null ? (
            <AnalysisSkeleton rows={6} compact />
          ) : (
            <div>
              <div className="mb-2 flex items-center justify-between border-b border-neutral-100 pb-1.5">
                <span className="text-[10px] font-semibold uppercase tracking-wider text-neutral-400">Job Role</span>
                <span className="text-[10px] font-semibold uppercase tracking-wider text-neutral-400">Attrition Rate</span>
              </div>
              <ul className="flex flex-col">
                {analysis.jobRole.slice(0, 7).map((row) => {
                  const max = Math.max(...analysis.jobRole.map((d) => d.attritionRate), 1);
                  const barW = Math.max((row.attritionRate / max) * 100, 3);
                  const s = severity(row.attritionRate);
                  return (
                    <li key={row.groupLabel} className="border-b border-neutral-50 last:border-0">
                      <Link to={`/employees?jobRole=${encodeURIComponent(row.groupLabel)}`} className="group flex items-center gap-3 py-2">
                        <span className="w-36 shrink-0 truncate text-xs text-neutral-700 transition-colors group-hover:text-brand-700">{row.groupLabel}</span>
                        <div className="flex flex-1 items-center gap-2">
                          <div className="h-1.5 flex-1 overflow-hidden rounded-full bg-neutral-100">
                            <div className={cx("h-full rounded-full transition-[width] duration-700 ease-out", SEVERITY_BAR[s])} style={{ width: `${barW}%` }} />
                          </div>
                          <span className={cx("num w-28 shrink-0 text-right text-xs font-bold", SEVERITY_TEXT[s])}>
                            {fmt(row.attritionRate)}%{" "}
                            <span className="font-normal text-neutral-400">({row.attritionCount}/{row.totalEmployees})</span>
                          </span>
                        </div>
                      </Link>
                    </li>
                  );
                })}
              </ul>
            </div>
          )}
        </div>
      </div>

      {/* ── US-13–16 + Age Group ──────────────────────────────────────── */}
      <div className="animate-section grid grid-cols-1 border-b border-neutral-100 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5" style={{ animationDelay: "300ms" }}>
        <div id="compensation" className="scroll-mt-24 py-6 sm:border-r sm:border-neutral-100 sm:pr-6">

          <h3 className="font-serif text-base font-semibold italic text-ink-900 leading-tight">Compensation</h3>
          <p className="mb-3 mt-0.5 text-xs text-neutral-400">Attrition by pay band</p>
          {analysis === null ? <AnalysisSkeleton rows={4} compact /> : <SlimBarList data={analysis.compensation.slice(0, 5)} queryKey="compensationBand" />}
        </div>

        <div id="age-group" className="scroll-mt-24 border-t border-neutral-100 py-6 sm:border-0 sm:border-r sm:border-neutral-100 sm:px-6 xl:px-6">

          <h3 className="font-serif text-base font-semibold italic text-ink-900 leading-tight">Attrition by Age Group</h3>
          <p className="mb-5 mt-0.5 text-xs text-neutral-400">Distribution across age groups</p>
          {ageGroups === null ? (
            <div className="flex items-end justify-between px-2" style={{ height: 96 }}>
              {[60, 85, 75, 50, 40].map((h, i) => (
                <div key={i} className="w-6 animate-pulse rounded-t-sm bg-neutral-100 sm:w-8" style={{ height: `${h}%` }} />
              ))}
            </div>
          ) : (
            <AgeGroupBars groups={ageGroups} />
          )}
        </div>

        <div id="demographics" className="scroll-mt-24 border-t border-neutral-100 py-6 sm:border-0 lg:border-r lg:border-neutral-100 sm:px-6 xl:px-6">

          <h3 className="font-serif text-base font-semibold italic text-ink-900 leading-tight">Attrition by Gender</h3>
          <p className="mb-3 mt-0.5 text-xs text-neutral-400">Comparison across genders</p>
          {analysis === null ? (
            <AnalysisSkeleton rows={3} compact />
          ) : (
            <GenderDonut data={analysis.demographics} overallRate={kpis?.attritionRate ?? 0} />
          )}
        </div>

        <div id="work-life-balance" className="scroll-mt-24 border-t border-neutral-100 py-6 sm:border-r sm:border-neutral-100 sm:px-6 xl:px-6">

          <h3 className="font-serif text-base font-semibold italic text-ink-900 leading-tight">Overtime vs Attrition</h3>
          <p className="mb-3 mt-0.5 text-xs text-neutral-400">Attrition rate by overtime status</p>
          {analysis === null ? <AnalysisSkeleton rows={3} compact /> : <SlimBarList data={analysis.workLifeBalance.slice(0, 4)} queryKey="overTime" />}
        </div>

        <div id="career-progression" className="scroll-mt-24 border-t border-neutral-100 py-6 sm:pl-6 xl:border-0">

          <h3 className="font-serif text-base font-semibold italic text-ink-900 leading-tight">Career Progression</h3>
          <p className="mb-3 mt-0.5 text-xs text-neutral-400">Attrition by promotion band</p>
          {analysis === null ? <AnalysisSkeleton rows={4} compact /> : <SlimBarList data={analysis.careerProgression.slice(0, 5)} queryKey="promotionBand" />}
        </div>
      </div>

      {/* ── Top Attrition Groups ─────────────────────────────────────── */}
      <div className="animate-section py-10" style={{ animationDelay: "400ms" }}>
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-ink-900">Top Attrition Groups</h2>
          <p className="mt-0.5 text-sm text-neutral-400">Highest-attrition group in each of the six analyses</p>
        </div>
        
        {!topGroups ? (
          <WatchlistSkeleton />
        ) : topGroups.length === 0 ? (
          <p className="text-sm text-neutral-500">No analysis data available.</p>
        ) : (
          <div className="grid grid-cols-1 gap-5 md:grid-cols-2 lg:grid-cols-3">
            {topGroups.map((g) => {
              const s = severity(g.row.attritionRate);
              return (
                <Link
                  key={g.anchor}
                  to={`/employees?${g.queryKey}=${encodeURIComponent(g.row.groupLabel)}`}
                  className="group flex flex-col justify-between border border-neutral-200 bg-white p-5 transition-all duration-300 hover:-translate-y-1 hover:border-neutral-300 hover:shadow-lg lg:p-6"
                >
                  <div className="mb-4 min-w-0 w-full">
                    <p className="mb-1.5 text-[10px] font-semibold uppercase tracking-wider text-neutral-400 line-clamp-1">{g.label}</p>
                    <p className="truncate text-lg font-medium text-ink-900">{g.row.groupLabel}</p>
                  </div>
                  <div className="mt-auto flex w-full items-end justify-between">
                    <span className="text-[10px] font-semibold uppercase tracking-widest text-neutral-300">Rate</span>
                    <span className={cx("num text-2xl font-bold leading-none", SEVERITY_TEXT[s])}>
                      {fmt(g.row.attritionRate)}%
                    </span>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </div>

      {/* ── Notifications ────────────────────────────────────────────── */}
      <div className="animate-section pb-10 pt-4" style={{ animationDelay: "500ms" }}>
        <div className="glass-card-dark rounded-xl p-6 text-white md:max-w-xl">
          <div className="mb-2 flex items-center justify-between">
            <h2 className="text-base font-semibold text-white">Recent Notifications</h2>
            <Link to="/notifications" className="text-xs text-white/50 hover:text-white transition-colors">
              View all →
            </Link>
          </div>
          <p className="mb-5 text-xs text-white/40">Notes sent to the HR team</p>
          {!notifications ? (
            <ActivitySkeleton />
          ) : notifications.length === 0 ? (
            <p className="text-sm text-white/40">No notifications yet. Send one from an employee's detail page.</p>
          ) : (
            <div className="flex flex-col gap-1.5">
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
                    <p className="mt-1 line-clamp-2 text-sm leading-snug text-white/55">{note.comment}</p>
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
        </div>
      </div>

      <div className="h-6" />
    </div>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function SectionEyebrow({ label }: { label: string }) {
  return <p className="mb-0.5 text-[10px] font-semibold uppercase tracking-widest text-neutral-400">{label}</p>;
}

function DashboardHeader() {
  const now = new Date();
  const dateStr = now.toLocaleDateString("en-US", { weekday: "short", month: "long", day: "numeric" });
  return (
    <div className="flex items-start justify-between gap-4">
      <div>
        <p className="text-[10px] font-semibold uppercase tracking-widest text-neutral-400">Workforce Overview</p>
        <h1 className="mt-1 font-serif text-2xl font-semibold italic tracking-tight text-ink-900">
          Attrition at a glance
        </h1>
        <p className="mt-0.5 text-sm text-neutral-500">Key insights from across your organization.</p>
      </div>
      {/* Calendar chip */}
      <div className="flex shrink-0 items-center gap-1.5 border border-neutral-100 bg-white px-3 py-1.5 text-xs text-neutral-500">
        <CalendarIcon className="h-3.5 w-3.5 text-neutral-300" />
        <span>{dateStr}</span>
      </div>
    </div>
  );
}

// ─── Gender Donut (SVG) ──────────────────────────────────────────────────────

const DONUT_COLORS: Record<string, string> = {
  Male:   "#f97316",
  Female: "#3f3f46",
  Other:  "#d4d4d8",
};
const DONUT_DEFAULT = "#e5e7eb";

function GenderDonut({ data, overallRate }: { data: AttritionAnalysis[]; overallRate: number }) {
  const total = data.reduce((s, r) => s + r.totalEmployees, 0);
  if (total === 0) return <p className="text-xs text-neutral-500">No data.</p>;

  const R = 38;
  const CX = 52;
  const CY = 52;
  const strokeW = 13;
  const circ = 2 * Math.PI * R;

  let offset = 0;
  const slices = data.map((row) => {
    const frac = row.totalEmployees / total;
    const dash = frac * circ;
    const slice = { row, frac, dash, offset };
    offset += dash;
    return slice;
  });

  return (
    <div className="flex items-center gap-4">
      <div className="relative shrink-0">
        <svg width="104" height="104" viewBox="0 0 104 104">
          <circle cx={CX} cy={CY} r={R} fill="none" stroke="#f3f4f6" strokeWidth={strokeW} />
          {slices.map(({ row, dash, offset: o }) => (
            <circle
              key={row.groupLabel}
              cx={CX} cy={CY} r={R}
              fill="none"
              stroke={DONUT_COLORS[row.groupLabel] ?? DONUT_DEFAULT}
              strokeWidth={strokeW}
              strokeDasharray={`${dash} ${circ - dash}`}
              strokeDashoffset={circ / 4 - o}
              strokeLinecap="butt"
            />
          ))}
        </svg>
        <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
          <span className="num text-sm font-bold text-ink-900 leading-none">{fmt(overallRate)}%</span>
          <span className="text-[9px] text-neutral-400 mt-0.5">Overall</span>
        </div>
      </div>
      <ul className="flex flex-col gap-1.5">
        {data.map((row) => {
          const pct = total > 0 ? ((row.totalEmployees / total) * 100).toFixed(1) : "0.0";
          return (
            <li key={row.groupLabel}>
              <Link to={`/employees?gender=${encodeURIComponent(row.groupLabel)}`} className="group flex items-center gap-2">
                <span className="h-2 w-2 shrink-0 rounded-full" style={{ background: DONUT_COLORS[row.groupLabel] ?? DONUT_DEFAULT }} />
                <span className="text-xs text-neutral-700 group-hover:text-brand-700 transition-colors">{row.groupLabel}</span>
                <span className="num ml-auto pl-2 text-xs font-semibold text-neutral-500">{pct}%</span>
              </Link>
            </li>
          );
        })}
      </ul>
    </div>
  );
}

// ─── Age-group bar chart ──────────────────────────────────────────────────────

function AgeGroupBars({ groups }: { groups: ReturnType<typeof computeAgeGroups> }) {
  const maxRate = Math.max(...groups.map((g) => g.rate), 1);
  return (
    <div className="flex items-end justify-between px-2">
      {groups.map((g) => {
        const barH = Math.max((g.rate / maxRate) * 88, 4);
        const s = severity(g.rate);
        return (
          <div key={g.label} className="flex flex-col items-center gap-1.5">
            <span className={cx("num text-[10px] font-bold leading-none", SEVERITY_TEXT[s])}>
              {fmt(g.rate)}%
            </span>
            <div
              className={cx("w-6 rounded-t-sm sm:w-8", SEVERITY_BAR[s])}
              style={{ height: `${barH}px`, opacity: 0.85 }}
            />
            <span className="text-[10px] leading-tight text-neutral-400 whitespace-nowrap">{g.label}</span>
          </div>
        );
      })}
    </div>
  );
}

// ─── Slim bar list ────────────────────────────────────────────────────────────

function SlimBarList({ data, queryKey }: { data: AttritionAnalysis[]; queryKey: string }) {
  if (data.length === 0) return <p className="text-xs text-neutral-500">No data.</p>;
  const max = Math.max(...data.map((d) => d.attritionRate), 1);
  return (
    <ul className="flex flex-col gap-3">
      {data.map((row) => {
        const barW = Math.max((row.attritionRate / max) * 100, 3);
        const s = severity(row.attritionRate);
        return (
          <li key={row.groupLabel}>
            <Link to={`/employees?${queryKey}=${encodeURIComponent(row.groupLabel)}`} className="group block transition-transform hover:translate-x-1">
              <div className="mb-1 flex items-baseline justify-between gap-2">
                <span className="truncate text-xs font-medium text-ink-900 transition-colors group-hover:text-brand-700">{row.groupLabel}</span>
                <span className={cx("num shrink-0 text-xs font-bold", SEVERITY_TEXT[s])}>{fmt(row.attritionRate)}%</span>
              </div>
              <div className="h-1.5 w-full overflow-hidden rounded-full bg-neutral-100">
                <div className={cx("h-full rounded-full transition-[width] duration-700 ease-out", SEVERITY_BAR[s])} style={{ width: `${barW}%` }} />
              </div>
            </Link>
          </li>
        );
      })}
    </ul>
  );
}

// ─── Skeletons ────────────────────────────────────────────────────────────────

function AnalysisSkeleton({ rows, compact = false }: { rows: number; compact?: boolean }) {
  return (
    <ul className={`flex flex-col ${compact ? "gap-3" : "gap-4"} animate-pulse`}>
      {Array.from({ length: rows }, (_, i) => (
        <li key={i}>
          <div className="mb-1.5 flex justify-between">
            <div className="h-3 rounded bg-neutral-100" style={{ width: `${40 + i * 7}%` }} />
            <div className="h-3 w-10 rounded bg-neutral-100" />
          </div>
          <div className="h-1.5 w-full overflow-hidden bg-neutral-100">
            <div className="h-full bg-neutral-200" style={{ width: `${70 - i * 8}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}

function WatchlistSkeleton() {
  return (
    <div className="grid grid-cols-1 gap-px border border-neutral-100 bg-neutral-100 sm:grid-cols-2 animate-pulse">
      {Array.from({ length: 6 }, (_, i) => (
        <div key={i} className="bg-white px-3.5 py-2.5">
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

// ─── Icons ────────────────────────────────────────────────────────────────────

function XIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" {...props}>
      <path d="M18 6L6 18M6 6l12 12" />
    </svg>
  );
}

function CalendarIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" />
    </svg>
  );
}


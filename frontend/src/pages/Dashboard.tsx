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
import { formatRelativeTime } from "../lib/utils";
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
        <div className="mt-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {loadError}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-0">
      <PageTitle name={greetingName} />

      {/* ── Primary metric ────────────────────────────────────────── */}
      <div className="mt-10 grid grid-cols-1 gap-0 lg:grid-cols-[1fr_1px_1fr_1px_1fr] lg:divide-x lg:divide-brand-900/8">
        <PrimaryMetric
          label="Overall Attrition Rate"
          value={kpis ? `${fmt(kpis.attritionRate)}%` : null}
          sub={kpis ? `${kpis.attritionCount} of ${kpis.total} employees` : null}
          emphasis
        />
        <div className="hidden lg:block" /> {/* divider */}
        <PrimaryMetric
          label="Top Department by Attrition"
          value={topGroups ? topGroups.find((g) => g.anchor === "department")?.row.groupLabel ?? "—" : null}
          sub={
            topGroups
              ? `${fmt(topGroups.find((g) => g.anchor === "department")?.row.attritionRate ?? 0)}% attrition`
              : null
          }
          danger
        />
        <div className="hidden lg:block" />
        <PrimaryMetric
          label="Departments Tracked"
          value={kpis ? String(kpis.departments) : null}
          sub="active workforce segments"
        />
      </div>

      <div className="mt-10 h-px w-full bg-brand-900/8" />

      {/* ── US-11 Department + US-12 Job Role ─────────────────────── */}
      <div className="mt-10 grid grid-cols-1 gap-14 lg:grid-cols-2">
        <div id="department" className="scroll-mt-24">
          <AnalysisSection
            eyebrow="01 · US-11"
            title="Attrition by Department"
            data={analysis?.department.slice(0, 7) ?? null}
          />
        </div>
        <div id="job-role" className="scroll-mt-24">
          <AnalysisSection
            eyebrow="02 · US-12"
            title="Attrition by Job Role"
            data={analysis?.jobRole.slice(0, 7) ?? null}
          />
        </div>
      </div>

      <div className="mt-12 h-px w-full bg-brand-900/8" />

      {/* ── US-13–US-16: Compensation, Demographics, Work-Life, Career ─ */}
      <div className="mt-10 grid grid-cols-1 gap-14 sm:grid-cols-2 lg:grid-cols-4">
        <div id="compensation" className="scroll-mt-24">
          <SmallAnalysisSection eyebrow="03 · US-13" title="Compensation" data={analysis?.compensation ?? null} />
        </div>
        <div id="demographics" className="scroll-mt-24">
          <SmallAnalysisSection eyebrow="04 · US-14" title="Demographics" data={analysis?.demographics ?? null} />
        </div>
        <div id="work-life-balance" className="scroll-mt-24">
          <SmallAnalysisSection
            eyebrow="05 · US-15"
            title="Work-Life Balance"
            data={analysis?.workLifeBalance ?? null}
          />
        </div>
        <div id="career-progression" className="scroll-mt-24">
          <SmallAnalysisSection
            eyebrow="06 · US-16"
            title="Career Progression"
            data={analysis?.careerProgression ?? null}
          />
        </div>
      </div>

      <div className="mt-12 h-px w-full bg-brand-900/8" />

      {/* ── Top Attrition Groups + Activity ──────────────────────── */}
      <div className="mt-10 grid grid-cols-1 gap-14 lg:grid-cols-2">
        {/* Top Attrition Groups - the single highest-rate group per US-11–US-16
            dimension, straight from the backend's own ranking. No score, no
            per-employee risk - just where the six analyses currently peak. */}
        <div>
          <SectionLabel>Top Attrition Groups</SectionLabel>
          <p className="mt-1 mb-6 text-sm text-brand-500">
            Highest-attrition group in each analysis
          </p>
          {!topGroups ? (
            <WatchlistSkeleton />
          ) : topGroups.length === 0 ? (
            <p className="text-sm text-brand-500">No analysis data available.</p>
          ) : (
            <div className="flex flex-col divide-y divide-brand-900/6">
              {topGroups.map((g) => (
                <Link
                  key={g.anchor}
                  to={`/employees?${g.queryKey}=${encodeURIComponent(g.row.groupLabel)}`}
                  className="group flex items-center justify-between gap-3 py-3 hover:bg-brand-50 -mx-2 px-2 rounded-lg transition-colors"
                >
                  <div className="min-w-0">
                    <p className="text-xs font-semibold uppercase tracking-wider text-brand-300">{g.label}</p>
                    <p className="truncate text-sm font-semibold text-brand-900">{g.row.groupLabel}</p>
                  </div>
                  <div className="flex shrink-0 items-center gap-2">
                    <span className="num text-sm font-semibold text-red-600">{fmt(g.row.attritionRate)}%</span>
                    <ChevronIcon className="h-3.5 w-3.5 text-brand-300 opacity-0 group-hover:opacity-100 transition-opacity" />
                  </div>
                </Link>
              ))}
            </div>
          )}
        </div>

        {/* Activity feed */}
        <div>
          <div className="flex items-baseline justify-between">
            <SectionLabel>Recent Notifications</SectionLabel>
            <Link to="/notifications" className="text-xs font-semibold text-brand-300 hover:text-brand-900 transition-colors">
              View all →
            </Link>
          </div>
          <p className="mt-1 mb-6 text-sm text-brand-500">Notes sent to the HR team</p>
          {!notifications ? (
            <ActivitySkeleton />
          ) : notifications.length === 0 ? (
            <p className="text-sm text-brand-500">No notifications yet. Send one from an employee's detail page.</p>
          ) : (
            <div className="flex flex-col gap-4">
              {notifications.slice(0, 5).map((note) => (
                <div key={note.id} className="group flex items-start gap-3">
                  <Avatar
                    firstName={note.employeeName.split(" ")[0]}
                    lastName={note.employeeName.split(" ")[1] ?? ""}
                    size="sm"
                  />
                  <div className="min-w-0 flex-1">
                    <div className="flex items-baseline gap-2 flex-wrap">
                      <span className="text-sm font-semibold text-brand-900">
                        {note.employeeName}
                      </span>
                      <span className="text-xs text-brand-300">
                        {formatRelativeTime(note.createdAt)}
                      </span>
                    </div>
                    <p className="mt-0.5 text-sm text-brand-500 leading-snug">{note.comment}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => handleDeleteNotification(note.id)}
                    disabled={deletingId === note.id}
                    aria-label="Dismiss"
                    className="flex h-6 w-6 shrink-0 items-center justify-center rounded text-brand-300 opacity-0 group-hover:opacity-100 hover:bg-red-50 hover:text-red-500 transition-all disabled:opacity-30"
                  >
                    <XIcon className="h-3 w-3" />
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function PageTitle({ name }: { name: string }) {
  const now = new Date();
  const dateStr = now.toLocaleDateString("en-US", { weekday: "long", month: "long", day: "numeric" });
  return (
    <div className="flex items-end justify-between gap-4 border-b border-brand-900/8 pb-6">
      <div>
        <h1 className="font-display text-3xl font-semibold tracking-tight text-brand-900">
          Good morning, {name}
        </h1>
        <p className="mt-1 text-sm text-brand-500">{dateStr}</p>
      </div>
      <Link
        to="/employees"
        className="hidden text-xs font-semibold text-brand-300 underline underline-offset-4 hover:text-brand-900 transition-colors sm:block"
      >
        Open Attrition Explorer →
      </Link>
    </div>
  );
}

function PrimaryMetric({
  label,
  value,
  sub,
  emphasis = false,
  danger = false,
}: {
  label: string;
  value: string | null;
  sub: string | null;
  emphasis?: boolean;
  danger?: boolean;
}) {
  return (
    <div className="flex flex-col py-6 lg:px-8 first:lg:pl-0 last:lg:pr-0">
      <span className="text-[10px] font-semibold uppercase tracking-[0.14em] text-brand-300">
        {label}
      </span>
      <div className="mt-3 flex items-baseline gap-2">
        {value ? (
          <span
            className={`num font-display font-semibold tracking-tight leading-none ${
              emphasis
                ? "text-5xl text-brand-900"
                : danger
                ? "text-4xl text-red-600"
                : "text-4xl text-brand-900"
            }`}
          >
            {value}
          </span>
        ) : (
          <Skeleton className="h-12 w-24 rounded-md bg-brand-100" />
        )}
      </div>
      {sub ? (
        <span className="mt-1.5 text-xs text-brand-500">{sub}</span>
      ) : (
        <Skeleton className="mt-2 h-3 w-32 rounded bg-brand-100" />
      )}
    </div>
  );
}

function SectionLabel({ children }: { children: React.ReactNode }) {
  return (
    <h2 className="text-[10px] font-semibold uppercase tracking-[0.14em] text-brand-300">
      {children}
    </h2>
  );
}

function AnalysisSection({
  eyebrow,
  title,
  data,
}: {
  eyebrow: string;
  title: string;
  data: AttritionAnalysis[] | null;
}) {
  return (
    <div>
      <SectionLabel>
        <span className="text-brand-300">{eyebrow}</span> {title}
      </SectionLabel>
      <div className="mt-5">
        {data === null ? (
          <AnalysisSkeleton rows={6} />
        ) : (
          <RankedBarList data={data} />
        )}
      </div>
    </div>
  );
}

function SmallAnalysisSection({
  eyebrow,
  title,
  data,
}: {
  eyebrow: string;
  title: string;
  data: AttritionAnalysis[] | null;
}) {
  return (
    <div>
      <SectionLabel>
        <span className="text-brand-300">{eyebrow}</span> {title}
      </SectionLabel>
      <div className="mt-4">
        {data === null ? (
          <AnalysisSkeleton rows={4} compact />
        ) : (
          <RankedBarList data={data.slice(0, 5)} compact />
        )}
      </div>
    </div>
  );
}

function RankedBarList({
  data,
  compact = false,
}: {
  data: AttritionAnalysis[];
  compact?: boolean;
}) {
  if (data.length === 0) return <p className="text-sm text-brand-500">No data.</p>;
  const max = Math.max(...data.map((d) => d.attritionRate), 1);
  return (
    <ul className={`flex flex-col ${compact ? "gap-3" : "gap-4"}`}>
      {data.map((row) => {
        const pct = fmt(row.attritionRate);
        const barWidth = (row.attritionRate / max) * 100;
        return (
          <li key={row.groupLabel}>
            <div className="flex items-baseline justify-between gap-3 mb-1.5">
              <span className={`text-brand-900 font-medium truncate ${compact ? "text-xs" : "text-sm"}`}>
                {row.groupLabel}
              </span>
              <span className={`shrink-0 num text-brand-500 ${compact ? "text-xs" : "text-sm"}`}>
                {pct}%{" "}
                <span className="text-brand-300 text-[11px]">
                  {row.attritionCount}/{row.totalEmployees}
                </span>
              </span>
            </div>
            <div className="data-bar-track">
              <div
                className="data-bar-fill"
                style={{ width: `${barWidth}%` }}
              />
            </div>
          </li>
        );
      })}
    </ul>
  );
}

function AnalysisSkeleton({ rows, compact = false }: { rows: number; compact?: boolean }) {
  return (
    <ul className={`flex flex-col ${compact ? "gap-3" : "gap-4"} animate-pulse`}>
      {Array.from({ length: rows }, (_, i) => (
        <li key={i}>
          <div className="flex justify-between mb-2">
            <div className="h-3 rounded bg-brand-100" style={{ width: `${40 + i * 7}%` }} />
            <div className="h-3 w-10 rounded bg-brand-100" />
          </div>
          <div className="data-bar-track">
            <div className="data-bar-fill bg-brand-100" style={{ width: `${70 - i * 8}%` }} />
          </div>
        </li>
      ))}
    </ul>
  );
}

function WatchlistSkeleton() {
  return (
    <div className="flex flex-col divide-y divide-brand-900/6 animate-pulse">
      {Array.from({ length: 4 }, (_, i) => (
        <div key={i} className="flex items-center gap-3 py-3">
          <div className="h-8 w-8 rounded-full bg-brand-100 shrink-0" />
          <div className="flex-1 space-y-1.5">
            <div className="h-3 w-32 rounded bg-brand-100" />
            <div className="h-2.5 w-48 rounded bg-brand-100" />
          </div>
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
          <div className="h-8 w-8 rounded-full bg-brand-100 shrink-0" />
          <div className="flex-1 space-y-1.5">
            <div className="h-3 w-24 rounded bg-brand-100" />
            <div className="h-2.5 w-full rounded bg-brand-100" />
            <div className="h-2.5 w-3/4 rounded bg-brand-100" />
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

import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { deleteNotification, getMyNotifications, type Notification } from "../lib/notificationApi";
import { getErrorMessage } from "../lib/apiClient";
import { cx, formatRelativeTime } from "../lib/utils";
import { useAuth } from "../context/AuthContext";
import { EmptyState } from "../components/ui/EmptyState";
import { Avatar } from "../components/ui/Avatar";
import { Skeleton } from "../components/ui/Skeleton";
import { Bell, Clock, Building2, MoreHorizontal } from "lucide-react";

const DAY_MS = 24 * 60 * 60 * 1000;
const WEEK_MS = 7 * DAY_MS;

// HR-only page (US-21 keeps Guests off /notifications entirely via
// ProtectedRoute + the Gateway's auth requirement on this route).
export function Notifications() {
  const { user } = useAuth();
  const senderName = user?.fullName ?? user?.email ?? "You";

  const [notifications, setNotifications] = useState<Notification[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);
  // Snapshot "now" once per mount rather than calling Date.now() inline
  // during render (keeps recency checks pure/stable across re-renders).
  const [now] = useState(() => Date.now());

  useEffect(() => {
    getMyNotifications()
      .then(setNotifications)
      .catch((err) => setLoadError(getErrorMessage(err, "Couldn't load notifications.")));
  }, []);

  async function handleDelete(id: number) {
    setDeletingId(id);
    try {
      await deleteNotification(id);
      setNotifications((prev) => prev?.filter((n) => n.id !== id) ?? prev);
    } catch {
      // leave the row in place; the button re-enables so the user can retry
    } finally {
      setDeletingId(null);
    }
  }

  // Every number below is derived straight from the real createdAt/department
  // fields already on each notification - nothing invented, no fake
  // read/unread server state (the API doesn't track that).
  const stats = useMemo(() => {
    if (!notifications) return null;
    const last24h = notifications.filter((n) => now - new Date(n.createdAt).getTime() < DAY_MS);
    const last7d = notifications.filter((n) => now - new Date(n.createdAt).getTime() < WEEK_MS);
    const departments = new Set(notifications.map((n) => n.department));
    const byDepartment = [...departments]
      .map((dept) => ({ dept, count: notifications.filter((n) => n.department === dept).length }))
      .sort((a, b) => b.count - a.count);
    const latest = notifications.reduce<Notification | null>(
      (acc, n) => (!acc || new Date(n.createdAt) > new Date(acc.createdAt) ? n : acc),
      null
    );
    return { total: notifications.length, last24h: last24h.length, last7d: last7d.length, departments, byDepartment, latest };
  }, [notifications, now]);

  if (loadError) {
    return (
      <div className="mx-auto max-w-6xl">
        <p className="rounded-2xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{loadError}</p>
      </div>
    );
  }

  return (
    <div className="mx-auto flex max-w-6xl flex-col gap-5">
      {/* ── Hero ─────────────────────────────────────────────────── */}
      <div
        className="relative overflow-hidden rounded-3xl border border-brand-900/10 p-7 shadow-[0_1px_2px_rgba(15,23,42,0.04),0_20px_44px_-24px_rgba(13,71,161,0.45)] sm:p-9"
        style={{ background: "linear-gradient(135deg, #0a2a55 0%, #0d47a1 60%, #1565c0 130%)" }}
      >
        <BellGlyph />
        <div className="relative flex items-center gap-4">
          <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-white/10 text-white ring-1 ring-white/10 backdrop-blur-sm">
            <Bell className="h-5 w-5" strokeWidth={1.75} />
          </div>
          <div>
            <p className="text-xs font-semibold uppercase tracking-wider text-white/45">HR</p>
            <h1 className="font-serif text-3xl font-semibold italic tracking-tight text-white sm:text-4xl">
              Notifications
            </h1>
          </div>
        </div>
        <p className="relative mt-3 max-w-lg text-sm leading-relaxed text-blue-100/70">
          Every note you've sent from an employee's record, in one place - who it's about, what you said, and when.
        </p>
      </div>

      {/* ── KPI row ──────────────────────────────────────────────── */}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <KpiTile label="Total" value={stats?.total} sub="all time" />
        <KpiTile label="Last 24h" value={stats?.last24h} sub="new activity" accent />
        <KpiTile label="This week" value={stats?.last7d} sub="past 7 days" />
        <KpiTile
          label="Latest"
          value={stats?.latest ? formatRelativeTime(stats.latest.createdAt) : undefined}
          sub={stats?.latest ? stats.latest.employeeName : "no activity yet"}
          isText
        />
      </div>

      {/* ── Main bento: timeline + side summary ─────────────────── */}
      <div className="grid grid-cols-1 gap-4 lg:grid-cols-12">
        <div className="glass-card rounded-3xl p-5 sm:p-7 lg:col-span-8">
          <div className="mb-5 flex items-baseline justify-between">
            <h2 className="font-serif text-lg font-semibold italic text-brand-900">Activity</h2>
            {stats ? (
              <span className="text-xs font-medium text-neutral-400">
                {stats.total} notification{stats.total === 1 ? "" : "s"}
              </span>
            ) : null}
          </div>

          {!notifications ? (
            <TimelineSkeleton />
          ) : notifications.length === 0 ? (
            <EmptyState
              title="No notifications yet"
              description="Send a notification from an employee's detail page to start a record here."
            />
          ) : (
            <ol className="relative flex flex-col">
              {notifications.map((note, i) => (
                <TimelineRow
                  key={note.id}
                  note={note}
                  senderName={senderName}
                  now={now}
                  isLast={i === notifications.length - 1}
                  onDelete={() => handleDelete(note.id)}
                  deleting={deletingId === note.id}
                />
              ))}
            </ol>
          )}
        </div>

        {/* ── Side column: recency + department breakdown ────────── */}
        <div className="flex flex-col gap-4 lg:col-span-4">
          <div className="glass-card rounded-3xl p-5 sm:p-6">
            <div className="mb-1 flex items-center gap-2">
              <Clock className="h-4 w-4 text-brand-500" strokeWidth={1.75} />
              <h3 className="text-sm font-semibold text-brand-900">Recent activity</h3>
            </div>
            <p className="mb-4 text-xs text-neutral-400">Most recent notifications sent</p>
            {!notifications ? (
              <div className="flex flex-col gap-3.5 animate-pulse">
                {Array.from({ length: 3 }, (_, i) => (
                  <div key={i} className="flex items-center gap-2.5">
                    <Skeleton className="h-7 w-7 shrink-0 rounded-full" />
                    <Skeleton className="h-3 flex-1" />
                  </div>
                ))}
              </div>
            ) : notifications.length === 0 ? (
              <p className="text-xs text-neutral-400">Nothing sent yet.</p>
            ) : (
              <ul className="flex flex-col gap-3.5">
                {notifications.slice(0, 4).map((n) => (
                  <li key={n.id}>
                    <Link
                      to={`/employees?department=${encodeURIComponent(n.department)}`}
                      className="group -mx-2 flex items-center gap-2.5 rounded-xl px-2 py-1.5 transition-colors duration-150 hover:bg-brand-50/60"
                    >
                      <Avatar firstName={n.employeeName.split(" ")[0]} lastName={n.employeeName.split(" ")[1] ?? ""} size="sm" />
                      <div className="min-w-0 flex-1">
                        <p className="truncate text-xs font-semibold text-brand-900 group-hover:text-brand-700">
                          {n.employeeName}
                        </p>
                        <p className="text-[11px] text-neutral-400">{formatRelativeTime(n.createdAt)}</p>
                      </div>
                    </Link>
                  </li>
                ))}
              </ul>
            )}
          </div>

          <div className="glass-card rounded-3xl p-5 sm:p-6">
            <div className="mb-1 flex items-center gap-2">
              <Building2 className="h-4 w-4 text-brand-500" strokeWidth={1.75} />
              <h3 className="text-sm font-semibold text-brand-900">By department</h3>
            </div>
            <p className="mb-4 text-xs text-neutral-400">Where notifications concentrate</p>
            {!stats ? (
              <div className="flex flex-col gap-3.5 animate-pulse">
                {Array.from({ length: 3 }, (_, i) => (
                  <Skeleton key={i} className="h-2.5 w-full rounded-full" />
                ))}
              </div>
            ) : stats.byDepartment.length === 0 ? (
              <p className="text-xs text-neutral-400">No data yet.</p>
            ) : (
              <ul className="flex flex-col gap-3.5">
                {stats.byDepartment.slice(0, 5).map(({ dept, count }) => {
                  const pct = Math.max((count / stats.total) * 100, 6);
                  return (
                    <li key={dept}>
                      <Link to={`/employees?department=${encodeURIComponent(dept)}`} className="group block">
                        <div className="mb-1.5 flex items-baseline justify-between gap-2">
                          <span className="truncate text-xs font-medium text-brand-900 group-hover:text-brand-700">
                            {dept}
                          </span>
                          <span className="num shrink-0 text-xs font-semibold text-neutral-400">{count}</span>
                        </div>
                        <div className="h-2 w-full overflow-hidden rounded-full bg-neutral-100">
                          <div
                            className="h-full rounded-full bg-gradient-to-r from-brand-500 to-indigo-500 transition-[width] duration-700 ease-out"
                            style={{ width: `${pct}%` }}
                          />
                        </div>
                      </Link>
                    </li>
                  );
                })}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function KpiTile({
  label,
  value,
  sub,
  accent = false,
  isText = false,
}: {
  label: string;
  value: string | number | undefined;
  sub: string;
  accent?: boolean;
  isText?: boolean;
}) {
  return (
    <div className="glass-card flex flex-col justify-between rounded-2xl p-4 transition-shadow duration-300 hover:shadow-[0_1px_2px_rgba(15,23,42,0.04),0_16px_32px_-16px_rgba(13,71,161,0.18)]">
      <span className="text-[10px] font-semibold uppercase tracking-wider text-neutral-400">{label}</span>
      {value !== undefined ? (
        <p
          className={cx(
            "num mt-2 truncate font-display font-semibold tracking-tight",
            isText ? "text-lg text-brand-900" : "text-2xl",
            accent ? "text-brand-600" : "text-brand-900"
          )}
        >
          {value}
        </p>
      ) : (
        <Skeleton className="mt-2 h-6 w-14 rounded-md" />
      )}
      <span className="mt-1 truncate text-[11px] text-neutral-400">{sub}</span>
    </div>
  );
}

// Timeline row - time / dot+connector / ticket id / message, matching the
// reference: a light glass rail with a hover-highlighted row and an
// overflow action that only shows up on that row.
function TimelineRow({
  note,
  senderName,
  now,
  isLast,
  onDelete,
  deleting,
}: {
  note: Notification;
  senderName: string;
  now: number;
  isLast: boolean;
  onDelete: () => void;
  deleting: boolean;
}) {
  const isNew = now - new Date(note.createdAt).getTime() < DAY_MS;
  const timeLabel = new Date(note.createdAt).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" });

  return (
    <li className="group/row relative">
      <div
        className={cx(
          "grid grid-cols-[44px_16px_1fr_28px] items-start gap-x-2.5 rounded-2xl px-2.5 py-3 transition-all duration-200 sm:grid-cols-[64px_16px_100px_1fr_28px] sm:gap-x-3.5 sm:px-3",
          "hover:bg-gradient-to-r hover:from-white hover:to-brand-50/50 hover:shadow-[0_1px_2px_rgba(15,23,42,0.04),0_8px_20px_-12px_rgba(13,71,161,0.18)] hover:ring-1 hover:ring-brand-900/[0.06]",
          "focus-within:bg-gradient-to-r focus-within:from-white focus-within:to-brand-50/50 focus-within:ring-1 focus-within:ring-brand-900/[0.06]"
        )}
      >
        {/* time */}
        <span className="hidden pt-0.5 text-right text-xs font-medium text-neutral-400 sm:block">{timeLabel}</span>

        {/* dot + connector */}
        <div className="flex flex-col items-center self-stretch">
          <span
            className={cx(
              "mt-1.5 h-2 w-2 shrink-0 rounded-full ring-4 ring-white",
              isNew ? "bg-brand-500" : "bg-neutral-300"
            )}
          />
          {!isLast && <span className="mt-1 w-px flex-1 bg-neutral-200" />}
        </div>

        {/* ticket / identifier */}
        <div className="hidden items-start gap-2 pt-0.5 sm:flex">
          <span className={cx("mt-0.5 h-3.5 w-[3px] shrink-0 rounded-full", isNew ? "bg-brand-500" : "bg-neutral-200")} />
          <Link
            to={`/employees?department=${encodeURIComponent(note.department)}`}
            className="truncate text-xs font-semibold text-neutral-500 hover:text-brand-700"
            title={note.department}
          >
            #{note.id}
          </Link>
        </div>

        {/* message */}
        <Link to={`/employees?department=${encodeURIComponent(note.department)}`} className="min-w-0 rounded-lg outline-none focus-visible:ring-2 focus-visible:ring-brand-500/40">
          <div className="flex flex-wrap items-baseline gap-x-2 gap-y-0.5 sm:hidden">
            <span className="text-xs font-medium text-neutral-400">{timeLabel}</span>
            <span className="text-xs font-semibold text-neutral-500">#{note.id}</span>
          </div>
          <p className="text-sm font-semibold text-brand-900">{note.employeeName}</p>
          <p className="mt-0.5 line-clamp-2 text-sm leading-relaxed text-neutral-600">{note.comment}</p>
          <p className="mt-1 text-xs text-neutral-400">
            {note.department} · Sent by {senderName}
          </p>
        </Link>

        {/* overflow action - only visible on hover/focus of this row */}
        <div className="flex justify-end pt-0.5">
          <button
            type="button"
            onClick={onDelete}
            disabled={deleting}
            aria-label="Delete notification"
            className="flex h-7 w-7 items-center justify-center rounded-full text-neutral-300 opacity-0 transition-all duration-200 hover:bg-red-50 hover:text-red-500 focus-visible:opacity-100 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-red-200 group-hover/row:opacity-100 group-focus-within/row:opacity-100 disabled:opacity-30"
          >
            <MoreHorizontal className="h-4 w-4" strokeWidth={2} />
          </button>
        </div>
      </div>
    </li>
  );
}

function TimelineSkeleton() {
  return (
    <div className="flex flex-col gap-1 animate-pulse">
      {Array.from({ length: 5 }, (_, i) => (
        <div key={i} className="grid grid-cols-[44px_16px_1fr] items-start gap-x-2.5 px-2.5 py-3 sm:grid-cols-[64px_16px_100px_1fr]">
          <Skeleton className="hidden h-3 w-10 justify-self-end rounded sm:block" />
          <div className="flex justify-center">
            <Skeleton className="mt-1.5 h-2 w-2 rounded-full" />
          </div>
          <Skeleton className="hidden h-3 w-10 rounded sm:block" />
          <div className="space-y-2">
            <Skeleton className="h-3.5 w-32" />
            <Skeleton className="h-3 w-full" />
          </div>
        </div>
      ))}
    </div>
  );
}

// Faint geometric accent for the hero card - decorative only.
function BellGlyph() {
  return (
    <svg
      viewBox="0 0 300 200"
      aria-hidden="true"
      className="pointer-events-none absolute -right-6 -top-8 h-44 w-64 opacity-[0.14]"
    >
      <circle cx="220" cy="70" r="70" fill="none" stroke="#e3f2fd" strokeWidth="1.5" />
      <circle cx="260" cy="140" r="34" fill="none" stroke="#e3f2fd" strokeWidth="1.5" />
      <circle cx="150" cy="160" r="16" fill="#e3f2fd" opacity="0.5" />
    </svg>
  );
}

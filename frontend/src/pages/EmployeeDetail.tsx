import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { motion, MotionConfig } from "framer-motion";
import { getEmployeeById, flagEmployee, type Employee } from "../lib/employeeApi";
import { getMyNotifications, type Notification } from "../lib/notificationApi";
import { avatarColorFor, salaryBandLabel, promotionBandLabel } from "../lib/employeeDisplay";
import { useAuth } from "../context/AuthContext";
import { getErrorMessage } from "../lib/apiClient";
import { formatCurrency, formatDate, formatRelativeTime } from "../lib/utils";
import { Avatar } from "../components/ui/Avatar";
import { AttritionBadge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import { Skeleton } from "../components/ui/Skeleton";

export function EmployeeDetail() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();

  const [employee, setEmployee] = useState<Employee | null | undefined>(undefined);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [notes, setNotes] = useState<Notification[]>([]);

  const [comment, setComment] = useState("");
  const [flagging, setFlagging] = useState(false);
  const [flagged, setFlagged] = useState(false);
  const [flagError, setFlagError] = useState<string | null>(null);

  useEffect(() => {
    if (!id) return;
    setEmployee(undefined);
    getEmployeeById(id)
      .then(setEmployee)
      .catch((err) => setLoadError(getErrorMessage(err, "Couldn't load this employee.")));
  }, [id]);

  useEffect(() => {
    if (!employee) return;
    getMyNotifications()
      .then((all) => setNotes(all.filter((n) => n.employeeId === employee.employeeId)))
      .catch(() => {
        // Notes are a secondary detail on this page; a failed fetch here
        // just leaves the thread empty rather than blocking the whole view.
      });
  }, [employee]);

  if (loadError) {
    return (
      <div className="mx-auto max-w-3xl py-16">
        <EmptyState
          title="Couldn't load this employee"
          description={loadError}
          action={
            <Link to="/employees">
              <Button variant="secondary" size="sm">
                Back to Employees
              </Button>
            </Link>
          }
        />
      </div>
    );
  }

  if (employee === null) {
    return (
      <div className="mx-auto max-w-3xl py-16">
        <EmptyState
          title="Employee not found"
          description="This employee may have been removed or the link is out of date."
          action={
            <Link to="/employees">
              <Button variant="secondary" size="sm">
                Back to Employees
              </Button>
            </Link>
          }
        />
      </div>
    );
  }

  if (employee === undefined) {
    return (
      <div className="mx-auto max-w-4xl py-8">
        <div className="flex items-center gap-4">
          <Skeleton className="h-20 w-20 rounded-full" />
          <div className="flex flex-col gap-3">
            <Skeleton className="h-8 w-64" />
            <Skeleton className="h-4 w-40" />
          </div>
        </div>
      </div>
    );
  }

  async function handleFlag(e: React.FormEvent) {
    e.preventDefault();
    if (!comment.trim() || !employee) return;
    setFlagging(true);
    setFlagError(null);
    try {
      await flagEmployee(employee.id, comment.trim());
      setNotes((prev) => [
        {
          id: Date.now(),
          employeeId: employee.employeeId,
          employeeName: `${employee.firstName} ${employee.lastName}`,
          department: employee.department,
          comment: comment.trim(),
          createdAt: new Date().toISOString(),
        },
        ...prev,
      ]);
      setComment("");
      setFlagged(true);
      window.setTimeout(() => setFlagged(false), 2500);
    } catch (err) {
      setFlagError(getErrorMessage(err, "Couldn't flag this employee. Try again."));
    } finally {
      setFlagging(false);
    }
  }

  // The six actual US-11–US-16 groupings this employee belongs to, linking
  // straight into the Attrition Explorer pre-filtered to that group - not a
  // score, just "here are the real peers this record shares a dimension with."
  const contextLinks = [
    { label: "Department", value: employee.department, query: `department=${encodeURIComponent(employee.department)}` },
    { label: "Job Role", value: employee.jobRole, query: `jobRole=${encodeURIComponent(employee.jobRole)}` },
    {
      label: "Compensation",
      value: salaryBandLabel(employee.salary),
      query: `compensationBand=${encodeURIComponent(salaryBandLabel(employee.salary))}`,
    },
    { label: "Demographics", value: employee.gender, query: `gender=${encodeURIComponent(employee.gender)}` },
    {
      label: "Work-Life Balance",
      value: `Overtime: ${employee.overTime}`,
      query: `overTime=${employee.overTime}`,
    },
    {
      label: "Career Progression",
      value: promotionBandLabel(employee.yearsSinceLastPromotion),
      query: `promotionBand=${encodeURIComponent(promotionBandLabel(employee.yearsSinceLastPromotion))}`,
    },
  ];

  return (
    <MotionConfig reducedMotion="user">
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35 }}
        className="mx-auto max-w-4xl"
      >
        <div className="mb-12">
          <Link
            to="/employees"
            className="inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wider text-neutral-400 hover:text-brand-900 transition-colors"
          >
            <ArrowLeftIcon className="h-3 w-3" /> Employee Roster
          </Link>
        </div>

        {/* Identity header */}
        <div className="flex flex-col md:flex-row md:items-start justify-between gap-8 mb-16">
          <div className="flex items-start gap-6">
            <Avatar
              firstName={employee.firstName}
              lastName={employee.lastName}
              color={avatarColorFor(employee.id)}
              size="xl"
            />
            <div className="pt-2">
              <h1 className="font-serif text-4xl md:text-5xl font-semibold italic tracking-tight text-ink-900 mb-2">
                {employee.firstName} {employee.lastName}
              </h1>
              <p className="text-lg text-neutral-600 font-medium">
                {employee.jobRole} &middot; {employee.department}
              </p>
              <div className="mt-4 flex items-center gap-3">
                <span className="font-mono text-xs text-neutral-500 border border-brand-900/10 rounded px-2 py-1 bg-white">
                  ID: {employee.employeeId}
                </span>
                <span className="text-xs text-neutral-400 border border-transparent px-2 py-1">
                  Hired {formatDate(employee.hireDate)}
                </span>
              </div>
            </div>
          </div>
          <div className="md:text-right pt-2">
            <p className="text-xs font-semibold uppercase tracking-wider text-neutral-400 mb-3">Attrition</p>
            <AttritionBadge attrition={employee.attrition} />
          </div>
        </div>

        <div className="h-px w-full bg-brand-900/10 mb-16"></div>

        <div className="grid grid-cols-1 md:grid-cols-12 gap-16">
          {/* Left Column: the employee's actual data */}
          <div className="md:col-span-7 flex flex-col gap-12">
            <section>
              <SectionHeader title="Personal" />
              <div className="grid grid-cols-2 gap-x-8 gap-y-6">
                <DataRow label="Age" value={employee.age.toString()} />
                <DataRow label="Gender" value={employee.gender} />
                <DataRow label="Marital Status" value={employee.maritalStatus} />
                <DataRow label="Education" value={employee.educationField} />
                <DataRow label="Ethnicity" value={employee.ethnicity} />
                <DataRow label="State" value={employee.state} />
                <DataRow label="Commute" value={`${employee.distanceFromHomeKm} km`} />
              </div>
            </section>

            <section>
              <SectionHeader title="Role & Compensation" />
              <div className="grid grid-cols-2 gap-x-8 gap-y-6">
                <DataRow label="Base Salary" value={formatCurrency(employee.salary)} highlight />
                <DataRow label="Stock Options" value={`Level ${employee.stockOptionLevel}`} />
                <DataRow label="Overtime" value={employee.overTime} />
                <DataRow label="Business Travel" value={employee.businessTravel} />
              </div>
            </section>

            <section>
              <SectionHeader title="Career Progression" />
              <div className="grid grid-cols-2 gap-x-8 gap-y-6">
                <DataRow label="Tenure" value={`${employee.yearsAtCompany} years`} />
                <DataRow label="Time in Role" value={`${employee.yearsInMostRecentRole} years`} />
                <DataRow label="Since Last Promotion" value={`${employee.yearsSinceLastPromotion} years`} />
                <DataRow label="With Current Manager" value={`${employee.yearsWithCurrManager} years`} />
              </div>
            </section>
          </div>

          {/* Right Column: where this record shows up in the six analyses,
              plus the flag/comment action. */}
          <div className="md:col-span-5 flex flex-col gap-8">
            <section className="bg-neutral-50 rounded-2xl p-6 border border-brand-900/5">
              <h3 className="font-serif text-lg font-semibold text-ink-900 mb-1">Attrition Analysis</h3>
              <p className="text-base text-neutral-500 mb-5">
                Where this employee falls across the six attrition dimensions (US-11–US-16).
              </p>
              <div className="flex flex-col divide-y divide-brand-900/8">
                {contextLinks.map((link) => (
                  <Link
                    key={link.label}
                    to={`/employees?${link.query}`}
                    className="group flex items-center justify-between gap-3 py-3 first:pt-0 last:pb-0"
                  >
                    <div className="min-w-0">
                      <p className="text-[10px] font-semibold uppercase tracking-wider text-neutral-400">
                        {link.label}
                      </p>
                      <p className="truncate text-sm font-medium text-ink-900">{link.value}</p>
                    </div>
                    <span className="shrink-0 text-xs font-semibold text-brand-500 opacity-0 transition-opacity group-hover:opacity-100">
                      View group &rarr;
                    </span>
                  </Link>
                ))}
              </div>
            </section>

            {user?.role !== "Guest" && (
              <section className="bg-white rounded-2xl p-6 border border-brand-900/10 shadow-sm">
                <h3 className="font-serif text-lg font-semibold text-ink-900 mb-2">Send Notification</h3>
                <p className="text-base text-neutral-500 mb-6">
                  Write a note for the HR team about this employee. It'll appear in Notifications for every HR user.
                </p>

                <form onSubmit={handleFlag} className="flex flex-col gap-3">
                  <textarea
                    value={comment}
                    onChange={(e) => setComment(e.target.value)}
                    placeholder="e.g. Requested a compensation review after a competing offer"
                    maxLength={1000}
                    rows={3}
                    className="w-full resize-none rounded-lg border border-brand-900/10 bg-neutral-50 p-3 text-sm outline-none transition-colors placeholder:text-neutral-400 focus:border-brand-900 focus:bg-white"
                  />
                  <Button type="submit" disabled={!comment.trim() || flagging} className="w-full">
                    {flagging ? "Sending..." : "Send Notification"}
                  </Button>
                </form>

                {flagged && <p className="mt-3 text-sm font-medium text-emerald-600 text-center">Notification sent.</p>}
                {flagError && <p className="mt-3 text-sm font-medium text-red-600 text-center">{flagError}</p>}
              </section>
            )}

            <section>
              <h3 className="text-xs font-semibold uppercase tracking-wider text-neutral-500 mb-4">Notifications</h3>
              {notes.length === 0 ? (
                <p className="text-sm text-neutral-400">Send a notification to start a record here.</p>
              ) : (
                <div className="flex flex-col gap-4">
                  {notes.map((note) => (
                    <div key={note.id} className="relative pl-4 border-l border-brand-900/10 pb-4 last:pb-0">
                      <div className="absolute w-2 h-2 bg-neutral-200 rounded-full -left-[4.5px] top-1.5"></div>
                      <div className="flex items-baseline justify-between gap-2 mb-1">
                        <span className="text-xs font-semibold text-ink-900">
                          Sent by {user?.fullName ?? user?.email ?? "you"}
                        </span>
                        <span className="text-[10px] text-neutral-400 uppercase tracking-wide">
                          {formatRelativeTime(note.createdAt)}
                        </span>
                      </div>
                      <p className="text-sm text-neutral-600 leading-relaxed">{note.comment}</p>
                    </div>
                  ))}
                </div>
              )}
            </section>
          </div>
        </div>
      </motion.div>
    </MotionConfig>
  );
}

function SectionHeader({ title }: { title: string }) {
  return (
    <h2 className="text-xs font-semibold uppercase tracking-wider text-neutral-400 mb-6 pb-2 border-b border-brand-900/5">
      {title}
    </h2>
  );
}

function DataRow({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="flex flex-col gap-1.5">
      <span className="text-[10px] font-semibold uppercase tracking-wider text-neutral-400">{label}</span>
      <span
        className={`text-sm ${highlight ? "font-display font-semibold text-lg tracking-tight text-ink-900" : "font-medium text-ink-900"}`}
      >
        {value}
      </span>
    </div>
  );
}

function ArrowLeftIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" {...props}>
      <path d="M19 12H5M12 19l-7-7 7-7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

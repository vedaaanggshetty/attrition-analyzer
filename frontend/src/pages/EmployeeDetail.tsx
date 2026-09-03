import { useMemo, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { employees, notifications as seedNotifications } from "../data/mockData";
import { formatCurrency, formatDate, formatRelativeTime } from "../lib/utils";
import { Card } from "../components/ui/Card";
import { Avatar } from "../components/ui/Avatar";
import { RiskBadge } from "../components/ui/Badge";
import { Button } from "../components/ui/Button";
import { EmptyState } from "../components/ui/EmptyState";
import type { FlaggedNotification } from "../types";

export function EmployeeDetail() {
  const { id } = useParams<{ id: string }>();
  const employee = useMemo(() => employees.find((e) => e.id === id), [id]);

  const [notes, setNotes] = useState<FlaggedNotification[]>(() =>
    seedNotifications.filter((n) => n.employeeId === employee?.employeeId)
  );
  const [comment, setComment] = useState("");
  const [flagging, setFlagging] = useState(false);
  const [flagged, setFlagged] = useState(false);

  if (!employee) {
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

  function handleFlag(e: React.FormEvent) {
    e.preventDefault();
    if (!comment.trim() || !employee) return;
    setFlagging(true);
    // UI-only mock submission; wire this to POST /employees/{id}/flag later.
    window.setTimeout(() => {
      setNotes((prev) => [
        {
          id: Date.now(),
          employeeId: employee.employeeId,
          employeeName: `${employee.firstName} ${employee.lastName}`,
          department: employee.department,
          comment: comment.trim(),
          createdAt: new Date().toISOString(),
          hrUserEmail: "you@attritionanalyzer.com",
        },
        ...prev,
      ]);
      setComment("");
      setFlagging(false);
      setFlagged(true);
      window.setTimeout(() => setFlagged(false), 2500);
    }, 600);
  }

  return (
    <div className="mx-auto max-w-5xl">
      <Link to="/employees" className="mb-6 inline-flex items-center gap-1.5 text-sm font-medium text-neutral-500 hover:text-brand-900">
        <ArrowLeftIcon className="h-4 w-4" /> Back to Employees
      </Link>

      <Card className="p-6 sm:p-8">
        <div className="flex flex-col items-start gap-6 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex items-center gap-4">
            <Avatar firstName={employee.firstName} lastName={employee.lastName} color={employee.avatarColor} size="lg" />
            <div>
              <h1 className="font-display text-2xl font-semibold tracking-tight text-brand-900 sm:text-3xl">
                {employee.firstName} {employee.lastName}
              </h1>
              <p className="mt-1 text-sm text-neutral-500">
                {employee.jobRole} &middot; {employee.department}
              </p>
              <p className="mt-1 text-xs text-neutral-400">ID {employee.employeeId}</p>
            </div>
          </div>
          <RiskBadge risk={employee.attritionRisk} />
        </div>

        <div className="mt-8 grid grid-cols-2 gap-5 border-t border-brand-900/8 pt-6 sm:grid-cols-4">
          <Field label="Age" value={employee.age.toString()} />
          <Field label="Gender" value={employee.gender} />
          <Field label="Marital Status" value={employee.maritalStatus} />
          <Field label="State" value={employee.state} />
          <Field label="Compensation" value={formatCurrency(employee.salary)} />
          <Field label="Overtime" value={employee.overTime} />
          <Field label="Hire Date" value={formatDate(employee.hireDate)} />
          <Field label="Years at Company" value={employee.yearsAtCompany.toString()} />
          <Field label="Years in Role" value={employee.yearsInMostRecentRole.toString()} />
          <Field label="Since Last Promotion" value={`${employee.yearsSinceLastPromotion} yrs`} />
          <Field label="Education Field" value={employee.educationField} />
          <Field label="Distance from Home" value={`${employee.distanceFromHomeKm} km`} />
        </div>
      </Card>

      <Card className="mt-5 p-6 sm:p-8">
        <h2 className="font-display text-lg font-semibold text-brand-900">Flag Employee</h2>
        <p className="mt-1 text-sm text-neutral-500">
          Note a concern for the HR team. It'll show up in Notifications for everyone to see.
        </p>
        <form onSubmit={handleFlag} className="mt-5 flex flex-col gap-3 sm:flex-row">
          <input
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="e.g. Requested a compensation review after a competing offer"
            maxLength={1000}
            className="flex-1 rounded-full border border-brand-900/12 bg-white px-5 py-2.5 text-sm outline-none transition-colors placeholder:text-neutral-400 focus:border-brand-900/40"
          />
          <Button type="submit" disabled={!comment.trim() || flagging}>
            {flagging ? "Flagging..." : "Flag Employee"}
          </Button>
        </form>
        {flagged && <p className="mt-3 text-sm font-medium text-emerald-600">Employee flagged.</p>}

        <div className="mt-6 flex flex-col divide-y divide-brand-900/8 border-t border-brand-900/8">
          {notes.length === 0 ? (
            <EmptyState title="No notes yet" description="Flag this employee to start a record here." />
          ) : (
            notes.map((note) => (
              <div key={note.id} className="flex items-start gap-3 py-4">
                <Avatar firstName={note.employeeName.split(" ")[0]} lastName={note.employeeName.split(" ")[1] ?? ""} size="sm" />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
                    <p className="text-sm font-semibold text-brand-900">{note.hrUserEmail}</p>
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

function Field({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-medium uppercase tracking-wide text-neutral-400">{label}</p>
      <p className="mt-1 text-sm font-semibold text-brand-900">{value}</p>
    </div>
  );
}

function ArrowLeftIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" {...props}>
      <path d="M19 12H5M12 19l-7-7 7-7" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

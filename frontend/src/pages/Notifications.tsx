import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { deleteNotification, getMyNotifications, type Notification } from "../lib/notificationApi";
import { getErrorMessage } from "../lib/apiClient";
import { formatDate, formatRelativeTime } from "../lib/utils";
import { useAuth } from "../context/AuthContext";
import { PageHeader } from "../components/ui/PageHeader";
import { EmptyState } from "../components/ui/EmptyState";
import { Avatar } from "../components/ui/Avatar";
import { Skeleton } from "../components/ui/Skeleton";

// HR-only page (US-21 keeps Guests off /notifications entirely via
// ProtectedRoute + the Gateway's auth requirement on this route).
export function Notifications() {
  const { user } = useAuth();
  const senderName = user?.fullName ?? user?.email ?? "You";

  const [notifications, setNotifications] = useState<Notification[] | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [deletingId, setDeletingId] = useState<number | null>(null);

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

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader
        eyebrow="HR"
        title="Notifications"
        description={
          notifications ? `${notifications.length} notification${notifications.length === 1 ? "" : "s"} you've sent` : undefined
        }
      />

      {loadError ? (
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{loadError}</p>
      ) : !notifications ? (
        <div className="flex flex-col gap-4">
          {Array.from({ length: 4 }, (_, i) => (
            <div key={i} className="flex items-start gap-4 rounded-xl border border-brand-900/8 p-5">
              <Skeleton className="h-10 w-10 shrink-0 rounded-full" />
              <div className="flex-1 space-y-2">
                <Skeleton className="h-4 w-40" />
                <Skeleton className="h-3 w-full" />
              </div>
            </div>
          ))}
        </div>
      ) : notifications.length === 0 ? (
        <EmptyState
          title="No notifications yet"
          description="Send a notification from an employee's detail page to start a record here."
        />
      ) : (
        <div className="flex flex-col divide-y divide-brand-900/8 border-y border-brand-900/8">
          {notifications.map((note) => (
            <div key={note.id} className="group flex items-start gap-4 py-5">
              <Avatar
                firstName={note.employeeName.split(" ")[0]}
                lastName={note.employeeName.split(" ")[1] ?? ""}
                size="md"
              />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-baseline justify-between gap-x-3 gap-y-1">
                  <Link
                    to={`/employees?department=${encodeURIComponent(note.department)}`}
                    className="text-sm font-semibold text-brand-900 hover:underline underline-offset-2"
                  >
                    Re: {note.employeeName}
                  </Link>
                  <span className="text-xs text-neutral-400" title={formatDate(note.createdAt)}>
                    {formatRelativeTime(note.createdAt)}
                  </span>
                </div>
                <p className="mt-0.5 text-xs font-medium text-neutral-400">{note.department}</p>
                <p className="mt-2 text-sm leading-relaxed text-neutral-700">{note.comment}</p>
                <p className="mt-2 text-xs text-neutral-400">Sent by {senderName}</p>
              </div>
              <button
                type="button"
                onClick={() => handleDelete(note.id)}
                disabled={deletingId === note.id}
                aria-label="Delete notification"
                className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-neutral-300 opacity-0 transition-all hover:bg-red-50 hover:text-red-500 group-hover:opacity-100 disabled:opacity-30"
              >
                <TrashIcon className="h-3.5 w-3.5" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function TrashIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" {...props}>
      <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m3 0-1 14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2L4 6" />
    </svg>
  );
}

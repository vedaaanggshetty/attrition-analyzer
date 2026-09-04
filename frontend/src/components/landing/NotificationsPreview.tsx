import { notifications } from "../../data/mockData";
import { formatRelativeTime } from "../../lib/utils";
import { useReveal } from "../../hooks/useReveal";
import { Card } from "../ui/Card";
import { Avatar } from "../ui/Avatar";

export function NotificationsPreview() {
  const ref = useReveal<HTMLDivElement>(90);

  return (
    <section className="mx-auto max-w-7xl px-6 py-24 lg:px-10 lg:py-32">
      <div className="grid items-center gap-14 lg:grid-cols-2">
        <div>
          <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-neutral-400">
            Flagged &amp; tracked
          </p>
          <h2 className="font-serif text-4xl font-semibold tracking-tight text-ink-900 sm:text-5xl">
            Flag a flight risk. Never lose the thread.
          </h2>
          <p className="mt-4 max-w-md text-base leading-relaxed text-neutral-500">
            When something looks off - overtime spikes, a stalled promotion, a comp complaint - flag the
            employee straight from their profile. It shows up here, for every HR user, instantly.
          </p>
        </div>

        <div ref={ref} className="flex flex-col gap-3">
          {notifications.map((note) => (
            <Card key={note.id} data-reveal className="flex items-start gap-4 p-5">
              <Avatar
                firstName={note.employeeName.split(" ")[0]}
                lastName={note.employeeName.split(" ")[1] ?? ""}
                size="sm"
              />
              <div className="min-w-0 flex-1">
                <div className="flex items-baseline justify-between gap-2">
                  <p className="truncate text-sm font-semibold text-ink-900">{note.employeeName}</p>
                  <span className="shrink-0 text-xs text-neutral-400">{formatRelativeTime(note.createdAt)}</span>
                </div>
                <p className="mt-1 text-xs font-medium text-neutral-400">{note.department}</p>
                <p className="mt-2 text-sm leading-relaxed text-neutral-600">{note.comment}</p>
              </div>
            </Card>
          ))}
        </div>
      </div>
    </section>
  );
}

import { useState } from "react";
import { currentUser } from "../data/mockData";
import { formatDate } from "../lib/utils";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Avatar } from "../components/ui/Avatar";
import { Button } from "../components/ui/Button";

export function Profile() {
  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [form, setForm] = useState({ fullName: currentUser.fullName, phone: currentUser.phone });

  function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    // UI-only mock submission; wire this to PUT /users/{id} later.
    window.setTimeout(() => {
      setSaving(false);
      setEditing(false);
      setSaved(true);
      window.setTimeout(() => setSaved(false), 2500);
    }, 600);
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader eyebrow="Account" title="Profile" description="Manage your account and settings." />

      <Card className="p-6 sm:p-8">
        <div className="flex items-center gap-4">
          <Avatar
            firstName={currentUser.fullName.split(" ")[0]}
            lastName={currentUser.fullName.split(" ")[1] ?? ""}
            size="lg"
          />
          <div>
            <h2 className="font-display text-xl font-semibold text-brand-900">{currentUser.fullName}</h2>
            <p className="text-sm text-neutral-500">{currentUser.email}</p>
          </div>
        </div>

        <form onSubmit={handleSave} className="mt-8 grid gap-5 border-t border-brand-900/8 pt-6 sm:grid-cols-2">
          <TextField
            label="Full name"
            value={form.fullName}
            disabled={!editing}
            onChange={(v) => setForm((f) => ({ ...f, fullName: v }))}
          />
          <TextField label="Email" value={currentUser.email} disabled />
          <TextField
            label="Phone"
            value={form.phone}
            disabled={!editing}
            onChange={(v) => setForm((f) => ({ ...f, phone: v }))}
          />
          <TextField label="Role" value={currentUser.role} disabled />

          <div className="flex items-center gap-3 sm:col-span-2">
            {editing ? (
              <>
                <Button type="submit" size="sm" disabled={saving}>
                  {saving ? "Saving..." : "Save changes"}
                </Button>
                <Button
                  type="button"
                  variant="secondary"
                  size="sm"
                  onClick={() => {
                    setForm({ fullName: currentUser.fullName, phone: currentUser.phone });
                    setEditing(false);
                  }}
                >
                  Cancel
                </Button>
              </>
            ) : (
              <Button type="button" variant="secondary" size="sm" onClick={() => setEditing(true)}>
                Edit profile
              </Button>
            )}
            {saved && <span className="text-sm font-medium text-emerald-600">Saved.</span>}
          </div>
        </form>
      </Card>

      <Card className="mt-5 p-6 sm:p-8">
        <h2 className="font-display text-lg font-semibold text-brand-900">Account</h2>
        <dl className="mt-5 flex flex-col gap-4 text-sm">
          <div className="flex items-center justify-between border-b border-brand-900/8 pb-4">
            <dt className="text-neutral-500">User ID</dt>
            <dd className="font-mono text-xs text-neutral-700">{currentUser.userId}</dd>
          </div>
          <div className="flex items-center justify-between border-b border-brand-900/8 pb-4">
            <dt className="text-neutral-500">Member since</dt>
            <dd className="font-medium text-brand-900">{formatDate(currentUser.createdAt)}</dd>
          </div>
          <div className="flex items-center justify-between">
            <dt className="text-neutral-500">Access level</dt>
            <dd className="font-medium text-brand-900">{currentUser.role}</dd>
          </div>
        </dl>
      </Card>
    </div>
  );
}

function TextField({
  label,
  value,
  disabled,
  onChange,
}: {
  label: string;
  value: string;
  disabled?: boolean;
  onChange?: (value: string) => void;
}) {
  return (
    <label className="flex flex-col gap-1.5">
      <span className="text-xs font-medium uppercase tracking-wide text-neutral-400">{label}</span>
      <input
        value={value}
        disabled={disabled}
        onChange={(e) => onChange?.(e.target.value)}
        className="rounded-xl border border-brand-900/12 bg-white px-4 py-2.5 text-sm outline-none transition-colors focus:border-brand-900/40 disabled:cursor-not-allowed disabled:bg-brand-50/50 disabled:text-neutral-500"
      />
    </label>
  );
}

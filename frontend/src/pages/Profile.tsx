import { useEffect, useState } from "react";
import { formatDate } from "../lib/utils";
import { getErrorMessage } from "../lib/apiClient";
import { getMyProfile, updateMyProfile, type ProfileResponse } from "../lib/authApi";
import { useAuth } from "../context/AuthContext";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Avatar } from "../components/ui/Avatar";
import { Button } from "../components/ui/Button";
import { Skeleton } from "../components/ui/Skeleton";

export function Profile() {
  const { user, setFullName } = useAuth();
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [editing, setEditing] = useState(false);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [form, setForm] = useState({ fullName: "", phone: "" });

  useEffect(() => {
    getMyProfile()
      .then((data) => {
        setProfile(data);
        setForm({ fullName: data.fullName, phone: data.phone });
      })
      .catch((err) => setLoadError(getErrorMessage(err, "Couldn't load your profile.")));
  }, []);

  async function handleSave(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    setSaveError(null);
    try {
      const updated = await updateMyProfile(form);
      setProfile(updated);
      setFullName(updated.fullName);
      setEditing(false);
      setSaved(true);
      window.setTimeout(() => setSaved(false), 2500);
    } catch (err) {
      setSaveError(getErrorMessage(err, "Couldn't save your changes."));
    } finally {
      setSaving(false);
    }
  }

  if (loadError) {
    return (
      <div className="mx-auto max-w-3xl">
        <PageHeader eyebrow="Account" title="Profile" description="Manage your account and settings." />
        <p className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">{loadError}</p>
      </div>
    );
  }

  if (!profile) {
    return (
      <div className="mx-auto max-w-3xl">
        <PageHeader eyebrow="Account" title="Profile" description="Manage your account and settings." />
        <Card className="p-6 sm:p-8">
          <div className="flex items-center gap-4">
            <Skeleton className="h-16 w-16 rounded-full" />
            <div className="flex flex-col gap-2">
              <Skeleton className="h-5 w-40" />
              <Skeleton className="h-4 w-56" />
            </div>
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader eyebrow="Account" title="Profile" description="Manage your account and settings." />

      <Card className="p-6 sm:p-8">
        <div className="flex items-center gap-4">
          <Avatar firstName={profile.fullName.split(" ")[0]} lastName={profile.fullName.split(" ")[1] ?? ""} size="lg" />
          <div>
            <h2 className="font-serif text-xl font-semibold text-ink-900">{profile.fullName}</h2>
            <p className="text-sm text-neutral-500">{profile.email}</p>
          </div>
        </div>

        <form onSubmit={handleSave} className="mt-8 grid gap-5 border-t border-brand-900/8 pt-6 sm:grid-cols-2">
          <TextField
            label="Full name"
            value={form.fullName}
            disabled={!editing}
            onChange={(v) => setForm((f) => ({ ...f, fullName: v }))}
          />
          <TextField label="Email" value={profile.email} disabled />
          <TextField
            label="Phone"
            value={form.phone}
            disabled={!editing}
            onChange={(v) => setForm((f) => ({ ...f, phone: v }))}
          />
          <TextField label="Role" value={user?.role ?? "HR User"} disabled />

          {saveError && (
            <p role="alert" className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700 sm:col-span-2">
              {saveError}
            </p>
          )}

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
                    setForm({ fullName: profile.fullName, phone: profile.phone });
                    setSaveError(null);
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
        <h2 className="font-serif text-lg font-semibold text-ink-900">Account</h2>
        <dl className="mt-5 flex flex-col gap-4 text-sm">
          <div className="flex items-center justify-between border-b border-brand-900/8 pb-4">
            <dt className="text-neutral-500">User ID</dt>
            <dd className="font-mono text-xs text-neutral-700">{profile.userId}</dd>
          </div>
          <div className="flex items-center justify-between border-b border-brand-900/8 pb-4">
            <dt className="text-neutral-500">Member since</dt>
            <dd className="font-medium text-ink-900">{formatDate(profile.createdAt)}</dd>
          </div>
          <div className="flex items-center justify-between">
            <dt className="text-neutral-500">Access level</dt>
            <dd className="font-medium text-ink-900">{user?.role ?? "HR User"}</dd>
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
        className="shadow-control rounded-xl border border-brand-900/12 bg-white px-4 py-2.5 text-sm text-ink-900 outline-none transition-all duration-200 hover:border-brand-900/20 focus:border-brand-900/40 focus:shadow-surface disabled:cursor-not-allowed disabled:bg-brand-50/50 disabled:text-neutral-500 disabled:shadow-none"
      />
    </label>
  );
}

import { useEffect, useState } from "react";
import { formatDate } from "../lib/utils";
import { getErrorMessage } from "../lib/apiClient";
import { getMyProfile, updateMyProfile, type ProfileResponse } from "../lib/authApi";
import { useAuth } from "../context/AuthContext";
import { PageHeader } from "../components/ui/PageHeader";
import { Card } from "../components/ui/Card";
import { Avatar } from "../components/ui/Avatar";
import { Badge } from "../components/ui/Badge";
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
        <Card className="overflow-hidden p-0">
          <div className="flex items-center gap-5 border-b border-brand-900/8 bg-brand-50/40 px-6 py-6 sm:px-8">
            <Skeleton className="h-20 w-20 rounded-full" />
            <div className="flex flex-col gap-2">
              <Skeleton className="h-5 w-40" />
              <Skeleton className="h-4 w-56" />
            </div>
          </div>
          <div className="px-6 py-6 sm:px-8">
            <Skeleton className="h-4 w-32" />
          </div>
        </Card>
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-3xl">
      <PageHeader eyebrow="Account" title="Profile" description="Manage your account and settings." />

      <Card className="overflow-hidden p-0">
        {/* Identity header - avatar, name, role, email read together as one unit */}
        <div className="flex flex-wrap items-center gap-5 border-b border-brand-900/8 bg-brand-50/40 px-6 py-6 sm:px-8">
          <Avatar firstName={profile.fullName.split(" ")[0]} lastName={profile.fullName.split(" ")[1] ?? ""} size="xl" />
          <div className="min-w-0">
            <div className="flex flex-wrap items-center gap-2.5">
              <h2 className="truncate font-serif text-2xl font-semibold tracking-tight text-ink-900">
                {profile.fullName}
              </h2>
              <Badge className="bg-brand-900/8 text-brand-800">{user?.role ?? "HR User"}</Badge>
            </div>
            <p className="mt-1 text-sm text-neutral-500">{profile.email}</p>
          </div>
        </div>

        <form onSubmit={handleSave} className="px-6 py-6 sm:px-8 sm:py-7">
          <div className="mb-5">
            <h3 className="text-sm font-semibold text-ink-900">Personal information</h3>
            <p className="mt-0.5 text-xs text-neutral-400">Your name and contact details.</p>
          </div>

          <div className="grid gap-5 sm:grid-cols-2">
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
          </div>

          {saveError && (
            <p role="alert" className="mt-5 rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
              {saveError}
            </p>
          )}

          <div className="mt-6 flex items-center gap-3 border-t border-brand-900/8 pt-5">
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
        <h3 className="text-sm font-semibold text-ink-900">Account details</h3>
        <dl className="mt-4 grid gap-x-8 gap-y-4 sm:grid-cols-2">
          <div className="flex flex-col gap-1 border-b border-brand-900/8 pb-4 sm:border-b-0 sm:pb-0">
            <dt className="text-xs font-medium uppercase tracking-wide text-neutral-400">Member since</dt>
            <dd className="text-sm font-medium text-ink-900">{formatDate(profile.createdAt)}</dd>
          </div>
          <div className="flex flex-col gap-1">
            <dt className="text-xs font-medium uppercase tracking-wide text-neutral-400">Access level</dt>
            <dd className="text-sm font-medium text-ink-900">{user?.role ?? "HR User"}</dd>
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

import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { AuthLayout } from "../components/layout/AuthLayout";
import { FormField } from "../components/ui/FormField";
import { Button } from "../components/ui/Button";
import { useAuth } from "../context/AuthContext";
import { getErrorMessage } from "../lib/apiClient";

interface Errors {
  fullName?: string;
  email?: string;
  password?: string;
  confirmPassword?: string;
}

export function Register() {
  const navigate = useNavigate();
  const { register } = useAuth();
  const [form, setForm] = useState({ fullName: "", email: "", password: "", confirmPassword: "" });
  const [errors, setErrors] = useState<Errors>({});
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  function update<K extends keyof typeof form>(key: K, value: string) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  function validate(): boolean {
    const next: Errors = {};
    if (!form.fullName.trim()) next.fullName = "Full name is required";
    if (!form.email.trim()) next.email = "Email is required";
    else if (!/^\S+@\S+\.\S+$/.test(form.email)) next.email = "Enter a valid email address";
    if (!form.password) next.password = "Password is required";
    else if (form.password.length < 8) next.password = "Must be at least 8 characters";
    if (form.confirmPassword !== form.password) next.confirmPassword = "Passwords don't match";
    setErrors(next);
    return Object.keys(next).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setFormError(null);
    if (!validate()) return;

    setSubmitting(true);
    try {
      await register(form.fullName, form.email, form.password);
      navigate("/dashboard", { replace: true });
    } catch (err) {
      setFormError(getErrorMessage(err, "Something went wrong. Try again."));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <AuthLayout
      title="Create your account"
      subtitle="Set up Attrition Analyzer for your HR team in minutes."
      footer={
        <>
          Already have an account?{" "}
          <Link to="/login" className="font-semibold text-brand-900 hover:underline">
            Log in
          </Link>
        </>
      }
    >
      <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">
        <FormField
          label="Full name"
          placeholder="Jordan Blake"
          value={form.fullName}
          onChange={(e) => update("fullName", e.target.value)}
          error={errors.fullName}
          autoComplete="name"
        />
        <FormField
          label="Email"
          type="email"
          placeholder="you@company.com"
          value={form.email}
          onChange={(e) => update("email", e.target.value)}
          error={errors.email}
          autoComplete="email"
        />
        <FormField
          label="Password"
          type="password"
          placeholder="At least 8 characters"
          value={form.password}
          onChange={(e) => update("password", e.target.value)}
          error={errors.password}
          autoComplete="new-password"
        />
        <FormField
          label="Confirm password"
          type="password"
          placeholder="••••••••"
          value={form.confirmPassword}
          onChange={(e) => update("confirmPassword", e.target.value)}
          error={errors.confirmPassword}
          autoComplete="new-password"
        />

        {formError && (
          <p role="alert" className="rounded-xl bg-red-50 px-4 py-3 text-sm font-medium text-red-700">
            {formError}
          </p>
        )}

        <Button type="submit" size="lg" disabled={submitting} className="w-full">
          {submitting ? "Creating account..." : "Create account"}
        </Button>
      </form>
    </AuthLayout>
  );
}

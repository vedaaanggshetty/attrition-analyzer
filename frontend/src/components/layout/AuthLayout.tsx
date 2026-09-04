import type { ReactNode } from "react";
import { Link } from "react-router-dom";
import { Wordmark } from "../ui/Wordmark";

export function AuthLayout({
  title,
  subtitle,
  children,
  footer,``
}: {
    title: string;
    subtitle: string;
    children: ReactNode;
    footer: ReactNode;
  }) {
  return (
    <div className="flex min-h-screen">
      <div className="hidden w-1/2 flex-col justify-between bg-brand-900 p-12 text-white lg:flex">
        <Link to="/">
          <Wordmark dark />
        </Link>
        <div>
          <p className="font-serif text-4xl font-semibold italic leading-tight tracking-tight xl:text-5xl">
            Understand your workforce.
            <br />
            Before they leave.
          </p>
          <p className="mt-4 max-w-sm text-sm text-white/50">
            Department-level attrition insight, flagged employees, and workforce intelligence in one
            place.
          </p>
        </div>
        <p className="text-xs text-white/30">&copy; {new Date().getFullYear()} Attrition Analyzer</p>
      </div>

      <div className="flex w-full flex-col justify-center px-6 py-16 sm:px-12 lg:w-1/2 lg:px-20">
        <Link to="/" className="mb-10 lg:hidden">
          <Wordmark />
        </Link>

        <div className="mx-auto w-full max-w-sm">
          <h1 className="font-serif text-4xl font-semibold italic tracking-tight text-ink-900">{title}</h1>
          <p className="mt-2 text-base text-neutral-500">{subtitle}</p>
          <div className="mt-8">{children}</div>
          <p className="mt-6 text-center text-sm text-neutral-500">{footer}</p>
        </div>
      </div>
    </div>
  );
}

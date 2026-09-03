import { Link } from "react-router-dom";
import { Wordmark } from "../ui/Wordmark";

const COLUMNS = [
  {
    title: "Product",
    links: [
      { label: "Analytics", to: "/dashboard" },
      { label: "Employees", to: "/employees" },
      { label: "Notifications", to: "/dashboard" },
    ],
  },
  {
    title: "Company",
    links: [
      { label: "About", to: "/about" },
      { label: "Get Started", to: "/register" },
      { label: "Log in", to: "/login" },
    ],
  },
];

export function Footer() {
  return (
    <footer className="border-t border-brand-900/10 bg-white">
      <div className="mx-auto max-w-7xl px-6 py-16 lg:px-10">
        <div className="flex flex-col justify-between gap-12 md:flex-row">
          <div className="max-w-xs">
            <Link to="/">
              <Wordmark />
            </Link>
            <p className="mt-4 text-sm leading-relaxed text-neutral-500">
              Understand your workforce. Before they leave.
            </p>
          </div>

          <div className="grid grid-cols-2 gap-10 sm:gap-20">
            {COLUMNS.map((column) => (
              <div key={column.title}>
                <p className="text-xs font-semibold uppercase tracking-wider text-neutral-400">
                  {column.title}
                </p>
                <ul className="mt-4 flex flex-col gap-3">
                  {column.links.map((link) => (
                    <li key={link.label}>
                      <Link to={link.to} className="text-sm text-neutral-600 transition-colors hover:text-brand-900">
                        {link.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
        </div>

        <div className="mt-16 flex flex-col gap-4 border-t border-brand-900/10 pt-8 text-xs text-neutral-400 sm:flex-row sm:items-center sm:justify-between">
          <p>&copy; {new Date().getFullYear()} Attrition Analyzer. All rights reserved.</p>
          <p>Built for HR teams who'd rather ask "why" before it's too late.</p>
        </div>
      </div>
    </footer>
  );
}

import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { cx } from "../../lib/utils";
import { useAuth } from "../../context/AuthContext";
import { Avatar } from "../ui/Avatar";
import { Wordmark } from "../ui/Wordmark";

const NAV_ITEMS = [
  { label: "Analytics", to: "/dashboard", icon: GridIcon },
  { label: "Employees", to: "/employees", icon: UsersIcon },
  { label: "Profile", to: "/profile", icon: UserIcon },
];

export function AppLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const displayName = user?.fullName ?? user?.email.split("@")[0] ?? "HR User";

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="h-screen overflow-hidden bg-neutral-100">
      <div className="relative flex h-screen overflow-hidden bg-white">
        <div
          className={cx(
            "fixed inset-0 z-30 bg-black/40 transition-opacity duration-300 lg:hidden",
            sidebarOpen ? "opacity-100" : "pointer-events-none opacity-0"
          )}
          onClick={() => setSidebarOpen(false)}
          aria-hidden="true"
        />

        <aside
          className={cx(
            "fixed inset-y-0 left-0 z-40 flex w-64 shrink-0 flex-col overflow-y-auto border-r border-brand-900/10 bg-white px-5 py-6 transition-transform duration-300 lg:static lg:translate-x-0",
            sidebarOpen ? "translate-x-0" : "-translate-x-full"
          )}
        >
          <Link to="/" className="px-2">
            <Wordmark />
          </Link>

          <nav className="mt-10 flex flex-col gap-1">
            {NAV_ITEMS.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                onClick={() => setSidebarOpen(false)}
                className={({ isActive }) =>
                  cx(
                    "flex items-center gap-3 rounded-xl px-3 py-2.5 text-sm font-medium transition-colors",
                    isActive ? "bg-brand-900 text-white" : "text-neutral-600 hover:bg-brand-50 hover:text-brand-900"
                  )
                }
              >
                <item.icon className="h-[18px] w-[18px]" />
                {item.label}
              </NavLink>
            ))}
          </nav>

          {/* Profile widget lives right under the nav, not pinned to the
              bottom of a sidebar that can grow taller than the viewport. */}
          <div className="mt-4 flex items-center gap-3 rounded-xl border border-brand-900/10 bg-brand-50/60 p-3 backdrop-blur-sm">
            <Avatar firstName={displayName} lastName="" size="sm" />
            <div className="min-w-0 flex-1">
              <p className="truncate text-sm font-semibold text-brand-900">{displayName}</p>
              <p className="truncate text-xs text-neutral-500">{user?.role ?? "HR User"}</p>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              aria-label="Log out"
              className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-neutral-400 transition-colors hover:bg-white hover:text-brand-900"
            >
              <LogoutIcon className="h-4 w-4" />
            </button>
          </div>
        </aside>

        <div className="flex min-w-0 flex-1 flex-col">
          <header className="flex h-16 shrink-0 items-center gap-4 border-b border-brand-900/10 bg-white/80 px-5 backdrop-blur-xl lg:px-8">
            <button
              type="button"
              aria-label="Open sidebar"
              onClick={() => setSidebarOpen(true)}
              className="flex h-9 w-9 items-center justify-center rounded-full hover:bg-brand-50 lg:hidden"
            >
              <MenuIcon className="h-5 w-5" />
            </button>
            <Link to="/" className="ml-auto text-sm font-medium text-neutral-500 hover:text-brand-900">
              &larr; Back to site
            </Link>
          </header>
          {/* Only this pane scrolls - the sidebar and header stay put. */}
          <main className="flex-1 overflow-y-auto px-5 py-8 lg:px-8 lg:py-10">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}

function GridIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <rect x="3" y="3" width="8" height="8" rx="1.5" />
      <rect x="13" y="3" width="8" height="8" rx="1.5" />
      <rect x="3" y="13" width="8" height="8" rx="1.5" />
      <rect x="13" y="13" width="8" height="8" rx="1.5" />
    </svg>
  );
}
function UsersIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <circle cx="9" cy="8" r="3.25" />
      <path d="M3 20c0-3.5 2.7-6 6-6s6 2.5 6 6" />
      <path d="M16 8.5a3 3 0 1 1 3.2 3" />
      <path d="M21 20c0-2.8-1.8-5-4.2-5.7" />
    </svg>
  );
}
function UserIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <circle cx="12" cy="8" r="3.5" />
      <path d="M4.5 20c0-4.1 3.4-7.5 7.5-7.5s7.5 3.4 7.5 7.5" />
    </svg>
  );
}
function LogoutIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M16 17l5-5-5-5M21 12H9" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}
function MenuIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <path d="M4 7h16M4 12h16M4 17h16" strokeLinecap="round" />
    </svg>
  );
}

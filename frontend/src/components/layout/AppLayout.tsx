import { Link, Outlet, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { Avatar } from "../ui/Avatar";
import { Wordmark } from "../ui/Wordmark";
import { Sidebar, SidebarBody, SidebarLink, useSidebar } from "../ui/sidebar";
import { motion } from "framer-motion";

const NAV_ITEMS = [
  { label: "Analytics", to: "/dashboard", icon: GridIcon },
  { label: "Employees", to: "/employees", icon: UsersIcon },
  { label: "Notifications", to: "/notifications", icon: BellIcon },
  { label: "Profile", to: "/profile", icon: UserIcon },
];

export function AppLayout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const displayName = user?.fullName ?? user?.email.split("@")[0] ?? "HR User";

  function handleLogout() {
    logout();
    navigate("/login", { replace: true });
  }

  return (
    <div className="h-screen overflow-hidden bg-neutral-100">
      <div className="relative flex h-screen flex-col overflow-hidden bg-white md:flex-row">
        {/* Hover-glide rail: starts expanded (252px, full labels), eases to
            a 76px icon rail on mouseleave and back on mouseenter. It's a
            normal in-flow flex child, so the main content genuinely resizes
            alongside it - never covered, never clipped. */}
        <Sidebar>
          <SidebarBody className="justify-between gap-8">
            <SidebarContent
              location={location}
              displayName={displayName}
              userRole={user?.role}
              onLogout={handleLogout}
            />
          </SidebarBody>
        </Sidebar>

        <div className="flex min-w-0 flex-1 flex-col">
          {/* Reuses the landing page's dot-grid background language, at a
              near-invisible opacity - purely decorative, strictly behind
              content, never a dark-mode change. */}
          <main className="bg-dot-grid relative flex-1 overflow-y-auto px-5 py-8 lg:px-8 lg:py-10">
            <Outlet />
          </main>
        </div>
      </div>
    </div>
  );
}

// Separated so it can call useSidebar (which requires being inside SidebarProvider).
function SidebarContent({
  location,
  displayName,
  userRole,
  onLogout,
}: {
  location: ReturnType<typeof useLocation>;
  displayName: string;
  userRole?: string;
  onLogout: () => void;
}) {
  const { open, animate } = useSidebar();

  // Same animate-in/out pattern as SidebarLink's label span.
  const fadeMotion = {
    animate: {
      display: animate ? (open ? "inline-block" : "none") : "inline-block",
      opacity: animate ? (open ? 1 : 0) : 1,
    },
    transition: { duration: 0.15 },
  };

  return (
    <>
      <div className="flex flex-1 flex-col overflow-x-hidden overflow-y-auto">
        {/* Logo — always shows the small dot, animates the text label out */}
        <Link to="/" className="flex items-center gap-2 px-2 py-1">
          <span className="flex h-6 w-6 shrink-0 items-center justify-center">
            <span className="block h-2.5 w-2.5 rounded-full bg-brand-700" />
          </span>
          <motion.span
            {...fadeMotion}
            className="!m-0 !p-0 overflow-hidden whitespace-nowrap"
          >
            <Wordmark />
          </motion.span>
        </Link>

        <nav className="mt-10 flex flex-col gap-1">
          {NAV_ITEMS.map((item) => (
            <SidebarLink
              key={item.to}
              link={{
                label: item.label,
                href: item.to,
                active: location.pathname.startsWith(item.to),
                icon: <item.icon className="h-[18px] w-[18px]" />,
              }}
            />
          ))}
        </nav>
      </div>

      {/* Bottom: avatar card + back link */}
      <div className="flex flex-col gap-2">
        <div className="flex items-center gap-2.5 rounded-xl border border-brand-900/10 bg-brand-50/60 p-2.5">
          {/* Avatar is always visible */}
          <Avatar firstName={displayName} lastName="" size="sm" />

          {/* Name + role animate out */}
          <motion.div
            {...fadeMotion}
            className="!m-0 !p-0 flex min-w-0 flex-1 flex-col overflow-hidden whitespace-nowrap"
          >
            <p className="truncate text-sm font-semibold text-ink-900">{displayName}</p>
            <p className="truncate text-xs text-neutral-500">{userRole ?? "HR User"}</p>
          </motion.div>

          {/* Logout button animates out with the text */}
          <motion.button
            {...fadeMotion}
            type="button"
            onClick={onLogout}
            aria-label="Log out"
            className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full text-neutral-400 transition-colors hover:bg-white hover:text-brand-900"
          >
            <LogoutIcon className="h-4 w-4" />
          </motion.button>
        </div>

        {/* "Back to site" animates out entirely */}
        <motion.div {...fadeMotion} className="!m-0 !p-0 overflow-hidden">
          <Link
            to="/"
            className="block whitespace-nowrap px-1 text-xs font-medium text-neutral-400 hover:text-brand-900"
          >
            &larr; Back to site
          </Link>
        </motion.div>
      </div>
    </>
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
function BellIcon(props: React.SVGProps<SVGSVGElement>) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" {...props}>
      <path d="M6 8a6 6 0 1 1 12 0c0 5 2 6 2 6H4s2-1 2-6Z" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M9.5 20a2.5 2.5 0 0 0 5 0" strokeLinecap="round" />
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

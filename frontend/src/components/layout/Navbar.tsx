import { useEffect, useState } from "react";
import { Link, NavLink, useNavigate } from "react-router-dom";
import { cx } from "../../lib/utils";
import { useAuth } from "../../context/AuthContext";
import { Button } from "../ui/Button";
import { Wordmark } from "../ui/Wordmark";

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);
  const { isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  // Guest = not logged in. Analytics/Employees both point at protected
  // routes so ProtectedRoute redirects a guest straight to /login; an
  // authenticated HR user lands on the real page.
  const LINKS = [
    { label: "Home", to: "/" },
    { label: "Analytics", to: "/dashboard" },
    { label: "Employees", to: "/employees" },
    { label: "About", to: "/about" },
  ];

  function handleLogout() {
    setMenuOpen(false);
    logout();
    navigate("/", { replace: true });
  }

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => {
    document.body.style.overflow = menuOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [menuOpen]);

  return (
    <>
      <header
        className={cx(
          "fixed inset-x-0 top-0 z-50 transition-all duration-300",
          scrolled ? "border-b border-brand-900/10 bg-white/70 backdrop-blur-xl" : "border-b border-transparent"
        )}
      >
        <nav className="mx-auto flex h-[72px] max-w-7xl items-center justify-between px-6 lg:px-10">
          <Link to="/" onClick={() => setMenuOpen(false)}>
            <Wordmark />
          </Link>

          <div className="hidden items-center gap-1 md:flex">
            {LINKS.map((link) => (
              <NavLink
                key={link.to}
                to={link.to}
                className={({ isActive }) =>
                  cx(
                    "group relative rounded-full px-4 py-2 text-sm font-medium transition-colors",
                    isActive ? "text-brand-900" : "text-neutral-500 hover:text-brand-900"
                  )
                }
              >
                {({ isActive }) => (
                  <>
                    {link.label}
                    <span
                      className={cx(
                        "pointer-events-none absolute inset-x-4 -bottom-0.5 h-px origin-left scale-x-0 bg-brand-900 transition-transform duration-300 group-hover:scale-x-100",
                        isActive && "scale-x-100"
                      )}
                    />
                  </>
                )}
              </NavLink>
            ))}
          </div>

          <div className="hidden items-center gap-2 md:flex">
            {isAuthenticated ? (
              <>
                <Link to="/dashboard">
                  <Button variant="ghost" size="sm">
                    Dashboard
                  </Button>
                </Link>
                <Button variant="primary" size="sm" onClick={handleLogout}>
                  Log out
                </Button>
              </>
            ) : (
              <>
                <Link to="/login">
                  <Button variant="ghost" size="sm">
                    Log in
                  </Button>
                </Link>
                <Link to="/register">
                  <Button variant="primary" size="sm">
                    Get Started
                  </Button>
                </Link>
              </>
            )}
          </div>

          <button
            type="button"
            aria-label={menuOpen ? "Close menu" : "Open menu"}
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((v) => !v)}
            className="flex h-10 w-10 items-center justify-center rounded-full transition-colors hover:bg-brand-50 md:hidden"
          >
            <div className="relative flex h-4 w-5 flex-col justify-between">
              <span
                className={cx(
                  "h-0.5 w-full origin-left rounded-full bg-brand-900 transition-all duration-300",
                  menuOpen && "translate-x-px rotate-45"
                )}
              />
              <span
                className={cx(
                  "h-0.5 w-full rounded-full bg-brand-900 transition-all duration-300",
                  menuOpen && "opacity-0"
                )}
              />
              <span
                className={cx(
                  "h-0.5 w-full origin-left rounded-full bg-brand-900 transition-all duration-300",
                  menuOpen && "translate-x-px -rotate-45"
                )}
              />
            </div>
          </button>
        </nav>
      </header>

      <div
        className={cx(
          "fixed inset-0 z-40 bg-black/40 transition-opacity duration-300 md:hidden",
          menuOpen ? "opacity-100" : "pointer-events-none opacity-0"
        )}
        onClick={() => setMenuOpen(false)}
        aria-hidden="true"
      />

      <div
        className={cx(
          "fixed inset-y-0 right-0 z-40 flex w-[82%] max-w-sm flex-col bg-brand-900 px-8 py-24 text-white transition-transform duration-500 ease-[cubic-bezier(0.65,0,0.35,1)] md:hidden",
          menuOpen ? "translate-x-0" : "translate-x-full"
        )}
      >
        <nav className="flex flex-col gap-1">
          {LINKS.map((link, i) => (
            <NavLink
              key={link.to}
              to={link.to}
              onClick={() => setMenuOpen(false)}
              style={{ transitionDelay: menuOpen ? `${80 + i * 60}ms` : "0ms" }}
              className={cx(
                "border-b border-white/10 py-4 font-display text-3xl font-semibold tracking-tight transition-all duration-500",
                menuOpen ? "translate-x-0 opacity-100" : "translate-x-6 opacity-0"
              )}
            >
              {link.label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-10 flex flex-col gap-3">
          {isAuthenticated ? (
            <>
              <Link to="/dashboard" onClick={() => setMenuOpen(false)}>
                <Button variant="secondary" className="w-full !border-white/25 !bg-transparent !text-white">
                  Dashboard
                </Button>
              </Link>
              <Button
                variant="primary"
                className="w-full !bg-white !text-brand-900 hover:!bg-white/90"
                onClick={handleLogout}
              >
                Log out
              </Button>
            </>
          ) : (
            <>
              <Link to="/login" onClick={() => setMenuOpen(false)}>
                <Button variant="secondary" className="w-full !border-white/25 !bg-transparent !text-white">
                  Log in
                </Button>
              </Link>
              <Link to="/register" onClick={() => setMenuOpen(false)}>
                <Button variant="primary" className="w-full !bg-white !text-brand-900 hover:!bg-white/90">
                  Get Started
                </Button>
              </Link>
            </>
          )}
        </div>
      </div>
    </>
  );
}

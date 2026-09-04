import { Link } from "react-router-dom";
import { Button } from "../ui/Button";
import { useReveal } from "../../hooks/useReveal";
import { useAuth } from "../../context/AuthContext";

export function CtaSection() {
  const ref = useReveal<HTMLDivElement>(80);
  const { isAuthenticated } = useAuth();

  return (
    <section className="px-6 pb-24 lg:px-10 lg:pb-32">
      <div
        ref={ref}
        data-reveal
        className="relative mx-auto max-w-6xl overflow-hidden rounded-[2rem] bg-brand-900 px-8 py-20 text-center sm:px-16"
      >
        <div
          className="pointer-events-none absolute inset-0 opacity-25"
          style={{
            backgroundImage:
              "radial-gradient(circle at 20% 20%, white 0, transparent 45%), radial-gradient(circle at 80% 70%, #2196F3 0, transparent 40%)",
          }}
          aria-hidden="true"
        />
        <div className="relative">
          <h2 className="font-display text-4xl font-semibold tracking-tight text-white sm:text-5xl">
            {isAuthenticated ? "Your workforce data, ready when you are." : "Stop guessing who's about to leave."}
          </h2>
          <p className="mx-auto mt-4 max-w-xl text-base text-white/60">
            {isAuthenticated
              ? "Jump back into the dashboard to explore employees and attrition by department, role, and more."
              : "Set up Attrition Analyzer in minutes. No credit card, no commitment - just clarity."}
          </p>
          <div className="mt-9 flex flex-col items-center justify-center gap-3 sm:flex-row">
            {isAuthenticated ? (
              <Link to="/dashboard">
                <Button size="lg" className="!bg-white !text-brand-900 hover:!bg-white/90">
                  Go to Dashboard
                </Button>
              </Link>
            ) : (
              <>
                <Link to="/register">
                  <Button size="lg" className="!bg-white !text-brand-900 hover:!bg-white/90">
                    Create your account
                  </Button>
                </Link>
                <Link to="/login">
                  <Button variant="secondary" size="lg" className="!border-white/25 !bg-transparent !text-white">
                    Log in
                  </Button>
                </Link>
              </>
            )}
          </div>
        </div>
      </div>
    </section>
  );
}

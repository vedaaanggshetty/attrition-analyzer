import { attritionByDepartment, attritionByJobRole } from "../../data/mockData";
import { useReveal } from "../../hooks/useReveal";
import { useParallax } from "../../hooks/useParallax";
import { Card } from "../ui/Card";
import { BarList } from "../ui/BarList";

export function AnalyticsShowcase() {
  const ref = useReveal<HTMLDivElement>(100);
  // Large decorative number - extremely slow, independent drift.
  const { ref: numeralRef, offset: numeralOffset } = useParallax<HTMLSpanElement>(0.02);

  return (
    <section className="bg-brand-50/40 py-24 lg:py-32">
      <div className="mx-auto max-w-7xl px-6 lg:px-10">
        <div className="mb-14 max-w-2xl">
          <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-neutral-400">Analytics</p>
          <h2 className="font-display text-4xl font-semibold tracking-tight text-brand-900 sm:text-5xl">
            Attrition, broken down the way HR actually thinks
          </h2>
          <p className="mt-4 text-base text-neutral-500">
            Live-recalculated the moment new employee data arrives. Sample data shown below.
          </p>
        </div>

        <div ref={ref} className="grid gap-5 lg:grid-cols-5">
          <Card className="relative overflow-hidden p-7 lg:col-span-3" data-reveal>
            <span
              ref={numeralRef}
              aria-hidden="true"
              className="pointer-events-none absolute -bottom-8 -right-4 select-none font-display font-bold text-brand-900/[0.04]"
              style={{ fontSize: "clamp(5rem, 16vw, 10rem)", lineHeight: 1, transform: `translateY(${numeralOffset}px)` }}
            >
              {attritionByDepartment[0]?.attritionRate}
            </span>
            <div className="relative">
              <h3 className="font-display text-lg font-semibold text-brand-900">Attrition by Department</h3>
              <p className="mt-1 text-sm text-neutral-500">Highest-risk departments, ranked</p>
              <div className="mt-6">
                <BarList data={attritionByDepartment.slice(0, 6)} />
              </div>
            </div>
          </Card>
          <Card className="p-7 lg:col-span-2" data-reveal>
            <h3 className="font-display text-lg font-semibold text-brand-900">Attrition by Job Role</h3>
            <p className="mt-1 text-sm text-neutral-500">Where turnover concentrates</p>
            <div className="mt-6">
              <BarList data={attritionByJobRole.slice(0, 6)} />
            </div>
          </Card>
        </div>
      </div>
    </section>
  );
}

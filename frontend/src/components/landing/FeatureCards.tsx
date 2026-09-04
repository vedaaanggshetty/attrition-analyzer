import { useReveal } from "../../hooks/useReveal";
import { useParallax } from "../../hooks/useParallax";

const FEATURES = [
  {
    title: "Employee Analytics",
    description:
      "Every employee record, searchable and filterable across department, role, tenure, and compensation.",
    stat: "84",
    statLabel: "employees tracked",
  },
  {
    title: "Attrition Insights",
    description: "Department and job-role attrition rates, ranked - no spreadsheets required.",
    stat: "6",
    statLabel: "departments",
  },
  {
    title: "Workforce Intelligence",
    description: "Compensation, demographics, work-life, and career progression, cross-referenced.",
    stat: "5",
    statLabel: "signal types",
  },
];

// Shared-background "masked card" mosaic: one large gradient behind the grid,
// each card offset via bg-position so together they read as a single image.
const GRID_BG = "linear-gradient(115deg, #071b3a 0%, #0d47a1 30%, #2196f3 55%, #0d47a1 80%, #071b3a 100%)";

export function FeatureCards() {
  const ref = useReveal<HTMLDivElement>(100);

  return (
    <section className="relative z-10 mx-auto -mt-10 max-w-7xl px-6 pb-24 pt-8 lg:-mt-16 lg:px-10 lg:pb-32">
      <div className="mb-14 flex flex-col items-start justify-between gap-6 sm:flex-row sm:items-end">
        <div className="max-w-2xl">
          <p className="mb-3 text-xs font-semibold uppercase tracking-wider text-neutral-400">Platform</p>
          <h2 className="font-serif text-4xl font-semibold tracking-tight text-brand-900 sm:text-5xl">
            Three ways to see your <span className="italic">people</span> clearly
          </h2>
        </div>
        {/* Editorial annotation - deliberately off the grid, hand-set feel */}
        <p className="hidden -rotate-2 text-right font-serif text-sm italic text-neutral-400 sm:block">
          not another dashboard
          <br />
          to configure &darr;
        </p>
      </div>

      <div ref={ref} className="grid gap-4 md:grid-cols-3 md:grid-rows-2">
        {FEATURES.map((feature, i) => (
          <FeatureCard key={feature.title} feature={feature} index={i} />
        ))}
      </div>
    </section>
  );
}

function FeatureCard({ feature, index: i }: { feature: (typeof FEATURES)[number]; index: number }) {
  // Each masked visual drifts at its own slow, independent speed within the
  // card's mask - the card itself stays put (and still handles hover/reveal).
  const { ref: visualRef, offset: visualOffset } = useParallax<HTMLDivElement>(0.03 + i * 0.015);
  // Large decorative numerals move even slower than the visual behind them.
  const { ref: numeralRef, offset: numeralOffset } = useParallax<HTMLSpanElement>(0.02);

  return (
    <div
      data-reveal
      className={
        "group relative overflow-hidden rounded-2xl text-white transition-transform duration-500 hover:-translate-y-1.5 " +
        (i === 0 ? "aspect-[4/5] md:col-span-2 md:row-span-2 md:aspect-auto" : "aspect-[4/5] md:aspect-auto")
      }
    >
      <div
        ref={visualRef}
        className="absolute -inset-y-[15%] inset-x-0"
        style={{
          backgroundImage: GRID_BG,
          backgroundSize: "320% 320%",
          backgroundPosition: `${(i / 2) * 100}% ${(i / 2) * 60}%`,
          transform: `translateY(${visualOffset}px)`,
        }}
      />
      <div className="absolute inset-0 bg-black/10 transition-colors duration-500 group-hover:bg-black/0" />

      {/* Oversized background numeral - the "unexpected large statistic" beat */}
      <span
        ref={numeralRef}
        aria-hidden="true"
        className="pointer-events-none absolute -bottom-6 -right-3 select-none font-display font-bold text-white/[0.06]"
        style={{ fontSize: i === 0 ? "clamp(6rem, 20vw, 13rem)" : "clamp(4rem, 14vw, 8rem)", lineHeight: 1, transform: `translateY(${numeralOffset}px)` }}
      >
        {feature.stat}
      </span>

      <div className="relative flex h-full flex-col justify-between p-7">
        <span className="font-display text-sm font-semibold text-white/50">0{i + 1}</span>
        <div>
          <h3 className="font-display text-2xl font-semibold tracking-tight">{feature.title}</h3>
          <p className="mt-3 max-w-xs text-sm leading-relaxed text-white/70">{feature.description}</p>
          <p className="mt-4 text-xs font-semibold uppercase tracking-wider text-white/40">
            {feature.stat} {feature.statLabel}
          </p>
        </div>
      </div>
    </div>
  );
}

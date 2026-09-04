import { useReveal } from "../../hooks/useReveal";
import {
  Search,
  LayoutGrid,
  TrendingUp,
  Bell,
  ShieldCheck,
  RefreshCw,
} from "lucide-react";

const ITEMS = [
  {
    icon: Search,
    title: "Attrition Explorer",
    description: "Filter every employee by department, role, compensation band, and more - combinable, in one place.",
    span: "tall" as const,
  },
  {
    icon: LayoutGrid,
    title: "Six-dimension analysis",
    description:
      "Department, Job Role, Compensation, Demographics, Work-Life Balance, and Career Progression - ranked, not guessed.",
    span: "tall" as const,
  },
  {
    icon: TrendingUp,
    title: "Career Progression",
    description: "See how time since last promotion tracks against attrition.",
    span: "short" as const,
  },
  {
    icon: Bell,
    title: "Notifications",
    description: "Send a note on an employee's record and track it through to resolution.",
    span: "short" as const,
  },
  {
    icon: ShieldCheck,
    title: "Guest & HR roles",
    description: "Guests get a limited read-only view; HR users get the full workforce picture.",
    span: "short" as const,
  },
  {
    icon: RefreshCw,
    title: "Survey-sourced data",
    description: "Employee records are pulled straight from the Survey API - no manual imports.",
    span: "short" as const,
  },
  {
    icon: ShieldCheck,
    title: "Secure by design",
    description: "JWT-authenticated access, scoped strictly to the signed-in HR user.",
    span: "short" as const,
  },
];

export function BentoFeatures() {
  const ref = useReveal<HTMLDivElement>(100);

  return (
    <section className="relative mx-auto max-w-7xl px-6 pb-24 pt-4 lg:px-10 lg:pb-32 lg:pt-8">
      <div
        ref={ref}
        data-reveal
        className="relative overflow-hidden rounded-[2rem] p-6 sm:p-8 lg:p-10"
        style={{
          background: "linear-gradient(160deg, #050505 0%, #101012 55%, #1c1c1f 130%)",
        }}
      >
        <NetworkIllustration />

        <div className="relative mb-10 max-w-xl">
          <h2 className="font-serif text-3xl font-semibold tracking-tight text-white sm:text-4xl">
            Everything HR needs, <span className="italic">nothing it doesn't</span>
          </h2>
        </div>

        <div className="relative grid grid-cols-1 gap-4 sm:grid-cols-2 md:grid-cols-3 md:auto-rows-[9.5rem]">
          {ITEMS.map((item) => (
            <BentoCard key={item.title} item={item} />
          ))}
        </div>
      </div>
    </section>
  );
}

function BentoCard({ item }: { item: (typeof ITEMS)[number] }) {
  const Icon = item.icon;
  return (
    <div
      className={
        "group relative flex flex-col justify-between overflow-hidden rounded-2xl border border-brand-300/[0.14] bg-brand-500/[0.06] p-6 backdrop-blur-sm transition-colors duration-300 hover:border-brand-300/25 hover:bg-brand-500/[0.11] " +
        (item.span === "tall" ? "md:row-span-2" : "")
      }
    >
      <div
        className="pointer-events-none absolute -inset-24 opacity-0 transition-opacity duration-500 group-hover:opacity-100"
        style={{
          background: "radial-gradient(circle at 30% 20%, rgba(255,255,255,0.1), transparent 60%)",
        }}
        aria-hidden="true"
      />
      <Icon
        className="relative h-6 w-6 text-brand-300/70 transition-colors duration-300 group-hover:text-brand-300"
        strokeWidth={1.5}
      />
      <div className="relative mt-8">
        <h3 className="text-base font-semibold text-white">{item.title}</h3>
        <p className="mt-2 text-sm leading-relaxed text-white/55">{item.description}</p>
      </div>
    </div>
  );
}

// Minimal geometric node network - a blue-toned abstract stand-in for
// "people/analytics", sitting behind the heading so the header row doesn't
// read as dead space above the card grid.
function NetworkIllustration() {
  const nodes = [
    { x: 620, y: 40, r: 5 },
    { x: 700, y: 90, r: 8 },
    { x: 660, y: 140, r: 4 },
    { x: 740, y: 30, r: 4 },
    { x: 780, y: 110, r: 6 },
    { x: 600, y: 110, r: 3 },
  ];
  return (
    <svg
      viewBox="0 0 820 180"
      aria-hidden="true"
      className="pointer-events-none absolute right-0 top-0 h-[180px] w-[420px] opacity-40 sm:opacity-60"
    >
      <g stroke="#9a9ca3" strokeWidth="1" opacity="0.5">
        <line x1="620" y1="40" x2="700" y2="90" />
        <line x1="700" y1="90" x2="660" y2="140" />
        <line x1="700" y1="90" x2="740" y2="30" />
        <line x1="700" y1="90" x2="780" y2="110" />
        <line x1="620" y1="40" x2="600" y2="110" />
        <line x1="660" y1="140" x2="600" y2="110" />
      </g>
      {nodes.map((n, i) => (
        <circle key={i} cx={n.x} cy={n.y} r={n.r} fill="#f4f4f5" opacity={i === 1 ? 0.9 : 0.55} />
      ))}
    </svg>
  );
}

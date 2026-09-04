import { useReveal } from "../hooks/useReveal";

const VALUES = [
  {
    title: "Signal over noise",
    description: "We surface the handful of metrics that actually predict attrition, not every metric that exists.",
  },
  {
    title: "Built for HR, not analysts",
    description: "No SQL, no dashboards to configure. Open it and the story is already there.",
  },
  {
    title: "Privacy by default",
    description: "Employee data stays scoped to the HR users who need it, nothing more.",
  },
];

export function About() {
  const ref = useReveal<HTMLDivElement>(100);

  return (
    <div className="mx-auto max-w-4xl px-6 pb-24 pt-40 lg:px-10">
      <p className="mb-4 text-xs font-semibold uppercase tracking-wider text-neutral-400">About</p>
      <h1 className="font-serif text-5xl font-semibold italic tracking-tight text-brand-900 sm:text-6xl">
        We built the tool we wished HR had.
      </h1>
      <p className="mt-6 max-w-2xl text-lg leading-relaxed text-neutral-500">
        Attrition Analyzer was built for HR teams drowning in spreadsheets and exit interviews that
        arrive too late. It turns employee data your organization already has into a clear, ranked view
        of where attrition risk is concentrating - by department, role, compensation band, and tenure.
      </p>

      <div ref={ref} className="mt-16 grid gap-4 sm:grid-cols-3">
        {VALUES.map((value) => (
          <div key={value.title} data-reveal className="rounded-2xl border border-brand-900/10 p-6">
            <h3 className="font-serif text-lg font-semibold text-brand-900">{value.title}</h3>
            <p className="mt-2 text-sm leading-relaxed text-neutral-500">{value.description}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

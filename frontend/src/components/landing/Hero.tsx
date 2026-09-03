import { Link } from "react-router-dom";
import { Button } from "../ui/Button";
import { useReveal } from "../../hooks/useReveal";
import { useParallax } from "../../hooks/useParallax";

export function Hero() {
  const ref = useReveal<HTMLDivElement>(120);

  return (
    <section className="relative overflow-hidden pb-24 pt-40 lg:pb-32 lg:pt-48">
      <AbstractWorkforceVisual />
      <CurvedLines />
      {/* Ambient keyframe-driven blobs - continuous, independent of scroll */}
      <div
        className="animate-drift pointer-events-none absolute -left-24 top-10 -z-10 h-72 w-72 rounded-full bg-brand-300/30 blur-3xl"
        aria-hidden="true"
      />
      <div
        className="animate-drift-slow pointer-events-none absolute -right-16 top-40 -z-10 h-96 w-96 rounded-full bg-brand-500/15 blur-3xl"
        aria-hidden="true"
      />
      <div ref={ref} className="relative mx-auto max-w-5xl px-6 text-center lg:px-10">
        <h1
          data-reveal
          className="font-display text-[clamp(2.75rem,7vw,6rem)] font-semibold leading-[0.98] tracking-tight text-brand-900"
        >
          Understand your <span className="font-serif italic text-neutral-800">workforce.</span>
          <br />
          <span className="text-neutral-400">Before they leave.</span>
        </h1>
        <p data-reveal className="mx-auto mt-6 max-w-xl text-lg leading-relaxed text-neutral-500">
          Attrition Analyzer turns raw employee data into clear, department-level insight -
          so you can act on flight risk weeks before it becomes an exit interview.
        </p>
        <div data-reveal className="mt-9 flex flex-col items-center justify-center gap-3 sm:flex-row">
          <Link to="/register">
            <Button size="lg">Get Started</Button>
          </Link>
          <Link to="/dashboard">
            <Button variant="secondary" size="lg">
              View Analytics
            </Button>
          </Link>
        </div>
      </div>

      {/* Progressive blur, matches the reference hero's bottom fade into the page */}
      <div
        className="pointer-events-none absolute inset-x-0 bottom-0 h-44 bg-gradient-to-b from-transparent to-white"
        aria-hidden="true"
      />
    </section>
  );
}

function CurvedLines() {
  // Extremely slow, independent drift - moves opposite the dot-grid behind it.
  const { ref, offset } = useParallax<HTMLDivElement>(0.05);

  const side = (edge: "left" | "right") =>
    Array.from({ length: 10 }, (_, i) => (
      <span
        key={`${edge}-${i}`}
        className="line-pulse absolute top-1/2 block -translate-y-1/2 border-2 border-brand-900/20"
        style={{
          [edge]: `${i * 22}px`,
          width: `${60 + i * 10}px`,
          height: "220px",
          borderRadius: edge === "left" ? "0 999px 999px 0" : "999px 0 0 999px",
          borderLeft: edge === "left" ? "none" : undefined,
          borderRight: edge === "right" ? "none" : undefined,
          animationDelay: `${i * 0.25}s`,
        }}
      />
    ));

  return (
    <div
      ref={ref}
      className="pointer-events-none absolute inset-0 -z-10 hidden overflow-hidden opacity-60 lg:block"
      style={{ transform: `translateY(${-offset}px)` }}
      aria-hidden="true"
    >
      {side("left")}
      {side("right")}
    </div>
  );
}

function AbstractWorkforceVisual() {
  // Very slow movement relative to scroll - the background reads as almost static.
  const { ref, offset } = useParallax<HTMLDivElement>(0.1);
  const nodes = Array.from({ length: 28 }, (_, i) => i);

  return (
    <div className="pointer-events-none absolute inset-0 -z-10 overflow-hidden" aria-hidden="true">
      <div
        ref={ref}
        className="absolute left-1/2 top-0 h-[900px] w-[1400px] -translate-x-1/2"
        style={{ transform: `translate(-50%, ${offset}px)` }}
      >
        <svg viewBox="0 0 1400 900" className="h-full w-full opacity-[0.07]">
          {nodes.map((i) => {
            const x = (i % 7) * 200 + 60;
            const y = Math.floor(i / 7) * 220 + 60;
            const next = i + 1;
            const nx = (next % 7) * 200 + 60;
            const ny = Math.floor(next / 7) * 220 + 60;
            return (
              <g key={i}>
                {i % 7 !== 6 && <line x1={x} y1={y} x2={nx} y2={ny} stroke="#0d47a1" strokeWidth="1" />}
                <circle cx={x} cy={y} r={i % 5 === 0 ? 10 : 5} fill="#0d47a1" />
              </g>
            );
          })}
        </svg>
      </div>
      <div className="absolute inset-0 bg-gradient-to-b from-transparent via-white/40 to-white" />
    </div>
  );
}

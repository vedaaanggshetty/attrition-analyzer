import { useEffect, useState } from "react";
import { cx } from "../../lib/utils";

export function SplashScreen({ onDone }: { onDone: () => void }) {
  const [count, setCount] = useState(0);
  const [leaving, setLeaving] = useState(false);

  useEffect(() => {
    const start = performance.now();
    const durationMs = 1400;

    let frame: number;
    const tick = (now: number) => {
      const progress = Math.min((now - start) / durationMs, 1);
      setCount(Math.round(progress * 100));
      if (progress < 1) {
        frame = requestAnimationFrame(tick);
      } else {
        setLeaving(true);
        window.setTimeout(onDone, 550);
      }
    };
    frame = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(frame);
  }, [onDone]);

  return (
    <div
      className={cx(
        "fixed inset-0 z-[100] flex flex-col items-center justify-center bg-brand-900 text-white transition-transform duration-500 ease-[cubic-bezier(0.65,0,0.35,1)]",
        leaving && "-translate-y-full"
      )}
    >
      <p className="mb-6 text-xs font-semibold uppercase tracking-[0.3em] text-white/50">
        Attrition Analyzer
      </p>
      <div className="font-display text-[clamp(4rem,14vw,9rem)] font-semibold leading-none tabular-nums">
        {count}
      </div>
      <div className="mt-8 h-px w-40 overflow-hidden bg-white/15">
        <div
          className="h-full bg-white transition-[width] duration-100 ease-linear"
          style={{ width: `${count}%` }}
        />
      </div>
    </div>
  );
}

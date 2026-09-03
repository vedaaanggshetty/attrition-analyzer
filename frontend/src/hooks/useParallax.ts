import { useEffect, useRef, useState } from "react";

/**
 * Subtle scroll-linked parallax. Returns a ref to attach and an offset (px)
 * to apply as `translateY`. `speed` is how far the element drifts relative
 * to scroll - keep these small (0.02-0.15) for an editorial feel, not a demo.
 * No-ops under prefers-reduced-motion.
 */
export function useParallax<T extends HTMLElement>(speed: number) {
  const ref = useRef<T | null>(null);
  const [offset, setOffset] = useState(0);

  useEffect(() => {
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;

    let ticking = false;
    const measure = () => {
      const el = ref.current;
      if (el) {
        const rect = el.getBoundingClientRect();
        const distanceFromCenter = rect.top + rect.height / 2 - window.innerHeight / 2;
        setOffset(-distanceFromCenter * speed);
      }
      ticking = false;
    };
    const onScroll = () => {
      if (ticking) return;
      ticking = true;
      requestAnimationFrame(measure);
    };

    measure();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll);
    return () => {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
    };
  }, [speed]);

  return { ref, offset };
}

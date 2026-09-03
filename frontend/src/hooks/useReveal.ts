import { useEffect, useRef } from "react";

/**
 * Adds the "reveal" class (defined in index.css) to every [data-reveal]
 * descendant of the returned ref, then un-hides them one by one as they
 * cross the viewport, staggered by DOM order. Re-runs on every render so
 * newly mounted content (e.g. after data loads) still gets observed.
 */
export function useReveal<T extends HTMLElement>(staggerMs = 80) {
  const containerRef = useRef<T | null>(null);

  useEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const targets = Array.from(container.querySelectorAll<HTMLElement>("[data-reveal]"));
    targets.forEach((el) => el.classList.add("reveal"));

    const observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (!entry.isIntersecting) return;
          const el = entry.target as HTMLElement;
          const index = targets.indexOf(el);
          window.setTimeout(() => {
            el.classList.add("reveal-visible");
          }, Math.max(index, 0) * staggerMs);
          observer.unobserve(el);
        });
      },
      { threshold: 0.15, rootMargin: "0px 0px -40px 0px" }
    );

    targets.forEach((el) => observer.observe(el));
    return () => observer.disconnect();
  });

  return containerRef;
}

import { useEffect, useRef, useState } from "react";

/**
 * Reveals `items` in batches as the user scrolls near the end, instead of
 * rendering the whole list at once. No network calls involved - `items` is
 * already fully loaded; this only controls how much of it is on the page.
 * Resets to the first batch whenever the item list itself changes (e.g. a
 * new search/filter result).
 */
export function useInfiniteBatch<T>(items: T[], batchSize: number) {
  const [visibleCount, setVisibleCount] = useState(batchSize);
  const [loadingMore, setLoadingMore] = useState(false);
  const sentinelRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    setVisibleCount(batchSize);
  }, [items, batchSize]);

  const hasMore = visibleCount < items.length;

  useEffect(() => {
    const sentinel = sentinelRef.current;
    if (!sentinel || !hasMore) return;

    const observer = new IntersectionObserver(
      ([entry]) => {
        if (!entry.isIntersecting) return;
        setLoadingMore(true);
        // A brief, deliberate pause so the "loading more" state is visible -
        // the data is already in memory, this just paces the reveal.
        window.setTimeout(() => {
          setVisibleCount((count) => Math.min(count + batchSize, items.length));
          setLoadingMore(false);
        }, 200);
      },
      { rootMargin: "300px" }
    );
    observer.observe(sentinel);
    return () => observer.disconnect();
  }, [hasMore, items.length, batchSize]);

  return { visibleItems: items.slice(0, visibleCount), hasMore, loadingMore, sentinelRef };
}

import { useEffect } from "react";
import { useLocation } from "react-router-dom";
import { Hero } from "../components/landing/Hero";
import { BentoFeatures } from "../components/landing/BentoFeatures";
import { AnalyticsShowcase } from "../components/landing/AnalyticsShowcase";
import { NotificationsPreview } from "../components/landing/NotificationsPreview";
import { CtaSection } from "../components/landing/CtaSection";

export function Landing() {
  const location = useLocation();

  // React Router doesn't scroll to a #hash on client-side navigation the way
  // a full page load would - needed for the Navbar/Hero "Analytics" links
  // that send guests to #analytics instead of the protected dashboard.
  useEffect(() => {
    if (!location.hash) return;
    const el = document.querySelector(location.hash);
    el?.scrollIntoView({ behavior: "smooth" });
  }, [location.hash]);

  return (
    <>
      <Hero />
      <BentoFeatures />
      <AnalyticsShowcase />
      <NotificationsPreview />
      <CtaSection />
    </>
  );
}

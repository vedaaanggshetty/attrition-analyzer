import { Hero } from "../components/landing/Hero";
import { FeatureCards } from "../components/landing/FeatureCards";
import { AnalyticsShowcase } from "../components/landing/AnalyticsShowcase";
import { NotificationsPreview } from "../components/landing/NotificationsPreview";
import { CtaSection } from "../components/landing/CtaSection";

export function Landing() {
  return (
    <>
      <Hero />
      <FeatureCards />
      <AnalyticsShowcase />
      <NotificationsPreview />
      <CtaSection />
    </>
  );
}

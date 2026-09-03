import { Link } from "react-router-dom";
import { Button } from "../components/ui/Button";

export function NotFound() {
  return (
    <div className="flex min-h-[70vh] flex-col items-center justify-center px-6 pt-24 text-center">
      <p className="font-display text-8xl font-semibold tracking-tight text-brand-900">404</p>
      <p className="mt-4 text-lg text-neutral-500">This page doesn&apos;t exist.</p>
      <Link to="/" className="mt-8">
        <Button>Back to home</Button>
      </Link>
    </div>
  );
}

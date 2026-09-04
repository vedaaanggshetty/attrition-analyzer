import { cx } from "../../lib/utils";
import { Link } from "react-router-dom";
import type { LinkProps } from "react-router-dom";
import React, { useState, createContext, useContext } from "react";
import { AnimatePresence, motion } from "framer-motion";
import { Menu, X } from "lucide-react";

// Adapted from the shadcn/Aceternity "sidebar" component for a Vite + React
// Router app (no next/link, no next/image - this project doesn't use Next.js).

interface Links {
  label: string;
  href: string;
  icon: React.JSX.Element | React.ReactNode;
  active?: boolean;
}

interface SidebarContextProps {
  open: boolean;
  setOpen: React.Dispatch<React.SetStateAction<boolean>>;
  animate: boolean;
}

const SidebarContext = createContext<SidebarContextProps | undefined>(undefined);

export const useSidebar = () => {
  const context = useContext(SidebarContext);
  if (!context) {
    throw new Error("useSidebar must be used within a SidebarProvider");
  }
  return context;
};

export const SidebarProvider = ({
  children,
  open: openProp,
  setOpen: setOpenProp,
  animate = true,
}: {
  children: React.ReactNode;
  open?: boolean;
  setOpen?: React.Dispatch<React.SetStateAction<boolean>>;
  animate?: boolean;
}) => {
  // Starts expanded (full labels visible) rather than collapsed, so the
  // sidebar never renders in the icon-only state on first paint - it only
  // glides shut once the pointer actually leaves it.
  const [openState, setOpenState] = useState(true);

  const open = openProp !== undefined ? openProp : openState;
  const setOpen = setOpenProp !== undefined ? setOpenProp : setOpenState;

  return <SidebarContext.Provider value={{ open, setOpen, animate }}>{children}</SidebarContext.Provider>;
};

export const Sidebar = ({
  children,
  open,
  setOpen,
  animate,
}: {
  children: React.ReactNode;
  open?: boolean;
  setOpen?: React.Dispatch<React.SetStateAction<boolean>>;
  animate?: boolean;
}) => {
  return (
    <SidebarProvider open={open} setOpen={setOpen} animate={animate}>
      {children}
    </SidebarProvider>
  );
};

export const SidebarBody = (props: React.ComponentProps<typeof motion.div>) => {
  return (
    <>
      <DesktopSidebar {...props} />
      <MobileSidebar {...(props as React.ComponentProps<"div">)} />
    </>
  );
};

export const DesktopSidebar = ({ className, children, ...props }: React.ComponentProps<typeof motion.div>) => {
  const { open, setOpen, animate } = useSidebar();
  return (
    <motion.div
      className={cx(
        "h-full shrink-0 overflow-hidden px-4 py-5 hidden md:flex md:flex-col bg-white border-r border-brand-900/10",
        className
      )}
      animate={{
        width: animate ? (open ? "252px" : "76px") : "252px",
      }}
      initial={false}
      transition={{ duration: 0.3, ease: [0.16, 1, 0.3, 1] }}
      onMouseEnter={() => animate && setOpen(true)}
      onMouseLeave={() => animate && setOpen(false)}
      {...props}
    >
      {children}
    </motion.div>
  );
};

export const MobileSidebar = ({ className, children, ...props }: React.ComponentProps<"div">) => {
  const { open, setOpen } = useSidebar();
  return (
    <div
      className="flex h-14 items-center justify-between border-b border-brand-900/10 bg-white px-4 md:hidden"
      {...props}
    >
      <button
        type="button"
        aria-label="Open sidebar"
        onClick={() => setOpen(!open)}
        className="flex h-9 w-9 items-center justify-center rounded-full text-brand-900 transition-colors hover:bg-brand-50"
      >
        <Menu className="h-5 w-5" />
      </button>
      <AnimatePresence>
        {open && (
          <motion.div
            initial={{ x: "-100%", opacity: 0 }}
            animate={{ x: 0, opacity: 1 }}
            exit={{ x: "-100%", opacity: 0 }}
            transition={{ duration: 0.3, ease: "easeInOut" }}
            className={cx("fixed inset-0 z-[100] flex h-full w-full flex-col justify-between bg-white p-8", className)}
          >
            <button
              type="button"
              aria-label="Close sidebar"
              onClick={() => setOpen(false)}
              className="absolute right-6 top-6 z-50 flex h-9 w-9 items-center justify-center rounded-full text-brand-900 hover:bg-brand-50"
            >
              <X className="h-5 w-5" />
            </button>
            {children}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
};

export const SidebarLink = ({
  link,
  className,
  ...props
}: {
  link: Links;
  className?: string;
  props?: LinkProps;
}) => {
  const { open, animate, setOpen } = useSidebar();
  return (
    <Link
      to={link.href}
      onClick={() => {
        // Only auto-close on mobile (the overlay drawer) - on desktop this
        // shares the same `open` state as the hover-glide rail, and closing
        // it on click would snap the rail shut mid-hover.
        if (typeof window !== "undefined" && window.innerWidth < 768) setOpen(false);
      }}
      className={cx(
        "group/sidebar flex items-center justify-start gap-3 rounded-xl px-3.5 py-3 text-sm font-medium transition-colors",
        link.active
          ? "bg-brand-50 text-brand-900 font-semibold [&_svg]:text-brand-600"
          : "text-neutral-600 hover:bg-brand-50/60 hover:text-brand-900",
        className
      )}
      {...props}
    >
      <span className="flex h-5 w-5 shrink-0 items-center justify-center">{link.icon}</span>
      <motion.span
        animate={{
          display: animate ? (open ? "inline-block" : "none") : "inline-block",
          opacity: animate ? (open ? 1 : 0) : 1,
        }}
        className="!m-0 inline-block whitespace-pre !p-0 transition duration-150 group-hover/sidebar:translate-x-0.5"
      >
        {link.label}
      </motion.span>
    </Link>
  );
};

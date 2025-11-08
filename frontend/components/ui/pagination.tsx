"use client";

import * as React from "react";
import Link from "next/link";
import { ChevronLeft, ChevronRight, MoreHorizontal } from "lucide-react";
import { cva, type VariantProps } from "class-variance-authority";

import { cn } from "@/lib/utils";

const paginationLinkVariants = cva(
  "inline-flex h-9 min-w-[2.25rem] items-center justify-center rounded-md border border-input bg-background px-3 text-sm font-medium transition-colors hover:bg-muted hover:text-foreground focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring disabled:pointer-events-none disabled:opacity-50",
  {
    variants: {
      size: {
        default: "h-9 min-w-9 px-3 text-sm",
        sm: "h-8 min-w-[2rem] px-2 text-xs",
        lg: "h-10 min-w-[2.5rem] px-4 text-base",
      },
      isActive: {
        true: "border-primary text-primary",
        false: "",
      },
    },
    defaultVariants: {
      size: "default",
      isActive: false,
    },
  },
);

const Pagination = ({ className, ...props }: React.ComponentProps<"nav">) => (
  <nav
    role="navigation"
    aria-label="pagination"
    className={cn("mx-auto flex w-full justify-center", className)}
    {...props}
  />
);

const PaginationContent = React.forwardRef<
  HTMLUListElement,
  React.HTMLAttributes<HTMLUListElement>
>(({ className, ...props }, ref) => (
  <ul ref={ref} className={cn("flex items-center gap-1", className)} {...props} />
));
PaginationContent.displayName = "PaginationContent";

const PaginationItem = React.forwardRef<HTMLLIElement, React.LiHTMLAttributes<HTMLLIElement>>(
  ({ className, ...props }, ref) => (
    <li ref={ref} className={cn("inline-flex", className)} {...props} />
  ),
);
PaginationItem.displayName = "PaginationItem";

type PaginationLinkProps = React.ComponentProps<typeof Link> &
  VariantProps<typeof paginationLinkVariants> & {
    isActive?: boolean;
  };

const PaginationLink = ({ className, isActive, size, ...props }: PaginationLinkProps) => (
  <Link
    className={cn(paginationLinkVariants({ isActive, size }), className)}
    aria-current={isActive ? "page" : undefined}
    {...props}
  />
);

const PaginationPrevious = ({ className, ...props }: PaginationLinkProps) => (
  <PaginationLink
    aria-label="前のページ"
    className={cn("gap-1 pl-2.5", className)}
    {...props}
  >
    <ChevronLeft className="h-4 w-4" />
    <span>前へ</span>
  </PaginationLink>
);

const PaginationNext = ({ className, ...props }: PaginationLinkProps) => (
  <PaginationLink aria-label="次のページ" className={cn("gap-1 pr-2.5", className)} {...props}>
    <span>次へ</span>
    <ChevronRight className="h-4 w-4" />
  </PaginationLink>
);

const PaginationEllipsis = ({ className, ...props }: React.HTMLAttributes<HTMLSpanElement>) => (
  <span
    aria-hidden
    className={cn(
      "flex h-9 min-w-[2.25rem] items-center justify-center rounded-md border border-transparent",
      className,
    )}
    {...props}
  >
    <MoreHorizontal className="h-4 w-4" />
    <span className="sr-only">さらにページがあります</span>
  </span>
);

export {
  Pagination,
  PaginationContent,
  PaginationEllipsis,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
};

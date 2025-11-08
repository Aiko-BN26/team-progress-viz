"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { ArrowUpRight } from "lucide-react";

import type { CommitActivity } from "@/app/organizations/[id]/types";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Pagination,
  PaginationContent,
  PaginationItem,
  PaginationLink,
  PaginationNext,
  PaginationPrevious,
} from "@/components/ui/pagination";

const PAGE_SIZE = 5;

const formatDateTime = (
  isoString: string,
  timeZone: string,
  options: Intl.DateTimeFormatOptions = {
    dateStyle: "medium",
    timeStyle: "short",
  },
) =>
  new Intl.DateTimeFormat("ja-JP", {
    ...options,
    timeZone,
  }).format(new Date(isoString));

type RecentCommitsCardProps = {
  commits: CommitActivity[];
  timezone: string;
};

export function RecentCommitsCard({ commits, timezone }: RecentCommitsCardProps) {
  const [currentPage, setCurrentPage] = useState(1);
  const pageCount = Math.max(1, Math.ceil(commits.length / PAGE_SIZE));
  const safeCurrentPage = Math.min(currentPage, pageCount);

  const paginatedCommits = useMemo(() => {
    const start = (safeCurrentPage - 1) * PAGE_SIZE;
    return commits.slice(start, start + PAGE_SIZE);
  }, [commits, safeCurrentPage]);

  const handleNavigate = (
    event: React.MouseEvent<HTMLAnchorElement>,
    targetPage: number,
  ) => {
    event.preventDefault();
    if (targetPage < 1 || targetPage > pageCount || targetPage === safeCurrentPage) {
      return;
    }
    setCurrentPage(targetPage);
  };

  const isFirstPage = safeCurrentPage === 1;
  const isLastPage = safeCurrentPage === pageCount;

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="text-base">最近のコミット/PR</CardTitle>
        <CardDescription>GitHubの最新活動から概要のみを表示しています</CardDescription>
      </CardHeader>
      <CardContent>
        {commits.length === 0 ? (
          <p className="text-sm text-muted-foreground">表示できる活動がありません。</p>
        ) : (
          <>
            <ul className="space-y-4">
              {paginatedCommits.map((commit) => (
                <li key={commit.id} className="rounded-lg border p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2 text-sm">
                    <p className="font-semibold">{commit.title}</p>
                    <span className="text-xs text-muted-foreground">
                      {formatDateTime(commit.committedAt, timezone)}
                    </span>
                  </div>
                  <p className="text-xs text-muted-foreground">
                    {commit.repository} / {commit.author}
                  </p>
                  <Link
                    href={commit.url}
                    className="mt-2 inline-flex items-center gap-1 text-sm text-primary underline-offset-4 hover:underline"
                  >
                    詳細を見る
                    <ArrowUpRight className="h-4 w-4" />
                  </Link>
                </li>
              ))}
            </ul>
            {pageCount > 1 ? (
              <Pagination className="mt-6">
                <PaginationContent>
                  <PaginationItem>
                    <PaginationPrevious
                      href="#"
                      onClick={(event) => handleNavigate(event, safeCurrentPage - 1)}
                      className={isFirstPage ? "pointer-events-none opacity-50" : undefined}
                      aria-disabled={isFirstPage}
                    />
                  </PaginationItem>
                  {Array.from({ length: pageCount }, (_, index) => {
                    const pageNumber = index + 1;
                    return (
                      <PaginationItem key={`page-${pageNumber}`}>
                        <PaginationLink
                          href="#"
                          onClick={(event) => handleNavigate(event, pageNumber)}
                          isActive={pageNumber === safeCurrentPage}
                        >
                          {pageNumber}
                        </PaginationLink>
                      </PaginationItem>
                    );
                  })}
                  <PaginationItem>
                    <PaginationNext
                      href="#"
                      onClick={(event) => handleNavigate(event, safeCurrentPage + 1)}
                      className={isLastPage ? "pointer-events-none opacity-50" : undefined}
                      aria-disabled={isLastPage}
                    />
                  </PaginationItem>
                </PaginationContent>
              </Pagination>
            ) : null}
          </>
        )}
      </CardContent>
    </Card>
  );
}

"use client";

import Image from "next/image";
import Link from "next/link";
import { useMemo, useState } from "react";
import { Filter, Search } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";
import type { MemberStatus } from "../../app/organizations/[id]/types";

type Props = {
  members: MemberStatus[];
  timezone: string;
};

const FILTERS = [
  { id: "all", label: "全員" },
  { id: "pending", label: "未提出" },
  { id: "完了", label: "完了" },
  { id: "集中", label: "集中" },
  { id: "休み", label: "休み" },
  { id: "ちょっと", label: "ちょっと" },
];

const STATUS_STYLES: Record<MemberStatus["status"], string> = {
  完了: "bg-emerald-100 text-emerald-700",
  集中: "bg-sky-100 text-sky-700",
  休み: "bg-amber-100 text-amber-700",
  ちょっと: "bg-violet-100 text-violet-700",
};

export function MemberStatusBoard({ members, timezone }: Props) {
  const [filter, setFilter] = useState<string>("all");
  const [query, setQuery] = useState("");

  const filteredMembers = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();
    return members.filter((member) => {
      const matchQuery = normalizedQuery
        ? member.displayName.toLowerCase().includes(normalizedQuery)
        : true;
      const matchFilter =
        filter === "all"
          ? true
          : filter === "pending"
          ? member.pending
          : member.pending
          ? false
          : member.status === filter;
      return matchQuery && matchFilter;
    });
  }, [filter, members, query]);

  const formatDateTime = (iso: string | null) => {
    if (!iso) return "--";
    return new Intl.DateTimeFormat("ja-JP", {
      dateStyle: "short",
      timeStyle: "short",
      timeZone: timezone,
    }).format(new Date(iso));
  };

  return (
    <Card className="h-full">
      <CardHeader className="gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div>
          <CardTitle className="text-base">メンバーステータス</CardTitle>
          <p className="text-sm text-muted-foreground">
            状態でフィルタリングして遅れを素早く確認
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {FILTERS.map((option) => (
            <Button
              key={option.id}
              type="button"
              size="sm"
              variant={filter === option.id ? "default" : "outline"}
              className="gap-1 px-2"
              onClick={() => setFilter(option.id)}
            >
              <Filter className="h-3.5 w-3.5" />
              {option.label}
            </Button>
          ))}
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center gap-2 rounded-md border px-3 py-2">
          <Search className="h-4 w-4 text-muted-foreground" />
          <input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="名前で検索"
            className="flex-1 bg-transparent text-sm outline-none"
          />
        </div>
        <div className="space-y-3">
          {filteredMembers.length === 0 ? (
            <p className="text-sm text-muted-foreground">
              該当するメンバーがいません。
            </p>
          ) : (
            filteredMembers.map((member) => {
              const statusLabel = member.pending ? "未提出" : member.status;
              const statusStyle = member.pending
                ? "bg-slate-100 text-slate-700"
                : STATUS_STYLES[member.status];
              return (
                <div
                  key={member.memberId}
                  className={cn(
                    "flex flex-col gap-3 rounded-lg border p-3 sm:flex-row sm:items-center",
                    member.pending ? "bg-slate-500 bg-opacity-10 opacity-70" : "",
                  )}
                >
                  <div className="flex items-center gap-3">
                    <div className="h-12 w-12 overflow-hidden rounded-full bg-muted">
                      <Image
                        src={member.avatarUrl}
                        alt={`${member.displayName} avatar`}
                        width={48}
                        height={48}
                        className="h-full w-full object-cover"
                      />
                    </div>
                    <div>
                      <p className="font-medium leading-tight">{member.displayName}</p>
                      <p className="text-xs text-muted-foreground">
                        最終提出: {formatDateTime(member.lastSubmittedAt)}
                      </p>
                    </div>
                  </div>
                  <div className="flex flex-1 flex-col gap-2 text-sm sm:flex-row sm:items-center sm:justify-between">
                    <span
                      className={cn(
                        "w-fit rounded-full px-2 py-1 text-xs font-medium",
                        statusStyle,
                      )}
                    >
                      {statusLabel}
                    </span>
                    <p className="text-muted-foreground">
                      {member.statusMessage || "コメントなし"}
                    </p>
                    <div className="flex flex-wrap items-center gap-3 text-sm text-muted-foreground">
                      <div className="flex flex-col gap-0 leading-tight">
                        <div className="grid w-48 grid-cols-3 gap-x-4 text-sm justify-items-center">
                          <span>コミット</span>
                          <span>稼働</span>
                          <span>連続</span>
                        </div>
                        <div className="grid w-48 grid-cols-3 gap-x-4 text-base font-semibold text-foreground justify-items-center">
                          <span>{member.commitCount}</span>
                          <span>{member.capacityHours ?? "--"}h</span>
                          <span>{member.streakDays}日</span>
                        </div>
                      </div>
                      {member.latestPrUrl ? (
                        <Link
                          href={member.latestPrUrl}
                          className="text-primary underline-offset-4 hover:underline"
                        >
                          最新PRを開く
                        </Link>
                      ) : (
                        <span>PRなし</span>
                      )}
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </CardContent>
    </Card>
  );
}

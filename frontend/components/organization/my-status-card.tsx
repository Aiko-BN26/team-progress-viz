"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { CalendarClock, PenSquare } from "lucide-react";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import type {
  MemberStatusState,
  PersonalStatus,
} from "../../app/organizations/[id]/types";

const STATUS_OPTIONS = [
  { value: "完了", label: "完了" },
  { value: "集中", label: "集中" },
  { value: "休み", label: "休み" },
  { value: "ちょっと", label: "ちょっと" },
];

type Props = {
  organizationId: string;
  timezone: string;
  personalStatus: PersonalStatus;
};

type ApiPersonalStatus = {
  submitted?: boolean;
  status?: string | null;
  statusMessage?: string | null;
  lastSubmittedAt?: string | null;
  commitCount?: number | null;
  capacityHours?: number | null;
  streakDays?: number | null;
  latestPrUrl?: string | null;
};

type ApiStatusUpdateResponse = {
  personalStatus?: ApiPersonalStatus | null;
  summary?: {
    activeToday?: number | null;
    pendingStatusCount?: number | null;
  } | null;
};

export function MyStatusCard({ organizationId, timezone, personalStatus }: Props) {
  const router = useRouter();
  const [currentStatus, setCurrentStatus] = useState(personalStatus);
  const [open, setOpen] = useState(!personalStatus.submitted);
  const [formStatus, setFormStatus] = useState<MemberStatusState>(
    (personalStatus.status ?? "集中") as MemberStatusState,
  );
  const [formMessage, setFormMessage] = useState(personalStatus.statusMessage ?? "");
  const [formCapacity, setFormCapacity] = useState(personalStatus.capacityHours ?? 3);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    setCurrentStatus(personalStatus);
  }, [personalStatus]);

  useEffect(() => {
    setFormStatus((currentStatus.status ?? "集中") as MemberStatusState);
    setFormMessage(currentStatus.statusMessage ?? "");
    setFormCapacity(currentStatus.capacityHours ?? 3);
  }, [currentStatus]);

  const formattedLastSubmitted = useMemo(() => {
    if (!currentStatus.lastSubmittedAt) return "--";
    return new Intl.DateTimeFormat("ja-JP", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: timezone,
    }).format(new Date(currentStatus.lastSubmittedAt));
  }, [currentStatus.lastSubmittedAt, timezone]);

  const showIncomplete = !currentStatus.submitted;
  const displayStatus = currentStatus.submitted
    ? currentStatus.status ?? "集中"
    : formStatus;
  const displayMessage = currentStatus.submitted
    ? currentStatus.statusMessage ?? "コメントなし"
    : formMessage || currentStatus.statusMessage || "コメントなし";
  const displayCapacity = currentStatus.submitted
    ? currentStatus.capacityHours ?? "--"
    : formCapacity ?? currentStatus.capacityHours ?? "--";

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitError(null);
    setIsSubmitting(true);
    try {
      const availableMinutes =
        typeof formCapacity === "number" && !Number.isNaN(formCapacity)
          ? Math.max(Math.round(formCapacity * 60), 0)
          : null;

      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/api/organizations/${organizationId}/statuses`,
        {
          method: "POST",
          credentials: "include",
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
          body: JSON.stringify({
            status: formStatus,
            statusMessage: formMessage,
            capacityHours: formCapacity,
            availableMinutes,
          }),
        },
      );

      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(errorText || "Failed to submit status");
      }

      const payload = (await response.json()) as ApiStatusUpdateResponse;
      const nowIso = new Date().toISOString();

      if (payload.personalStatus) {
        setCurrentStatus(mapApiPersonalStatus(payload.personalStatus));
      } else {
        setCurrentStatus((prev) => ({
          ...prev,
          submitted: true,
          status: formStatus,
          statusMessage: formMessage || null,
          lastSubmittedAt: nowIso,
          capacityHours: formCapacity ?? prev.capacityHours ?? null,
        }));
      }
      setOpen(false);
      router.refresh();
    } catch (error) {
      console.error("[MyStatusCard] failed to submit status", error);
      setSubmitError("保存に失敗しました。時間をおいて再度お試しください。");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card className="h-full">
      <CardHeader>
        <CardTitle className="flex items-center justify-between text-base">
          <span>今日のあなたのステータス</span>
          <Dialog open={open} onOpenChange={setOpen}>
            <DialogTrigger asChild>
              <Button variant={showIncomplete ? "default" : "outline"} size="sm">
                <PenSquare className="h-4 w-4" />
                {showIncomplete ? "ステータスを入力" : "ステータスを編集"}
              </Button>
            </DialogTrigger>
            <DialogContent>
              <DialogHeader>
                <DialogTitle>今日の作業予定を共有</DialogTitle>
                <DialogDescription>
                  入力内容はリアルタイムで保存され、チームと共有されます。
                </DialogDescription>
              </DialogHeader>
              <form className="space-y-4" onSubmit={handleSubmit}>
                <div className="space-y-2">
                  <label className="text-sm font-medium">ステータス</label>
                  <select
                    value={formStatus}
                    onChange={(event) =>
                      setFormStatus(event.target.value as MemberStatusState)
                    }
                    className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    disabled={isSubmitting}
                  >
                    {STATUS_OPTIONS.map((option) => (
                      <option key={option.value} value={option.value}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">コメント</label>
                  <textarea
                    value={formMessage}
                    onChange={(event) => setFormMessage(event.target.value)}
                    rows={3}
                    className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    placeholder="今日の進め方やブロッカーを共有しましょう"
                    disabled={isSubmitting}
                  />
                </div>
                <div className="space-y-2">
                  <label className="text-sm font-medium">稼働予定(時間)</label>
                  <input
                    type="number"
                    min={0}
                    max={12}
                    value={formCapacity}
                    onChange={(event) => {
                      const nextValue = Number(event.target.value);
                      setFormCapacity(Number.isNaN(nextValue) ? 0 : nextValue);
                    }}
                    className="w-full rounded-md border border-input bg-transparent px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-1 focus-visible:ring-ring"
                    disabled={isSubmitting}
                  />
                </div>
                {submitError && (
                  <p className="text-sm text-destructive">{submitError}</p>
                )}
                <DialogFooter>
                  <Button
                    type="button"
                    variant="outline"
                    onClick={() => setOpen(false)}
                    disabled={isSubmitting}
                  >
                    キャンセル
                  </Button>
                  <Button type="submit" disabled={isSubmitting}>
                    {isSubmitting ? "保存中..." : "保存"}
                  </Button>
                </DialogFooter>
              </form>
            </DialogContent>
          </Dialog>
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="rounded-lg border p-4">
          {showIncomplete ? (
            <p className="text-sm text-muted-foreground">
              まだ今日のステータスが入力されていません。モーダルから予定を共有しましょう。
            </p>
          ) : (
            <div className="space-y-2 text-sm">
              <p className="font-medium">
                ステータス: {
                  STATUS_OPTIONS.find((option) => option.value === displayStatus)?.label ?? displayStatus
                }
              </p>
              <p className="text-muted-foreground">{displayMessage}</p>
              <p className="text-muted-foreground">稼働予定: {displayCapacity}時間</p>
            </div>
          )}
        </div>
        <div className="flex flex-wrap items-center gap-3 text-xs text-muted-foreground">
          <span>コミット {currentStatus.commitCount}</span>
          <span>稼働 {currentStatus.capacityHours ?? "--"}h</span>
          <span>連続 {currentStatus.streakDays}日</span>
          {currentStatus.latestPrUrl ? (
            <Link
              href={currentStatus.latestPrUrl}
              className="text-primary underline-offset-4 hover:underline"
            >
              最新PRを開く
            </Link>
          ) : (
            <span>PRなし</span>
          )}
        </div>
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <CalendarClock className="h-4 w-4" />
          最終入力: {formattedLastSubmitted}
        </div>
      </CardContent>
    </Card>
  );
}

function mapApiPersonalStatus(api: ApiPersonalStatus): PersonalStatus {
  console.log("api.status:", api.status);
  return {
    submitted: Boolean(api.submitted),
    status: api.status as MemberStatusState | null,
    statusMessage: api.statusMessage ?? null,
    lastSubmittedAt: api.lastSubmittedAt ?? null,
    commitCount: api.commitCount ?? 0,
    capacityHours: api.capacityHours ?? null,
    streakDays: api.streakDays ?? 0,
    latestPrUrl: api.latestPrUrl ?? null,
  };
}


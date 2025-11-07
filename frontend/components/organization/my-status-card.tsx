"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
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
  timezone: string;
  personalStatus: PersonalStatus;
};

export function MyStatusCard({ timezone, personalStatus }: Props) {
  const shouldAutoOpen = !personalStatus.submitted;
  const [open, setOpen] = useState(shouldAutoOpen);
  const [formStatus, setFormStatus] = useState<MemberStatusState>(
    (personalStatus.status ?? "集中") as MemberStatusState,
  );
  const [formMessage, setFormMessage] = useState(personalStatus.statusMessage ?? "");
  const [formCapacity, setFormCapacity] = useState(
    personalStatus.capacityHours ?? 3,
  );
  const [savedAt, setSavedAt] = useState<string | null>(null);

  const formattedLastSubmitted = useMemo(() => {
    if (!personalStatus.lastSubmittedAt) return "--";
    return new Intl.DateTimeFormat("ja-JP", {
      dateStyle: "medium",
      timeStyle: "short",
      timeZone: timezone,
    }).format(new Date(personalStatus.lastSubmittedAt));
  }, [personalStatus.lastSubmittedAt, timezone]);

  const showIncomplete = !personalStatus.submitted;
  const displayStatus = personalStatus.submitted
    ? personalStatus.status ?? "集中"
    : formStatus;
  const displayMessage = personalStatus.submitted
    ? personalStatus.statusMessage ?? "コメントなし"
    : formMessage || personalStatus.statusMessage || "コメントなし";
  const displayCapacity = personalStatus.submitted
    ? personalStatus.capacityHours ?? "--"
    : formCapacity ?? personalStatus.capacityHours ?? "--";

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSavedAt(
      new Intl.DateTimeFormat("ja-JP", {
        dateStyle: "medium",
        timeStyle: "short",
        timeZone: timezone,
      }).format(new Date()),
    );
    setOpen(false);
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
                  入力内容はまだモック状態で保存されません。操作感を確認できます。
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
                  />
                </div>
                <DialogFooter>
                  <Button type="button" variant="outline" onClick={() => setOpen(false)}>
                    キャンセル
                  </Button>
                  <Button type="submit">モック保存</Button>
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
          <span>コミット {personalStatus.commitCount}</span>
          <span>稼働 {personalStatus.capacityHours ?? "--"}h</span>
          <span>連続 {personalStatus.streakDays}日</span>
          {personalStatus.latestPrUrl && (
            <Link
              href={personalStatus.latestPrUrl}
              className="text-primary underline-offset-4 hover:underline"
            >
              最新PRを開く
            </Link>
          )}
        </div>
        <div className="flex items-center gap-2 text-xs text-muted-foreground">
          <CalendarClock className="h-4 w-4" />
          最終入力: {formattedLastSubmitted}
        </div>
        {savedAt && (
          <p className="text-xs text-emerald-600">モック保存完了: {savedAt}</p>
        )}
      </CardContent>
    </Card>
  );
}

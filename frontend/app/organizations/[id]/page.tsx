import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import {
  AlertCircle,
  ArrowLeft,
  ArrowUpRight,
  CalendarDays,
  Loader2,
  Users,
} from "lucide-react";

import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";

import { ActivityChart } from "@/components/organization/activity-chart";
import { MemberStatusBoard } from "@/components/organization/member-status-board";
import { MyStatusCard } from "@/components/organization/my-status-card";
import { loadOrganizationViewData } from "./data";
import type { CommitActivity, OrganizationViewData } from "./types";

type PageProps = {
  params: Promise<{ id: string }>;
};

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

const StatCard = ({
  label,
  value,
  subLabel,
  icon: Icon,
  highlight,
}: {
  label: string;
  value: string;
  subLabel?: string;
  icon: React.ComponentType<React.SVGProps<SVGSVGElement>>;
  highlight?: string;
}) => (
  <Card>
    <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
      <CardTitle className="text-sm font-medium text-muted-foreground">
        {label}
      </CardTitle>
      <Icon className="h-4 w-4 text-muted-foreground" />
    </CardHeader>
    <CardContent>
      <div className="text-2xl font-semibold">{value}</div>
      {subLabel && <p className="text-xs text-muted-foreground">{subLabel}</p>}
      {highlight && <p className="text-xs text-emerald-600">{highlight}</p>}
    </CardContent>
  </Card>
);

const RecentCommitsCard = ({
  commits,
  timezone,
}: {
  commits: CommitActivity[];
  timezone: string;
}) => (
  <Card className="h-full">
    <CardHeader>
      <CardTitle className="text-base">最近のコミット/PR</CardTitle>
      <CardDescription>GitHubの最新活動から概要のみを表示しています</CardDescription>
    </CardHeader>
    <CardContent>
      {commits.length === 0 ? (
        <p className="text-sm text-muted-foreground">表示できる活動がありません。</p>
      ) : (
        <ul className="space-y-4">
          {commits.map((commit) => (
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
      )}
    </CardContent>
  </Card>
);

async function getPageData(organizationId: string): Promise<OrganizationViewData | null> {
  return loadOrganizationViewData(organizationId);
}

export default async function OrganizationDetailPage({ params }: PageProps) {
  const { id } = await params;
  const data = await getPageData(id);

  if (!data) {
    notFound();
  }

  const {
    detail,
    members,
    activity,
    commits,
    personalStatus,
  } = data;

  const activityRate = detail.memberCount
    ? Math.min(100, Math.round((detail.activeToday / detail.memberCount) * 100))
    : 0;

  return (
    <main className="space-y-8 px-6 py-7">
      <div>
        <Link
          href="/organizations"
          className="inline-flex items-center gap-2 rounded-full border px-3 py-1 text-sm font-medium text-muted-foreground transition hover:bg-muted"
        >
          <ArrowLeft className="h-4 w-4" />
          <span>組織一覧へ戻る</span>
        </Link>
      </div>
      <section className="flex flex-col gap-4 rounded-2xl border bg-card p-6 shadow-sm md:flex-row md:items-center md:justify-between">
        <div className="flex items-start gap-4">
          <div className="h-16 w-16 overflow-hidden rounded-full border bg-muted">
            <Image
              src={detail.avatarUrl}
              alt={`${detail.name} avatar`}
              width={64}
              height={64}
              className="h-full w-full object-cover"
            />
          </div>
          <div>
            <h1 className="text-2xl font-semibold">{detail.name}</h1>
            <p className="text-sm text-muted-foreground">{detail.description}</p>
            <p className="mt-2 text-xs text-muted-foreground">
              最終更新: {formatDateTime(detail.lastActivity, detail.timezone)}
            </p>
          </div>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="メンバー"
          value={`${detail.memberCount}人`}
          subLabel="登録済み"
          icon={Users}
        />
        <StatCard
          label="今日の報告率"
          value={`${activityRate}%`}
          subLabel={`${detail.activeToday} / ${detail.memberCount}`}
          icon={CalendarDays}
          highlight={activityRate >= 80 ? "順調" : undefined}
        />
        <StatCard
          label="未入力"
          value={`${detail.pendingStatusCount}名`}
          subLabel="フォローが必要"
          icon={AlertCircle}
        />
        <StatCard
          label="継続日数"
          value={detail.streakDays != null ? `${detail.streakDays}日` : "--"}
          subLabel={detail.streakDays != null ? "連続で報告中" : "記録なし"}
          icon={Loader2}
        />
      </section>

      <section className="grid gap-6 lg:grid-cols-[2fr,1fr]">
        <MemberStatusBoard timezone={detail.timezone} members={members} />
        <MyStatusCard
          organizationId={detail.id}
          timezone={detail.timezone}
          personalStatus={personalStatus}
        />
      </section>

      <section className="grid gap-6 lg:grid-cols-2">
        <ActivityChart timezone={detail.timezone} activity={activity} />
        <RecentCommitsCard timezone={detail.timezone} commits={commits} />
      </section>
    </main>
  );
}

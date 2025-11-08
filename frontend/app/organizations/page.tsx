import Image from "next/image";
import Link from "next/link";

import {
  Button,
  buttonVariants,
} from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import { cn } from "@/lib/utils";

type OrganizationSummary = {
  id: string;
  name: string;
  description: string;
  memberCount: number;
  activeToday: number;
  pendingStatusCount: number;
  lastActivity: string;
  avatarUrl: string;
};

const mockOrganizations: OrganizationSummary[] = [
  {
    id: "astro-lab",
    name: "Astro Lab",
    description: "週1で集まる社会人LTコミュニティ",
    memberCount: 6,
    activeToday: 4,
    pendingStatusCount: 1,
    lastActivity: "2025-11-05T14:25:00+09:00",
    avatarUrl: "https://avatars.githubusercontent.com/u/9919?s=64&v=4",
  },
  {
    id: "campus-dev",
    name: "Campus Dev",
    description: "学内ハッカソン向け実験チーム",
    memberCount: 9,
    activeToday: 7,
    pendingStatusCount: 0,
    lastActivity: "2025-11-04T23:40:00+09:00",
    avatarUrl: "https://avatars.githubusercontent.com/u/1342004?s=64&v=4",
  },
  {
    id: "fox-systems",
    name: "Fox Systems",
    description: "受託案件の週次レビュー用",
    memberCount: 4,
    activeToday: 2,
    pendingStatusCount: 2,
    lastActivity: "2025-11-06T08:10:00+09:00",
    avatarUrl: "https://avatars.githubusercontent.com/u/7569241?s=64&v=4",
  },
];

const formatActivityTime = (isoDate: string) =>
  new Intl.DateTimeFormat("ja-JP", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(new Date(isoDate));

const OrganizationCard = ({
  organization,
}: {
  organization: OrganizationSummary;
}) => (
  <Link href={`/organizations/${organization.id}`} className="block">
    <Card className="h-full transition hover:border-primary/50 hover:shadow-lg">
      <CardHeader className="flex-row items-center gap-4">
        <div className="flex h-12 w-12 items-center justify-center overflow-hidden rounded-full bg-muted">
          <Image
            src={organization.avatarUrl}
            alt={`${organization.name} avatar`}
            width={48}
            height={48}
            className="h-full w-full object-cover"
          />
        </div>
        <div className="space-y-1">
          <CardTitle>{organization.name}</CardTitle>
          <CardDescription>{organization.description}</CardDescription>
        </div>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">メンバー</span>
          <span className="font-medium">{organization.memberCount}人</span>
        </div>
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">今日の報告</span>
          <span className="font-medium">
            {organization.activeToday} / {organization.memberCount}
          </span>
        </div>
        <div className="flex items-center justify-between text-sm">
          <span className="text-muted-foreground">未入力</span>
          <span className="font-medium text-amber-600">
            {organization.pendingStatusCount}名
          </span>
        </div>
        <p className="text-xs text-muted-foreground">
          最終更新: {formatActivityTime(organization.lastActivity)}
        </p>
      </CardContent>
    </Card>
  </Link>
);

export default function OrganizationsPage() {
  const hasOrganizations = mockOrganizations.length > 0;

  return (
    <main className="space-y-8 px-6 py-10">
      <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="space-y-1">
          <h1 className="text-2xl font-semibold tracking-tight">
            Organization一覧
          </h1>
          <p className="text-sm text-muted-foreground">
            所属中のチームを確認し、報告状況をチェックできます。
          </p>
        </div>
        <div className="flex flex-wrap gap-3">
          <Button asChild>
            <Link href="/enroll-organizations">Organizationを追加</Link>
          </Button>
          <Button variant="outline">再読み込み</Button>
        </div>
      </header>

      {hasOrganizations ? (
        <section className="grid gap-6 sm:grid-cols-2 xl:grid-cols-3">
          {mockOrganizations.map((org) => (
            <OrganizationCard key={org.id} organization={org} />
          ))}
        </section>
      ) : (
        <section className="flex flex-col items-center justify-center rounded-xl border border-dashed p-12 text-center">
          <p className="text-lg font-semibold">
            まだOrganizationが登録されていません
          </p>
          <p className="mt-2 text-sm text-muted-foreground">
            GitHub上のorganizationを連携して、チームの進捗を可視化しましょう。
          </p>
          <Link
            href="/enroll-organizations"
            className={cn(buttonVariants({ className: "mt-6" }))}
          >
            Organizationを登録する
          </Link>
        </section>
      )}
    </main>
  );
}

import type { OrganizationViewData } from "./types";

const today = "2025-11-06";

const MOCK_DATA: Record<string, OrganizationViewData> = {
  "astro-lab": {
    detail: {
      id: "astro-lab",
      name: "Astro Lab",
      description: "週1で集まる社会人LTコミュニティ",
      avatarUrl: "https://avatars.githubusercontent.com/u/9919?s=96&v=4",
      timezone: "Asia/Tokyo",
      memberCount: 6,
      activeToday: 4,
      pendingStatusCount: 1,
      streakDays: 5,
      lastActivity: "2025-11-05T14:25:00+09:00",
      publicShareUrl: "https://example.com/share/astro-lab",
    },
    members: [
      {
        memberId: "u1",
        displayName: "Keita",
        avatarUrl: "https://avatars.githubusercontent.com/u/9919?v=4",
        status: "完了",
        statusMessage: "API設計を完了。夕方にレビューします。",
        lastSubmittedAt: "2025-11-06T08:10:00+09:00",
        commitCount: 3,
        capacityHours: 5,
        streakDays: 5,
        latestPrUrl: "https://github.com/org/repo/pull/12",
      },
      {
        memberId: "u2",
        displayName: "Mai",
        avatarUrl: "https://avatars.githubusercontent.com/u/583231?v=4",
        status: "集中",
        statusMessage: "午後はリファクタリングに集中",
        lastSubmittedAt: "2025-11-06T09:05:00+09:00",
        commitCount: 2,
        capacityHours: 6,
        streakDays: 4,
        latestPrUrl: "https://github.com/org/repo/pull/15",
      },
      {
        memberId: "u3",
        displayName: "Naoki",
        avatarUrl: "https://avatars.githubusercontent.com/u/139426?v=4",
        status: "休み",
        statusMessage: null,
        lastSubmittedAt: null,
        commitCount: 0,
        capacityHours: null,
        streakDays: 0,
        latestPrUrl: null,
      },
      {
        memberId: "u4",
        displayName: "Arisa",
        avatarUrl: "https://avatars.githubusercontent.com/u/3430433?v=4",
        status: "ちょっと",
        statusMessage: "午前の顧客MTGで遅れています",
        lastSubmittedAt: "2025-11-06T06:55:00+09:00",
        commitCount: 1,
        capacityHours: 2,
        streakDays: 1,
        latestPrUrl: null,
      },
      {
        memberId: "u5",
        displayName: "Jo",
        avatarUrl: "https://avatars.githubusercontent.com/u/73425668?v=4",
        status: "完了",
        statusMessage: "週次レポートアップデート完了",
        lastSubmittedAt: "2025-11-05T22:10:00+09:00",
        commitCount: 4,
        capacityHours: 5,
        streakDays: 7,
        latestPrUrl: "https://github.com/org/repo/pull/14",
      },
      {
        memberId: "u6",
        displayName: "Sara",
        avatarUrl: "https://avatars.githubusercontent.com/u/64288621?v=4",
        status: "集中",
        statusMessage: "グラフUI改善を実装中",
        lastSubmittedAt: "2025-11-06T07:40:00+09:00",
        commitCount: 2,
        capacityHours: 4,
        streakDays: 3,
        latestPrUrl: null,
      },
    ],
    activity: {
      daily: Array.from({ length: 14 }).map((_, index) => {
        const date = new Date(`${today}T00:00:00`);
        date.setDate(date.getDate() - index);
        return {
          bucketStart: date.toISOString().slice(0, 10),
          activeMembers: Math.max(2, 6 - (index % 4)),
          totalCommits: 10 + (index % 5) * 3,
          avgStatusScore: 3.2 + (index % 3) * 0.3,
        };
      }).reverse(),
      weekly: Array.from({ length: 6 }).map((_, index) => {
        const date = new Date(`${today}T00:00:00`);
        date.setDate(date.getDate() - index * 7);
        return {
          bucketStart: date.toISOString().slice(0, 10),
          activeMembers: 4 + (index % 2),
          totalCommits: 40 + index * 5,
          avgStatusScore: 3.5 + index * 0.1,
        };
      }).reverse(),
    },
    commits: [
      {
        id: "c1",
        title: "feat: add status modal",
        repository: "astro-lab/frontend",
        author: "Keita",
        committedAt: "2025-11-06T08:30:00+09:00",
        url: "https://github.com/org/repo/commit/1",
      },
      {
        id: "c2",
        title: "chore: update workflows",
        repository: "astro-lab/devops",
        author: "Sara",
        committedAt: "2025-11-05T23:10:00+09:00",
        url: "https://github.com/org/repo/commit/2",
      },
      {
        id: "c3",
        title: "fix: adjust dashboard spacing",
        repository: "astro-lab/frontend",
        author: "Mai",
        committedAt: "2025-11-05T19:50:00+09:00",
        url: "https://github.com/org/repo/commit/3",
      },
    ],
    personalStatus: {
      submitted: false,
      status: null,
      statusMessage: null,
      lastSubmittedAt: "2025-11-05T20:30:00+09:00",
      commitCount: 0,
      capacityHours: null,
      streakDays: 1,
      latestPrUrl: null,
    },
  },
  "campus-dev": {
    detail: {
      id: "campus-dev",
      name: "Campus Dev",
      description: "学内ハッカソン向け実験チーム",
      avatarUrl: "https://avatars.githubusercontent.com/u/1342004?s=96&v=4",
      timezone: "Asia/Tokyo",
      memberCount: 9,
      activeToday: 7,
      pendingStatusCount: 0,
      streakDays: 12,
      lastActivity: "2025-11-04T23:40:00+09:00",
      publicShareUrl: "https://example.com/share/campus-dev",
    },
    members: [],
    activity: {
      daily: [],
      weekly: [],
    },
    commits: [],
    personalStatus: {
      submitted: true,
      status: "完了",
      statusMessage: "審査向け資料のブラッシュアップ",
      lastSubmittedAt: "2025-11-06T07:15:00+09:00",
      commitCount: 6,
      capacityHours: 5,
      streakDays: 9,
      latestPrUrl: "https://github.com/org/repo/pull/22",
    },
  },
  "fox-systems": {
    detail: {
      id: "fox-systems",
      name: "Fox Systems",
      description: "受託案件の週次レビュー用",
      avatarUrl: "https://avatars.githubusercontent.com/u/7569241?s=96&v=4",
      timezone: "Asia/Tokyo",
      memberCount: 4,
      activeToday: 2,
      pendingStatusCount: 2,
      streakDays: 2,
      lastActivity: "2025-11-06T08:10:00+09:00",
      publicShareUrl: "https://example.com/share/fox-systems",
    },
    members: [],
    activity: {
      daily: [],
      weekly: [],
    },
    commits: [],
    personalStatus: {
      submitted: false,
      status: null,
      statusMessage: null,
      lastSubmittedAt: null,
      commitCount: 0,
      capacityHours: null,
      streakDays: 0,
      latestPrUrl: null,
    },
  },
};

type MockOptions = {
  simulateDelay?: boolean;
};

export async function fetchMockOrganizationViewData(
  organizationId: string,
  options?: MockOptions,
): Promise<OrganizationViewData | null> {
  const shouldDelay = options?.simulateDelay ?? true;
  if (shouldDelay) {
    await new Promise((resolve) => setTimeout(resolve, 300));
  }
  return MOCK_DATA[organizationId] ?? null;
}

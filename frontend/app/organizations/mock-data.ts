import type { OrganizationListItem } from "./types";

export const mockOrganizations: OrganizationListItem[] = [
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

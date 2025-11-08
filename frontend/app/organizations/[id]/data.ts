import { randomUUID } from "node:crypto";

import type {
  OrganizationViewData,
  MemberStatus,
  PersonalStatus,
  OrganizationActivity,
  CommitActivity,
} from "./types";
import { backendFetchJson } from "@/lib/server-api";

const DEFAULT_TIMEZONE = process.env.DEFAULT_ORGANIZATION_TIMEZONE ?? "Asia/Tokyo";
const DEFAULT_AVATAR = "https://avatars.githubusercontent.com/u/0?v=4";
const DEFAULT_SHARE_URL = "";
const ACTIVITY_DAYS_LOOKBACK = 14;
const ACTIVITY_WEEKS_LOOKBACK = 8;
const COMMIT_FEED_LIMIT = 60;

const EMPTY_ACTIVITY: OrganizationActivity = { daily: [], weekly: [] };

type ApiOrganizationDetailResponse = {
  organization?: {
    id: number;
    login: string;
    name?: string | null;
    description?: string | null;
    avatarUrl?: string | null;
    htmlUrl?: string | null;
    defaultLinkUrl?: string | null;
  };
  members?: Array<{
    userId: number | null;
  }>;
  recentCommits?: Array<{
    id?: number | null;
    sha?: string | null;
    message?: string | null;
    repositoryFullName?: string | null;
    authorName?: string | null;
    committerName?: string | null;
    committedAt?: string | null;
    pushedAt?: string | null;
    htmlUrl?: string | null;
  }>;
};

type ApiDashboardResponse = {
  statuses?: Array<{
    userId?: number | null;
    login?: string | null;
    name?: string | null;
    avatarUrl?: string | null;
    availableMinutes?: number | null;
    status?: string | null;
    statusMessage?: string | null;
    updatedAt?: string | null;
  }>;
  commits?: Array<{
    id?: number | null;
    sha?: string | null;
    repositoryFullName?: string | null;
    message?: string | null;
    authorName?: string | null;
    committedAt?: string | null;
    url?: string | null;
  }>;
};

type ApiActivitySummaryItemResponse = {
  userId: number | null;
  login?: string | null;
  name?: string | null;
  avatarUrl?: string | null;
  commitCount: number;
  filesChanged: number;
  additions: number;
  deletions: number;
  availableMinutes: number;
};

type ApiCommitFeedResponse = {
  items?: Array<{
    id?: number | null;
    sha?: string | null;
    repositoryFullName?: string | null;
    message?: string | null;
    authorName?: string | null;
    committerName?: string | null;
    committedAt?: string | null;
    url?: string | null;
  }>;
};

type StatusListItemResponse = {
  statusId: number;
  userId: number | null;
  login?: string | null;
  name?: string | null;
  avatarUrl?: string | null;
  date?: string | null;
  availableMinutes?: number | null;
  status?: string | null;
  statusMessage?: string | null;
  updatedAt?: string | null;
};

type UserResponse = {
  id: number;
  githubId: number;
  login: string;
  name?: string | null;
  avatarUrl?: string | null;
};

type NormalizedStatusItem = {
  statusId?: number | null;
  userId?: number | null;
  login?: string | null;
  name?: string | null;
  avatarUrl?: string | null;
  availableMinutes?: number | null;
  status?: string | null;
  statusMessage?: string | null;
  updatedAt?: string | null;
};

type ActivityBucket = {
  commits: number;
  authors: Set<string>;
};

export async function loadOrganizationViewData(
  organizationId: string,
): Promise<OrganizationViewData | null> {
  if (!organizationId) {
    return null;
  }

  try {
    const detailResult = await backendFetchJson<ApiOrganizationDetailResponse>(
      `/api/organizations/${organizationId}`,
    );

    if (detailResult.status === 404 || !detailResult.data) {
      return null;
    }

    const today = new Date();
    const todayIso = formatDateInput(today);
    const activityStart = addDays(today, -(ACTIVITY_DAYS_LOOKBACK - 1));
    const activityStartIso = formatDateInput(activityStart);

    const [
      statusesResult,
      userResult,
      dashboardResult,
      activitySummaryResult,
      commitFeedResult,
    ] = await Promise.allSettled([
      backendFetchJson<StatusListItemResponse[]>(
        `/api/organizations/${organizationId}/statuses?date=${todayIso}`,
      ),
      backendFetchJson<UserResponse>(`/api/users/me`),
      backendFetchJson<ApiDashboardResponse>(`/api/organizations/${organizationId}/dashboard`),
      backendFetchJson<ApiActivitySummaryItemResponse[]>(
        `/api/organizations/${organizationId}/activity/summary?startDate=${activityStartIso}&endDate=${todayIso}&groupBy=user`,
      ),
      backendFetchJson<ApiCommitFeedResponse>(
        `/api/organizations/${organizationId}/git-commit/feed?limit=${COMMIT_FEED_LIMIT}`,
      ),
    ]);

    const statusesData =
      statusesResult.status === "fulfilled" ? statusesResult.value.data ?? [] : [];
    const dashboardData =
      dashboardResult.status === "fulfilled" ? dashboardResult.value.data ?? null : null;
    const activitySummaryItems =
      activitySummaryResult.status === "fulfilled"
        ? activitySummaryResult.value.data ?? []
        : [];
    const commitFeedItems: NonNullable<ApiCommitFeedResponse["items"]> =
      commitFeedResult.status === "fulfilled" ? commitFeedResult.value.data?.items ?? [] : [];

    const normalizedStatuses =
      statusesData.length > 0
        ? (statusesData as NormalizedStatusItem[])
        : normalizeDashboardStatuses(dashboardData?.statuses ?? []);

    const activitySummaryMap = buildActivitySummaryMap(activitySummaryItems);
    const members = normalizedStatuses
      .map((status) => toMemberStatus(status, activitySummaryMap))
      .sort(sortByLastSubmittedDesc);

    const currentUserId =
      userResult.status === "fulfilled" && userResult.value.data
        ? userResult.value.data.id
        : null;

    const memberCount = detailResult.data.members?.length ?? members.length;
    const activeToday = members.length;
    const pendingStatusCount = Math.max(memberCount - activeToday, 0);

    const commits = buildCommitList(dashboardData, commitFeedItems);
    const activity = buildActivityFromCommits(commitFeedItems, today);

    const organization = detailResult.data.organization;
    const detail = {
      id: organization?.id != null ? organization.id.toString() : organizationId,
      name: organization?.name ?? organization?.login ?? "Organization",
      description: organization?.description ?? "",
      avatarUrl: organization?.avatarUrl ?? DEFAULT_AVATAR,
      timezone: DEFAULT_TIMEZONE,
      memberCount,
      activeToday,
      pendingStatusCount,
      streakDays: null,
      lastActivity:
        resolveLatestActivityTimestamp(members, commits) ?? new Date().toISOString(),
      publicShareUrl:
        organization?.defaultLinkUrl ??
        organization?.htmlUrl ??
        DEFAULT_SHARE_URL,
    } satisfies OrganizationViewData["detail"];

    const personalStatus = derivePersonalStatus(members, currentUserId);

    return {
      detail,
      members,
      activity,
      commits,
      personalStatus,
    } satisfies OrganizationViewData;
  } catch (error) {
    console.error("[organizations] loadOrganizationViewData failed", error);
    return null;
  }
}

function normalizeDashboardStatuses(statuses: ApiDashboardResponse["statuses"]): NormalizedStatusItem[] {
  return (statuses ?? []).map((status, index) => ({
    statusId: status.userId ?? index,
    userId: status.userId ?? null,
    login: status.login ?? null,
    name: status.name ?? null,
    avatarUrl: status.avatarUrl ?? null,
    availableMinutes: status.availableMinutes ?? null,
    status: status.status ?? null,
    statusMessage: status.statusMessage ?? null,
    updatedAt: status.updatedAt ?? null,
  }));
}

function buildActivitySummaryMap(items: ApiActivitySummaryItemResponse[]) {
  const map = new Map<number, ApiActivitySummaryItemResponse>();
  items.forEach((item) => {
    if (item.userId != null) {
      map.set(item.userId, item);
    }
  });
  return map;
}

function toMemberStatus(
  item: NormalizedStatusItem,
  activitySummaryMap: Map<number, ApiActivitySummaryItemResponse>,
): MemberStatus {
  const defaultStatus: MemberStatus["status"] = "集中";
  const memberIdentifier = item.userId ?? item.statusId;
  const summary = item.userId != null ? activitySummaryMap.get(item.userId) : undefined;
  const commitCount = summary?.commitCount ?? 0;
  const availableMinutes = summary?.availableMinutes ?? item.availableMinutes ?? null;

  return {
    memberId: memberIdentifier != null ? memberIdentifier.toString() : randomUUID(),
    displayName: item.name ?? item.login ?? "メンバー",
    avatarUrl: item.avatarUrl ?? DEFAULT_AVATAR,
    status: (item.status as MemberStatus["status"]) ?? defaultStatus,
    statusMessage: item.statusMessage ?? null,
    lastSubmittedAt: item.updatedAt ?? null,
    commitCount,
    capacityHours: minutesToHours(availableMinutes),
    streakDays: 0,
    latestPrUrl: null,
  };
}

function minutesToHours(value?: number | null) {
  if (value == null) return null;
  return Math.round(value / 60);
}

function sortByLastSubmittedDesc(left: MemberStatus, right: MemberStatus) {
  const leftTime = left.lastSubmittedAt ? Date.parse(left.lastSubmittedAt) : 0;
  const rightTime = right.lastSubmittedAt ? Date.parse(right.lastSubmittedAt) : 0;
  return rightTime - leftTime;
}

function derivePersonalStatus(members: MemberStatus[], currentUserId: number | null): PersonalStatus {
  const fallback: PersonalStatus = {
    submitted: false,
    status: null,
    statusMessage: null,
    lastSubmittedAt: null,
    commitCount: 0,
    capacityHours: null,
    streakDays: 0,
    latestPrUrl: null,
  };

  if (!currentUserId) {
    return fallback;
  }

  const match = members.find((member) => member.memberId === currentUserId.toString());
  if (!match) {
    return fallback;
  }

  return {
    submitted: true,
    status: match.status,
    statusMessage: match.statusMessage,
    lastSubmittedAt: match.lastSubmittedAt,
    commitCount: match.commitCount,
    capacityHours: match.capacityHours,
    streakDays: match.streakDays,
    latestPrUrl: match.latestPrUrl,
  };
}

function resolveLatestActivityTimestamp(
  members: MemberStatus[],
  commits: OrganizationViewData["commits"],
) {
  const timestamps: Array<{ value: string; epoch: number }> = [];
  members.forEach((member) => {
    if (member.lastSubmittedAt) {
      const epoch = Date.parse(member.lastSubmittedAt);
      if (!Number.isNaN(epoch)) {
        timestamps.push({ value: member.lastSubmittedAt, epoch });
      }
    }
  });
  commits.forEach((commit) => {
    if (commit.committedAt) {
      const epoch = Date.parse(commit.committedAt);
      if (!Number.isNaN(epoch)) {
        timestamps.push({ value: commit.committedAt, epoch });
      }
    }
  });
  if (timestamps.length === 0) {
    return null;
  }
  return timestamps.sort((a, b) => a.epoch - b.epoch).at(-1)?.value ?? null;
}

function buildCommitList(
  dashboardData: ApiDashboardResponse | null,
  commitFeedItems: ApiCommitFeedResponse["items"],
): CommitActivity[] {
  const fallbackCommits = commitFeedItems ?? [];
  const sourceCommits = dashboardData?.commits?.length
    ? dashboardData.commits
    : fallbackCommits.slice(0, 5);

  return sourceCommits.map((commit) => ({
    id: commit.id != null ? commit.id.toString() : commit.sha ?? randomUUID(),
    title: commit.message ?? "(no message)",
    repository: commit.repositoryFullName ?? "unknown repository",
    author: commit.authorName ?? "unknown",
    committedAt: commit.committedAt ?? new Date().toISOString(),
    url: commit.url ?? "#",
  }));
}

function buildActivityFromCommits(
  commitFeedItems: ApiCommitFeedResponse["items"],
  referenceDate: Date,
): OrganizationActivity {
  if (!commitFeedItems || commitFeedItems.length === 0) {
    return EMPTY_ACTIVITY;
  }

  const dailyBuckets = groupCommitsByDay(commitFeedItems);
  const weeklyBuckets = groupCommitsByWeek(commitFeedItems);

  const daily: OrganizationActivity["daily"] = [];
  for (let offset = ACTIVITY_DAYS_LOOKBACK - 1; offset >= 0; offset -= 1) {
    const date = addDays(referenceDate, -offset);
    const key = formatDateInput(date);
    const bucket = dailyBuckets.get(key);
    daily.push({
      bucketStart: key,
      activeMembers: bucket ? bucket.authors.size : 0,
      totalCommits: bucket ? bucket.commits : 0,
      avgStatusScore: 0,
    });
  }

  const weekly: OrganizationActivity["weekly"] = [];
  for (let offset = ACTIVITY_WEEKS_LOOKBACK - 1; offset >= 0; offset -= 1) {
    const date = addDays(referenceDate, -offset * 7);
    const weekStart = startOfWeek(date);
    const key = formatDateInput(weekStart);
    const bucket = weeklyBuckets.get(key);
    weekly.push({
      bucketStart: key,
      activeMembers: bucket ? bucket.authors.size : 0,
      totalCommits: bucket ? bucket.commits : 0,
      avgStatusScore: 0,
    });
  }

  return { daily, weekly };
}

function groupCommitsByDay(
  commits: ApiCommitFeedResponse["items"],
): Map<string, ActivityBucket> {
  const map = new Map<string, ActivityBucket>();
  (commits ?? []).forEach((commit) => {
    if (!commit.committedAt) {
      return;
    }
    const date = new Date(commit.committedAt);
    if (Number.isNaN(date.getTime())) {
      return;
    }
    const bucketKey = formatDateInput(date);
    const entry = map.get(bucketKey) ?? { commits: 0, authors: new Set<string>() };
    entry.commits += 1;
    if (commit.authorName) {
      entry.authors.add(commit.authorName);
    }
    map.set(bucketKey, entry);
  });
  return map;
}

function groupCommitsByWeek(
  commits: ApiCommitFeedResponse["items"],
): Map<string, ActivityBucket> {
  const map = new Map<string, ActivityBucket>();
  (commits ?? []).forEach((commit) => {
    if (!commit.committedAt) {
      return;
    }
    const date = new Date(commit.committedAt);
    if (Number.isNaN(date.getTime())) {
      return;
    }
    const weekStart = startOfWeek(date);
    const bucketKey = formatDateInput(weekStart);
    const entry = map.get(bucketKey) ?? { commits: 0, authors: new Set<string>() };
    entry.commits += 1;
    if (commit.authorName) {
      entry.authors.add(commit.authorName);
    }
    map.set(bucketKey, entry);
  });
  return map;
}

function formatDateInput(date: Date) {
  return date.toISOString().slice(0, 10);
}

function addDays(date: Date, days: number) {
  const result = new Date(date);
  result.setUTCDate(result.getUTCDate() + days);
  return result;
}

function startOfWeek(date: Date) {
  const result = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
  const day = result.getUTCDay();
  const diff = (day + 6) % 7;
  result.setUTCDate(result.getUTCDate() - diff);
  return result;
}

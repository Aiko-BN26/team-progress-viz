import { randomUUID } from "node:crypto";

import type { OrganizationViewData, MemberStatus, PersonalStatus, OrganizationActivity } from "./types";
import { fetchMockOrganizationViewData } from "./mock-data";
import { backendFetchJson } from "@/lib/server-api";

const DEFAULT_TIMEZONE = process.env.DEFAULT_ORGANIZATION_TIMEZONE ?? "Asia/Tokyo";
const DEFAULT_AVATAR = "https://avatars.githubusercontent.com/u/0?v=4";
const DEFAULT_SHARE_URL = "";

type ApiOrganizationDetailResponse = {
  organization: {
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
    login?: string | null;
    name?: string | null;
    avatarUrl?: string | null;
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

const EMPTY_ACTIVITY: OrganizationActivity = { daily: [], weekly: [] };

export async function loadOrganizationViewData(
  organizationId: string,
): Promise<OrganizationViewData | null> {
  const mockDataPromise = fetchMockOrganizationViewData(organizationId, { simulateDelay: false });
  const today = new Date().toISOString().slice(0, 10);

  const [detailResult, statusResult, userResult] = await Promise.allSettled([
    backendFetchJson<ApiOrganizationDetailResponse>(`/api/organizations/${organizationId}`),
    backendFetchJson<StatusListItemResponse[]>(
      `/api/organizations/${organizationId}/statuses?date=${today}`,
    ),
    backendFetchJson<UserResponse>(`/api/users/me`),
  ]);

  if (
    detailResult.status === "rejected" ||
    !detailResult.value.data ||
    detailResult.value.status === 404
  ) {
    return mockDataPromise;
  }

  const apiDetail = detailResult.value.data;
  const mockData = await mockDataPromise;

  const statuses: StatusListItemResponse[] =
    statusResult.status === "fulfilled" && statusResult.value.data
      ? statusResult.value.data
      : [];

  if (statusResult.status === "rejected") {
    console.error("Failed to load statuses", statusResult.reason);
  }

  if (userResult.status === "rejected") {
    console.error("Failed to load current user", userResult.reason);
  }

  const currentUserId =
    userResult.status === "fulfilled" && userResult.value.data
      ? userResult.value.data.id
      : null;

  const members = statuses.map(toMemberStatus).sort(sortByLastSubmittedDesc);
  const memberCount = apiDetail.members?.length ?? members.length;
  const activeToday = members.length;

  const commits = (apiDetail.recentCommits ?? []).map((commit) => ({
    id: commit.id?.toString() ?? commit.sha ?? randomUUID(),
    title: commit.message ?? "(no message)",
    repository: commit.repositoryFullName ?? "unknown repository",
    author: commit.authorName ?? commit.committerName ?? "unknown", 
    committedAt: commit.committedAt ?? commit.pushedAt ?? new Date().toISOString(),
    url: commit.htmlUrl ?? "#",
  }));

  const lastActivity = resolveLatestActivityTimestamp(members, commits) ??
    mockData?.detail.lastActivity ??
    new Date().toISOString();

  const detail = {
    id: apiDetail.organization?.id?.toString() ?? organizationId,
    name: apiDetail.organization?.name ?? apiDetail.organization?.login ?? "Organization",
    description: apiDetail.organization?.description ?? mockData?.detail.description ?? "",
    avatarUrl: apiDetail.organization?.avatarUrl ?? mockData?.detail.avatarUrl ?? DEFAULT_AVATAR,
    timezone: mockData?.detail.timezone ?? DEFAULT_TIMEZONE,
    memberCount,
    activeToday,
    pendingStatusCount: Math.max(memberCount - activeToday, 0),
    streakDays: null,
    lastActivity,
    publicShareUrl:
      apiDetail.organization?.defaultLinkUrl ??
      apiDetail.organization?.htmlUrl ??
      mockData?.detail.publicShareUrl ??
      DEFAULT_SHARE_URL,
  } satisfies OrganizationViewData["detail"];

  const personalStatus = derivePersonalStatus(members, currentUserId);

  return {
    detail,
    members,
    activity: mockData?.activity ?? EMPTY_ACTIVITY,
    commits,
    personalStatus,
  } satisfies OrganizationViewData;
}

function minutesToHours(value?: number | null) {
  if (value == null) return null;
  return Math.round(value / 60);
}

function toMemberStatus(item: StatusListItemResponse): MemberStatus {
  const defaultStatus: MemberStatus["status"] = "集中";
  return {
    memberId: (item.userId ?? item.statusId).toString(),
    displayName: item.name ?? item.login ?? "メンバー",
    avatarUrl: item.avatarUrl ?? DEFAULT_AVATAR,
    status: (item.status as MemberStatus["status"]) ?? defaultStatus,
    statusMessage: item.statusMessage ?? null,
    lastSubmittedAt: item.updatedAt ?? null,
    commitCount: 0,
    capacityHours: minutesToHours(item.availableMinutes),
    streakDays: 0,
    latestPrUrl: null,
  };
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

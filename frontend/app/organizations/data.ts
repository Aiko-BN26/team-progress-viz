import { backendFetchJson } from "@/lib/server-api";
import type { OrganizationListItem } from "./types";

type ApiOrganizationSummary = {
  id: number;
  githubId: number;
  login: string;
  name?: string | null;
  avatarUrl?: string | null;
  description?: string | null;
};

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
  activitySummaryLast7Days?: {
    activeMembers?: number | null;
    commitCount?: number | null;
  };
  recentCommits?: Array<{
    committedAt?: string | null;
    pushedAt?: string | null;
  }>;
};

type StatusListItemResponse = {
  statusId: number;
  userId: number | null;
  updatedAt?: string | null;
};

export async function loadOrganizations(): Promise<OrganizationListItem[]> {
  try {
    const summariesResult = await backendFetchJson<ApiOrganizationSummary[]>("/api/organizations");
    console.log("summariesResult:", summariesResult);
    const summaries = summariesResult.data;

    if (!summaries || summaries.length === 0) {
      console.warn("[organizations] /api/organizations returned no data");
      return [];
    }

    const enriched = await Promise.all(
      summaries.map(async (summary) => {
        const [detailResult, statusResult] = await Promise.allSettled([
          backendFetchJson<ApiOrganizationDetailResponse>(`/api/organizations/${summary.id}`),
          backendFetchJson<StatusListItemResponse[]>(
            `/api/organizations/${summary.id}/statuses`,
          ),
        ]);

        const detailData =
          detailResult.status === "fulfilled" ? detailResult.value.data : null;
        if (detailResult.status === "rejected") {
          console.error("Failed to load organization detail", summary.id, detailResult.reason);
        }

        const statusData =
          statusResult.status === "fulfilled" ? statusResult.value.data ?? [] : [];
        if (statusResult.status === "rejected") {
          console.error("Failed to load organization statuses", summary.id, statusResult.reason);
        }

        return mapToListItem(summary, detailData, statusData);
      }),
    );

    return enriched.filter((item): item is OrganizationListItem => Boolean(item));
  } catch (error) {
    console.error("[organizations] loadOrganizations failed", error);
    return [];
  }
}

function mapToListItem(
  summary: ApiOrganizationSummary,
  detail: ApiOrganizationDetailResponse | null,
  statuses: StatusListItemResponse[],
): OrganizationListItem {
  const organization = detail?.organization;
  const id =
    organization?.id?.toString() ?? summary.id?.toString() ?? summary.login ?? "0";
  const name = organization?.name ?? summary.name ?? summary.login ?? "Organization";
  const description = organization?.description ?? summary.description ?? "";
  const avatarUrl = organization?.avatarUrl ?? summary.avatarUrl ?? "";

  const memberCount = detail?.members?.length ?? 0;
  const activeToday = statuses.length;
  const pendingStatusCount = memberCount > 0 ? Math.max(memberCount - activeToday, 0) : 0;

  const lastActivity =
    extractLatestTimestampFromData(statuses, detail?.recentCommits) ??
    new Date().toISOString();

  return {
    id,
    name,
    description,
    avatarUrl,
    memberCount,
    activeToday,
    pendingStatusCount,
    lastActivity,
  };
}

function extractLatestStatusTimestamp(statuses: StatusListItemResponse[]) {
  const timestamps = statuses
    .map((status) => status.updatedAt)
    .filter((value): value is string => Boolean(value));
  if (timestamps.length === 0) {
    return null;
  }
  return timestamps.sort().at(-1) ?? null;
}

function extractLatestTimestampFromData(
  statuses: StatusListItemResponse[],
  recentCommits?: ApiOrganizationDetailResponse["recentCommits"],
) {
  const statusTimestamp = extractLatestStatusTimestamp(statuses);
  const commitTimestamps = recentCommits
    ?.map((commit) => commit.committedAt ?? commit.pushedAt ?? null)
    .filter((value): value is string => Boolean(value));
  const latestCommit = commitTimestamps?.sort().at(-1) ?? null;

  if (statusTimestamp && latestCommit) {
    return statusTimestamp > latestCommit ? statusTimestamp : latestCommit;
  }
  return statusTimestamp ?? latestCommit ?? null;
}

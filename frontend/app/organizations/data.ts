import { backendFetchJson } from "@/lib/server-api";
import { mockOrganizations } from "./mock-data";
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
  members?: Array<{
    userId: number | null;
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

    const today = new Date().toISOString().slice(0, 10);
    const fallbackMap = buildFallbackMap();

    const enriched = await Promise.all(
      summaries.map(async (summary) => {
        const [detailResult, statusResult] = await Promise.allSettled([
          backendFetchJson<ApiOrganizationDetailResponse>(`/api/organizations/${summary.id}`),
          backendFetchJson<StatusListItemResponse[]>(
            `/api/organizations/${summary.id}/statuses?date=${today}`,
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

        const fallback = getFallback(summary, fallbackMap);
        return mapToListItem(summary, detailData, statusData, fallback);
      }),
    );

    return enriched.filter((item): item is OrganizationListItem => Boolean(item));
  } catch (error) {
    console.error("[organizations] loadOrganizations failed, falling back to mock", error);
    return mockOrganizations;
  }
}

function buildFallbackMap() {
  const map = new Map<string, OrganizationListItem>();
  mockOrganizations.forEach((org) => {
    map.set(org.id, org);
    map.set(org.name, org);
  });
  return map;
}

function getFallback(summary: ApiOrganizationSummary, map: Map<string, OrganizationListItem>) {
  if (!summary) return undefined;
  return map.get(summary.login) ?? map.get(summary.name ?? "") ?? undefined;
}

function mapToListItem(
  summary: ApiOrganizationSummary,
  detail: ApiOrganizationDetailResponse | null,
  statuses: StatusListItemResponse[],
  fallback?: OrganizationListItem,
): OrganizationListItem {
  const id = summary.id?.toString() ?? fallback?.id ?? summary.login;
  const name = summary.name ?? summary.login ?? fallback?.name ?? "Organization";
  const description = summary.description ?? fallback?.description ?? "";
  const avatarUrl = summary.avatarUrl ?? fallback?.avatarUrl ?? "";

  const memberCount = detail?.members?.length ?? fallback?.memberCount ?? 0;
  const activeToday = statuses.length > 0 ? statuses.length : fallback?.activeToday ?? 0;
  const pendingStatusCount = memberCount > 0
    ? Math.max(memberCount - activeToday, 0)
    : fallback?.pendingStatusCount ?? 0;

  const lastActivity =
    extractLatestStatusTimestamp(statuses) ?? fallback?.lastActivity ?? new Date().toISOString();

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

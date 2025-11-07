'use client';

import Link from "next/link";
import { FormEvent, useCallback, useEffect, useState } from "react";

import {
  ActionStatus,
  API_BASE_URL,
  formatJson,
  parseJobStatus,
  parseJobSubmission,
  parseOrganizationDetail,
  parseOrganizations,
  parseOrganizationMembers,
  parseOrganizationRegistration,
  parseOrganizationSummaries,
  parseRepositorySyncStatuses,
  parseRepositories,
  parseSessionUser,
  parseActivitySummary,
  parseCommentCreateResult,
  parseCommentList,
  parseCommitFeedResponse,
  parseDashboardResponse,
  parsePullRequestDetail,
  parsePullRequestFeed,
  parsePullRequestFiles,
  parsePullRequestList,
  parseStatusList,
  parseStatusUpdateResponse,
  parseUserProfile,
  readJson,
} from "@/lib/api";
import type {
  GitHubOrganization,
  GitHubOrganizationMember,
  GitHubRepository,
  JobStatus,
  JobSubmission,
  OrganizationDetail,
  OrganizationRegistration,
  OrganizationSummary,
  RepositorySyncStatus,
  SessionUser,
  ActivitySummaryItem,
  CommentCreateResult,
  CommentListItem,
  CommitFeed,
  CommitFeedItem,
  DashboardSnapshot,
  PullRequestDetail,
  PullRequestFeed,
  PullRequestFeedItem,
  PullRequestFileItem,
  PullRequestListItem,
  StatusListItem,
  StatusUpdatePayload,
  UserProfile,
} from "@/lib/api";

type LoginResult = {
  status: ActionStatus;
  url?: string;
  payload?: unknown;
  error?: string;
};

type SessionResult = {
  status: ActionStatus;
  user?: SessionUser | null;
  payload?: unknown;
  error?: string;
};

type LogoutResult = {
  status: ActionStatus;
  message?: string;
  error?: string;
};

type OrganizationsResult = {
  status: ActionStatus;
  items?: GitHubOrganization[];
  payload?: unknown;
  error?: string;
};

type RepositoriesResult = {
  status: ActionStatus;
  organization?: string;
  items?: GitHubRepository[];
  payload?: unknown;
  error?: string;
};

type MembersResult = {
  status: ActionStatus;
  organization?: string;
  items?: GitHubOrganizationMember[];
  payload?: unknown;
  error?: string;
};

type StoredOrganizationsResult = {
  status: ActionStatus;
  items?: OrganizationSummary[];
  payload?: unknown;
  error?: string;
};

type OrganizationRegistrationResult = {
  status: ActionStatus;
  data?: OrganizationRegistration;
  payload?: unknown;
  error?: string;
};

type OrganizationDetailState = {
  status: ActionStatus;
  data?: OrganizationDetail;
  payload?: unknown;
  error?: string;
};

type JobSubmissionResult = {
  status: ActionStatus;
  data?: JobSubmission;
  payload?: unknown;
  error?: string;
};

type JobStatusResult = {
  status: ActionStatus;
  data?: JobStatus;
  payload?: unknown;
  error?: string;
};

type SyncJobResult = {
  status: ActionStatus;
  organizationId?: number;
  data?: JobSubmission;
  payload?: unknown;
  error?: string;
};

type SyncStatusResult = {
  status: ActionStatus;
  organizationId?: number;
  items?: RepositorySyncStatus[];
  payload?: unknown;
  error?: string;
};

type RegistrationFormState = {
  login: string;
  defaultLinkUrl: string;
};

type UserProfileResult = {
  status: ActionStatus;
  user?: UserProfile;
  payload?: unknown;
  error?: string;
};

type DashboardResult = {
  status: ActionStatus;
  organizationId?: number;
  data?: DashboardSnapshot;
  payload?: unknown;
  error?: string;
};

type StatusFormState = {
  organizationId: string;
  status: string;
  statusMessage: string;
  capacityHours: string;
  availableMinutes: string;
  date: string;
};

type StatusUpdateResult = {
  status: ActionStatus;
  organizationId?: number;
  data?: StatusUpdatePayload;
  payload?: unknown;
  error?: string;
};

type StatusListQuery = {
  organizationId: string;
  date: string;
};

type StatusListResult = {
  status: ActionStatus;
  organizationId?: number;
  items?: StatusListItem[];
  payload?: unknown;
  error?: string;
};

type StatusDeleteState = {
  status: ActionStatus;
  organizationId?: number;
  statusId?: number;
  payload?: unknown;
  error?: string;
};

type ActivitySummaryQuery = {
  organizationId: string;
  startDate: string;
  endDate: string;
  groupBy: string;
};

type ActivitySummaryResult = {
  status: ActionStatus;
  organizationId?: number;
  items?: ActivitySummaryItem[];
  payload?: unknown;
  error?: string;
};

type CommitFeedQuery = {
  organizationId: string;
  limit: string;
  cursor: string;
};

type CommitFeedResultState = {
  status: ActionStatus;
  organizationId?: number;
  data?: CommitFeed;
  payload?: unknown;
  error?: string;
};

type PullFeedQuery = {
  organizationId: string;
  limit: string;
  cursor: string;
};

type PullFeedResultState = {
  status: ActionStatus;
  organizationId?: number;
  data?: PullRequestFeed;
  payload?: unknown;
  error?: string;
};

type PullListQuery = {
  repositoryId: string;
  state: string;
  limit: string;
  page: string;
};

type PullListResultState = {
  status: ActionStatus;
  repositoryId?: number;
  items?: PullRequestListItem[];
  payload?: unknown;
  error?: string;
};

type PullDetailQuery = {
  repositoryId: string;
  pullNumber: string;
};

type PullDetailResultState = {
  status: ActionStatus;
  repositoryId?: number;
  pullNumber?: number;
  data?: PullRequestDetail;
  payload?: unknown;
  error?: string;
};

type PullFilesResultState = {
  status: ActionStatus;
  repositoryId?: number;
  pullNumber?: number;
  items?: PullRequestFileItem[];
  payload?: unknown;
  error?: string;
};

type PullSyncQuery = {
  repositoryId: string;
};

type CommentFormState = {
  organizationId: string;
  targetType: string;
  targetId: string;
  parentCommentId: string;
  content: string;
};

type CommentCreateResultState = {
  status: ActionStatus;
  organizationId?: number;
  data?: CommentCreateResult;
  payload?: unknown;
  error?: string;
};

type CommentListQuery = {
  organizationId: string;
  targetType: string;
  targetId: string;
};

type CommentListResultState = {
  status: ActionStatus;
  organizationId?: number;
  items?: CommentListItem[];
  payload?: unknown;
  error?: string;
};

type CommentDeleteState = {
  status: ActionStatus;
  organizationId?: number;
  commentId?: number;
  payload?: unknown;
  error?: string;
};

type WebhookFormState = {
  eventType: string;
  deliveryId: string;
  signature: string;
  payload: string;
};

type WebhookResultState = {
  status: ActionStatus;
  payload?: unknown;
  error?: string;
};

export default function Home() {
  const [loginResult, setLoginResult] = useState<LoginResult>({ status: "idle" });
  const [sessionResult, setSessionResult] = useState<SessionResult>({ status: "idle" });
  const [logoutResult, setLogoutResult] = useState<LogoutResult>({ status: "idle" });
  const [organizationsResult, setOrganizationsResult] = useState<OrganizationsResult>({
    status: "idle",
  });
  const [repositoriesResult, setRepositoriesResult] = useState<RepositoriesResult>({
    status: "idle",
  });
  const [membersResult, setMembersResult] = useState<MembersResult>({
    status: "idle",
  });
  const [storedOrganizationsResult, setStoredOrganizationsResult] = useState<StoredOrganizationsResult>({
    status: "idle",
  });
  const [registrationForm, setRegistrationForm] = useState<RegistrationFormState>({
    login: "",
    defaultLinkUrl: "",
  });
  const [registrationResult, setRegistrationResult] = useState<OrganizationRegistrationResult>({
    status: "idle",
  });
  const [detailResult, setDetailResult] = useState<OrganizationDetailState>({
    status: "idle",
  });
  const [deleteResult, setDeleteResult] = useState<JobSubmissionResult>({
    status: "idle",
  });
  const [jobStatusResult, setJobStatusResult] = useState<JobStatusResult>({
    status: "idle",
  });
  const [syncJobResult, setSyncJobResult] = useState<SyncJobResult>({
    status: "idle",
  });
  const [syncStatusResult, setSyncStatusResult] = useState<SyncStatusResult>({
    status: "idle",
  });
  const [userProfileResult, setUserProfileResult] = useState<UserProfileResult>({
    status: "idle",
  });
  const [userDeletionResult, setUserDeletionResult] = useState<JobSubmissionResult>({
    status: "idle",
  });
  const [dashboardOrganizationId, setDashboardOrganizationId] = useState("");
  const [dashboardResult, setDashboardResult] = useState<DashboardResult>({
    status: "idle",
  });
  const [statusForm, setStatusForm] = useState<StatusFormState>({
    organizationId: "",
    status: "",
    statusMessage: "",
    capacityHours: "",
    availableMinutes: "",
    date: "",
  });
  const [statusUpdateResult, setStatusUpdateResult] = useState<StatusUpdateResult>({
    status: "idle",
  });
  const [statusListQuery, setStatusListQuery] = useState<StatusListQuery>({
    organizationId: "",
    date: "",
  });
  const [statusListResult, setStatusListResult] = useState<StatusListResult>({
    status: "idle",
  });
  const [statusDeleteState, setStatusDeleteState] = useState<StatusDeleteState>({
    status: "idle",
  });
  const [activitySummaryQuery, setActivitySummaryQuery] = useState<ActivitySummaryQuery>({
    organizationId: "",
    startDate: "",
    endDate: "",
    groupBy: "",
  });
  const [activitySummaryResult, setActivitySummaryResult] = useState<ActivitySummaryResult>({
    status: "idle",
  });
  const [commitFeedQuery, setCommitFeedQuery] = useState<CommitFeedQuery>({
    organizationId: "",
    limit: "",
    cursor: "",
  });
  const [commitFeedResult, setCommitFeedResult] = useState<CommitFeedResultState>({
    status: "idle",
  });
  const [pullFeedQuery, setPullFeedQuery] = useState<PullFeedQuery>({
    organizationId: "",
    limit: "",
    cursor: "",
  });
  const [pullFeedResult, setPullFeedResult] = useState<PullFeedResultState>({
    status: "idle",
  });
  const [pullListQuery, setPullListQuery] = useState<PullListQuery>({
    repositoryId: "",
    state: "all",
    limit: "",
    page: "",
  });
  const [pullListResult, setPullListResult] = useState<PullListResultState>({
    status: "idle",
  });
  const [pullDetailQuery, setPullDetailQuery] = useState<PullDetailQuery>({
    repositoryId: "",
    pullNumber: "",
  });
  const [pullDetailResult, setPullDetailResult] = useState<PullDetailResultState>({
    status: "idle",
  });
  const [pullFilesResult, setPullFilesResult] = useState<PullFilesResultState>({
    status: "idle",
  });
  const [pullSyncQuery, setPullSyncQuery] = useState<PullSyncQuery>({
    repositoryId: "",
  });
  const [pullSyncResult, setPullSyncResult] = useState<JobSubmissionResult>({
    status: "idle",
  });
  const [commentForm, setCommentForm] = useState<CommentFormState>({
    organizationId: "",
    targetType: "",
    targetId: "",
    parentCommentId: "",
    content: "",
  });
  const [commentCreateResult, setCommentCreateResult] = useState<CommentCreateResultState>({
    status: "idle",
  });
  const [commentListQuery, setCommentListQuery] = useState<CommentListQuery>({
    organizationId: "",
    targetType: "",
    targetId: "",
  });
  const [commentListResult, setCommentListResult] = useState<CommentListResultState>({
    status: "idle",
  });
  const [commentDeleteState, setCommentDeleteState] = useState<CommentDeleteState>({
    status: "idle",
  });
  const [webhookForm, setWebhookForm] = useState<WebhookFormState>({
    eventType: "",
    deliveryId: "",
    signature: "",
    payload: '{\n  "example": true\n}',
  });
  const [webhookResult, setWebhookResult] = useState<WebhookResultState>({
    status: "idle",
  });
  const [jobQueryId, setJobQueryId] = useState("");
  const [selectedOrganizationId, setSelectedOrganizationId] = useState<number | null>(null);

  const isAuthenticated = sessionResult.status === "success" && !!sessionResult.user;

  const loadSession = useCallback(async () => {
    setSessionResult({ status: "loading" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/session`, {
        credentials: "include",
      });

      const data = await readJson(response);
      const body = (data ?? undefined) as Record<string, unknown> | undefined;

      if (response.status === 401) {
        setSessionResult({
          status: "success",
          user: null,
          payload: body,
        });
        return;
      }

      if (!response.ok) {
        const message =
          typeof body?.error === "string"
            ? body.error
            : `Request failed (${response.status})`;
        setSessionResult({
          status: "error",
          error: message,
          payload: body,
        });
        return;
      }

      const user = parseSessionUser(body);
      if (!user) {
        setSessionResult({
          status: "error",
          error: "Unexpected response payload",
          payload: body,
        });
        return;
      }

      setSessionResult({
        status: "success",
        user,
        payload: body,
      });
    } catch (error) {
      setSessionResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, []);

  useEffect(() => {
    // Surface the initial session state when the page loads.
    const id = requestAnimationFrame(() => {
      void loadSession();
    });
    return () => cancelAnimationFrame(id);
  }, [loadSession]);

  const startLogin = useCallback(async () => {
    setLoginResult({ status: "loading" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/github/login`, {
        credentials: "include",
      });
      const data = await readJson(response);
      const body = (data ?? undefined) as Record<string, unknown> | undefined;

      if (!response.ok) {
        const message =
          typeof body?.error === "string"
            ? body.error
            : `Request failed (${response.status})`;
        setLoginResult({
          status: "error",
          error: message,
          payload: body,
        });
        return;
      }

      const url =
        typeof body?.authorizationUrl === "string"
          ? body.authorizationUrl
          : undefined;

      setLoginResult({
        status: "success",
        url,
        payload: body,
      });
    } catch (error) {
      setLoginResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, []);

  const endSession = useCallback(async () => {
    setLogoutResult({ status: "loading" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/auth/logout`, {
        method: "POST",
        credentials: "include",
      });

      if (!response.ok) {
        const data = await readJson(response);
        const body = (data ?? undefined) as Record<string, unknown> | undefined;
        const message =
          typeof body?.error === "string"
            ? body.error
            : `Request failed (${response.status})`;

        setLogoutResult({
          status: "error",
          error: message,
        });
        return;
      }

      setLogoutResult({
        status: "success",
        message: "Session cleared (204 No Content)",
      });

      await loadSession();
    } catch (error) {
      setLogoutResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [loadSession]);

  const loadOrganizations = useCallback(async () => {
    setOrganizationsResult({ status: "loading" });
    setRepositoriesResult({ status: "idle" });
    setMembersResult({ status: "idle" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/github/organizations`, {
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setOrganizationsResult({
          status: "error",
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setOrganizationsResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      const organizations = parseOrganizations(payload);
      if (organizations === undefined) {
        setOrganizationsResult({
          status: "error",
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setOrganizationsResult({
        status: "success",
        items: organizations,
        payload,
      });
    } catch (error) {
      setOrganizationsResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, []);

  const loadRepositories = useCallback(async (organization: string) => {
    setRepositoriesResult({ status: "loading", organization });
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/github/organizations/${encodeURIComponent(organization)}/repositories`,
        {
          credentials: "include",
        },
      );

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setRepositoriesResult({
          status: "error",
          error: "Unauthorized (login required)",
          organization,
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setRepositoriesResult({
          status: "error",
          error: message,
          organization,
          payload,
        });
        return;
      }

      const repositories = parseRepositories(payload);
      if (repositories === undefined) {
        setRepositoriesResult({
          status: "error",
          error: "Unexpected response payload",
          organization,
          payload,
        });
        return;
      }

      setRepositoriesResult({
        status: "success",
        organization,
        items: repositories,
        payload,
      });
    } catch (error) {
      setRepositoriesResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
        organization,
      });
    }
  }, []);

  const loadMembers = useCallback(async (organization: string) => {
    setMembersResult({ status: "loading", organization });
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/github/organizations/${encodeURIComponent(organization)}/members`,
        {
          credentials: "include",
        },
      );

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setMembersResult({
          status: "error",
          error: "Unauthorized (login required)",
          organization,
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setMembersResult({
          status: "error",
          error: message,
          organization,
          payload,
        });
        return;
      }

      const members = parseOrganizationMembers(payload);
      if (members === undefined) {
        setMembersResult({
          status: "error",
          error: "Unexpected response payload",
          organization,
          payload,
        });
        return;
      }

      setMembersResult({
        status: "success",
        organization,
        items: members,
        payload,
      });
    } catch (error) {
      setMembersResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
        organization,
      });
    }
  }, []);

  const loadStoredOrganizations = useCallback(async () => {
    if (!isAuthenticated) {
      setStoredOrganizationsResult({
        status: "error",
        error: "Sign in to fetch organizations",
      });
      return;
    }

    setStoredOrganizationsResult({ status: "loading" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations`, {
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401 || response.status === 403) {
        setStoredOrganizationsResult({
          status: "error",
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setStoredOrganizationsResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      const organizations = parseOrganizationSummaries(payload);
      if (organizations === undefined) {
        setStoredOrganizationsResult({
          status: "error",
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setStoredOrganizationsResult({
        status: "success",
        items: organizations,
        payload,
      });
    } catch (error) {
      setStoredOrganizationsResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated]);

  const submitOrganizationRegistration = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedLogin = registrationForm.login.trim();
    const trimmedDefaultLink = registrationForm.defaultLinkUrl.trim();

    if (!trimmedLogin) {
      setRegistrationResult({
        status: "error",
        error: "Organization login is required",
      });
      return;
    }

    if (!isAuthenticated) {
      setRegistrationResult({
        status: "error",
        error: "Sign in before registering an organization",
      });
      return;
    }

    setRegistrationResult({ status: "loading" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify({
          login: trimmedLogin,
          defaultLinkUrl: trimmedDefaultLink.length > 0 ? trimmedDefaultLink : null,
        }),
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setRegistrationResult({
          status: "error",
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setRegistrationResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      const data = parseOrganizationRegistration(payload);
      if (data === undefined) {
        setRegistrationResult({
          status: "error",
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setRegistrationResult({
        status: "success",
        data,
        payload,
      });
      setRegistrationForm({ login: "", defaultLinkUrl: "" });
      setJobQueryId(data.jobId);
      await loadStoredOrganizations();
    } catch (error) {
      setRegistrationResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [registrationForm, loadStoredOrganizations, isAuthenticated]);

  const fetchOrganizationDetail = useCallback(async (organizationId: number) => {
    if (!isAuthenticated) {
      setDetailResult({
        status: "error",
        error: "Sign in to load organization detail",
      });
      return;
    }

    setDetailResult({ status: "loading" });
    setSelectedOrganizationId(organizationId);
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations/${organizationId}`, {
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setDetailResult({
          status: "error",
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setDetailResult({
          status: "error",
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setDetailResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      const detail = parseOrganizationDetail(payload);
      if (detail === undefined) {
        setDetailResult({
          status: "error",
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setDetailResult({
        status: "success",
        data: detail,
        payload,
      });
    } catch (error) {
      setDetailResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated]);

  const deleteOrganization = useCallback(async (organizationId: number) => {
    if (typeof window !== "undefined") {
      const confirmed = window.confirm("Delete this organization? This cannot be undone.");
      if (!confirmed) {
        return;
      }
    }

    if (!isAuthenticated) {
      setDeleteResult({
        status: "error",
        error: "Sign in to delete organizations",
      });
      return;
    }

    setDeleteResult({ status: "loading" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations/${organizationId}`, {
        method: "DELETE",
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setDeleteResult({
          status: "error",
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 403) {
        setDeleteResult({
          status: "error",
          error: "Forbidden (admin role required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setDeleteResult({
          status: "error",
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setDeleteResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      const job = parseJobSubmission(payload);
      if (job === undefined) {
        setDeleteResult({
          status: "error",
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setDeleteResult({
        status: "success",
        data: job,
        payload,
      });
      setJobQueryId(job.jobId);
      setDetailResult((previous) => {
        if (previous.status === "success" && previous.data?.organization.id === organizationId) {
          return { status: "idle" };
        }
        return previous;
      });
      setSelectedOrganizationId((current) => (current === organizationId ? null : current));
      await loadStoredOrganizations();
    } catch (error) {
      setDeleteResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [loadStoredOrganizations, isAuthenticated]);

  const queryJobStatus = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = jobQueryId.trim();
    if (!trimmed) {
      setJobStatusResult({
        status: "error",
        error: "Job ID is required",
      });
      return;
    }

    if (!isAuthenticated) {
      setJobStatusResult({
        status: "error",
        error: "Sign in to check job status",
      });
      return;
    }

    setJobStatusResult({ status: "loading" });
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/jobs/${encodeURIComponent(trimmed)}`,
        {
          credentials: "include",
        },
      );

      const payload = (await readJson(response)) ?? undefined;

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setJobStatusResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      const jobStatus = parseJobStatus(payload);
      if (jobStatus === undefined) {
        setJobStatusResult({
          status: "error",
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setJobStatusResult({
        status: "success",
        data: jobStatus,
        payload,
      });
    } catch (error) {
      setJobStatusResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [jobQueryId, isAuthenticated]);

  const triggerOrganizationSync = useCallback(async (organizationId: number) => {
    if (!isAuthenticated) {
      setSyncJobResult({
        status: "error",
        organizationId,
        error: "Sign in to trigger repository sync",
      });
      return;
    }

    setSyncJobResult({ status: "loading", organizationId });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations/${organizationId}/sync`, {
        method: "POST",
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setSyncJobResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setSyncJobResult({
          status: "error",
          organizationId,
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setSyncJobResult({
          status: "error",
          organizationId,
          error: message,
          payload,
        });
        return;
      }

      const job = parseJobSubmission(payload);
      if (job === undefined) {
        setSyncJobResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setSyncJobResult({
        status: "success",
        organizationId,
        data: job,
        payload,
      });
      setJobQueryId(job.jobId);
    } catch (error) {
      setSyncJobResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated]);

  const loadSyncStatus = useCallback(async (organizationId: number) => {
    if (!isAuthenticated) {
      setSyncStatusResult({
        status: "error",
        organizationId,
        error: "Sign in to fetch sync status",
      });
      return;
    }

    setSyncStatusResult({ status: "loading", organizationId });
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/organizations/${organizationId}/sync/status`,
        {
          credentials: "include",
        },
      );

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setSyncStatusResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setSyncStatusResult({
          status: "error",
          organizationId,
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setSyncStatusResult({
          status: "error",
          organizationId,
          error: message,
          payload,
        });
        return;
      }

      const statuses = parseRepositorySyncStatuses(payload);
      if (statuses === undefined) {
        setSyncStatusResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setSyncStatusResult({
        status: "success",
        organizationId,
        items: statuses,
        payload,
      });
    } catch (error) {
      setSyncStatusResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated]);

  const loadUserProfile = useCallback(async () => {
    setUserProfileResult({ status: "loading" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/users/me`, {
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setUserProfileResult({
          status: "error",
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setUserProfileResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      const profile = parseUserProfile(payload);
      if (!profile) {
        setUserProfileResult({
          status: "error",
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setUserProfileResult({
        status: "success",
        user: profile,
        payload,
      });
    } catch (error) {
      setUserProfileResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, []);

  const deleteCurrentUser = useCallback(async () => {
    setUserDeletionResult({ status: "loading" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/users/me`, {
        method: "DELETE",
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setUserDeletionResult({
          status: "error",
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setUserDeletionResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      const job = parseJobSubmission(payload);
      if (!job) {
        setUserDeletionResult({
          status: "error",
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setUserDeletionResult({
        status: "success",
        data: job,
        payload,
      });
      setJobQueryId(job.jobId);
      await loadSession();
    } catch (error) {
      setUserDeletionResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [loadSession]);

  const fetchDashboardSnapshot = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = dashboardOrganizationId.trim();
    const organizationId = Number.parseInt(trimmed, 10);
    if (Number.isNaN(organizationId)) {
      setDashboardResult({
        status: "error",
        error: "Organization ID must be a number",
      });
      return;
    }

    if (!isAuthenticated) {
      setDashboardResult({
        status: "error",
        organizationId,
        error: "Sign in to load dashboard data",
      });
      return;
    }

    setDashboardResult({ status: "loading", organizationId });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations/${organizationId}/dashboard`, {
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setDashboardResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setDashboardResult({
          status: "error",
          organizationId,
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setDashboardResult({
          status: "error",
          organizationId,
          error: message,
          payload,
        });
        return;
      }

      const data = parseDashboardResponse(payload);
      if (!data) {
        setDashboardResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setDashboardResult({
        status: "success",
        organizationId,
        data,
        payload,
      });
    } catch (error) {
      setDashboardResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [dashboardOrganizationId, isAuthenticated]);

  const submitStatusUpdate = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedOrg = statusForm.organizationId.trim();
    const organizationId = Number.parseInt(trimmedOrg, 10);
    if (Number.isNaN(organizationId)) {
      setStatusUpdateResult({
        status: "error",
        error: "Organization ID must be a number",
      });
      return;
    }

    const trimmedStatus = statusForm.status.trim();
    if (!trimmedStatus) {
      setStatusUpdateResult({
        status: "error",
        error: "Status is required",
      });
      return;
    }

    if (!isAuthenticated) {
      setStatusUpdateResult({
        status: "error",
        organizationId,
        error: "Sign in to submit a status",
      });
      return;
    }

    const payload: Record<string, unknown> = {
      status: trimmedStatus,
    };
    const statusMessage = statusForm.statusMessage.trim();
    if (statusMessage.length > 0) {
      payload.statusMessage = statusMessage;
    }
    const capacityValue = statusForm.capacityHours.trim();
    if (capacityValue.length > 0) {
      const capacityNumber = Number.parseInt(capacityValue, 10);
      if (Number.isNaN(capacityNumber)) {
        setStatusUpdateResult({
          status: "error",
          organizationId,
          error: "Capacity hours must be a number",
        });
        return;
      }
      payload.capacityHours = capacityNumber;
    }
    const availableValue = statusForm.availableMinutes.trim();
    if (availableValue.length > 0) {
      const availableNumber = Number.parseInt(availableValue, 10);
      if (Number.isNaN(availableNumber)) {
        setStatusUpdateResult({
          status: "error",
          organizationId,
          error: "Available minutes must be a number",
        });
        return;
      }
      payload.availableMinutes = availableNumber;
    }
    const dateValue = statusForm.date.trim();
    if (dateValue.length > 0) {
      payload.date = dateValue;
    }

    setStatusUpdateResult({ status: "loading", organizationId });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations/${organizationId}/statuses`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      const data = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setStatusUpdateResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload: data,
        });
        return;
      }

      if (response.status === 404) {
        setStatusUpdateResult({
          status: "error",
          organizationId,
          error: "Organization not found",
          payload: data,
        });
        return;
      }

      if (!response.ok) {
        const message =
          data && typeof data === "object" && "error" in data &&
          typeof (data as Record<string, unknown>).error === "string"
            ? ((data as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setStatusUpdateResult({
          status: "error",
          organizationId,
          error: message,
          payload: data,
        });
        return;
      }

      const parsed = parseStatusUpdateResponse(data);
      if (!parsed) {
        setStatusUpdateResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload: data,
        });
        return;
      }

      setStatusUpdateResult({
        status: "success",
        organizationId,
        data: parsed,
        payload: data,
      });
    } catch (error) {
      setStatusUpdateResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated, statusForm]);

  const loadStatusList = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedOrg = statusListQuery.organizationId.trim();
    const organizationId = Number.parseInt(trimmedOrg, 10);
    if (Number.isNaN(organizationId)) {
      setStatusListResult({
        status: "error",
        error: "Organization ID must be a number",
      });
      return;
    }

    if (!isAuthenticated) {
      setStatusListResult({
        status: "error",
        organizationId,
        error: "Sign in to list statuses",
      });
      return;
    }

    const params = new URLSearchParams();
    const date = statusListQuery.date.trim();
    if (date.length > 0) {
      params.set("date", date);
    }

    setStatusListResult({ status: "loading", organizationId });
    try {
      const query = params.toString();
      const url = query.length > 0
        ? `${API_BASE_URL}/api/organizations/${organizationId}/statuses?${query}`
        : `${API_BASE_URL}/api/organizations/${organizationId}/statuses`;
      const response = await fetch(url, { credentials: "include" });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setStatusListResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setStatusListResult({
          status: "error",
          organizationId,
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setStatusListResult({
          status: "error",
          organizationId,
          error: message,
          payload,
        });
        return;
      }

      const items = parseStatusList(payload);
      if (!items) {
        setStatusListResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setStatusListResult({
        status: "success",
        organizationId,
        items,
        payload,
      });
    } catch (error) {
      setStatusListResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated, statusListQuery]);

  const removeStatus = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const orgValue = (event.currentTarget.elements.namedItem("status-delete-org") as HTMLInputElement | null)?.value ?? "";
    const statusValue = (event.currentTarget.elements.namedItem("status-delete-id") as HTMLInputElement | null)?.value ?? "";
    const organizationId = Number.parseInt(orgValue.trim(), 10);
    const statusId = Number.parseInt(statusValue.trim(), 10);
    if (Number.isNaN(organizationId) || Number.isNaN(statusId)) {
      setStatusDeleteState({
        status: "error",
        error: "Organization ID and status ID must be numbers",
      });
      return;
    }

    if (!isAuthenticated) {
      setStatusDeleteState({
        status: "error",
        organizationId,
        statusId,
        error: "Sign in to delete statuses",
      });
      return;
    }

    setStatusDeleteState({ status: "loading", organizationId, statusId });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations/${organizationId}/statuses/${statusId}`, {
        method: "DELETE",
        credentials: "include",
      });

      if (response.status === 401) {
        const payload = (await readJson(response)) ?? undefined;
        setStatusDeleteState({
          status: "error",
          organizationId,
          statusId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 403) {
        const payload = (await readJson(response)) ?? undefined;
        setStatusDeleteState({
          status: "error",
          organizationId,
          statusId,
          error: "Forbidden (admin role required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        const payload = (await readJson(response)) ?? undefined;
        setStatusDeleteState({
          status: "error",
          organizationId,
          statusId,
          error: "Status not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const payload = (await readJson(response)) ?? undefined;
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setStatusDeleteState({
          status: "error",
          organizationId,
          statusId,
          error: message,
          payload,
        });
        return;
      }

      setStatusDeleteState({
        status: "success",
        organizationId,
        statusId,
      });
    } catch (error) {
      setStatusDeleteState({
        status: "error",
        organizationId,
        statusId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated]);

  const loadActivitySummary = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedOrg = activitySummaryQuery.organizationId.trim();
    const organizationId = Number.parseInt(trimmedOrg, 10);
    if (Number.isNaN(organizationId)) {
      setActivitySummaryResult({
        status: "error",
        error: "Organization ID must be a number",
      });
      return;
    }

    const start = activitySummaryQuery.startDate.trim();
    const end = activitySummaryQuery.endDate.trim();
    if (!start || !end) {
      setActivitySummaryResult({
        status: "error",
        organizationId,
        error: "Start and end dates are required",
      });
      return;
    }

    if (!isAuthenticated) {
      setActivitySummaryResult({
        status: "error",
        organizationId,
        error: "Sign in to fetch activity summary",
      });
      return;
    }

    const params = new URLSearchParams();
    params.set("startDate", start);
    params.set("endDate", end);
    const groupBy = activitySummaryQuery.groupBy.trim();
    if (groupBy.length > 0) {
      params.set("groupBy", groupBy);
    }

    setActivitySummaryResult({ status: "loading", organizationId });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations/${organizationId}/activity/summary?${params.toString()}`, {
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setActivitySummaryResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setActivitySummaryResult({
          status: "error",
          organizationId,
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setActivitySummaryResult({
          status: "error",
          organizationId,
          error: message,
          payload,
        });
        return;
      }

      const items = parseActivitySummary(payload);
      if (!items) {
        setActivitySummaryResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setActivitySummaryResult({
        status: "success",
        organizationId,
        items,
        payload,
      });
    } catch (error) {
      setActivitySummaryResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [activitySummaryQuery, isAuthenticated]);

  const loadCommitFeed = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedOrg = commitFeedQuery.organizationId.trim();
    const organizationId = Number.parseInt(trimmedOrg, 10);
    if (Number.isNaN(organizationId)) {
      setCommitFeedResult({
        status: "error",
        error: "Organization ID must be a number",
      });
      return;
    }

    if (!isAuthenticated) {
      setCommitFeedResult({
        status: "error",
        organizationId,
        error: "Sign in to fetch commit feed",
      });
      return;
    }

    const params = new URLSearchParams();
    const limitValue = commitFeedQuery.limit.trim();
    if (limitValue.length > 0) {
      const limitNumber = Number.parseInt(limitValue, 10);
      if (Number.isNaN(limitNumber)) {
        setCommitFeedResult({
          status: "error",
          organizationId,
          error: "Limit must be a number",
        });
        return;
      }
      params.set("limit", limitNumber.toString());
    }
    const cursorValue = commitFeedQuery.cursor.trim();
    if (cursorValue.length > 0) {
      const cursorNumber = Number.parseInt(cursorValue, 10);
      if (Number.isNaN(cursorNumber)) {
        setCommitFeedResult({
          status: "error",
          organizationId,
          error: "Cursor must be a number",
        });
        return;
      }
      params.set("cursor", cursorNumber.toString());
    }

    setCommitFeedResult({ status: "loading", organizationId });
    try {
      const query = params.toString();
      const url = query.length > 0
        ? `${API_BASE_URL}/api/organizations/${organizationId}/git-commit/feed?${query}`
        : `${API_BASE_URL}/api/organizations/${organizationId}/git-commit/feed`;
      const response = await fetch(url, { credentials: "include" });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setCommitFeedResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setCommitFeedResult({
          status: "error",
          organizationId,
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setCommitFeedResult({
          status: "error",
          organizationId,
          error: message,
          payload,
        });
        return;
      }

      const data = parseCommitFeedResponse(payload);
      if (!data) {
        setCommitFeedResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setCommitFeedResult({
        status: "success",
        organizationId,
        data,
        payload,
      });
    } catch (error) {
      setCommitFeedResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [commitFeedQuery, isAuthenticated]);

  const loadPullFeed = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedOrg = pullFeedQuery.organizationId.trim();
    const organizationId = Number.parseInt(trimmedOrg, 10);
    if (Number.isNaN(organizationId)) {
      setPullFeedResult({
        status: "error",
        error: "Organization ID must be a number",
      });
      return;
    }

    if (!isAuthenticated) {
      setPullFeedResult({
        status: "error",
        organizationId,
        error: "Sign in to fetch pull request feed",
      });
      return;
    }

    const params = new URLSearchParams();
    const limitValue = pullFeedQuery.limit.trim();
    if (limitValue.length > 0) {
      const limitNumber = Number.parseInt(limitValue, 10);
      if (Number.isNaN(limitNumber)) {
        setPullFeedResult({
          status: "error",
          organizationId,
          error: "Limit must be a number",
        });
        return;
      }
      params.set("limit", limitNumber.toString());
    }
    const cursorValue = pullFeedQuery.cursor.trim();
    if (cursorValue.length > 0) {
      const cursorNumber = Number.parseInt(cursorValue, 10);
      if (Number.isNaN(cursorNumber)) {
        setPullFeedResult({
          status: "error",
          organizationId,
          error: "Cursor must be a number",
        });
        return;
      }
      params.set("cursor", cursorNumber.toString());
    }

    setPullFeedResult({ status: "loading", organizationId });
    try {
      const query = params.toString();
      const url = query.length > 0
        ? `${API_BASE_URL}/api/organizations/${organizationId}/pulls/feed?${query}`
        : `${API_BASE_URL}/api/organizations/${organizationId}/pulls/feed`;
      const response = await fetch(url, { credentials: "include" });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setPullFeedResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setPullFeedResult({
          status: "error",
          organizationId,
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setPullFeedResult({
          status: "error",
          organizationId,
          error: message,
          payload,
        });
        return;
      }

      const data = parsePullRequestFeed(payload);
      if (!data) {
        setPullFeedResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setPullFeedResult({
        status: "success",
        organizationId,
        data,
        payload,
      });
    } catch (error) {
      setPullFeedResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated, pullFeedQuery]);

  const loadPullList = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedRepo = pullListQuery.repositoryId.trim();
    const repositoryId = Number.parseInt(trimmedRepo, 10);
    if (Number.isNaN(repositoryId)) {
      setPullListResult({
        status: "error",
        error: "Repository ID must be a number",
      });
      return;
    }

    if (!isAuthenticated) {
      setPullListResult({
        status: "error",
        repositoryId,
        error: "Sign in to list pull requests",
      });
      return;
    }

    const params = new URLSearchParams();
    if (pullListQuery.state.trim().length > 0 && pullListQuery.state !== "all") {
      params.set("state", pullListQuery.state.trim());
    }
    const limitValue = pullListQuery.limit.trim();
    if (limitValue.length > 0) {
      const limitNumber = Number.parseInt(limitValue, 10);
      if (Number.isNaN(limitNumber)) {
        setPullListResult({
          status: "error",
          repositoryId,
          error: "Limit must be a number",
        });
        return;
      }
      params.set("limit", limitNumber.toString());
    }
    const pageValue = pullListQuery.page.trim();
    if (pageValue.length > 0) {
      const pageNumber = Number.parseInt(pageValue, 10);
      if (Number.isNaN(pageNumber)) {
        setPullListResult({
          status: "error",
          repositoryId,
          error: "Page must be a number",
        });
        return;
      }
      params.set("page", pageNumber.toString());
    }

    setPullListResult({ status: "loading", repositoryId });
    try {
      const query = params.toString();
      const url = query.length > 0
        ? `${API_BASE_URL}/api/repositories/${repositoryId}/pulls?${query}`
        : `${API_BASE_URL}/api/repositories/${repositoryId}/pulls`;
      const response = await fetch(url, { credentials: "include" });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setPullListResult({
          status: "error",
          repositoryId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setPullListResult({
          status: "error",
          repositoryId,
          error: "Repository not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setPullListResult({
          status: "error",
          repositoryId,
          error: message,
          payload,
        });
        return;
      }

      const items = parsePullRequestList(payload);
      if (!items) {
        setPullListResult({
          status: "error",
          repositoryId,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setPullListResult({
        status: "success",
        repositoryId,
        items,
        payload,
      });
    } catch (error) {
      setPullListResult({
        status: "error",
        repositoryId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated, pullListQuery]);

  const loadPullDetail = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const repoValue = pullDetailQuery.repositoryId.trim();
    const repositoryId = Number.parseInt(repoValue, 10);
    const pullValue = pullDetailQuery.pullNumber.trim();
    const pullNumber = Number.parseInt(pullValue, 10);
    if (Number.isNaN(repositoryId) || Number.isNaN(pullNumber)) {
      setPullDetailResult({
        status: "error",
        error: "Repository ID and pull number must be numbers",
      });
      return;
    }

    if (!isAuthenticated) {
      setPullDetailResult({
        status: "error",
        repositoryId,
        pullNumber,
        error: "Sign in to fetch pull request detail",
      });
      return;
    }

    setPullDetailResult({ status: "loading", repositoryId, pullNumber });
    try {
      const response = await fetch(`${API_BASE_URL}/api/repositories/${repositoryId}/pulls/${pullNumber}`, {
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setPullDetailResult({
          status: "error",
          repositoryId,
          pullNumber,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setPullDetailResult({
          status: "error",
          repositoryId,
          pullNumber,
          error: "Pull request not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setPullDetailResult({
          status: "error",
          repositoryId,
          pullNumber,
          error: message,
          payload,
        });
        return;
      }

      const data = parsePullRequestDetail(payload);
      if (!data) {
        setPullDetailResult({
          status: "error",
          repositoryId,
          pullNumber,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setPullDetailResult({
        status: "success",
        repositoryId,
        pullNumber,
        data,
        payload,
      });
    } catch (error) {
      setPullDetailResult({
        status: "error",
        repositoryId,
        pullNumber,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated, pullDetailQuery]);

  const loadPullFiles = useCallback(async () => {
    const repoValue = pullDetailQuery.repositoryId.trim();
    const pullValue = pullDetailQuery.pullNumber.trim();
    const repositoryId = Number.parseInt(repoValue, 10);
    const pullNumber = Number.parseInt(pullValue, 10);
    if (Number.isNaN(repositoryId) || Number.isNaN(pullNumber)) {
      setPullFilesResult({
        status: "error",
        error: "Repository ID and pull number must be numbers",
      });
      return;
    }

    if (!isAuthenticated) {
      setPullFilesResult({
        status: "error",
        repositoryId,
        pullNumber,
        error: "Sign in to fetch pull request files",
      });
      return;
    }

    setPullFilesResult({ status: "loading", repositoryId, pullNumber });
    try {
      const response = await fetch(`${API_BASE_URL}/api/repositories/${repositoryId}/pulls/${pullNumber}/files`, {
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setPullFilesResult({
          status: "error",
          repositoryId,
          pullNumber,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setPullFilesResult({
          status: "error",
          repositoryId,
          pullNumber,
          error: "Pull request not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setPullFilesResult({
          status: "error",
          repositoryId,
          pullNumber,
          error: message,
          payload,
        });
        return;
      }

      const items = parsePullRequestFiles(payload);
      if (!items) {
        setPullFilesResult({
          status: "error",
          repositoryId,
          pullNumber,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setPullFilesResult({
        status: "success",
        repositoryId,
        pullNumber,
        items,
        payload,
      });
    } catch (error) {
      setPullFilesResult({
        status: "error",
        repositoryId,
        pullNumber,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated, pullDetailQuery]);

  const triggerPullSync = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const repoValue = pullSyncQuery.repositoryId.trim();
    const repositoryId = Number.parseInt(repoValue, 10);
    if (Number.isNaN(repositoryId)) {
      setPullSyncResult({
        status: "error",
        error: "Repository ID must be a number",
      });
      return;
    }

    if (!isAuthenticated) {
      setPullSyncResult({
        status: "error",
        error: "Sign in to trigger a pull request sync",
      });
      return;
    }

    setPullSyncResult({ status: "loading" });
    try {
      const response = await fetch(`${API_BASE_URL}/api/repositories/${repositoryId}/pulls/sync`, {
        method: "POST",
        credentials: "include",
      });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setPullSyncResult({
          status: "error",
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setPullSyncResult({
          status: "error",
          error: "Repository not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setPullSyncResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      const job = parseJobSubmission(payload);
      if (!job) {
        setPullSyncResult({
          status: "error",
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setPullSyncResult({
        status: "success",
        data: job,
        payload,
      });
      setJobQueryId(job.jobId);
    } catch (error) {
      setPullSyncResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated, pullSyncQuery]);

  const submitComment = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedOrg = commentForm.organizationId.trim();
    const organizationId = Number.parseInt(trimmedOrg, 10);
    if (Number.isNaN(organizationId)) {
      setCommentCreateResult({
        status: "error",
        error: "Organization ID must be a number",
      });
      return;
    }

    const targetType = commentForm.targetType.trim();
    if (!targetType) {
      setCommentCreateResult({
        status: "error",
        organizationId,
        error: "Target type is required",
      });
      return;
    }

    const content = commentForm.content.trim();
    if (!content) {
      setCommentCreateResult({
        status: "error",
        organizationId,
        error: "Comment content is required",
      });
      return;
    }

    if (!isAuthenticated) {
      setCommentCreateResult({
        status: "error",
        organizationId,
        error: "Sign in to create comments",
      });
      return;
    }

    const payload: Record<string, unknown> = {
      targetType,
      content,
    };
    const targetIdValue = commentForm.targetId.trim();
    if (targetIdValue.length > 0) {
      const targetId = Number.parseInt(targetIdValue, 10);
      if (Number.isNaN(targetId)) {
        setCommentCreateResult({
          status: "error",
          organizationId,
          error: "Target ID must be a number",
        });
        return;
      }
      payload.targetId = targetId;
    }
    const parentIdValue = commentForm.parentCommentId.trim();
    if (parentIdValue.length > 0) {
      const parentId = Number.parseInt(parentIdValue, 10);
      if (Number.isNaN(parentId)) {
        setCommentCreateResult({
          status: "error",
          organizationId,
          error: "Parent comment ID must be a number",
        });
        return;
      }
      payload.parentCommentId = parentId;
    }

    setCommentCreateResult({ status: "loading", organizationId });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations/${organizationId}/comments`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(payload),
      });

      const data = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setCommentCreateResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload: data,
        });
        return;
      }

      if (!response.ok) {
        const message =
          data && typeof data === "object" && "error" in data &&
          typeof (data as Record<string, unknown>).error === "string"
            ? ((data as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setCommentCreateResult({
          status: "error",
          organizationId,
          error: message,
          payload: data,
        });
        return;
      }

      const result = parseCommentCreateResult(data);
      if (!result) {
        setCommentCreateResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload: data,
        });
        return;
      }

      setCommentCreateResult({
        status: "success",
        organizationId,
        data: result,
        payload: data,
      });
    } catch (error) {
      setCommentCreateResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [commentForm, isAuthenticated]);

  const loadComments = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmedOrg = commentListQuery.organizationId.trim();
    const organizationId = Number.parseInt(trimmedOrg, 10);
    if (Number.isNaN(organizationId)) {
      setCommentListResult({
        status: "error",
        error: "Organization ID must be a number",
      });
      return;
    }

    if (!isAuthenticated) {
      setCommentListResult({
        status: "error",
        organizationId,
        error: "Sign in to list comments",
      });
      return;
    }

    const params = new URLSearchParams();
    const targetType = commentListQuery.targetType.trim();
    if (targetType.length > 0) {
      params.set("targetType", targetType);
    }
    const targetIdValue = commentListQuery.targetId.trim();
    if (targetIdValue.length > 0) {
      const targetId = Number.parseInt(targetIdValue, 10);
      if (Number.isNaN(targetId)) {
        setCommentListResult({
          status: "error",
          organizationId,
          error: "Target ID must be a number",
        });
        return;
      }
      params.set("targetId", targetId.toString());
    }

    setCommentListResult({ status: "loading", organizationId });
    try {
      const query = params.toString();
      const url = query.length > 0
        ? `${API_BASE_URL}/api/organizations/${organizationId}/comments?${query}`
        : `${API_BASE_URL}/api/organizations/${organizationId}/comments`;
      const response = await fetch(url, { credentials: "include" });

      const payload = (await readJson(response)) ?? undefined;

      if (response.status === 401) {
        setCommentListResult({
          status: "error",
          organizationId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        setCommentListResult({
          status: "error",
          organizationId,
          error: "Organization not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setCommentListResult({
          status: "error",
          organizationId,
          error: message,
          payload,
        });
        return;
      }

      const items = parseCommentList(payload);
      if (!items) {
        setCommentListResult({
          status: "error",
          organizationId,
          error: "Unexpected response payload",
          payload,
        });
        return;
      }

      setCommentListResult({
        status: "success",
        organizationId,
        items,
        payload,
      });
    } catch (error) {
      setCommentListResult({
        status: "error",
        organizationId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [commentListQuery, isAuthenticated]);

  const removeComment = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const orgValue = (event.currentTarget.elements.namedItem("comment-delete-org") as HTMLInputElement | null)?.value ?? "";
    const commentValue = (event.currentTarget.elements.namedItem("comment-delete-id") as HTMLInputElement | null)?.value ?? "";
    const organizationId = Number.parseInt(orgValue.trim(), 10);
    const commentId = Number.parseInt(commentValue.trim(), 10);
    if (Number.isNaN(organizationId) || Number.isNaN(commentId)) {
      setCommentDeleteState({
        status: "error",
        error: "Organization ID and comment ID must be numbers",
      });
      return;
    }

    if (!isAuthenticated) {
      setCommentDeleteState({
        status: "error",
        organizationId,
        commentId,
        error: "Sign in to delete comments",
      });
      return;
    }

    setCommentDeleteState({ status: "loading", organizationId, commentId });
    try {
      const response = await fetch(`${API_BASE_URL}/api/organizations/${organizationId}/comments/${commentId}`, {
        method: "DELETE",
        credentials: "include",
      });

      if (response.status === 401) {
        const payload = (await readJson(response)) ?? undefined;
        setCommentDeleteState({
          status: "error",
          organizationId,
          commentId,
          error: "Unauthorized (login required)",
          payload,
        });
        return;
      }

      if (response.status === 403) {
        const payload = (await readJson(response)) ?? undefined;
        setCommentDeleteState({
          status: "error",
          organizationId,
          commentId,
          error: "Forbidden (admin role required)",
          payload,
        });
        return;
      }

      if (response.status === 404) {
        const payload = (await readJson(response)) ?? undefined;
        setCommentDeleteState({
          status: "error",
          organizationId,
          commentId,
          error: "Comment not found",
          payload,
        });
        return;
      }

      if (!response.ok) {
        const payload = (await readJson(response)) ?? undefined;
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setCommentDeleteState({
          status: "error",
          organizationId,
          commentId,
          error: message,
          payload,
        });
        return;
      }

      setCommentDeleteState({
        status: "success",
        organizationId,
        commentId,
      });
    } catch (error) {
      setCommentDeleteState({
        status: "error",
        organizationId,
        commentId,
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [isAuthenticated]);

  const submitWebhook = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setWebhookResult({ status: "loading" });
    try {
      const headers: Record<string, string> = {};
      if (webhookForm.eventType.trim().length > 0) {
        headers["X-GitHub-Event"] = webhookForm.eventType.trim();
      }
      if (webhookForm.deliveryId.trim().length > 0) {
        headers["X-GitHub-Delivery"] = webhookForm.deliveryId.trim();
      }
      if (webhookForm.signature.trim().length > 0) {
        headers["X-Hub-Signature-256"] = webhookForm.signature.trim();
      }

      const response = await fetch(`${API_BASE_URL}/api/webhooks/github`, {
        method: "POST",
        headers,
        body: webhookForm.payload,
      });

      const payload = (await readJson(response)) ?? undefined;

      if (!response.ok) {
        const message =
          payload && typeof payload === "object" && "error" in payload &&
          typeof (payload as Record<string, unknown>).error === "string"
            ? ((payload as Record<string, unknown>).error as string)
            : `Request failed (${response.status})`;

        setWebhookResult({
          status: "error",
          error: message,
          payload,
        });
        return;
      }

      setWebhookResult({
        status: "success",
        payload,
      });
    } catch (error) {
      setWebhookResult({
        status: "error",
        error: error instanceof Error ? error.message : "Unknown error",
      });
    }
  }, [webhookForm]);

  return (
    <div className="min-h-screen bg-slate-100 py-12">
      <main className="mx-auto flex max-w-4xl flex-col gap-8 px-4">
        <header className="space-y-2">
          <h1 className="text-3xl font-semibold text-slate-900">
            Team Progress Viz Backend API Playground
          </h1>
          <p className="text-sm text-slate-600">
            Requests are sent to <span className="font-mono">{API_BASE_URL}</span> with
            cookies included.
          </p>
        </header>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                7. Manage stored organizations
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">GET /api/organizations</code>,{" "}
                <code className="font-mono text-xs">POST /api/organizations</code>,{" "}
                <code className="font-mono text-xs">DELETE /api/organizations/&lt;id&gt;</code>, and{" "}
                <code className="font-mono text-xs">GET /api/jobs/&lt;jobId&gt;</code> to verify database-backed flows.
              </p>
            </div>
            <StatusBadge status={storedOrganizationsResult.status} />
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={loadStoredOrganizations}
              disabled={storedOrganizationsResult.status === "loading"}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
            >
              Fetch registered organizations
            </button>
            {storedOrganizationsResult.status === "success" && storedOrganizationsResult.items && (
              <span className="text-xs text-slate-500">
                {storedOrganizationsResult.items.length} organization(s) available.
              </span>
            )}
          </div>

          {storedOrganizationsResult.error && (
            <p className="mt-3 text-sm text-rose-600">{storedOrganizationsResult.error}</p>
          )}

          {storedOrganizationsResult.status === "success" && (
            storedOrganizationsResult.items && storedOrganizationsResult.items.length > 0 ? (
              <div className="mt-4 overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">ID</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">GitHub ID</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Login</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Name</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Description</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {storedOrganizationsResult.items.map((organization) => {
                      const isSelected = selectedOrganizationId === organization.id;
                      return (
                        <tr
                          key={`stored-${organization.id}-${organization.login}`}
                          className={isSelected ? "bg-blue-50" : "hover:bg-slate-50"}
                        >
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">{organization.id}</td>
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">{organization.githubId}</td>
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">{organization.login}</td>
                          <td className="px-3 py-2 text-slate-600">{organization.name ?? "—"}</td>
                          <td className="px-3 py-2 text-slate-600">
                            {organization.description ? organization.description : "—"}
                          </td>
                          <td className="px-3 py-2 text-xs text-slate-600">
                            <button
                              type="button"
                              onClick={() => {
                                void fetchOrganizationDetail(organization.id);
                              }}
                              disabled={
                                detailResult.status === "loading" && selectedOrganizationId === organization.id
                              }
                              className="rounded border border-slate-300 px-3 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-40"
                            >
                              Load detail
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                void triggerOrganizationSync(organization.id);
                              }}
                              disabled={
                                syncJobResult.status === "loading" &&
                                syncJobResult.organizationId === organization.id
                              }
                              className="ml-2 rounded border border-blue-300 px-3 py-1 text-xs font-medium text-blue-700 hover:bg-blue-50 disabled:opacity-40"
                            >
                              Sync now
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                void loadSyncStatus(organization.id);
                              }}
                              disabled={
                                syncStatusResult.status === "loading" &&
                                syncStatusResult.organizationId === organization.id
                              }
                              className="ml-2 rounded border border-emerald-300 px-3 py-1 text-xs font-medium text-emerald-700 hover:bg-emerald-50 disabled:opacity-40"
                            >
                              View sync status
                            </button>
                            <button
                              type="button"
                              onClick={() => {
                                void deleteOrganization(organization.id);
                              }}
                              disabled={deleteResult.status === "loading"}
                              className="ml-2 rounded border border-rose-300 px-3 py-1 text-xs font-medium text-rose-700 hover:bg-rose-50 disabled:opacity-40"
                            >
                              Delete
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="mt-3 text-sm text-slate-600">
                No organizations registered yet.
              </p>
            )
          )}

          {storedOrganizationsResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(storedOrganizationsResult.payload)}
            </pre>
          )}

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">
                Register a new organization
              </h3>
              <StatusBadge status={registrationResult.status} />
            </div>

            <form
              onSubmit={submitOrganizationRegistration}
              className="mt-4 flex flex-col gap-4 md:flex-row md:items-end"
            >
              <label className="flex flex-1 flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Organization login</span>
                <input
                  type="text"
                  value={registrationForm.login}
                  onChange={(event) =>
                    setRegistrationForm((previous) => ({ ...previous, login: event.target.value }))
                  }
                  placeholder="octo-org"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex flex-1 flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Default link URL (optional)</span>
                <input
                  type="url"
                  value={registrationForm.defaultLinkUrl}
                  onChange={(event) =>
                    setRegistrationForm((previous) => ({
                      ...previous,
                      defaultLinkUrl: event.target.value,
                    }))
                  }
                  placeholder="https://example.com/dashboard"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <button
                type="submit"
                disabled={registrationResult.status === "loading"}
                className="w-full rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white md:w-auto disabled:opacity-40"
              >
                Register organization
              </button>
            </form>

            {registrationResult.error && (
              <p className="mt-3 text-sm text-rose-600">{registrationResult.error}</p>
            )}

            {registrationResult.status === "success" && registrationResult.data && (
              <div className="mt-4 rounded border border-emerald-200 bg-emerald-50 p-4 text-sm text-slate-800">
                <p className="font-medium">
                  Job {registrationResult.data.jobId} accepted ({registrationResult.data.status})
                </p>
                <p className="mt-1 text-slate-600">
                  Organization ID: {registrationResult.data.organizationId} · GitHub ID: {registrationResult.data.githubId}
                </p>
              </div>
            )}

            {registrationResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(registrationResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">
                Organization detail
              </h3>
              <StatusBadge status={detailResult.status} />
            </div>

            <p className="mt-3 text-sm text-slate-600">
              Use the table above to load detail for a specific organization.
            </p>

            {detailResult.error && (
              <p className="mt-3 text-sm text-rose-600">{detailResult.error}</p>
            )}

            {detailResult.status === "success" && detailResult.data && (
              <div className="mt-4 space-y-3 rounded border border-slate-200 bg-slate-50 p-4 text-sm text-slate-800">
                <div>
                  <div className="font-semibold text-slate-900">
                    {detailResult.data.organization.name ?? detailResult.data.organization.login}
                  </div>
                  <div className="text-xs font-mono text-slate-600">
                    {detailResult.data.organization.login} · #{detailResult.data.organization.id}
                  </div>
                </div>
                {detailResult.data.organization.description && (
                  <p className="text-slate-700">{detailResult.data.organization.description}</p>
                )}
                <div className="text-slate-700">
                  Members: {detailResult.data.members.length} · Repositories: {detailResult.data.repositories.length}
                </div>
                <div className="text-slate-700">
                  Latest activity (7d): {detailResult.data.activitySummaryLast7Days.commitCount} commits /{" "}
                  {detailResult.data.activitySummaryLast7Days.additions} additions /{" "}
                  {detailResult.data.activitySummaryLast7Days.deletions} deletions
                </div>
                {detailResult.data.organization.defaultLinkUrl && (
                  <a
                    href={detailResult.data.organization.defaultLinkUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-blue-600 underline hover:text-blue-800"
                  >
                    Open default link
                  </a>
                )}
              </div>
            )}

            {detailResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(detailResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">
                Manual sync jobs
              </h3>
              <StatusBadge status={syncJobResult.status} />
            </div>

            <p className="mt-3 text-sm text-slate-600">
              Launch a sync from the table above to queue repository updates for the selected organization.
            </p>

            {syncJobResult.error && (
              <p className="mt-3 text-sm text-rose-600">{syncJobResult.error}</p>
            )}

            {syncJobResult.status === "success" && syncJobResult.data && (
              <div className="mt-4 rounded border border-blue-200 bg-blue-50 p-4 text-sm text-slate-800">
                <p className="font-medium">
                  Job {syncJobResult.data.jobId} ({syncJobResult.data.status})
                </p>
                {typeof syncJobResult.organizationId === "number" && (
                  <p className="mt-1 text-slate-600">
                    Organization ID: {syncJobResult.organizationId}
                  </p>
                )}
                <p className="mt-1 text-slate-600">
                  Track progress with the job status tool below.
                </p>
              </div>
            )}

            {syncJobResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(syncJobResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">
                Repository sync status
              </h3>
              <StatusBadge status={syncStatusResult.status} />
            </div>

            <p className="mt-3 text-sm text-slate-600">
              Fetch latest sync records for an organization to review commit markers and errors.
            </p>

            {syncStatusResult.error && (
              <p className="mt-3 text-sm text-rose-600">{syncStatusResult.error}</p>
            )}

            {syncStatusResult.status === "success" && syncStatusResult.items && (
              syncStatusResult.items.length > 0 ? (
                <div className="mt-4 overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Repository</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Last synced</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Commit SHA</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Error</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {syncStatusResult.items.map((status) => (
                        <tr key={`sync-${status.repositoryId}-${status.repositoryFullName}`}>
                          <td className="px-3 py-2 text-xs text-slate-700">
                            <div className="font-mono text-xs text-slate-600">{status.repositoryId}</div>
                            <div>{status.repositoryFullName}</div>
                          </td>
                          <td className="px-3 py-2 text-xs text-slate-600">
                            {status.lastSyncedAt ?? "—"}
                          </td>
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">
                            {status.lastSyncedCommitSha ?? "—"}
                          </td>
                          <td className="px-3 py-2 text-xs text-rose-600">
                            {status.errorMessage ?? "—"}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="mt-3 text-sm text-slate-600">
                  No sync records found for this organization yet.
                </p>
              )
            )}

            {syncStatusResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(syncStatusResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">
                Deletion jobs
              </h3>
              <StatusBadge status={deleteResult.status} />
            </div>

            <p className="mt-3 text-sm text-slate-600">
              Trigger deletion from the organization table when you are ready.
            </p>

            {deleteResult.error && (
              <p className="mt-3 text-sm text-rose-600">{deleteResult.error}</p>
            )}

            {deleteResult.status === "success" && deleteResult.data && (
              <div className="mt-4 rounded border border-amber-200 bg-amber-50 p-4 text-sm text-slate-800">
                <p className="font-medium">
                  Job {deleteResult.data.jobId} ({deleteResult.data.status})
                </p>
                <p className="mt-1 text-slate-600">
                  Track progress below with the job status form.
                </p>
              </div>
            )}

            {deleteResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(deleteResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">
                Check job status
              </h3>
              <StatusBadge status={jobStatusResult.status} />
            </div>

            <form
              onSubmit={queryJobStatus}
              className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center"
            >
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-sm">
                <span className="font-medium text-slate-900">Job ID</span>
                <input
                  type="text"
                  value={jobQueryId}
                  onChange={(event) => setJobQueryId(event.target.value)}
                  placeholder="job-sync-org-1234"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <button
                type="submit"
                disabled={jobStatusResult.status === "loading"}
                className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
              >
                Fetch job status
              </button>
            </form>

            {jobStatusResult.error && (
              <p className="mt-3 text-sm text-rose-600">{jobStatusResult.error}</p>
            )}

            {jobStatusResult.status === "success" && jobStatusResult.data && (
              <div className="mt-4 space-y-1 rounded border border-blue-200 bg-blue-50 p-4 text-sm text-slate-800">
                <div className="font-medium">Job {jobStatusResult.data.jobId}</div>
                <div className="text-slate-600">
                  Type: <span className="font-mono text-xs">{jobStatusResult.data.type}</span>
                </div>
                <div className="text-slate-600">
                  Status: <span className="font-mono text-xs uppercase">{jobStatusResult.data.status}</span>
                </div>
                <div className="text-slate-600">
                  Created: <span className="font-mono text-xs">{jobStatusResult.data.createdAt ?? "—"}</span>
                </div>
                <div className="text-slate-600">
                  Started: <span className="font-mono text-xs">{jobStatusResult.data.startedAt ?? "—"}</span>
                </div>
                <div className="text-slate-600">
                  Finished: <span className="font-mono text-xs">{jobStatusResult.data.finishedAt ?? "—"}</span>
                </div>
                {jobStatusResult.data.errorMessage && (
                  <p className="text-rose-600">{jobStatusResult.data.errorMessage}</p>
                )}
              </div>
            )}

            {jobStatusResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(jobStatusResult.payload)}
              </pre>
            )}
          </div>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                Manage application user account
              </h2>
              <p className="text-sm text-slate-600">
                Fetch the persisted profile via <code className="font-mono text-xs">GET /api/users/me</code> or queue deletion with <code className="font-mono text-xs">DELETE /api/users/me</code>.
              </p>
            </div>
            <StatusBadge status={userProfileResult.status} />
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={() => {
                void loadUserProfile();
              }}
              disabled={userProfileResult.status === "loading"}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
            >
              Fetch user profile
            </button>
            <button
              type="button"
              onClick={() => {
                void deleteCurrentUser();
              }}
              disabled={userDeletionResult.status === "loading"}
              className="rounded bg-rose-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
            >
              Delete account
            </button>
          </div>

          {userProfileResult.error && (
            <p className="mt-3 text-sm text-rose-600">{userProfileResult.error}</p>
          )}

          {userProfileResult.status === "success" && userProfileResult.user && (
            <div className="mt-4 flex items-center gap-4 rounded border border-slate-200 bg-slate-50 p-4">
              <div>
                <p className="text-sm font-semibold text-slate-900">{userProfileResult.user.name}</p>
                <p className="text-sm text-slate-600">{userProfileResult.user.login}</p>
                <p className="text-xs text-slate-500">User ID {userProfileResult.user.id} · GitHub ID {userProfileResult.user.githubId}</p>
              </div>
              <a
                href={userProfileResult.user.avatarUrl}
                target="_blank"
                rel="noreferrer"
                className="text-xs text-blue-600 underline hover:text-blue-800"
              >
                View avatar
              </a>
            </div>
          )}

          {userProfileResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(userProfileResult.payload)}
            </pre>
          )}

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">Account deletion job</h3>
              <StatusBadge status={userDeletionResult.status} />
            </div>

            {userDeletionResult.error && (
              <p className="mt-3 text-sm text-rose-600">{userDeletionResult.error}</p>
            )}

            {userDeletionResult.status === "success" && userDeletionResult.data && (
              <div className="mt-4 rounded border border-amber-200 bg-amber-50 p-4 text-sm text-slate-800">
                <p className="font-medium text-slate-900">
                  Job {userDeletionResult.data.jobId} ({userDeletionResult.data.status})
                </p>
                <p className="mt-1 text-slate-700">Monitor via the job status tool below.</p>
              </div>
            )}

            {userDeletionResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(userDeletionResult.payload)}
              </pre>
            )}
          </div>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                Webhook replay tester
              </h2>
              <p className="text-sm text-slate-600">
                Sends arbitrary payloads to <code className="font-mono text-xs">POST /api/webhooks/github</code> with optional GitHub headers.
              </p>
            </div>
            <StatusBadge status={webhookResult.status} />
          </div>

          <form
            onSubmit={submitWebhook}
            className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2"
          >
            <label className="flex flex-col gap-1 text-sm text-slate-700">
              <span className="font-medium text-slate-900">Event type header</span>
              <input
                type="text"
                value={webhookForm.eventType}
                onChange={(event) =>
                  setWebhookForm((previous) => ({ ...previous, eventType: event.target.value }))
                }
                placeholder="pull_request"
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm text-slate-700">
              <span className="font-medium text-slate-900">Delivery ID header</span>
              <input
                type="text"
                value={webhookForm.deliveryId}
                onChange={(event) =>
                  setWebhookForm((previous) => ({ ...previous, deliveryId: event.target.value }))
                }
                placeholder="f0b1c2d3"
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              />
            </label>
            <label className="flex flex-col gap-1 text-sm text-slate-700">
              <span className="font-medium text-slate-900">Signature header</span>
              <input
                type="text"
                value={webhookForm.signature}
                onChange={(event) =>
                  setWebhookForm((previous) => ({ ...previous, signature: event.target.value }))
                }
                placeholder="sha256=..."
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              />
            </label>
            <label className="md:col-span-2">
              <span className="text-sm font-medium text-slate-900">Payload</span>
              <textarea
                value={webhookForm.payload}
                onChange={(event) =>
                  setWebhookForm((previous) => ({ ...previous, payload: event.target.value }))
                }
                className="mt-1 h-48 w-full rounded border border-slate-300 px-3 py-2 font-mono text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              />
            </label>
            <button
              type="submit"
              disabled={webhookResult.status === "loading"}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40 md:col-span-2"
            >
              Send webhook payload
            </button>
          </form>

          {webhookResult.error && (
            <p className="mt-3 text-sm text-rose-600">{webhookResult.error}</p>
          )}

          {webhookResult.status === "success" && (
            <p className="mt-3 text-sm text-emerald-600">Webhook accepted by the backend.</p>
          )}

          {webhookResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(webhookResult.payload)}
            </pre>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                Comment moderation
              </h2>
              <p className="text-sm text-slate-600">
                Interact with <code className="font-mono text-xs">POST /api/organizations/&lt;id&gt;/comments</code>, <code className="font-mono text-xs">GET /api/organizations/&lt;id&gt;/comments</code>, and <code className="font-mono text-xs">DELETE /api/organizations/&lt;id&gt;/comments/&lt;commentId&gt;</code>.
              </p>
            </div>
            <StatusBadge status={commentCreateResult.status} />
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">Submit a comment</h3>
              <StatusBadge status={commentCreateResult.status} />
            </div>

            <form
              onSubmit={submitComment}
              className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2"
            >
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Organization ID</span>
                <input
                  type="text"
                  value={commentForm.organizationId}
                  onChange={(event) =>
                    setCommentForm((previous) => ({ ...previous, organizationId: event.target.value }))
                  }
                  placeholder="123"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Target type</span>
                <input
                  type="text"
                  value={commentForm.targetType}
                  onChange={(event) =>
                    setCommentForm((previous) => ({ ...previous, targetType: event.target.value }))
                  }
                  placeholder="PULL_REQUEST or STATUS"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Target ID (optional)</span>
                <input
                  type="text"
                  value={commentForm.targetId}
                  onChange={(event) =>
                    setCommentForm((previous) => ({ ...previous, targetId: event.target.value }))
                  }
                  placeholder="456"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Parent comment ID (optional)</span>
                <input
                  type="text"
                  value={commentForm.parentCommentId}
                  onChange={(event) =>
                    setCommentForm((previous) => ({ ...previous, parentCommentId: event.target.value }))
                  }
                  placeholder="789"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <label className="md:col-span-2">
                <span className="text-sm font-medium text-slate-900">Comment content</span>
                <textarea
                  value={commentForm.content}
                  onChange={(event) =>
                    setCommentForm((previous) => ({ ...previous, content: event.target.value }))
                  }
                  placeholder="Sharing progress on the latest integration..."
                  className="mt-1 h-24 w-full rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <button
                type="submit"
                disabled={commentCreateResult.status === "loading"}
                className="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-40 md:col-span-2"
              >
                Submit comment
              </button>
            </form>

            {commentCreateResult.error && (
              <p className="mt-3 text-sm text-rose-600">{commentCreateResult.error}</p>
            )}

            {commentCreateResult.status === "success" && commentCreateResult.data && (
              <div className="mt-4 rounded border border-emerald-200 bg-emerald-50 p-4 text-sm text-slate-800">
                <p className="font-medium text-slate-900">
                  Comment {commentCreateResult.data.commentId} created.
                </p>
                <p className="mt-1 text-slate-700">Created at {commentCreateResult.data.createdAt ?? "—"}</p>
              </div>
            )}

            {commentCreateResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(commentCreateResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">List comments</h3>
              <StatusBadge status={commentListResult.status} />
            </div>

            <form
              onSubmit={loadComments}
              className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end"
            >
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Organization ID</span>
                <input
                  type="text"
                  value={commentListQuery.organizationId}
                  onChange={(event) =>
                    setCommentListQuery((previous) => ({ ...previous, organizationId: event.target.value }))
                  }
                  placeholder="123"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Target type (optional)</span>
                <input
                  type="text"
                  value={commentListQuery.targetType}
                  onChange={(event) =>
                    setCommentListQuery((previous) => ({ ...previous, targetType: event.target.value }))
                  }
                  placeholder="PULL_REQUEST"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Target ID (optional)</span>
                <input
                  type="text"
                  value={commentListQuery.targetId}
                  onChange={(event) =>
                    setCommentListQuery((previous) => ({ ...previous, targetId: event.target.value }))
                  }
                  placeholder="456"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <button
                type="submit"
                disabled={commentListResult.status === "loading"}
                className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
              >
                Fetch comments
              </button>
            </form>

            {commentListResult.error && (
              <p className="mt-3 text-sm text-rose-600">{commentListResult.error}</p>
            )}

            {commentListResult.status === "success" && commentListResult.items && (
              commentListResult.items.length > 0 ? (
                <div className="mt-4 overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Comment</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Target</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Created</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {commentListResult.items.map((comment) => (
                        <tr key={`comment-${comment.commentId}`} className="hover:bg-slate-50">
                          <td className="px-3 py-2 text-slate-600">
                            <div className="font-medium text-slate-900">{comment.login ?? comment.name ?? "—"}</div>
                            <div>{comment.content ?? "No content"}</div>
                          </td>
                          <td className="px-3 py-2 text-slate-600">
                            <div>{comment.targetType ?? "—"}</div>
                            <div className="text-xs text-slate-500">ID: {comment.targetId ?? "—"}</div>
                          </td>
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">{comment.createdAt ?? "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="mt-3 text-sm text-slate-600">No comments were found.</p>
              )
            )}

            {commentListResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(commentListResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">Delete a comment</h3>
              <StatusBadge status={commentDeleteState.status} />
            </div>

            <form
              onSubmit={removeComment}
              className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end"
            >
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Organization ID</span>
                <input
                  type="text"
                  name="comment-delete-org"
                  placeholder="123"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Comment ID</span>
                <input
                  type="text"
                  name="comment-delete-id"
                  placeholder="987"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <button
                type="submit"
                disabled={commentDeleteState.status === "loading"}
                className="w-full rounded bg-rose-600 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
              >
                Delete comment
              </button>
            </form>

            {commentDeleteState.status === "success" && (
              <p className="mt-3 text-sm text-emerald-600">
                Comment {commentDeleteState.commentId} deleted.
              </p>
            )}

            {commentDeleteState.error && (
              <p className="mt-3 text-sm text-rose-600">{commentDeleteState.error}</p>
            )}

            {commentDeleteState.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(commentDeleteState.payload)}
              </pre>
            )}
          </div>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                Pull request tracking
              </h2>
              <p className="text-sm text-slate-600">
                Cover organization feeds, repository listings, detailed records, and sync jobs for pull requests.
              </p>
            </div>
            <StatusBadge status={pullFeedResult.status} />
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">Organization pull feed</h3>
              <StatusBadge status={pullFeedResult.status} />
            </div>

            <form
              onSubmit={loadPullFeed}
              className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end"
            >
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Organization ID</span>
                <input
                  type="text"
                  value={pullFeedQuery.organizationId}
                  onChange={(event) =>
                    setPullFeedQuery((previous) => ({ ...previous, organizationId: event.target.value }))
                  }
                  placeholder="123"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-[9rem]">
                <span className="font-medium text-slate-900">Limit</span>
                <input
                  type="text"
                  value={pullFeedQuery.limit}
                  onChange={(event) =>
                    setPullFeedQuery((previous) => ({ ...previous, limit: event.target.value }))
                  }
                  placeholder="20"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-[12rem]">
                <span className="font-medium text-slate-900">Cursor</span>
                <input
                  type="text"
                  value={pullFeedQuery.cursor}
                  onChange={(event) =>
                    setPullFeedQuery((previous) => ({ ...previous, cursor: event.target.value }))
                  }
                  placeholder="1693412400000"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <button
                type="submit"
                disabled={pullFeedResult.status === "loading"}
                className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
              >
                Fetch pull feed
              </button>
            </form>

            {pullFeedResult.error && (
              <p className="mt-3 text-sm text-rose-600">{pullFeedResult.error}</p>
            )}

            {pullFeedResult.status === "success" && pullFeedResult.data && (
              <div className="mt-4 space-y-3">
                <div className="overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Pull request</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Repository</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">State</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Updated</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {pullFeedResult.data.items.map((item) => (
                        <tr key={`pull-feed-${item.id}-${item.number}`} className="hover:bg-slate-50">
                          <td className="px-3 py-2 text-slate-600">
                            <div className="font-medium text-slate-900">
                              #{item.number} · {item.title ?? "Untitled"}
                            </div>
                            {item.url && (
                              <a
                                href={item.url}
                                target="_blank"
                                rel="noreferrer"
                                className="text-xs text-blue-600 underline hover:text-blue-800"
                              >
                                View on GitHub
                              </a>
                            )}
                          </td>
                          <td className="px-3 py-2 text-slate-600">{item.repositoryFullName ?? "—"}</td>
                          <td className="px-3 py-2 text-slate-600">{item.state ?? "—"}</td>
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">{item.updatedAt ?? "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                <div className="rounded border border-slate-200 bg-slate-50 p-4 text-xs text-slate-600">
                  <div>Next cursor: {pullFeedResult.data.nextCursor ?? "—"}</div>
                </div>
              </div>
            )}

            {pullFeedResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(pullFeedResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">Repository pull list</h3>
              <StatusBadge status={pullListResult.status} />
            </div>

            <form
              onSubmit={loadPullList}
              className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2 lg:grid-cols-3"
            >
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Repository ID</span>
                <input
                  type="text"
                  value={pullListQuery.repositoryId}
                  onChange={(event) =>
                    setPullListQuery((previous) => ({ ...previous, repositoryId: event.target.value }))
                  }
                  placeholder="321"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">State</span>
                <select
                  value={pullListQuery.state}
                  onChange={(event) =>
                    setPullListQuery((previous) => ({ ...previous, state: event.target.value }))
                  }
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                >
                  <option value="all">All</option>
                  <option value="open">Open</option>
                  <option value="closed">Closed</option>
                  <option value="merged">Merged</option>
                </select>
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Limit</span>
                <input
                  type="text"
                  value={pullListQuery.limit}
                  onChange={(event) =>
                    setPullListQuery((previous) => ({ ...previous, limit: event.target.value }))
                  }
                  placeholder="20"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Page</span>
                <input
                  type="text"
                  value={pullListQuery.page}
                  onChange={(event) =>
                    setPullListQuery((previous) => ({ ...previous, page: event.target.value }))
                  }
                  placeholder="1"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <button
                type="submit"
                disabled={pullListResult.status === "loading"}
                className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40 md:col-span-2 lg:col-span-1"
              >
                Fetch pull requests
              </button>
            </form>

            {pullListResult.error && (
              <p className="mt-3 text-sm text-rose-600">{pullListResult.error}</p>
            )}

            {pullListResult.status === "success" && pullListResult.items && (
              pullListResult.items.length > 0 ? (
                <div className="mt-4 overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Pull request</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Repository</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">State</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Updated</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {pullListResult.items.map((item) => (
                        <tr key={`pull-list-${item.id}-${item.number}`} className="hover:bg-slate-50">
                          <td className="px-3 py-2 text-slate-600">
                            <div className="font-medium text-slate-900">
                              #{item.number} · {item.title ?? "Untitled"}
                            </div>
                            {item.user?.login && (
                              <div className="text-xs text-slate-500">Created by {item.user.login}</div>
                            )}
                          </td>
                          <td className="px-3 py-2 text-slate-600">{item.repositoryFullName ?? "—"}</td>
                          <td className="px-3 py-2 text-slate-600">{item.state ?? "—"}</td>
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">{item.updatedAt ?? "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="mt-3 text-sm text-slate-600">No pull requests matched the criteria.</p>
              )
            )}

            {pullListResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(pullListResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">Pull request detail</h3>
              <StatusBadge status={pullDetailResult.status} />
            </div>

            <form
              onSubmit={loadPullDetail}
              className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end"
            >
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Repository ID</span>
                <input
                  type="text"
                  value={pullDetailQuery.repositoryId}
                  onChange={(event) =>
                    setPullDetailQuery((previous) => ({ ...previous, repositoryId: event.target.value }))
                  }
                  placeholder="321"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Pull number</span>
                <input
                  type="text"
                  value={pullDetailQuery.pullNumber}
                  onChange={(event) =>
                    setPullDetailQuery((previous) => ({ ...previous, pullNumber: event.target.value }))
                  }
                  placeholder="7"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <button
                type="submit"
                disabled={pullDetailResult.status === "loading"}
                className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
              >
                Fetch detail
              </button>
              <button
                type="button"
                onClick={() => {
                  void loadPullFiles();
                }}
                disabled={pullFilesResult.status === "loading"}
                className="w-full rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 sm:w-auto disabled:opacity-40"
              >
                Fetch changed files
              </button>
            </form>

            {pullDetailResult.error && (
              <p className="mt-3 text-sm text-rose-600">{pullDetailResult.error}</p>
            )}

            {pullDetailResult.status === "success" && pullDetailResult.data && (
              <div className="mt-4 space-y-3 rounded border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700">
                <div className="flex flex-col gap-1 text-slate-800">
                  <span className="text-base font-semibold text-slate-900">
                    #{pullDetailResult.data.number} · {pullDetailResult.data.title ?? "Untitled"}
                  </span>
                  <span className="text-xs font-mono text-slate-500">{pullDetailResult.data.repositoryFullName ?? "Unknown repository"}</span>
                </div>
                <div>State: {pullDetailResult.data.state ?? "—"}</div>
                <div>Merged: {pullDetailResult.data.merged === null ? "—" : pullDetailResult.data.merged ? "Yes" : "No"}</div>
                <div>
                  Changes: +{pullDetailResult.data.additions ?? 0} / -{pullDetailResult.data.deletions ?? 0} · Files: {pullDetailResult.data.changedFiles ?? "—"}
                </div>
                <div className="text-xs text-slate-500">
                  Created: {pullDetailResult.data.createdAt ?? "—"} · Updated: {pullDetailResult.data.updatedAt ?? "—"}
                </div>
                {pullDetailResult.data.htmlUrl && (
                  <a
                    href={pullDetailResult.data.htmlUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-blue-600 underline hover:text-blue-800"
                  >
                    View on GitHub
                  </a>
                )}
              </div>
            )}

            {pullDetailResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(pullDetailResult.payload)}
              </pre>
            )}

            <div className="mt-6 rounded border border-slate-200 bg-slate-50 p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <h4 className="text-sm font-semibold text-slate-900">Changed files</h4>
                <StatusBadge status={pullFilesResult.status} />
              </div>

              {pullFilesResult.error && (
                <p className="mt-3 text-sm text-rose-600">{pullFilesResult.error}</p>
              )}

              {pullFilesResult.status === "success" && pullFilesResult.items && (
                pullFilesResult.items.length > 0 ? (
                  <div className="mt-3 overflow-x-auto">
                    <table className="min-w-full divide-y divide-slate-200 text-sm">
                      <thead className="bg-slate-100">
                        <tr>
                          <th className="px-3 py-2 text-left font-semibold text-slate-700">Path</th>
                          <th className="px-3 py-2 text-left font-semibold text-slate-700">Δ</th>
                          <th className="px-3 py-2 text-left font-semibold text-slate-700">Changes</th>
                        </tr>
                      </thead>
                      <tbody className="divide-y divide-slate-200">
                        {pullFilesResult.items.map((file) => (
                          <tr key={`pull-file-${file.id}-${file.path}`} className="hover:bg-slate-100">
                            <td className="px-3 py-2 text-slate-600">
                              <div className="font-mono text-xs text-slate-700">{file.path}</div>
                              {file.rawBlobUrl && (
                                <a
                                  href={file.rawBlobUrl}
                                  target="_blank"
                                  rel="noreferrer"
                                  className="text-xs text-blue-600 underline hover:text-blue-800"
                                >
                                  Raw diff
                                </a>
                              )}
                            </td>
                            <td className="px-3 py-2 font-mono text-xs text-slate-600">
                              +{file.additions ?? 0} / -{file.deletions ?? 0}
                            </td>
                            <td className="px-3 py-2 font-mono text-xs text-slate-600">{file.changes ?? "—"}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <p className="mt-3 text-sm text-slate-600">No file information available for the current pull request.</p>
                )
              )}

              {pullFilesResult.payload !== undefined && (
                <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                  {formatJson(pullFilesResult.payload)}
                </pre>
              )}
            </div>
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">Trigger pull sync job</h3>
              <StatusBadge status={pullSyncResult.status} />
            </div>

            <form
              onSubmit={triggerPullSync}
              className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end"
            >
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Repository ID</span>
                <input
                  type="text"
                  value={pullSyncQuery.repositoryId}
                  onChange={(event) =>
                    setPullSyncQuery({ repositoryId: event.target.value })
                  }
                  placeholder="321"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <button
                type="submit"
                disabled={pullSyncResult.status === "loading"}
                className="w-full rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
              >
                Queue sync
              </button>
            </form>

            {pullSyncResult.error && (
              <p className="mt-3 text-sm text-rose-600">{pullSyncResult.error}</p>
            )}

            {pullSyncResult.status === "success" && pullSyncResult.data && (
              <div className="mt-4 rounded border border-emerald-200 bg-emerald-50 p-4 text-sm text-slate-800">
                <p className="font-medium text-slate-900">
                  Job {pullSyncResult.data.jobId} ({pullSyncResult.data.status})
                </p>
                <p className="mt-1 text-slate-700">Track progress via the job status form above.</p>
              </div>
            )}

            {pullSyncResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(pullSyncResult.payload)}
              </pre>
            )}
          </div>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                Commit activity feed
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">GET /api/organizations/&lt;id&gt;/git-commit/feed</code> to page through recent commits.
              </p>
            </div>
            <StatusBadge status={commitFeedResult.status} />
          </div>

          <form
            onSubmit={loadCommitFeed}
            className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end"
          >
            <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
              <span className="font-medium text-slate-900">Organization ID</span>
              <input
                type="text"
                value={commitFeedQuery.organizationId}
                onChange={(event) =>
                  setCommitFeedQuery((previous) => ({ ...previous, organizationId: event.target.value }))
                }
                placeholder="123"
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                required
              />
            </label>
            <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-[9rem]">
              <span className="font-medium text-slate-900">Limit</span>
              <input
                type="text"
                value={commitFeedQuery.limit}
                onChange={(event) =>
                  setCommitFeedQuery((previous) => ({ ...previous, limit: event.target.value }))
                }
                placeholder="20"
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              />
            </label>
            <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-[12rem]">
              <span className="font-medium text-slate-900">Cursor</span>
              <input
                type="text"
                value={commitFeedQuery.cursor}
                onChange={(event) =>
                  setCommitFeedQuery((previous) => ({ ...previous, cursor: event.target.value }))
                }
                placeholder="1693412400000"
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              />
            </label>
            <button
              type="submit"
              disabled={commitFeedResult.status === "loading"}
              className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
            >
              Fetch commit feed
            </button>
          </form>

          {commitFeedResult.error && (
            <p className="mt-3 text-sm text-rose-600">{commitFeedResult.error}</p>
          )}

          {commitFeedResult.status === "success" && commitFeedResult.data && (
            <div className="mt-4 space-y-3">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Commit</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Repository</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Author</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Committed at</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {commitFeedResult.data.items.map((commit) => (
                      <tr key={`commit-feed-${commit.id}-${commit.sha}`} className="hover:bg-slate-50">
                        <td className="px-3 py-2 text-slate-600">
                          <div className="font-medium text-slate-900">{commit.message ?? "No message"}</div>
                          {commit.url && (
                            <a
                              href={commit.url}
                              target="_blank"
                              rel="noreferrer"
                              className="text-xs text-blue-600 underline hover:text-blue-800"
                            >
                              View on GitHub
                            </a>
                          )}
                        </td>
                        <td className="px-3 py-2 text-slate-600">{commit.repositoryFullName ?? "—"}</td>
                        <td className="px-3 py-2 text-slate-600">{commit.authorName ?? commit.committerName ?? "—"}</td>
                        <td className="px-3 py-2 font-mono text-xs text-slate-600">{commit.committedAt ?? "—"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="rounded border border-slate-200 bg-slate-50 p-4 text-xs text-slate-600">
                <div>Next cursor: {commitFeedResult.data.nextCursor ?? "—"}</div>
              </div>
            </div>
          )}

          {commitFeedResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(commitFeedResult.payload)}
            </pre>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                Activity summary analytics
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">GET /api/organizations/&lt;id&gt;/activity/summary</code> to aggregate commit and availability metrics.
              </p>
            </div>
            <StatusBadge status={activitySummaryResult.status} />
          </div>

          <form
            onSubmit={loadActivitySummary}
            className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2"
          >
            <label className="flex flex-col gap-1 text-sm text-slate-700">
              <span className="font-medium text-slate-900">Organization ID</span>
              <input
                type="text"
                value={activitySummaryQuery.organizationId}
                onChange={(event) =>
                  setActivitySummaryQuery((previous) => ({ ...previous, organizationId: event.target.value }))
                }
                placeholder="123"
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                required
              />
            </label>
            <label className="flex flex-col gap-1 text-sm text-slate-700">
              <span className="font-medium text-slate-900">Group by</span>
              <select
                value={activitySummaryQuery.groupBy}
                onChange={(event) =>
                  setActivitySummaryQuery((previous) => ({ ...previous, groupBy: event.target.value }))
                }
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              >
                <option value="">Default (member)</option>
                <option value="repository">Repository</option>
                <option value="member">Member</option>
              </select>
            </label>
            <label className="flex flex-col gap-1 text-sm text-slate-700">
              <span className="font-medium text-slate-900">Start date</span>
              <input
                type="date"
                value={activitySummaryQuery.startDate}
                onChange={(event) =>
                  setActivitySummaryQuery((previous) => ({ ...previous, startDate: event.target.value }))
                }
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                required
              />
            </label>
            <label className="flex flex-col gap-1 text-sm text-slate-700">
              <span className="font-medium text-slate-900">End date</span>
              <input
                type="date"
                value={activitySummaryQuery.endDate}
                onChange={(event) =>
                  setActivitySummaryQuery((previous) => ({ ...previous, endDate: event.target.value }))
                }
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                required
              />
            </label>
            <button
              type="submit"
              disabled={activitySummaryResult.status === "loading"}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40 md:col-span-2"
            >
              Fetch activity summary
            </button>
          </form>

          {activitySummaryResult.error && (
            <p className="mt-3 text-sm text-rose-600">{activitySummaryResult.error}</p>
          )}

          {activitySummaryResult.status === "success" && activitySummaryResult.items && (
            activitySummaryResult.items.length > 0 ? (
              <div className="mt-4 overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Member</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Commits</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Files</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Additions</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Deletions</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Availability (min)</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {activitySummaryResult.items.map((item, index) => (
                      <tr key={`activity-${item.userId ?? item.login ?? index}`} className="hover:bg-slate-50">
                        <td className="px-3 py-2 text-slate-600">
                          <div className="font-medium text-slate-900">{item.name ?? item.login ?? "—"}</div>
                          {item.avatarUrl && (
                            <a
                              href={item.avatarUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="text-xs text-blue-600 underline hover:text-blue-800"
                            >
                              Avatar
                            </a>
                          )}
                        </td>
                        <td className="px-3 py-2 font-mono text-xs text-slate-600">{item.commitCount}</td>
                        <td className="px-3 py-2 font-mono text-xs text-slate-600">{item.filesChanged}</td>
                        <td className="px-3 py-2 font-mono text-xs text-slate-600">{item.additions}</td>
                        <td className="px-3 py-2 font-mono text-xs text-slate-600">{item.deletions}</td>
                        <td className="px-3 py-2 font-mono text-xs text-slate-600">{item.availableMinutes}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="mt-3 text-sm text-slate-600">No activity found for the selected window.</p>
            )
          )}

          {activitySummaryResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(activitySummaryResult.payload)}
            </pre>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                Organization status reporting
              </h2>
              <p className="text-sm text-slate-600">
                Exercise <code className="font-mono text-xs">POST /api/organizations/&lt;id&gt;/statuses</code>, <code className="font-mono text-xs">GET /api/organizations/&lt;id&gt;/statuses</code>, and <code className="font-mono text-xs">DELETE /api/organizations/&lt;id&gt;/statuses/&lt;statusId&gt;</code>.
              </p>
            </div>
            <StatusBadge status={statusUpdateResult.status} />
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">
                Submit a status update
              </h3>
              <StatusBadge status={statusUpdateResult.status} />
            </div>

            <form
              onSubmit={submitStatusUpdate}
              className="mt-4 grid grid-cols-1 gap-4 md:grid-cols-2"
            >
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Organization ID</span>
                <input
                  type="text"
                  value={statusForm.organizationId}
                  onChange={(event) =>
                    setStatusForm((previous) => ({ ...previous, organizationId: event.target.value }))
                  }
                  placeholder="123"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Status</span>
                <input
                  type="text"
                  value={statusForm.status}
                  onChange={(event) =>
                    setStatusForm((previous) => ({ ...previous, status: event.target.value }))
                  }
                  placeholder="On track"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Status message (optional)</span>
                <input
                  type="text"
                  value={statusForm.statusMessage}
                  onChange={(event) =>
                    setStatusForm((previous) => ({ ...previous, statusMessage: event.target.value }))
                  }
                  placeholder="Deploying the new dashboard"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Capacity hours (optional)</span>
                <input
                  type="text"
                  value={statusForm.capacityHours}
                  onChange={(event) =>
                    setStatusForm((previous) => ({ ...previous, capacityHours: event.target.value }))
                  }
                  placeholder="6"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Available minutes (optional)</span>
                <input
                  type="text"
                  value={statusForm.availableMinutes}
                  onChange={(event) =>
                    setStatusForm((previous) => ({ ...previous, availableMinutes: event.target.value }))
                  }
                  placeholder="180"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <label className="flex flex-col gap-1 text-sm text-slate-700">
                <span className="font-medium text-slate-900">Date (optional)</span>
                <input
                  type="date"
                  value={statusForm.date}
                  onChange={(event) =>
                    setStatusForm((previous) => ({ ...previous, date: event.target.value }))
                  }
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <button
                type="submit"
                disabled={statusUpdateResult.status === "loading"}
                className="rounded bg-emerald-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-40 md:col-span-2"
              >
                Submit status update
              </button>
            </form>

            {statusUpdateResult.error && (
              <p className="mt-3 text-sm text-rose-600">{statusUpdateResult.error}</p>
            )}

            {statusUpdateResult.status === "success" && statusUpdateResult.data && (
              <div className="mt-4 rounded border border-emerald-200 bg-emerald-50 p-4 text-sm text-slate-800">
                <p className="font-medium text-slate-900">Status submitted for {statusUpdateResult.data.member.displayName ?? statusUpdateResult.data.member.login ?? "member"}</p>
                <p className="mt-1 text-slate-700">
                  Status: <span className="font-semibold">{statusUpdateResult.data.personalStatus.status ?? "—"}</span>
                  {statusUpdateResult.data.personalStatus.statusMessage && (
                    <span className="ml-2 text-xs text-slate-500">{statusUpdateResult.data.personalStatus.statusMessage}</span>
                  )}
                </p>
                <p className="mt-1 text-slate-700">
                  Active today: {statusUpdateResult.data.summary.activeToday} · Pending: {statusUpdateResult.data.summary.pendingStatusCount}
                </p>
              </div>
            )}

            {statusUpdateResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(statusUpdateResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">
                List submitted statuses
              </h3>
              <StatusBadge status={statusListResult.status} />
            </div>

            <form
              onSubmit={loadStatusList}
              className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end"
            >
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Organization ID</span>
                <input
                  type="text"
                  value={statusListQuery.organizationId}
                  onChange={(event) =>
                    setStatusListQuery((previous) => ({ ...previous, organizationId: event.target.value }))
                  }
                  placeholder="123"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Date (optional)</span>
                <input
                  type="date"
                  value={statusListQuery.date}
                  onChange={(event) =>
                    setStatusListQuery((previous) => ({ ...previous, date: event.target.value }))
                  }
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                />
              </label>
              <button
                type="submit"
                disabled={statusListResult.status === "loading"}
                className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
              >
                Fetch status list
              </button>
            </form>

            {statusListResult.error && (
              <p className="mt-3 text-sm text-rose-600">{statusListResult.error}</p>
            )}

            {statusListResult.status === "success" && statusListResult.items && (
              statusListResult.items.length > 0 ? (
                <div className="mt-4 overflow-x-auto">
                  <table className="min-w-full divide-y divide-slate-200 text-sm">
                    <thead className="bg-slate-50">
                      <tr>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">ID</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Member</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Status</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Date</th>
                        <th className="px-3 py-2 text-left font-semibold text-slate-700">Updated</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-200">
                      {statusListResult.items.map((status) => (
                        <tr key={`status-item-${status.statusId}`} className="hover:bg-slate-50">
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">{status.statusId}</td>
                          <td className="px-3 py-2 text-slate-600">
                            <div className="font-medium text-slate-900">{status.name ?? status.login ?? "—"}</div>
                            {status.avatarUrl && (
                              <a
                                href={status.avatarUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="text-xs text-blue-600 underline hover:text-blue-800"
                              >
                                Avatar
                              </a>
                            )}
                          </td>
                          <td className="px-3 py-2 text-slate-600">
                            <div>{status.status ?? "—"}</div>
                            {status.statusMessage && (
                              <div className="text-xs text-slate-500">{status.statusMessage}</div>
                            )}
                          </td>
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">{status.date ?? "—"}</td>
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">{status.updatedAt ?? "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <p className="mt-3 text-sm text-slate-600">No statuses matched the current filters.</p>
              )
            )}

            {statusListResult.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(statusListResult.payload)}
              </pre>
            )}
          </div>

          <div className="mt-6 border-t border-slate-200 pt-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <h3 className="text-base font-semibold text-slate-900">
                Delete a status
              </h3>
              <StatusBadge status={statusDeleteState.status} />
            </div>

            <form
              onSubmit={removeStatus}
              className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-end"
            >
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Organization ID</span>
                <input
                  type="text"
                  name="status-delete-org"
                  placeholder="123"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
                <span className="font-medium text-slate-900">Status ID</span>
                <input
                  type="text"
                  name="status-delete-id"
                  placeholder="456"
                  className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
                  required
                />
              </label>
              <button
                type="submit"
                disabled={statusDeleteState.status === "loading"}
                className="w-full rounded bg-rose-600 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
              >
                Delete status
              </button>
            </form>

            {statusDeleteState.status === "success" && (
              <p className="mt-3 text-sm text-emerald-600">
                Status {statusDeleteState.statusId} removed from organization {statusDeleteState.organizationId}.
              </p>
            )}

            {statusDeleteState.error && (
              <p className="mt-3 text-sm text-rose-600">{statusDeleteState.error}</p>
            )}

            {statusDeleteState.payload !== undefined && (
              <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
                {formatJson(statusDeleteState.payload)}
              </pre>
            )}
          </div>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                Organization dashboard snapshot
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">GET /api/organizations/&lt;id&gt;/dashboard</code> to
                combine status, commit, and comment highlights.
              </p>
            </div>
            <StatusBadge status={dashboardResult.status} />
          </div>

          <form
            onSubmit={fetchDashboardSnapshot}
            className="mt-4 flex flex-col gap-3 sm:flex-row sm:items-center"
          >
            <label className="flex w-full flex-col gap-1 text-sm text-slate-700 sm:max-w-xs">
              <span className="font-medium text-slate-900">Organization ID</span>
              <input
                type="text"
                value={dashboardOrganizationId}
                onChange={(event) => setDashboardOrganizationId(event.target.value)}
                placeholder="123"
                className="rounded border border-slate-300 px-3 py-2 text-sm text-slate-900 focus:border-slate-500 focus:outline-none"
              />
            </label>
            <button
              type="submit"
              disabled={dashboardResult.status === "loading"}
              className="w-full rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white sm:w-auto disabled:opacity-40"
            >
              Fetch dashboard snapshot
            </button>
          </form>

          {dashboardResult.error && (
            <p className="mt-3 text-sm text-rose-600">{dashboardResult.error}</p>
          )}

          {dashboardResult.status === "success" && dashboardResult.data && (
            <div className="mt-4 space-y-4 text-sm text-slate-700">
              <div className="rounded border border-slate-200 bg-slate-50 p-4">
                <div className="text-sm font-semibold text-slate-900">
                  Status updates ({dashboardResult.data.statuses.length})
                </div>
                {dashboardResult.data.statuses.length > 0 ? (
                  <ul className="mt-2 space-y-2">
                    {dashboardResult.data.statuses.slice(0, 6).map((status, index) => (
                      <li
                        key={`dashboard-status-${status.userId ?? status.login ?? index}`}
                        className="flex flex-col gap-1 border-b border-slate-200 pb-2 last:border-b-0 last:pb-0"
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-medium text-slate-900">{status.name ?? status.login ?? "Unknown member"}</span>
                          <span className="font-mono text-xs text-slate-500">{status.updatedAt ?? "—"}</span>
                        </div>
                        <div className="text-slate-700">{status.status ?? "No status"}</div>
                        {status.statusMessage && (
                          <div className="text-xs text-slate-500">{status.statusMessage}</div>
                        )}
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="mt-2 text-xs text-slate-500">No statuses recorded yet.</p>
                )}
              </div>

              <div className="rounded border border-slate-200 bg-slate-50 p-4">
                <div className="text-sm font-semibold text-slate-900">
                  Recent commits ({dashboardResult.data.commits.length})
                </div>
                {dashboardResult.data.commits.length > 0 ? (
                  <ul className="mt-2 space-y-2">
                    {dashboardResult.data.commits.slice(0, 6).map((commit) => (
                      <li
                        key={`dashboard-commit-${commit.id}-${commit.sha}`}
                        className="flex flex-col gap-1 border-b border-slate-200 pb-2 last:border-b-0 last:pb-0"
                      >
                        <div className="text-slate-900">{commit.message ?? "No commit message"}</div>
                        <div className="text-xs text-slate-500">
                          {commit.repositoryFullName ?? "Unknown repo"} · {commit.committedAt ?? "—"}
                        </div>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="mt-2 text-xs text-slate-500">No commits captured.</p>
                )}
              </div>

              <div className="rounded border border-slate-200 bg-slate-50 p-4">
                <div className="text-sm font-semibold text-slate-900">
                  Feedback comments ({dashboardResult.data.comments.length})
                </div>
                {dashboardResult.data.comments.length > 0 ? (
                  <ul className="mt-2 space-y-2">
                    {dashboardResult.data.comments.slice(0, 6).map((comment) => (
                      <li
                        key={`dashboard-comment-${comment.commentId}`}
                        className="flex flex-col gap-1 border-b border-slate-200 pb-2 last:border-b-0 last:pb-0"
                      >
                        <div className="flex items-center justify-between">
                          <span className="font-medium text-slate-900">{comment.login ?? "Unknown user"}</span>
                          <span className="font-mono text-xs text-slate-500">{comment.createdAt ?? "—"}</span>
                        </div>
                        <div className="text-slate-700">{comment.content ?? "No comment body"}</div>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="mt-2 text-xs text-slate-500">No comments found.</p>
                )}
              </div>
            </div>
          )}

          {dashboardResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(dashboardResult.payload)}
            </pre>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                1. Start the GitHub OAuth flow
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">GET /api/auth/github/login</code> to
                create an authorization URL.
              </p>
            </div>
            <StatusBadge status={loginResult.status} />
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={startLogin}
              disabled={loginResult.status === "loading"}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
            >
              Request authorization URL
            </button>
            {loginResult.url && (
              <button
                type="button"
                onClick={() => window.open(loginResult.url, "_blank", "noopener")}
                className="rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
              >
                Open GitHub consent screen
              </button>
            )}
          </div>

          {loginResult.error && (
            <p className="mt-3 text-sm text-rose-600">{loginResult.error}</p>
          )}

          {loginResult.url && (
            <div className="mt-4">
              <p className="text-sm font-medium text-slate-700">Authorization URL</p>
              <code className="mt-1 block break-all rounded bg-slate-100 px-3 py-2 text-xs text-slate-800">
                {loginResult.url}
              </code>
            </div>
          )}

          {loginResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(loginResult.payload)}
            </pre>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                2. Review the backend redirect
              </h2>
              <p className="text-sm text-slate-600">
                After GitHub redirects back, the backend sends users to
                <code className="font-mono text-xs"> /auth/callback</code>. This page shows the
                final status and session snapshot.
              </p>
            </div>
            <StatusBadge status="idle" />
          </div>

          <div className="mt-4">
            <Link
              href="/auth/callback"
              className="inline-flex items-center rounded border border-slate-300 px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Open callback viewer
            </Link>
          </div>

          <p className="mt-3 text-xs text-slate-500">
            Complete the OAuth flow in a new tab, then check the callback viewer tab to
            confirm the redirect payload.
          </p>
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                3. Inspect the authenticated session
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">GET /api/auth/session</code> to read the
                current HTTP session.
              </p>
            </div>
            <StatusBadge status={sessionResult.status} />
          </div>

          <div className="mt-4">
            <button
              type="button"
              onClick={loadSession}
              disabled={sessionResult.status === "loading"}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
            >
              Refresh session
            </button>
          </div>

          {sessionResult.status === "success" && sessionResult.user && (
            <div className="mt-4 flex items-center gap-4 rounded border border-emerald-200 bg-emerald-50 p-4">
              <div>
                <p className="text-sm font-semibold text-slate-900">
                  {sessionResult.user.name}
                </p>
                <p className="text-sm text-slate-600">{sessionResult.user.login}</p>
              </div>
              <a
                href={sessionResult.user.avatarUrl}
                target="_blank"
                rel="noreferrer"
                className="text-xs text-slate-500 underline hover:text-slate-700"
              >
                View avatar
              </a>
            </div>
          )}

          {sessionResult.status === "success" && sessionResult.user === null && (
            <p className="mt-3 text-sm text-slate-600">
              No authenticated user in the session (401 Unauthorized).
            </p>
          )}

          {sessionResult.status === "error" && sessionResult.error && (
            <p className="mt-3 text-sm text-rose-600">{sessionResult.error}</p>
          )}

          {sessionResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(sessionResult.payload)}
            </pre>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                4. Fetch GitHub organizations
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">GET /api/github/organizations</code> using the stored access token.
              </p>
            </div>
            <StatusBadge status={organizationsResult.status} />
          </div>

          <div className="mt-4 flex flex-wrap items-center gap-3">
            <button
              type="button"
              onClick={loadOrganizations}
              disabled={organizationsResult.status === "loading"}
              className="rounded bg-slate-900 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
            >
              Fetch organizations
            </button>
            {organizationsResult.status === "success" && organizationsResult.items && (
              <p className="text-xs text-slate-500">
                Select an organization below to list its repositories or members.
              </p>
            )}
          </div>

          {organizationsResult.error && (
            <p className="mt-3 text-sm text-rose-600">{organizationsResult.error}</p>
          )}

          {organizationsResult.status === "success" && (
            organizationsResult.items && organizationsResult.items.length > 0 ? (
              <div className="mt-4 overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Organization</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Login</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Description</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Links</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {organizationsResult.items.map((org) => {
                      const isRepoSelected = repositoriesResult.organization === org.login;
                      const isMemberSelected = membersResult.organization === org.login;
                      const isSelected = isRepoSelected || isMemberSelected;
                      return (
                        <tr
                          key={`org-${org.id}-${org.login}`}
                          className={isSelected ? "bg-blue-50" : "hover:bg-slate-50"}
                        >
                          <td className="px-3 py-2">
                            <div className="font-medium text-slate-900">
                              {org.name ?? org.login}
                            </div>
                          </td>
                          <td className="px-3 py-2 font-mono text-xs text-slate-600">
                            {org.login}
                          </td>
                          <td className="px-3 py-2 text-slate-600">
                            {org.description ?? "—"}
                          </td>
                          <td className="px-3 py-2 text-xs text-slate-600">
                            {org.htmlUrl ? (
                              <a
                                href={org.htmlUrl}
                                target="_blank"
                                rel="noreferrer"
                                className="text-blue-600 underline hover:text-blue-800"
                              >
                                View on GitHub
                              </a>
                            ) : (
                              <span>—</span>
                            )}
                          </td>
                          <td className="px-3 py-2">
                            <button
                              type="button"
                              onClick={() => loadRepositories(org.login)}
                              disabled={repositoriesResult.status === "loading"}
                              className="rounded border border-slate-300 px-3 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-40"
                            >
                              List repositories
                            </button>
                            <button
                              type="button"
                              onClick={() => loadMembers(org.login)}
                              disabled={membersResult.status === "loading"}
                              className="ml-2 rounded border border-slate-300 px-3 py-1 text-xs font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-40"
                            >
                              List members
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="mt-3 text-sm text-slate-600">
                No organizations were returned for this account.
              </p>
            )
          )}

          {organizationsResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(organizationsResult.payload)}
            </pre>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                5. Review organization members
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">GET /api/github/organizations/&lt;org&gt;/members</code> to list public and private members.
              </p>
            </div>
            <StatusBadge status={membersResult.status} />
          </div>

          {membersResult.organization ? (
            <p className="mt-3 text-sm text-slate-600">
              Showing members for <span className="font-mono text-xs">{membersResult.organization}</span>.
            </p>
          ) : (
            <p className="mt-3 text-sm text-slate-600">
              Use the organization list above to select an organization and view its members.
            </p>
          )}

          {membersResult.error && (
            <p className="mt-3 text-sm text-rose-600">{membersResult.error}</p>
          )}

          {membersResult.status === "success" && membersResult.items && (
            membersResult.items.length > 0 ? (
              <div className="mt-4 overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Login</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Type</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Site admin</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Avatar</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Profile</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {membersResult.items.map((member) => (
                      <tr key={`member-${member.id}-${member.login}`} className="hover:bg-slate-50">
                        <td className="px-3 py-2 font-mono text-xs text-slate-700">{member.login}</td>
                        <td className="px-3 py-2 text-slate-600">{member.type ?? "—"}</td>
                        <td className="px-3 py-2 text-slate-600">{member.siteAdmin ? "Yes" : "No"}</td>
                        <td className="px-3 py-2 text-xs text-slate-600">
                          {member.avatarUrl ? (
                            <a
                              href={member.avatarUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="text-blue-600 underline hover:text-blue-800"
                            >
                              View avatar
                            </a>
                          ) : (
                            <span>—</span>
                          )}
                        </td>
                        <td className="px-3 py-2 text-xs text-slate-600">
                          {member.htmlUrl ? (
                            <a
                              href={member.htmlUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="text-blue-600 underline hover:text-blue-800"
                            >
                              View on GitHub
                            </a>
                          ) : (
                            <span>—</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="mt-3 text-sm text-slate-600">
                This organization has no visible members.
              </p>
            )
          )}

          {membersResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(membersResult.payload)}
            </pre>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                6. Explore organization repositories
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">GET /api/github/organizations/&lt;org&gt;/repositories</code> to include both public and private repositories.
              </p>
            </div>
            <StatusBadge status={repositoriesResult.status} />
          </div>

          {repositoriesResult.organization ? (
            <p className="mt-3 text-sm text-slate-600">
              Showing repositories for <span className="font-mono text-xs">{repositoriesResult.organization}</span>.
            </p>
          ) : (
            <p className="mt-3 text-sm text-slate-600">
              Fetch organizations above and choose one to inspect its repositories.
            </p>
          )}

          {repositoriesResult.error && (
            <p className="mt-3 text-sm text-rose-600">{repositoriesResult.error}</p>
          )}

          {repositoriesResult.status === "success" && repositoriesResult.items && (
            repositoriesResult.items.length > 0 ? (
              <div className="mt-4 overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-50">
                    <tr>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Repository</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Visibility</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Language</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Stars</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Forks</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Default branch</th>
                      <th className="px-3 py-2 text-left font-semibold text-slate-700">Archived</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-200">
                    {repositoriesResult.items.map((repo) => (
                      <tr key={`repo-${repo.id}-${repo.name}`} className="hover:bg-slate-50">
                        <td className="px-3 py-2">
                          <div className="font-medium text-slate-900">{repo.name}</div>
                          {repo.description && (
                            <div className="text-xs text-slate-500">{repo.description}</div>
                          )}
                          {repo.htmlUrl && (
                            <a
                              href={repo.htmlUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="text-xs text-blue-600 underline hover:text-blue-800"
                            >
                              View on GitHub
                            </a>
                          )}
                        </td>
                        <td className="px-3 py-2 text-slate-600">
                          {repo.isPrivate ? "Private" : "Public"}
                        </td>
                        <td className="px-3 py-2 text-slate-600">
                          {repo.language ?? "—"}
                        </td>
                        <td className="px-3 py-2 font-mono text-xs text-slate-600">
                          {repo.stargazersCount}
                        </td>
                        <td className="px-3 py-2 font-mono text-xs text-slate-600">
                          {repo.forksCount}
                        </td>
                        <td className="px-3 py-2 text-slate-600">
                          {repo.defaultBranch ?? "—"}
                        </td>
                        <td className="px-3 py-2 text-slate-600">
                          {repo.archived ? "Yes" : "No"}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            ) : (
              <p className="mt-3 text-sm text-slate-600">
                This organization has no repositories that match the current filters.
              </p>
            )
          )}

          {repositoriesResult.payload !== undefined && (
            <pre className="mt-4 overflow-x-auto rounded bg-slate-950/90 p-4 text-xs text-slate-100">
              {formatJson(repositoriesResult.payload)}
            </pre>
          )}
        </section>

        <section className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-900">
                8. End the session
              </h2>
              <p className="text-sm text-slate-600">
                Calls <code className="font-mono text-xs">POST /api/auth/logout</code> and refreshes
                the session view.
              </p>
            </div>
            <StatusBadge status={logoutResult.status} />
          </div>

          <div className="mt-4">
            <button
              type="button"
              onClick={endSession}
              disabled={logoutResult.status === "loading"}
              className="rounded bg-rose-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-40"
            >
              Logout
            </button>
          </div>

          {logoutResult.status === "success" && logoutResult.message && (
            <p className="mt-3 text-sm text-emerald-600">{logoutResult.message}</p>
          )}

          {logoutResult.status === "error" && logoutResult.error && (
            <p className="mt-3 text-sm text-rose-600">{logoutResult.error}</p>
          )}
        </section>
      </main>
    </div>
  );
}

function StatusBadge({ status }: { status: ActionStatus }) {
  const colors: Record<ActionStatus, string> = {
    idle: "bg-slate-200 text-slate-700",
    loading: "bg-blue-100 text-blue-700",
    success: "bg-emerald-100 text-emerald-700",
    error: "bg-rose-100 text-rose-700",
  };

  const labels: Record<ActionStatus, string> = {
    idle: "Idle",
    loading: "Loading",
    success: "Success",
    error: "Error",
  };

  return (
    <span
      className={`inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold uppercase tracking-wide ${colors[status]}`}
    >
      {labels[status]}
    </span>
  );
}

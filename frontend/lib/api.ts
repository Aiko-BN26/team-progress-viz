export type SessionUser = {
  id: number;
  login: string;
  name: string;
  avatarUrl: string;
};

export type GitHubOrganization = {
  id: number;
  login: string;
  name: string | null;
  description: string | null;
  avatarUrl: string | null;
  htmlUrl: string | null;
};

export type GitHubRepository = {
  id: number;
  name: string;
  description: string | null;
  htmlUrl: string | null;
  language: string | null;
  stargazersCount: number;
  forksCount: number;
  defaultBranch: string | null;
  isPrivate: boolean;
  archived: boolean;
};

export type GitHubOrganizationMember = {
  id: number;
  login: string;
  avatarUrl: string | null;
  htmlUrl: string | null;
  type: string | null;
  siteAdmin: boolean;
};

export type ActionStatus = "idle" | "loading" | "success" | "error";

export type OrganizationSummary = {
  id: number;
  githubId: number;
  login: string;
  name: string | null;
  avatarUrl: string | null;
  description: string | null;
};

export type OrganizationRegistration = {
  organizationId: number;
  githubId: number;
  login: string;
  name: string | null;
  htmlUrl: string | null;
  status: string;
  jobId: string;
  syncedRepositories: number;
};

export type JobSubmission = {
  jobId: string;
  status: string;
};

export type OrganizationDetailMember = {
  userId: number;
  githubId: number | null;
  login: string;
  name: string | null;
  avatarUrl: string | null;
  role: string | null;
};

export type OrganizationDetailRepository = {
  id: number;
  githubId: number | null;
  fullName: string;
  htmlUrl: string | null;
  language: string | null;
  stargazersCount: number | null;
  forksCount: number | null;
  updatedAt: string | null;
};

export type OrganizationActivitySummary = {
  commitCount: number;
  additions: number;
  deletions: number;
  activeMembers: number;
};

export type OrganizationUserSummary = {
  userId: number | null;
  githubId: number | null;
  login: string | null;
  name: string | null;
  avatarUrl: string | null;
};

export type OrganizationPullRequestSummary = {
  openCount: number;
  closedCount: number;
  mergedCount: number;
};

export type OrganizationPullRequest = {
  id: number;
  number: number;
  repositoryId: number | null;
  repositoryFullName: string | null;
  title: string | null;
  state: string | null;
  merged: boolean;
  htmlUrl: string | null;
  author: OrganizationUserSummary | null;
  mergedBy: OrganizationUserSummary | null;
  additions: number | null;
  deletions: number | null;
  changedFiles: number | null;
  createdAt: string | null;
  updatedAt: string | null;
  mergedAt: string | null;
  closedAt: string | null;
};

export type OrganizationCommit = {
  id: number;
  sha: string;
  message: string | null;
  repositoryId: number | null;
  repositoryFullName: string | null;
  htmlUrl: string | null;
  authorName: string | null;
  committerName: string | null;
  committedAt: string | null;
  pushedAt: string | null;
};

export type OrganizationComment = {
  id: number;
  user: OrganizationUserSummary | null;
  targetType: string | null;
  targetId: number | null;
  parentCommentId: number | null;
  content: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export type OrganizationDetail = {
  organization: {
    id: number;
    githubId: number;
    login: string;
    name: string | null;
    description: string | null;
    avatarUrl: string | null;
    htmlUrl: string | null;
    defaultLinkUrl: string | null;
  };
  members: OrganizationDetailMember[];
  repositories: OrganizationDetailRepository[];
  activitySummaryLast7Days: OrganizationActivitySummary;
  pullRequestSummary: OrganizationPullRequestSummary;
  recentPullRequests: OrganizationPullRequest[];
  recentCommits: OrganizationCommit[];
  recentComments: OrganizationComment[];
};

export type JobStatus = {
  jobId: string;
  type: string;
  status: string;
  createdAt: string | null;
  startedAt: string | null;
  finishedAt: string | null;
  errorMessage: string | null;
};

export type RepositorySyncStatus = {
  repositoryId: number;
  repositoryFullName: string;
  lastSyncedAt: string | null;
  lastSyncedCommitSha: string | null;
  errorMessage: string | null;
};

export type UserProfile = {
  id: number;
  githubId: number;
  login: string;
  name: string;
  avatarUrl: string;
};

export type DashboardStatusItem = {
  userId: number | null;
  login: string | null;
  name: string | null;
  avatarUrl: string | null;
  availableMinutes: number | null;
  status: string | null;
  statusMessage: string | null;
  updatedAt: string | null;
};

export type DashboardCommitItem = {
  id: number;
  sha: string;
  repositoryFullName: string | null;
  message: string | null;
  authorName: string | null;
  committedAt: string | null;
  url: string | null;
};

export type DashboardCommentItem = {
  commentId: number;
  userId: number | null;
  login: string | null;
  avatarUrl: string | null;
  content: string | null;
  createdAt: string | null;
};

export type DashboardSnapshot = {
  statuses: DashboardStatusItem[];
  commits: DashboardCommitItem[];
  comments: DashboardCommentItem[];
};

export type StatusUpdatePersonalStatus = {
  submitted: boolean;
  status: string | null;
  statusMessage: string | null;
  lastSubmittedAt: string | null;
  commitCount: number | null;
  capacityHours: number | null;
  streakDays: number | null;
  latestPrUrl: string | null;
};

export type StatusUpdateMemberStatus = {
  memberId: string | null;
  displayName: string | null;
  avatarUrl: string | null;
  status: string | null;
  statusMessage: string | null;
  lastSubmittedAt: string | null;
  commitCount: number | null;
  capacityHours: number | null;
  streakDays: number | null;
  latestPrUrl: string | null;
};

export type StatusUpdateSummary = {
  activeToday: number;
  pendingStatusCount: number;
};

export type StatusUpdatePayload = {
  personalStatus: StatusUpdatePersonalStatus;
  member: StatusUpdateMemberStatus;
  summary: StatusUpdateSummary;
};

export type StatusListItem = {
  statusId: number;
  userId: number;
  login: string | null;
  name: string | null;
  avatarUrl: string | null;
  date: string | null;
  availableMinutes: number | null;
  status: string | null;
  statusMessage: string | null;
  updatedAt: string | null;
};

export type ActivitySummaryItem = {
  userId: number | null;
  login: string | null;
  name: string | null;
  avatarUrl: string | null;
  commitCount: number;
  filesChanged: number;
  additions: number;
  deletions: number;
  availableMinutes: number;
};

export type CommitFeedItem = {
  id: number;
  sha: string;
  repositoryFullName: string | null;
  message: string | null;
  authorName: string | null;
  committerName: string | null;
  committedAt: string | null;
  url: string | null;
};

export type CommitFeed = {
  items: CommitFeedItem[];
  nextCursor: string | null;
};

export type PullRequestUserSummary = {
  userId: number | null;
  githubId: number | null;
  login: string | null;
  avatarUrl: string | null;
};

export type PullRequestListItem = {
  id: number;
  number: number;
  title: string | null;
  state: string | null;
  repositoryFullName: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  user: PullRequestUserSummary | null;
};

export type PullRequestDetail = {
  id: number;
  number: number;
  title: string | null;
  body: string | null;
  state: string | null;
  merged: boolean | null;
  htmlUrl: string | null;
  additions: number | null;
  deletions: number | null;
  changedFiles: number | null;
  createdAt: string | null;
  updatedAt: string | null;
  mergedAt: string | null;
  closedAt: string | null;
  repositoryFullName: string | null;
  user: PullRequestUserSummary | null;
  mergedBy: PullRequestUserSummary | null;
};

export type PullRequestFileItem = {
  id: number;
  path: string;
  extension: string | null;
  additions: number | null;
  deletions: number | null;
  changes: number | null;
  rawBlobUrl: string | null;
};

export type PullRequestFeedItem = {
  id: number;
  number: number;
  title: string | null;
  repositoryFullName: string | null;
  state: string | null;
  url: string | null;
  createdAt: string | null;
  updatedAt: string | null;
  user: PullRequestUserSummary | null;
};

export type PullRequestFeed = {
  items: PullRequestFeedItem[];
  nextCursor: string | null;
};

export type CommentCreateResult = {
  commentId: number;
  createdAt: string | null;
};

export type CommentListItem = {
  commentId: number;
  userId: number | null;
  login: string | null;
  name: string | null;
  avatarUrl: string | null;
  targetType: string | null;
  targetId: number | null;
  parentCommentId: number | null;
  content: string | null;
  createdAt: string | null;
  updatedAt: string | null;
};

export const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

/**
 * Attempt to decode a JSON transport payload without throwing when empty.
 */
export async function readJson(
  response: Response,
): Promise<unknown | undefined> {
  try {
    return await response.json();
  } catch {
    return undefined;
  }
}

/**
 * Validate the shape of a session payload before displaying it.
 */
export function parseSessionUser(value: unknown): SessionUser | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  const id = record.id;
  const login = record.login;
  const name = record.name;
  const avatarUrl = record.avatarUrl;

  if (
    typeof id === "number" &&
    typeof login === "string" &&
    typeof name === "string" &&
    typeof avatarUrl === "string"
  ) {
    return { id, login, name, avatarUrl };
  }

  return undefined;
}

export function parseOrganizations(value: unknown): GitHubOrganization[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const organizations: GitHubOrganization[] = [];

  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }

    const record = entry as Record<string, unknown>;
    if (typeof record.id !== "number" || typeof record.login !== "string") {
      continue;
    }

    organizations.push({
      id: record.id,
      login: record.login,
      name: typeof record.name === "string" ? record.name : null,
      description: typeof record.description === "string" ? record.description : null,
      avatarUrl: typeof record.avatarUrl === "string" ? record.avatarUrl : null,
      htmlUrl: typeof record.htmlUrl === "string" ? record.htmlUrl : null,
    });
  }

  return organizations.length > 0 ? organizations : [];
}

export function parseRepositories(value: unknown): GitHubRepository[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const repositories: GitHubRepository[] = [];

  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }

    const record = entry as Record<string, unknown>;
    if (typeof record.id !== "number" || typeof record.name !== "string") {
      continue;
    }

    repositories.push({
      id: record.id,
      name: record.name,
      description: typeof record.description === "string" ? record.description : null,
      htmlUrl: typeof record.htmlUrl === "string" ? record.htmlUrl : null,
      language: typeof record.language === "string" ? record.language : null,
      stargazersCount: typeof record.stargazersCount === "number" ? record.stargazersCount : 0,
      forksCount: typeof record.forksCount === "number" ? record.forksCount : 0,
      defaultBranch: typeof record.defaultBranch === "string" ? record.defaultBranch : null,
      isPrivate: typeof record.isPrivate === "boolean" ? record.isPrivate : false,
      archived: typeof record.archived === "boolean" ? record.archived : false,
    });
  }

  return repositories.length > 0 ? repositories : [];
}

export function parseOrganizationMembers(value: unknown): GitHubOrganizationMember[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const members: GitHubOrganizationMember[] = [];

  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }

    const record = entry as Record<string, unknown>;
    if (typeof record.id !== "number" || typeof record.login !== "string") {
      continue;
    }

    members.push({
      id: record.id,
      login: record.login,
      avatarUrl: typeof record.avatarUrl === "string" ? record.avatarUrl : null,
      htmlUrl: typeof record.htmlUrl === "string" ? record.htmlUrl : null,
      type: typeof record.type === "string" ? record.type : null,
      siteAdmin: typeof record.siteAdmin === "boolean" ? record.siteAdmin : false,
    });
  }

  return members.length > 0 ? members : [];
}

export function parseOrganizationSummaries(value: unknown): OrganizationSummary[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const organizations: OrganizationSummary[] = [];

  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }

    const record = entry as Record<string, unknown>;
    if (typeof record.id !== "number" || typeof record.login !== "string") {
      continue;
    }

    if (typeof record.githubId !== "number") {
      continue;
    }

    organizations.push({
      id: record.id,
      githubId: record.githubId,
      login: record.login,
      name: typeof record.name === "string" ? record.name : null,
      avatarUrl: typeof record.avatarUrl === "string" ? record.avatarUrl : null,
      description: typeof record.description === "string" ? record.description : null,
    });
  }

  return organizations;
}

export function parseOrganizationRegistration(value: unknown): OrganizationRegistration | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  if (
    typeof record.organizationId !== "number" ||
    typeof record.githubId !== "number" ||
    typeof record.login !== "string" ||
    typeof record.status !== "string" ||
    typeof record.jobId !== "string" ||
    typeof record.syncedRepositories !== "number"
  ) {
    return undefined;
  }

  return {
    organizationId: record.organizationId,
    githubId: record.githubId,
    login: record.login,
    name: typeof record.name === "string" ? record.name : null,
    htmlUrl: typeof record.htmlUrl === "string" ? record.htmlUrl : null,
    status: record.status,
    jobId: record.jobId,
    syncedRepositories: record.syncedRepositories,
  };
}

export function parseJobSubmission(value: unknown): JobSubmission | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  if (typeof record.jobId !== "string" || typeof record.status !== "string") {
    return undefined;
  }

  return {
    jobId: record.jobId,
    status: record.status,
  };
}

function parseOrganizationUserSummary(value: unknown): OrganizationUserSummary | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const record = value as Record<string, unknown>;
  const userId = typeof record.userId === "number" ? record.userId : null;
  const githubId = typeof record.githubId === "number" ? record.githubId : null;
  const login = typeof record.login === "string" ? record.login : null;
  const name = typeof record.name === "string" ? record.name : null;
  const avatarUrl = typeof record.avatarUrl === "string" ? record.avatarUrl : null;

  if (
    userId === null &&
    githubId === null &&
    login === null &&
    name === null &&
    avatarUrl === null
  ) {
    return null;
  }

  return {
    userId,
    githubId,
    login,
    name,
    avatarUrl,
  };
}

export function parseOrganizationDetail(value: unknown): OrganizationDetail | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  const organization = record.organization;
  const members = record.members;
  const repositories = record.repositories;
  const activity = record.activitySummaryLast7Days;

  if (!organization || typeof organization !== "object") {
    return undefined;
  }

  const organizationRecord = organization as Record<string, unknown>;
  if (
    typeof organizationRecord.id !== "number" ||
    typeof organizationRecord.githubId !== "number" ||
    typeof organizationRecord.login !== "string"
  ) {
    return undefined;
  }

  const memberEntries: OrganizationDetailMember[] = [];
  if (Array.isArray(members)) {
    for (const entry of members) {
      if (!entry || typeof entry !== "object") {
        continue;
      }
      const memberRecord = entry as Record<string, unknown>;
      if (
        typeof memberRecord.userId !== "number" ||
        (memberRecord.githubId !== null && typeof memberRecord.githubId !== "number") ||
        typeof memberRecord.login !== "string"
      ) {
        continue;
      }
      memberEntries.push({
        userId: memberRecord.userId,
        githubId: (memberRecord.githubId ?? null) as number | null,
        login: memberRecord.login,
        name: typeof memberRecord.name === "string" ? memberRecord.name : null,
        avatarUrl: typeof memberRecord.avatarUrl === "string" ? memberRecord.avatarUrl : null,
        role: typeof memberRecord.role === "string" ? memberRecord.role : null,
      });
    }
  }

  const repositoryEntries: OrganizationDetailRepository[] = [];
  if (Array.isArray(repositories)) {
    for (const entry of repositories) {
      if (!entry || typeof entry !== "object") {
        continue;
      }
      const repoRecord = entry as Record<string, unknown>;
      if (typeof repoRecord.id !== "number" || typeof repoRecord.fullName !== "string") {
        continue;
      }
      if (repoRecord.githubId !== null && typeof repoRecord.githubId !== "number") {
        continue;
      }
      repositoryEntries.push({
        id: repoRecord.id,
        githubId: (repoRecord.githubId ?? null) as number | null,
        fullName: repoRecord.fullName,
        htmlUrl: typeof repoRecord.htmlUrl === "string" ? repoRecord.htmlUrl : null,
        language: typeof repoRecord.language === "string" ? repoRecord.language : null,
        stargazersCount: typeof repoRecord.stargazersCount === "number"
          ? repoRecord.stargazersCount
          : null,
        forksCount: typeof repoRecord.forksCount === "number" ? repoRecord.forksCount : null,
        updatedAt: typeof repoRecord.updatedAt === "string" ? repoRecord.updatedAt : null,
      });
    }
  }

  let activitySummary: OrganizationActivitySummary | undefined;
  if (activity && typeof activity === "object") {
    const activityRecord = activity as Record<string, unknown>;
    if (
      typeof activityRecord.commitCount === "number" &&
      typeof activityRecord.additions === "number" &&
      typeof activityRecord.deletions === "number" &&
      typeof activityRecord.activeMembers === "number"
    ) {
      activitySummary = {
        commitCount: activityRecord.commitCount,
        additions: activityRecord.additions,
        deletions: activityRecord.deletions,
        activeMembers: activityRecord.activeMembers,
      };
    }
  }

  if (!activitySummary) {
    activitySummary = { commitCount: 0, additions: 0, deletions: 0, activeMembers: 0 };
  }

  let pullRequestSummary: OrganizationPullRequestSummary = {
    openCount: 0,
    closedCount: 0,
    mergedCount: 0,
  };
  const pullRequestSummaryValue = (record.pullRequestSummary ?? null) as unknown;
  if (pullRequestSummaryValue && typeof pullRequestSummaryValue === "object") {
    const summaryRecord = pullRequestSummaryValue as Record<string, unknown>;
    if (
      typeof summaryRecord.openCount === "number" &&
      typeof summaryRecord.closedCount === "number" &&
      typeof summaryRecord.mergedCount === "number"
    ) {
      pullRequestSummary = {
        openCount: summaryRecord.openCount,
        closedCount: summaryRecord.closedCount,
        mergedCount: summaryRecord.mergedCount,
      };
    }
  }

  const pullRequestEntries: OrganizationPullRequest[] = [];
  const pullRequestSource = (record.recentPullRequests ?? null) as unknown;
  if (Array.isArray(pullRequestSource)) {
    for (const entry of pullRequestSource) {
      if (!entry || typeof entry !== "object") {
        continue;
      }
      const prRecord = entry as Record<string, unknown>;
      if (typeof prRecord.id !== "number" || typeof prRecord.number !== "number") {
        continue;
      }

      pullRequestEntries.push({
        id: prRecord.id,
        number: prRecord.number,
        repositoryId: typeof prRecord.repositoryId === "number" ? prRecord.repositoryId : null,
        repositoryFullName:
          typeof prRecord.repositoryFullName === "string" ? prRecord.repositoryFullName : null,
        title: typeof prRecord.title === "string" ? prRecord.title : null,
        state: typeof prRecord.state === "string" ? prRecord.state : null,
        merged: typeof prRecord.merged === "boolean" ? prRecord.merged : false,
        htmlUrl: typeof prRecord.htmlUrl === "string" ? prRecord.htmlUrl : null,
        author: parseOrganizationUserSummary(prRecord.author),
        mergedBy: parseOrganizationUserSummary(prRecord.mergedBy),
        additions: typeof prRecord.additions === "number" ? prRecord.additions : null,
        deletions: typeof prRecord.deletions === "number" ? prRecord.deletions : null,
        changedFiles: typeof prRecord.changedFiles === "number" ? prRecord.changedFiles : null,
        createdAt: typeof prRecord.createdAt === "string" ? prRecord.createdAt : null,
        updatedAt: typeof prRecord.updatedAt === "string" ? prRecord.updatedAt : null,
        mergedAt: typeof prRecord.mergedAt === "string" ? prRecord.mergedAt : null,
        closedAt: typeof prRecord.closedAt === "string" ? prRecord.closedAt : null,
      });
    }
  }

  const commitEntries: OrganizationCommit[] = [];
  const commitSource = (record.recentCommits ?? null) as unknown;
  if (Array.isArray(commitSource)) {
    for (const entry of commitSource) {
      if (!entry || typeof entry !== "object") {
        continue;
      }
      const commitRecord = entry as Record<string, unknown>;
      if (typeof commitRecord.id !== "number" || typeof commitRecord.sha !== "string") {
        continue;
      }

      commitEntries.push({
        id: commitRecord.id,
        sha: commitRecord.sha,
        message: typeof commitRecord.message === "string" ? commitRecord.message : null,
        repositoryId:
          typeof commitRecord.repositoryId === "number" ? commitRecord.repositoryId : null,
        repositoryFullName:
          typeof commitRecord.repositoryFullName === "string"
            ? commitRecord.repositoryFullName
            : null,
        htmlUrl: typeof commitRecord.htmlUrl === "string" ? commitRecord.htmlUrl : null,
        authorName: typeof commitRecord.authorName === "string" ? commitRecord.authorName : null,
        committerName:
          typeof commitRecord.committerName === "string" ? commitRecord.committerName : null,
        committedAt:
          typeof commitRecord.committedAt === "string" ? commitRecord.committedAt : null,
        pushedAt: typeof commitRecord.pushedAt === "string" ? commitRecord.pushedAt : null,
      });
    }
  }

  const commentEntries: OrganizationComment[] = [];
  const commentSource = (record.recentComments ?? null) as unknown;
  if (Array.isArray(commentSource)) {
    for (const entry of commentSource) {
      if (!entry || typeof entry !== "object") {
        continue;
      }
      const commentRecord = entry as Record<string, unknown>;
      if (typeof commentRecord.id !== "number") {
        continue;
      }

      commentEntries.push({
        id: commentRecord.id,
        user: parseOrganizationUserSummary(commentRecord.user),
        targetType: typeof commentRecord.targetType === "string" ? commentRecord.targetType : null,
        targetId: typeof commentRecord.targetId === "number" ? commentRecord.targetId : null,
        parentCommentId:
          typeof commentRecord.parentCommentId === "number" ? commentRecord.parentCommentId : null,
        content: typeof commentRecord.content === "string" ? commentRecord.content : null,
        createdAt: typeof commentRecord.createdAt === "string" ? commentRecord.createdAt : null,
        updatedAt: typeof commentRecord.updatedAt === "string" ? commentRecord.updatedAt : null,
      });
    }
  }

  return {
    organization: {
      id: organizationRecord.id,
      githubId: organizationRecord.githubId,
      login: organizationRecord.login,
      name: typeof organizationRecord.name === "string" ? organizationRecord.name : null,
      description:
        typeof organizationRecord.description === "string"
          ? organizationRecord.description
          : null,
      avatarUrl:
        typeof organizationRecord.avatarUrl === "string"
          ? organizationRecord.avatarUrl
          : null,
      htmlUrl:
        typeof organizationRecord.htmlUrl === "string" ? organizationRecord.htmlUrl : null,
      defaultLinkUrl:
        typeof organizationRecord.defaultLinkUrl === "string"
          ? organizationRecord.defaultLinkUrl
          : null,
    },
    members: memberEntries,
    repositories: repositoryEntries,
    activitySummaryLast7Days: activitySummary,
    pullRequestSummary,
    recentPullRequests: pullRequestEntries,
    recentCommits: commitEntries,
    recentComments: commentEntries,
  };
}

export function parseJobStatus(value: unknown): JobStatus | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  if (
    typeof record.jobId !== "string" ||
    typeof record.type !== "string" ||
    typeof record.status !== "string"
  ) {
    return undefined;
  }

  return {
    jobId: record.jobId,
    type: record.type,
    status: record.status,
    createdAt: typeof record.createdAt === "string" ? record.createdAt : null,
    startedAt: typeof record.startedAt === "string" ? record.startedAt : null,
    finishedAt: typeof record.finishedAt === "string" ? record.finishedAt : null,
    errorMessage:
      typeof record.errorMessage === "string" ? record.errorMessage : null,
  };
}

export function parseRepositorySyncStatuses(value: unknown): RepositorySyncStatus[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const statuses: RepositorySyncStatus[] = [];

  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }

    const record = entry as Record<string, unknown>;
    if (typeof record.repositoryId !== "number" || typeof record.repositoryFullName !== "string") {
      continue;
    }

    statuses.push({
      repositoryId: record.repositoryId,
      repositoryFullName: record.repositoryFullName,
      lastSyncedAt: typeof record.lastSyncedAt === "string" ? record.lastSyncedAt : null,
      lastSyncedCommitSha:
        typeof record.lastSyncedCommitSha === "string" ? record.lastSyncedCommitSha : null,
      errorMessage: typeof record.errorMessage === "string" ? record.errorMessage : null,
    });
  }

  return statuses;
}

function parsePullRequestUser(value: unknown): PullRequestUserSummary | null {
  if (!value || typeof value !== "object") {
    return null;
  }

  const record = value as Record<string, unknown>;
  const userId = typeof record.userId === "number" ? record.userId : null;
  const githubId = typeof record.githubId === "number" ? record.githubId : null;
  const login = typeof record.login === "string" ? record.login : null;
  const avatarUrl = typeof record.avatarUrl === "string" ? record.avatarUrl : null;

  if (userId === null && githubId === null && login === null && avatarUrl === null) {
    return null;
  }

  return {
    userId,
    githubId,
    login,
    avatarUrl,
  };
}

export function parseUserProfile(value: unknown): UserProfile | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  if (
    typeof record.id !== "number" ||
    typeof record.githubId !== "number" ||
    typeof record.login !== "string" ||
    typeof record.name !== "string" ||
    typeof record.avatarUrl !== "string"
  ) {
    return undefined;
  }

  return {
    id: record.id,
    githubId: record.githubId,
    login: record.login,
    name: record.name,
    avatarUrl: record.avatarUrl,
  };
}

export function parseDashboardResponse(value: unknown): DashboardSnapshot | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  if (!Array.isArray(record.statuses) || !Array.isArray(record.commits) || !Array.isArray(record.comments)) {
    return undefined;
  }

  const statuses: DashboardStatusItem[] = [];
  for (const entry of record.statuses as unknown[]) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const statusRecord = entry as Record<string, unknown>;
    statuses.push({
      userId: typeof statusRecord.userId === "number" ? statusRecord.userId : null,
      login: typeof statusRecord.login === "string" ? statusRecord.login : null,
      name: typeof statusRecord.name === "string" ? statusRecord.name : null,
      avatarUrl: typeof statusRecord.avatarUrl === "string" ? statusRecord.avatarUrl : null,
      availableMinutes:
        typeof statusRecord.availableMinutes === "number" ? statusRecord.availableMinutes : null,
      status: typeof statusRecord.status === "string" ? statusRecord.status : null,
      statusMessage:
        typeof statusRecord.statusMessage === "string" ? statusRecord.statusMessage : null,
      updatedAt: typeof statusRecord.updatedAt === "string" ? statusRecord.updatedAt : null,
    });
  }

  const commits: DashboardCommitItem[] = [];
  for (const entry of record.commits as unknown[]) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const commitRecord = entry as Record<string, unknown>;
    if (typeof commitRecord.id !== "number" || typeof commitRecord.sha !== "string") {
      continue;
    }
    commits.push({
      id: commitRecord.id,
      sha: commitRecord.sha,
      repositoryFullName:
        typeof commitRecord.repositoryFullName === "string"
          ? commitRecord.repositoryFullName
          : null,
      message: typeof commitRecord.message === "string" ? commitRecord.message : null,
      authorName: typeof commitRecord.authorName === "string" ? commitRecord.authorName : null,
      committedAt:
        typeof commitRecord.committedAt === "string" ? commitRecord.committedAt : null,
      url: typeof commitRecord.url === "string" ? commitRecord.url : null,
    });
  }

  const comments: DashboardCommentItem[] = [];
  for (const entry of record.comments as unknown[]) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const commentRecord = entry as Record<string, unknown>;
    if (typeof commentRecord.commentId !== "number") {
      continue;
    }
    comments.push({
      commentId: commentRecord.commentId,
      userId: typeof commentRecord.userId === "number" ? commentRecord.userId : null,
      login: typeof commentRecord.login === "string" ? commentRecord.login : null,
      avatarUrl: typeof commentRecord.avatarUrl === "string" ? commentRecord.avatarUrl : null,
      content: typeof commentRecord.content === "string" ? commentRecord.content : null,
      createdAt: typeof commentRecord.createdAt === "string" ? commentRecord.createdAt : null,
    });
  }

  return {
    statuses,
    commits,
    comments,
  };
}

export function parseStatusUpdateResponse(value: unknown): StatusUpdatePayload | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  const personal = record.personalStatus;
  const member = record.member;
  const summary = record.summary;

  if (!personal || typeof personal !== "object") {
    return undefined;
  }
  if (!member || typeof member !== "object") {
    return undefined;
  }
  if (!summary || typeof summary !== "object") {
    return undefined;
  }

  const personalRecord = personal as Record<string, unknown>;
  if (typeof personalRecord.submitted !== "boolean") {
    return undefined;
  }

  const memberRecord = member as Record<string, unknown>;
  const summaryRecord = summary as Record<string, unknown>;
  if (
    typeof summaryRecord.activeToday !== "number" ||
    typeof summaryRecord.pendingStatusCount !== "number"
  ) {
    return undefined;
  }

  const personalPayload: StatusUpdatePersonalStatus = {
    submitted: personalRecord.submitted,
    status: typeof personalRecord.status === "string" ? personalRecord.status : null,
    statusMessage:
      typeof personalRecord.statusMessage === "string" ? personalRecord.statusMessage : null,
    lastSubmittedAt:
      typeof personalRecord.lastSubmittedAt === "string"
        ? personalRecord.lastSubmittedAt
        : null,
    commitCount:
      typeof personalRecord.commitCount === "number" ? personalRecord.commitCount : null,
    capacityHours:
      typeof personalRecord.capacityHours === "number" ? personalRecord.capacityHours : null,
    streakDays:
      typeof personalRecord.streakDays === "number" ? personalRecord.streakDays : null,
    latestPrUrl:
      typeof personalRecord.latestPrUrl === "string" ? personalRecord.latestPrUrl : null,
  };

  const memberPayload: StatusUpdateMemberStatus = {
    memberId: typeof memberRecord.memberId === "string" ? memberRecord.memberId : null,
    displayName: typeof memberRecord.displayName === "string" ? memberRecord.displayName : null,
    avatarUrl: typeof memberRecord.avatarUrl === "string" ? memberRecord.avatarUrl : null,
    status: typeof memberRecord.status === "string" ? memberRecord.status : null,
    statusMessage:
      typeof memberRecord.statusMessage === "string" ? memberRecord.statusMessage : null,
    lastSubmittedAt:
      typeof memberRecord.lastSubmittedAt === "string" ? memberRecord.lastSubmittedAt : null,
    commitCount:
      typeof memberRecord.commitCount === "number" ? memberRecord.commitCount : null,
    capacityHours:
      typeof memberRecord.capacityHours === "number" ? memberRecord.capacityHours : null,
    streakDays:
      typeof memberRecord.streakDays === "number" ? memberRecord.streakDays : null,
    latestPrUrl:
      typeof memberRecord.latestPrUrl === "string" ? memberRecord.latestPrUrl : null,
  };

  const summaryPayload: StatusUpdateSummary = {
    activeToday: summaryRecord.activeToday,
    pendingStatusCount: summaryRecord.pendingStatusCount,
  };

  return {
    personalStatus: personalPayload,
    member: memberPayload,
    summary: summaryPayload,
  };
}

export function parseStatusList(value: unknown): StatusListItem[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const statuses: StatusListItem[] = [];
  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const record = entry as Record<string, unknown>;
    if (typeof record.statusId !== "number" || typeof record.userId !== "number") {
      continue;
    }
    statuses.push({
      statusId: record.statusId,
      userId: record.userId,
      login: typeof record.login === "string" ? record.login : null,
      name: typeof record.name === "string" ? record.name : null,
      avatarUrl: typeof record.avatarUrl === "string" ? record.avatarUrl : null,
      date: typeof record.date === "string" ? record.date : null,
      availableMinutes:
        typeof record.availableMinutes === "number" ? record.availableMinutes : null,
      status: typeof record.status === "string" ? record.status : null,
      statusMessage:
        typeof record.statusMessage === "string" ? record.statusMessage : null,
      updatedAt: typeof record.updatedAt === "string" ? record.updatedAt : null,
    });
  }

  return statuses;
}

export function parseActivitySummary(value: unknown): ActivitySummaryItem[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const items: ActivitySummaryItem[] = [];
  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const record = entry as Record<string, unknown>;
    if (
      typeof record.commitCount !== "number" ||
      typeof record.filesChanged !== "number" ||
      typeof record.additions !== "number" ||
      typeof record.deletions !== "number" ||
      typeof record.availableMinutes !== "number"
    ) {
      continue;
    }
    items.push({
      userId: typeof record.userId === "number" ? record.userId : null,
      login: typeof record.login === "string" ? record.login : null,
      name: typeof record.name === "string" ? record.name : null,
      avatarUrl: typeof record.avatarUrl === "string" ? record.avatarUrl : null,
      commitCount: record.commitCount,
      filesChanged: record.filesChanged,
      additions: record.additions,
      deletions: record.deletions,
      availableMinutes: record.availableMinutes,
    });
  }

  return items;
}

export function parseCommitFeedResponse(value: unknown): CommitFeed | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  const itemsValue = record.items;
  if (!Array.isArray(itemsValue)) {
    return undefined;
  }

  const items: CommitFeedItem[] = [];
  for (const entry of itemsValue) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const itemRecord = entry as Record<string, unknown>;
    if (typeof itemRecord.id !== "number" || typeof itemRecord.sha !== "string") {
      continue;
    }
    items.push({
      id: itemRecord.id,
      sha: itemRecord.sha,
      repositoryFullName:
        typeof itemRecord.repositoryFullName === "string"
          ? itemRecord.repositoryFullName
          : null,
      message: typeof itemRecord.message === "string" ? itemRecord.message : null,
      authorName: typeof itemRecord.authorName === "string" ? itemRecord.authorName : null,
      committerName:
        typeof itemRecord.committerName === "string" ? itemRecord.committerName : null,
      committedAt:
        typeof itemRecord.committedAt === "string" ? itemRecord.committedAt : null,
      url: typeof itemRecord.url === "string" ? itemRecord.url : null,
    });
  }

  return {
    items,
    nextCursor: typeof record.nextCursor === "string" ? record.nextCursor : null,
  };
}

export function parsePullRequestList(value: unknown): PullRequestListItem[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const items: PullRequestListItem[] = [];
  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const record = entry as Record<string, unknown>;
    if (typeof record.id !== "number" || typeof record.number !== "number") {
      continue;
    }
    items.push({
      id: record.id,
      number: record.number,
      title: typeof record.title === "string" ? record.title : null,
      state: typeof record.state === "string" ? record.state : null,
      repositoryFullName:
        typeof record.repositoryFullName === "string"
          ? record.repositoryFullName
          : null,
      createdAt: typeof record.createdAt === "string" ? record.createdAt : null,
      updatedAt: typeof record.updatedAt === "string" ? record.updatedAt : null,
      user: parsePullRequestUser(record.user),
    });
  }

  return items;
}

export function parsePullRequestDetail(value: unknown): PullRequestDetail | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  if (typeof record.id !== "number" || typeof record.number !== "number") {
    return undefined;
  }

  return {
    id: record.id,
    number: record.number,
    title: typeof record.title === "string" ? record.title : null,
    body: typeof record.body === "string" ? record.body : null,
    state: typeof record.state === "string" ? record.state : null,
    merged: typeof record.merged === "boolean" ? record.merged : null,
    htmlUrl: typeof record.htmlUrl === "string" ? record.htmlUrl : null,
    additions: typeof record.additions === "number" ? record.additions : null,
    deletions: typeof record.deletions === "number" ? record.deletions : null,
    changedFiles: typeof record.changedFiles === "number" ? record.changedFiles : null,
    createdAt: typeof record.createdAt === "string" ? record.createdAt : null,
    updatedAt: typeof record.updatedAt === "string" ? record.updatedAt : null,
    mergedAt: typeof record.mergedAt === "string" ? record.mergedAt : null,
    closedAt: typeof record.closedAt === "string" ? record.closedAt : null,
    repositoryFullName:
      typeof record.repositoryFullName === "string" ? record.repositoryFullName : null,
    user: parsePullRequestUser(record.user),
    mergedBy: parsePullRequestUser(record.mergedBy),
  };
}

export function parsePullRequestFiles(value: unknown): PullRequestFileItem[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const items: PullRequestFileItem[] = [];
  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const record = entry as Record<string, unknown>;
    if (typeof record.id !== "number" || typeof record.path !== "string") {
      continue;
    }
    items.push({
      id: record.id,
      path: record.path,
      extension: typeof record.extension === "string" ? record.extension : null,
      additions: typeof record.additions === "number" ? record.additions : null,
      deletions: typeof record.deletions === "number" ? record.deletions : null,
      changes: typeof record.changes === "number" ? record.changes : null,
      rawBlobUrl: typeof record.rawBlobUrl === "string" ? record.rawBlobUrl : null,
    });
  }

  return items;
}

export function parsePullRequestFeed(value: unknown): PullRequestFeed | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  if (!Array.isArray(record.items)) {
    return undefined;
  }

  const items: PullRequestFeedItem[] = [];
  for (const entry of record.items as unknown[]) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const itemRecord = entry as Record<string, unknown>;
    if (typeof itemRecord.id !== "number" || typeof itemRecord.number !== "number") {
      continue;
    }
    items.push({
      id: itemRecord.id,
      number: itemRecord.number,
      title: typeof itemRecord.title === "string" ? itemRecord.title : null,
      repositoryFullName:
        typeof itemRecord.repositoryFullName === "string"
          ? itemRecord.repositoryFullName
          : null,
      state: typeof itemRecord.state === "string" ? itemRecord.state : null,
      url: typeof itemRecord.url === "string" ? itemRecord.url : null,
      createdAt: typeof itemRecord.createdAt === "string" ? itemRecord.createdAt : null,
      updatedAt: typeof itemRecord.updatedAt === "string" ? itemRecord.updatedAt : null,
      user: parsePullRequestUser(itemRecord.user),
    });
  }

  return {
    items,
    nextCursor: typeof record.nextCursor === "string" ? record.nextCursor : null,
  };
}

export function parseCommentCreateResult(value: unknown): CommentCreateResult | undefined {
  if (!value || typeof value !== "object") {
    return undefined;
  }

  const record = value as Record<string, unknown>;
  if (typeof record.commentId !== "number") {
    return undefined;
  }

  return {
    commentId: record.commentId,
    createdAt: typeof record.createdAt === "string" ? record.createdAt : null,
  };
}

export function parseCommentList(value: unknown): CommentListItem[] | undefined {
  if (!Array.isArray(value)) {
    return undefined;
  }

  const items: CommentListItem[] = [];
  for (const entry of value) {
    if (!entry || typeof entry !== "object") {
      continue;
    }
    const record = entry as Record<string, unknown>;
    if (typeof record.commentId !== "number") {
      continue;
    }
    items.push({
      commentId: record.commentId,
      userId: typeof record.userId === "number" ? record.userId : null,
      login: typeof record.login === "string" ? record.login : null,
      name: typeof record.name === "string" ? record.name : null,
      avatarUrl: typeof record.avatarUrl === "string" ? record.avatarUrl : null,
      targetType: typeof record.targetType === "string" ? record.targetType : null,
      targetId: typeof record.targetId === "number" ? record.targetId : null,
      parentCommentId:
        typeof record.parentCommentId === "number" ? record.parentCommentId : null,
      content: typeof record.content === "string" ? record.content : null,
      createdAt: typeof record.createdAt === "string" ? record.createdAt : null,
      updatedAt: typeof record.updatedAt === "string" ? record.updatedAt : null,
    });
  }

  return items;
}

export function formatJson(value: unknown): string {
  if (value === undefined) {
    return "(no response body)";
  }

  return JSON.stringify(value, null, 2);
}

export type OrganizationDetail = {
  id: string;
  name: string;
  description: string;
  avatarUrl: string;
  timezone: string;
  memberCount: number;
  activeToday: number;
  pendingStatusCount: number;
  streakDays: number;
  lastActivity: string;
  publicShareUrl: string;
};

export type PersonalStatus = {
  date: string;
  submitted: boolean;
  status?: string | null;
  statusMessage?: string | null;
  capacityHours?: number | null;
  lastSubmittedAt?: string | null;
  pendingReason?: string | null;
};

export type MemberStatus = {
  memberId: string;
  displayName: string;
  avatarUrl: string;
  status: "done" | "pending" | "late" | "focus";
  statusMessage: string | null;
  updatedAt: string | null;
  commitCount: number;
  latestPrUrl: string | null;
};

export type ActivityPoint = {
  bucketStart: string;
  activeMembers: number;
  totalCommits: number;
  avgStatusScore: number;
};

export type CommitActivity = {
  id: string;
  title: string;
  repository: string;
  author: string;
  committedAt: string;
  url: string;
};

export type OrganizationActivity = {
  daily: ActivityPoint[];
  weekly: ActivityPoint[];
};

export type OrganizationViewData = {
  detail: OrganizationDetail;
  members: MemberStatus[];
  activity: OrganizationActivity;
  commits: CommitActivity[];
  personalStatus: PersonalStatus;
};

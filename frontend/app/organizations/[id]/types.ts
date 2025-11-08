export type OrganizationDetail = {
  id: string;
  name: string;
  description: string;
  avatarUrl: string;
  timezone: string;
  memberCount: number;
  activeToday: number;
  pendingStatusCount: number;
  streakDays?: number | null;
  lastActivity: string;
  publicShareUrl: string;
};

export type MemberStatusState = "完了" | "集中" | "休み" | "ちょっと";

export type PersonalStatus = {
  submitted: boolean;
  status?: MemberStatusState | null;
  statusMessage?: string | null;
  lastSubmittedAt: string | null;
  commitCount: number;
  capacityHours: number | null;
  streakDays: number;
  latestPrUrl: string | null;
};

export type MemberStatus = {
  memberId: string;
  displayName: string;
  avatarUrl: string;
  status: MemberStatusState;
  statusMessage: string | null;
  lastSubmittedAt: string | null;
  commitCount: number;
  capacityHours: number | null;
  streakDays: number;
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

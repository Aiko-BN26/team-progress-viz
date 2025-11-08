export type OrganizationListItem = {
  id: string;
  name: string;
  description: string;
  memberCount: number;
  activeToday: number;
  pendingStatusCount: number;
  lastActivity: string;
  avatarUrl: string;
};

package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;

public record StatusUpdateResponse(
        PersonalStatus personalStatus,
        MemberStatus member,
        StatusSummary summary
) {

    public record PersonalStatus(
            boolean submitted,
            String status,
            String statusMessage,
            OffsetDateTime lastSubmittedAt,
            int commitCount,
            int capacityHours,
            int streakDays,
            String latestPrUrl
    ) {}

    public record MemberStatus(
            String memberId,
            String displayName,
            String avatarUrl,
            String status,
            String statusMessage,
            OffsetDateTime lastSubmittedAt,
            int commitCount,
            int capacityHours,
            int streakDays,
            String latestPrUrl
    ) {}

    public record StatusSummary(
            int activeToday,
            int pendingStatusCount
    ) {}
}

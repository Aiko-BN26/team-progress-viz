package io.github.aikobn26.teamprogressviz.feature.repository.dto.response;

import java.time.OffsetDateTime;

public record PullRequestDetailResponse(
        Long id,
        Integer number,
        String title,
        String body,
        String state,
        Boolean merged,
        String htmlUrl,
        UserSummary user,
        UserSummary mergedBy,
        Integer additions,
        Integer deletions,
        Integer changedFiles,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime mergedAt,
        OffsetDateTime closedAt,
        String repositoryFullName
) {

    public record UserSummary(
            Long userId,
            Long githubId,
            String login,
            String avatarUrl
    ) {}
}

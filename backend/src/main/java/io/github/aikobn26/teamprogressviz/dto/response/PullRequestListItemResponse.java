package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;

public record PullRequestListItemResponse(
        Long id,
        Integer number,
        String title,
        String state,
        UserSummary user,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String repositoryFullName
) {

    public record UserSummary(
            Long userId,
            Long githubId,
            String login,
            String avatarUrl
    ) {}
}

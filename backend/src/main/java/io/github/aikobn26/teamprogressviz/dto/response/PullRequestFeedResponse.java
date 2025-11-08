package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record PullRequestFeedResponse(
        List<Item> items,
        String nextCursor
) {

    public record Item(
            Long id,
            Integer number,
            String title,
            String repositoryFullName,
            String state,
            PullRequestUser user,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            String url
    ) {}

    public record PullRequestUser(
            Long userId,
            Long githubId,
            String login,
            String avatarUrl
    ) {}
}

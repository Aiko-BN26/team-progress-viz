package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record CommitFeedResponse(
        List<Item> items,
        String nextCursor
) {

    public record Item(
            Long id,
            String sha,
            String repositoryFullName,
            String message,
            String authorName,
            String committerName,
            OffsetDateTime committedAt,
            String url
    ) {}
}

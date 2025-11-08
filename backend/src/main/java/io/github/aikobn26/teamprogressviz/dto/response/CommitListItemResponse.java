package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;

public record CommitListItemResponse(
        Long id,
        String sha,
        String message,
        String repositoryFullName,
        String authorName,
        String committerName,
        OffsetDateTime committedAt,
        String url
) {}

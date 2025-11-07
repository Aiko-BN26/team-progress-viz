package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;

public record CommitDetailResponse(
        Long id,
        String sha,
        Long repositoryId,
        String repositoryFullName,
        String message,
        String url,
        String authorName,
        String authorEmail,
        String committerName,
        String committerEmail,
        OffsetDateTime committedAt,
        OffsetDateTime pushedAt
) {}

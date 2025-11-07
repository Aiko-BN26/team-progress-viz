package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;

public record RepositorySyncStatusResponse(
        Long repositoryId,
        String repositoryFullName,
        OffsetDateTime lastSyncedAt,
        String lastSyncedCommitSha,
        String errorMessage
) {}
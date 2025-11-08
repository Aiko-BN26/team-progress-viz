package io.github.aikobn26.teamprogressviz.feature.organization.dto.response;

import java.time.OffsetDateTime;

public record RepositorySyncStatusResponse(
        Long repositoryId,
        String repositoryFullName,
        OffsetDateTime lastSyncedAt,
        String lastSyncedCommitSha,
        String errorMessage
) {}
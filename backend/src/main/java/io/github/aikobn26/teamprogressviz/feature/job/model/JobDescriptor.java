package io.github.aikobn26.teamprogressviz.feature.job.model;

import java.time.OffsetDateTime;

public record JobDescriptor(
        String id,
        String type,
        JobStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        int progress,
        String errorMessage
) {
}

package io.github.aikobn26.teamprogressviz.job;

import java.time.OffsetDateTime;

public record JobDescriptor(
        String id,
        String type,
        JobStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorMessage
) {
}

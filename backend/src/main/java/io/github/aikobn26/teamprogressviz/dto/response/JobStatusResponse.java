package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;

import io.github.aikobn26.teamprogressviz.job.JobStatus;

public record JobStatusResponse(
        String jobId,
        String type,
        JobStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorMessage
) {}

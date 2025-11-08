package io.github.aikobn26.teamprogressviz.feature.job.dto.response;

import java.time.OffsetDateTime;

import io.github.aikobn26.teamprogressviz.feature.job.model.JobStatus;


public record JobStatusResponse(
        String jobId,
        String type,
        JobStatus status,
        OffsetDateTime createdAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        String errorMessage
) {}

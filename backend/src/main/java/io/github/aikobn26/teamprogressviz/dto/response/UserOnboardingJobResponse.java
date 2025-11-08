package io.github.aikobn26.teamprogressviz.dto.response;

public record UserOnboardingJobResponse(
        Long organizationId,
        String organizationLogin,
        String jobId
) {}

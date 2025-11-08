package io.github.aikobn26.teamprogressviz.feature.user.dto.response;

public record UserOnboardingJobResponse(
        Long organizationId,
        String organizationLogin,
        String jobId
) {}

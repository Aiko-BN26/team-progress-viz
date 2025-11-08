package io.github.aikobn26.teamprogressviz.dto.response;

public record OrganizationRegistrationResponse(
        Long organizationId,
        Long githubId,
        String login,
        String name,
        String htmlUrl,
        String status,
        String jobId,
        int syncedRepositories
) {}

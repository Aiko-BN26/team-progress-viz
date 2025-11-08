package io.github.aikobn26.teamprogressviz.feature.organization.dto.response;

public record OrganizationSummaryResponse(
        Long id,
        Long githubId,
        String login,
        String name,
        String avatarUrl,
        String description
) {}

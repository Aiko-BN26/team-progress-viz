package io.github.aikobn26.teamprogressviz.dto.response;

public record OrganizationSummaryResponse(
        Long id,
        Long githubId,
        String login,
        String name,
        String avatarUrl,
        String description
) {}

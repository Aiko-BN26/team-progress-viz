package io.github.aikobn26.teamprogressviz.feature.github.model;

public record GitHubOrganizationMember(
        Long id,
        String login,
        String avatarUrl,
        String htmlUrl,
        String type,
        boolean siteAdmin
) {}
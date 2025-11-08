package io.github.aikobn26.teamprogressviz.feature.github.model;

public record GitHubOrganization(
        Long id,
        String login,
        String name,
        String description,
        String avatarUrl,
        String htmlUrl
) {}

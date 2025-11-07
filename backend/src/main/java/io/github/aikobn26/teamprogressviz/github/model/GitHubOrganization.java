package io.github.aikobn26.teamprogressviz.github.model;

public record GitHubOrganization(
        Long id,
        String login,
        String name,
        String description,
        String avatarUrl,
        String htmlUrl
) {}

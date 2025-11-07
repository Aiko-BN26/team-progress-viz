package io.github.aikobn26.teamprogressviz.github.model;

public record GitHubRepository(
        Long id,
        String name,
        String description,
        String htmlUrl,
        String language,
        Integer stargazersCount,
        Integer forksCount,
        String defaultBranch,
        boolean isPrivate,
        boolean archived
) {}

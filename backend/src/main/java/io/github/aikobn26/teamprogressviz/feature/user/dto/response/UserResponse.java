package io.github.aikobn26.teamprogressviz.feature.user.dto.response;

public record UserResponse(
        Long id,
        Long githubId,
        String login,
        String name,
        String avatarUrl
) {}

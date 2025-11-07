package io.github.aikobn26.teamprogressviz.dto.response;

public record UserResponse(
        Long id,
        Long githubId,
        String login,
        String name,
        String avatarUrl
) {}

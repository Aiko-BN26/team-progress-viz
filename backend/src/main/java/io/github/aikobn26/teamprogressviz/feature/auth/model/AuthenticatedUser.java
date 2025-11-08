package io.github.aikobn26.teamprogressviz.feature.auth.model;

import java.io.Serializable;

public record AuthenticatedUser(
        Long id,
        String login,
        String name,
        String avatarUrl
) implements Serializable {}

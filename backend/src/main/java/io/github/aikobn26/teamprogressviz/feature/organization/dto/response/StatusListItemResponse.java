package io.github.aikobn26.teamprogressviz.feature.organization.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record StatusListItemResponse(
        Long statusId,
        Long userId,
        String login,
        String name,
        String avatarUrl,
        LocalDate date,
        Integer availableMinutes,
        String status,
        String statusMessage,
        OffsetDateTime updatedAt
) {
}

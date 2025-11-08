package io.github.aikobn26.teamprogressviz.feature.organization.dto.response;

public record ActivitySummaryItemResponse(
        Long userId,
        String login,
        String name,
        String avatarUrl,
        long commitCount,
        long filesChanged,
        long additions,
        long deletions,
        long availableMinutes
) {
}

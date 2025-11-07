package io.github.aikobn26.teamprogressviz.dto.response;

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

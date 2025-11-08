package io.github.aikobn26.teamprogressviz.feature.organization.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

public record DashboardResponse(
        List<StatusItem> statuses,
        List<CommitItem> commits,
        List<CommentItem> comments
) {

    public record StatusItem(
            Long userId,
            String login,
            String name,
            String avatarUrl,
            Integer availableMinutes,
            String status,
            String statusMessage,
            OffsetDateTime updatedAt
    ) {}

    public record CommitItem(
            Long id,
            String sha,
            String repositoryFullName,
            String message,
            String authorName,
            OffsetDateTime committedAt,
            String url
    ) {}

    public record CommentItem(
            Long commentId,
            Long userId,
            String login,
            String avatarUrl,
            String content,
            OffsetDateTime createdAt
    ) {}
}

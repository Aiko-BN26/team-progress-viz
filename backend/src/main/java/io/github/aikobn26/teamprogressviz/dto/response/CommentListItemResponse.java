package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;

public record CommentListItemResponse(
        Long commentId,
        Long userId,
        String login,
        String name,
        String avatarUrl,
        String targetType,
        Long targetId,
        Long parentCommentId,
        String content,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

package io.github.aikobn26.teamprogressviz.dto.response;

import java.time.OffsetDateTime;

public record CommentCreateResponse(
        Long commentId,
        OffsetDateTime createdAt
) {
}

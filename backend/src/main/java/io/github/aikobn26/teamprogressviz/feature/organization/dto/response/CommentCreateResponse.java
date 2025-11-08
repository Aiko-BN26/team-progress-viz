package io.github.aikobn26.teamprogressviz.feature.organization.dto.response;

import java.time.OffsetDateTime;

public record CommentCreateResponse(
        Long commentId,
        OffsetDateTime createdAt
) {
}

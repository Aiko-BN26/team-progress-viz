package io.github.aikobn26.teamprogressviz.feature.organization.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CommentCreateRequest(
        @NotBlank(message = "targetType is required")
        String targetType,
        Long targetId,
        Long parentCommentId,
        @NotNull(message = "content is required")
        @Size(min = 1, max = 4000, message = "content must be between 1 and 4000 characters")
        String content
) {
}

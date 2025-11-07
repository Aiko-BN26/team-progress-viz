package io.github.aikobn26.teamprogressviz.service.organization;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.dto.request.CommentCreateRequest;
import io.github.aikobn26.teamprogressviz.dto.response.CommentCreateResponse;
import io.github.aikobn26.teamprogressviz.dto.response.CommentListItemResponse;
import io.github.aikobn26.teamprogressviz.entity.Comment;
import io.github.aikobn26.teamprogressviz.entity.Organization;
import io.github.aikobn26.teamprogressviz.entity.User;
import io.github.aikobn26.teamprogressviz.entity.UserOrganization;
import io.github.aikobn26.teamprogressviz.exception.ForbiddenException;
import io.github.aikobn26.teamprogressviz.exception.ResourceNotFoundException;
import io.github.aikobn26.teamprogressviz.exception.ValidationException;
import io.github.aikobn26.teamprogressviz.repository.CommentRepository;
import io.github.aikobn26.teamprogressviz.repository.UserOrganizationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {

    private static final int COMMENT_FETCH_LIMIT = 100;

    private final OrganizationService organizationService;
    private final CommentRepository commentRepository;
    private final UserOrganizationRepository userOrganizationRepository;

    public CommentCreateResponse createComment(User user,
                                               Long organizationId,
                                               CommentCreateRequest request) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        validateCreateRequest(request);

        Comment parent = null;
        if (request.parentCommentId() != null) {
            parent = commentRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(request.parentCommentId(), organization.getId())
                    .orElseThrow(() -> new ValidationException("Parent comment not found"));
        }

        Comment comment = Comment.builder()
                .user(user)
                .organization(organization)
                .targetType(request.targetType().trim())
                .targetId(request.targetId())
                .parent(parent)
                .content(request.content().trim())
                .deletedAt(null)
                .build();

        Comment saved = commentRepository.save(comment);
        return new CommentCreateResponse(saved.getId(), saved.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<CommentListItemResponse> listComments(User user,
                                                      Long organizationId,
                                                      String targetType,
                                                      Long targetId) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        var pageable = PageRequest.of(0, COMMENT_FETCH_LIMIT, sort);

        List<Comment> comments;
        if (StringUtils.hasText(targetType) && targetId != null) {
            comments = commentRepository
                    .findByOrganizationIdAndTargetTypeIgnoreCaseAndTargetIdAndDeletedAtIsNull(
                            organization.getId(),
                            targetType.trim(),
                            targetId,
                            pageable)
                    .getContent();
        } else if (StringUtils.hasText(targetType)) {
            comments = commentRepository
                    .findByOrganizationIdAndTargetTypeIgnoreCaseAndDeletedAtIsNull(
                            organization.getId(),
                            targetType.trim(),
                            pageable)
                    .getContent();
        } else {
            comments = commentRepository
                    .findByOrganizationIdAndDeletedAtIsNull(organization.getId(), pageable)
                    .getContent();
        }

        return comments.stream()
                .map(this::toResponse)
                .toList();
    }

    public void deleteComment(User user, Long organizationId, Long commentId) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        Comment comment = commentRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(commentId, organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        boolean sameUser = comment.getUser() != null && Objects.equals(comment.getUser().getId(), user.getId());
        if (!sameUser && !hasAdminPrivilege(user, organization)) {
            throw new ForbiddenException("You do not have permission to delete this comment");
        }

        comment.setDeletedAt(OffsetDateTime.now());
        commentRepository.save(comment);
    }

    private void validateCreateRequest(CommentCreateRequest request) {
        if (request == null) {
            throw new ValidationException("Request body is required");
        }
        if (!StringUtils.hasText(request.targetType())) {
            throw new ValidationException("targetType is required");
        }
        if (!StringUtils.hasText(request.content())) {
            throw new ValidationException("content is required");
        }
    }

    private boolean hasAdminPrivilege(User user, Organization organization) {
        return userOrganizationRepository
                .findByUserIdAndOrganizationIdAndDeletedAtIsNull(user.getId(), organization.getId())
                .map(UserOrganization::getRole)
                .map(role -> {
                    if (!StringUtils.hasText(role)) {
                        return false;
                    }
                    String normalized = role.toLowerCase(Locale.ROOT);
                    return "admin".equals(normalized) || "owner".equals(normalized);
                })
                .orElse(false);
    }

    private CommentListItemResponse toResponse(Comment comment) {
        User owner = comment.getUser();
        return new CommentListItemResponse(
                comment.getId(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getLogin() : null,
                owner != null ? owner.getName() : null,
                owner != null ? owner.getAvatarUrl() : null,
                comment.getTargetType(),
                comment.getTargetId(),
                comment.getParent() != null ? comment.getParent().getId() : null,
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}

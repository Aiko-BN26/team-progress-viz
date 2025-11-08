package io.github.aikobn26.teamprogressviz.feature.organization.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.feature.organization.entity.Comment;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByOrganizationIdAndDeletedAtIsNull(Long organizationId, Pageable pageable);

    Page<Comment> findByOrganizationIdAndTargetTypeIgnoreCaseAndDeletedAtIsNull(Long organizationId, String targetType, Pageable pageable);

    Page<Comment> findByOrganizationIdAndTargetTypeIgnoreCaseAndTargetIdAndDeletedAtIsNull(Long organizationId, String targetType, Long targetId, Pageable pageable);

    Optional<Comment> findByIdAndOrganizationIdAndDeletedAtIsNull(Long commentId, Long organizationId);
}

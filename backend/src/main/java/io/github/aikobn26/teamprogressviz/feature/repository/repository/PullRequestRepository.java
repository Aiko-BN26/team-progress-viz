package io.github.aikobn26.teamprogressviz.feature.repository.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.feature.repository.entity.PullRequest;

@Repository
public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {

    Page<PullRequest> findByRepositoryIdAndDeletedAtIsNull(Long repositoryId, Pageable pageable);

    Page<PullRequest> findByRepositoryIdAndStateInAndDeletedAtIsNull(Long repositoryId, Iterable<String> states, Pageable pageable);

    Optional<PullRequest> findByRepositoryIdAndNumberAndDeletedAtIsNull(Long repositoryId, Integer number);

    Page<PullRequest> findByRepositoryOrganizationIdAndDeletedAtIsNull(Long organizationId, Pageable pageable);

    Page<PullRequest> findByRepositoryOrganizationIdAndIdLessThanAndDeletedAtIsNull(Long organizationId, Long id, Pageable pageable);

    Optional<PullRequest> findFirstByRepositoryOrganizationIdAndAuthorIdAndDeletedAtIsNullOrderByUpdatedAtDesc(Long organizationId, Long authorId);

        long countByRepositoryOrganizationIdAndStateIgnoreCaseAndDeletedAtIsNull(Long organizationId, String state);

        long countByRepositoryOrganizationIdAndMergedIsTrueAndDeletedAtIsNull(Long organizationId);

        @Query("""
                        select coalesce(sum(pr.additions), 0)
                        from PullRequest pr
                        where pr.repository.organization.id = :organizationId
                            and pr.deletedAt is null
                            and pr.updatedAt is not null
                            and pr.updatedAt >= :since
                        """)
        long sumAdditionsSince(@Param("organizationId") Long organizationId, @Param("since") OffsetDateTime since);

        @Query("""
                        select coalesce(sum(pr.deletions), 0)
                        from PullRequest pr
                        where pr.repository.organization.id = :organizationId
                            and pr.deletedAt is null
                            and pr.updatedAt is not null
                            and pr.updatedAt >= :since
                        """)
        long sumDeletionsSince(@Param("organizationId") Long organizationId, @Param("since") OffsetDateTime since);
}

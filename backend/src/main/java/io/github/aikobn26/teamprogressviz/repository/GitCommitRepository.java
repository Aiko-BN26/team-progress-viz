package io.github.aikobn26.teamprogressviz.repository;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.aikobn26.teamprogressviz.entity.GitCommit;

@Repository
public interface GitCommitRepository extends JpaRepository<GitCommit, Long> {

    Page<GitCommit> findByRepositoryOrganizationIdAndDeletedAtIsNull(Long organizationId, Pageable pageable);

    Page<GitCommit> findByRepositoryOrganizationIdAndIdLessThanAndDeletedAtIsNull(Long organizationId, Long id, Pageable pageable);

    Page<GitCommit> findByRepositoryIdAndDeletedAtIsNull(Long repositoryId, Pageable pageable);

    Optional<GitCommit> findByRepositoryIdAndShaAndDeletedAtIsNull(Long repositoryId, String sha);

    @Query("""
                    select count(g)
                    from GitCommit g
                    where g.repository.organization.id = :organizationId
                        and g.deletedAt is null
                        and ((g.committedAt is not null and g.committedAt >= :since)
                                 or (g.committedAt is null and g.createdAt >= :since))
                    """)
    long countRecentCommits(@Param("organizationId") Long organizationId, @Param("since") OffsetDateTime since);
}

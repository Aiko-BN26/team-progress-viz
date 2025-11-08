package io.github.aikobn26.teamprogressviz.feature.repository.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.aikobn26.teamprogressviz.feature.organization.entity.Organization;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.Repository;


public interface RepositoryRepository extends JpaRepository<Repository, Long> {

    Optional<Repository> findByGithubId(Long githubId);

    Optional<Repository> findByIdAndDeletedAtIsNull(Long repositoryId);

    List<Repository> findByOrganizationAndDeletedAtIsNull(Organization organization);

    List<Repository> findByOrganizationIdAndDeletedAtIsNull(Long organizationId);
}

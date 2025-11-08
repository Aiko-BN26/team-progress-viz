package io.github.aikobn26.teamprogressviz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.aikobn26.teamprogressviz.entity.Organization;
import io.github.aikobn26.teamprogressviz.entity.Repository;

public interface RepositoryRepository extends JpaRepository<Repository, Long> {

    Optional<Repository> findByGithubId(Long githubId);

    List<Repository> findByOrganizationAndDeletedAtIsNull(Organization organization);
}

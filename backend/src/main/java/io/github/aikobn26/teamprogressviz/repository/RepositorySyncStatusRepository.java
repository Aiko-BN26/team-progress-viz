package io.github.aikobn26.teamprogressviz.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.aikobn26.teamprogressviz.entity.RepositorySyncStatus;

public interface RepositorySyncStatusRepository extends JpaRepository<RepositorySyncStatus, Long> {

    Optional<RepositorySyncStatus> findByRepositoryId(Long repositoryId);

    List<RepositorySyncStatus> findByRepositoryOrganizationIdAndDeletedAtIsNull(Long organizationId);
}
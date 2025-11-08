package io.github.aikobn26.teamprogressviz.service.organization;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.aikobn26.teamprogressviz.entity.Repository;
import io.github.aikobn26.teamprogressviz.entity.RepositorySyncStatus;
import io.github.aikobn26.teamprogressviz.repository.RepositorySyncStatusRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RepositorySyncStatusService {

    private final RepositorySyncStatusRepository repositorySyncStatusRepository;

    public void markSynced(Repository repository, OffsetDateTime lastSyncedAt, String lastSyncedCommitSha) {
        if (repository == null || repository.getId() == null) {
            return;
        }
        RepositorySyncStatus status = repositorySyncStatusRepository.findByRepositoryId(repository.getId())
                .orElseGet(() -> RepositorySyncStatus.builder().repository(repository).build());
        status.setRepository(repository);
        status.setLastSyncedAt(lastSyncedAt);
        status.setLastSyncedCommitSha(lastSyncedCommitSha);
        status.setErrorMessage(null);
        status.setDeletedAt(null);
        repositorySyncStatusRepository.save(status);
    }

    public void markFailure(Repository repository, OffsetDateTime lastAttemptAt, String errorMessage) {
        if (repository == null || repository.getId() == null) {
            return;
        }
        RepositorySyncStatus status = repositorySyncStatusRepository.findByRepositoryId(repository.getId())
                .orElseGet(() -> RepositorySyncStatus.builder().repository(repository).build());
        status.setRepository(repository);
        status.setLastSyncedAt(lastAttemptAt);
        status.setErrorMessage(errorMessage);
        status.setDeletedAt(null);
        repositorySyncStatusRepository.save(status);
    }

    public void markDeleted(Repository repository) {
        if (repository == null || repository.getId() == null) {
            return;
        }
        repositorySyncStatusRepository.findByRepositoryId(repository.getId()).ifPresent(status -> {
            status.setDeletedAt(OffsetDateTime.now());
            repositorySyncStatusRepository.save(status);
        });
    }

    @Transactional(readOnly = true)
    public List<RepositorySyncStatus> findActiveByOrganization(Long organizationId) {
        if (organizationId == null) {
            return List.of();
        }
        return repositorySyncStatusRepository.findByRepositoryOrganizationIdAndDeletedAtIsNull(organizationId);
    }
}
package io.github.aikobn26.teamprogressviz.feature.repository.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.feature.github.exception.GitHubApiException;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubCommit;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubCommitDetail;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubCommitFile;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubPullRequest;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubPullRequestFile;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubPullRequestSummary;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubSimpleUser;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.Organization;
import io.github.aikobn26.teamprogressviz.feature.organization.properties.OrganizationSyncProperties;
import io.github.aikobn26.teamprogressviz.feature.organization.service.RepositorySyncStatusService;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.CommitFile;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.GitCommit;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.PullRequest;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.PullRequestFile;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.Repository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.CommitFileRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.GitCommitRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.PullRequestFileRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.PullRequestRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.RepositoryRepository;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.shared.exception.ResourceNotFoundException;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RepositoryActivitySyncService {

    private static final Logger log = LoggerFactory.getLogger(RepositoryActivitySyncService.class);

    private static final int MAX_PULL_REQUESTS = 50;
    private static final int MAX_PULL_REQUEST_FILES = 100;
    private static final int MAX_COMMITS = 100;
    private static final int ACTIVITY_LOOKBACK_DAYS = 30;

    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;
    private final PullRequestFileRepository pullRequestFileRepository;
    private final CommitFileRepository commitFileRepository;
    private final GitCommitRepository gitCommitRepository;
    private final RepositorySyncStatusService repositorySyncStatusService;
    private final GitHubRepositoryService gitHubRepositoryService;
    private final UserService userService;
    private final OrganizationSyncProperties organizationSyncProperties;
    private final PlatformTransactionManager transactionManager;

    private static final IntConsumer NO_OP_PROGRESS = progress -> { };

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void synchronizeActivities(Organization organization, String accessToken) {
        synchronizeActivities(organization, accessToken, NO_OP_PROGRESS);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void synchronizeActivities(Organization organization, String accessToken, IntConsumer progressConsumer) {
        if (organization == null || organization.getId() == null) {
            if (progressConsumer != null) {
                progressConsumer.accept(100);
            }
            return;
        }
        IntConsumer progress = progressConsumer != null ? progressConsumer : NO_OP_PROGRESS;

        List<RepositorySyncTarget> targets = executeInTransaction(() -> repositoryRepository
                .findByOrganizationIdAndDeletedAtIsNull(organization.getId())
                .stream()
                .map(this::toSyncTarget)
                .filter(Objects::nonNull)
                .toList());

        if (targets.isEmpty()) {
            progress.accept(100);
            return;
        }

        int total = targets.size();
        int processed = 0;
        for (RepositorySyncTarget target : targets) {
            synchronizeRepositoryInternal(target, accessToken);
            processed++;
            int percent = (int) Math.round((processed * 100.0) / total);
            progress.accept(Math.min(100, Math.max(0, percent)));
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void synchronizeRepository(Long repositoryId, String accessToken) {
        if (repositoryId == null) {
            throw new ValidationException("repositoryId must not be null");
        }
        RepositorySyncTarget target = executeInTransaction(() -> repositoryRepository
                .findByIdAndDeletedAtIsNull(repositoryId)
                .map(this::toSyncTarget)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found")));

        synchronizeRepositoryInternal(target, accessToken);
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void synchronizeRepository(Repository repository, String accessToken) {
        if (repository == null || repository.getId() == null) {
            return;
        }

        RepositorySyncTarget target = executeInTransaction(() -> repositoryRepository
                .findByIdAndDeletedAtIsNull(repository.getId())
                .map(this::toSyncTarget)
                .orElse(null));

        if (target != null) {
            synchronizeRepositoryInternal(target, accessToken);
        }
    }

    private void synchronizeRepositoryInternal(RepositorySyncTarget target, String accessToken) {
        if (target == null || target.id() == null) {
            return;
        }
        OwnerRepo ownerRepo = resolveOwnerAndName(target);
        if (ownerRepo == null || !StringUtils.hasText(accessToken)) {
            return;
        }

        OffsetDateTime attemptStartedAt = OffsetDateTime.now();
        String latestCommitSha = null;
        try {
            List<GitHubPullRequestSummary> summaries = gitHubRepositoryService.listPullRequestSummaries(
                    accessToken,
                    ownerRepo.owner(),
                    ownerRepo.name(),
                    MAX_PULL_REQUESTS);

            for (GitHubPullRequestSummary summary : summaries) {
                if (summary == null || summary.number() == null) {
                    continue;
                }
                Optional<GitHubPullRequest> detailOpt = gitHubRepositoryService.getPullRequest(
                        accessToken,
                        ownerRepo.owner(),
                        ownerRepo.name(),
                        summary.number());
                if (detailOpt.isEmpty()) {
                    continue;
                }
                GitHubPullRequest detail = detailOpt.get();
                List<GitHubPullRequestFile> files = organizationSyncProperties.isFetchPullRequestDetails()
                        ? gitHubRepositoryService.listPullRequestFiles(
                                accessToken,
                                ownerRepo.owner(),
                                ownerRepo.name(),
                                detail.number(),
                                MAX_PULL_REQUEST_FILES)
                        : List.of();
                executeInTransaction(() -> {
                    Repository managedRepository = getActiveRepository(target.id());
                    if (managedRepository == null) {
                        return;
                    }
                    persistPullRequest(managedRepository, detail, files);
                });
            }

            List<GitHubCommit> commits = gitHubRepositoryService.listCommits(
                    accessToken,
                    ownerRepo.owner(),
                    ownerRepo.name(),
                    MAX_COMMITS,
                    OffsetDateTime.now().minusDays(ACTIVITY_LOOKBACK_DAYS));

            for (GitHubCommit commit : commits) {
                if (commit == null || !StringUtils.hasText(commit.sha())) {
                    continue;
                }
                CommitUpsertResult upsertResult = executeInTransaction(() -> {
                    Repository managedRepository = getActiveRepository(target.id());
                    if (managedRepository == null) {
                        return null;
                    }
                    return persistCommit(managedRepository, commit);
                });
                if (upsertResult == null) {
                    continue;
                }

                if (organizationSyncProperties.isFetchCommitDetails()) {
                    if (!upsertResult.hasExistingFiles() && ownerRepo != null && StringUtils.hasText(accessToken)) {
            Optional<GitHubCommitDetail> detailOpt = gitHubRepositoryService.getCommit(
                                accessToken,
                                ownerRepo.owner(),
                                ownerRepo.name(),
                                commit.sha());
                        detailOpt.ifPresent(detail -> executeInTransaction(() -> {
                            synchronizeCommitFiles(upsertResult.commit(), detail.files());
                        }));
                    }
                } else {
                    executeInTransaction(() -> {
                        clearCommitFiles(upsertResult.commit());
                    });
                }
            }

            if (!commits.isEmpty() && commits.get(0) != null) {
                latestCommitSha = commits.get(0).sha();
            }

            String latestShaForStatus = latestCommitSha;
            OffsetDateTime finishedAt = OffsetDateTime.now();
            executeInTransaction(() -> {
                Repository managedRepository = getActiveRepository(target.id());
                if (managedRepository == null) {
                    return;
                }
                repositorySyncStatusService.markSynced(managedRepository, finishedAt, latestShaForStatus);
            });
        } catch (GitHubApiException e) {
            String repositoryName = StringUtils.hasText(target.fullName())
                    ? target.fullName()
                    : String.valueOf(target.id());
            log.warn("Failed to synchronize repository {}: {}", repositoryName, e.getMessage());
            String message = e.getMessage();
            executeInTransaction(() -> {
                Repository managedRepository = getActiveRepository(target.id());
                if (managedRepository == null) {
                    return;
                }
                repositorySyncStatusService.markFailure(managedRepository, attemptStartedAt, message);
            });
        } catch (RuntimeException e) {
            String message = e.getMessage();
            executeInTransaction(() -> {
                Repository managedRepository = getActiveRepository(target.id());
                if (managedRepository == null) {
                    return;
                }
                repositorySyncStatusService.markFailure(managedRepository, attemptStartedAt, message);
            });
            throw e;
        }
    }

    private void persistPullRequest(Repository repository, GitHubPullRequest pullRequest, List<GitHubPullRequestFile> files) {
        PullRequest entity = pullRequestRepository
                .findByRepositoryIdAndNumberAndDeletedAtIsNull(repository.getId(), pullRequest.number())
                .orElseGet(() -> PullRequest.builder()
                        .repository(repository)
                        .number(pullRequest.number())
                        .build());

        entity.setRepository(repository);
        entity.setGithubId(pullRequest.id());
        entity.setTitle(pullRequest.title());
        entity.setBody(pullRequest.body());
        entity.setState(pullRequest.state());
        entity.setMerged(pullRequest.merged());
        entity.setHtmlUrl(pullRequest.htmlUrl());
        entity.setAdditions(pullRequest.additions());
        entity.setDeletions(pullRequest.deletions());
        entity.setChangedFiles(pullRequest.changedFiles());
        entity.setCreatedAt(pullRequest.createdAt());
        entity.setUpdatedAt(pullRequest.updatedAt());
        entity.setMergedAt(pullRequest.mergedAt());
        entity.setClosedAt(pullRequest.closedAt());
        entity.setAuthor(toUser(pullRequest.author()));
        entity.setMergedBy(toUser(pullRequest.mergedBy()));
        entity.setDeletedAt(null);

        PullRequest saved = pullRequestRepository.save(entity);
        if (organizationSyncProperties.isFetchPullRequestDetails()) {
            synchronizePullRequestFiles(saved, files);
        } else {
            clearPullRequestFiles(saved);
        }
    }

    private void synchronizePullRequestFiles(PullRequest pullRequest, List<GitHubPullRequestFile> files) {
        Map<String, PullRequestFile> existing = new HashMap<>();
        List<PullRequestFile> current = pullRequestFileRepository
                .findByPullRequestIdAndDeletedAtIsNullOrderByPathAsc(pullRequest.getId());
        for (PullRequestFile file : current) {
            if (file.getPath() != null) {
                existing.put(file.getPath(), file);
            }
        }

        Set<String> incomingPaths = new HashSet<>();
        if (files != null) {
            for (GitHubPullRequestFile file : files) {
                if (file == null || !StringUtils.hasText(file.path())) {
                    continue;
                }
                incomingPaths.add(file.path());
                PullRequestFile entity = existing.getOrDefault(file.path(), PullRequestFile.builder()
                        .pullRequest(pullRequest)
                        .path(file.path())
                        .build());

                entity.setPullRequest(pullRequest);
                entity.setPath(file.path());
                entity.setExtension(extractExtension(file.path()));
                entity.setAdditions(file.additions());
                entity.setDeletions(file.deletions());
                entity.setChanges(file.changes());
                entity.setRawBlobUrl(file.rawUrl());
                entity.setDeletedAt(null);

                pullRequestFileRepository.save(entity);
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<String, PullRequestFile> entry : existing.entrySet()) {
            if (!incomingPaths.contains(entry.getKey())) {
                PullRequestFile obsolete = entry.getValue();
                obsolete.setDeletedAt(now);
                pullRequestFileRepository.save(obsolete);
            }
        }
    }

    private CommitUpsertResult persistCommit(Repository repository, GitHubCommit commit) {
        if (commit == null || !StringUtils.hasText(commit.sha())) {
            return null;
        }
        GitCommit entity = gitCommitRepository
                .findByRepositoryIdAndShaAndDeletedAtIsNull(repository.getId(), commit.sha())
                .orElseGet(() -> GitCommit.builder()
                        .repository(repository)
                        .sha(commit.sha())
                        .build());

        entity.setRepository(repository);
        entity.setMessage(commit.message());
        entity.setHtmlUrl(commit.htmlUrl());
        entity.setAuthorName(commit.authorName());
        entity.setAuthorEmail(commit.authorEmail());
        entity.setCommitterName(commit.committerName());
        entity.setCommitterEmail(commit.committerEmail());
        OffsetDateTime committedAt = commit.committedAt() != null ? commit.committedAt() : commit.authoredAt();
        entity.setCommittedAt(committedAt);
        entity.setPushedAt(commit.committedAt());
        entity.setDeletedAt(null);

        GitCommit saved = gitCommitRepository.save(entity);

        boolean hasFiles = commitFileRepository.existsByCommitIdAndDeletedAtIsNull(saved.getId());
        return new CommitUpsertResult(saved, hasFiles);
    }

    private void synchronizeCommitFiles(GitCommit commit,
                                        List<GitHubCommitFile> files) {
        Map<String, CommitFile> existing = new HashMap<>();
        List<CommitFile> current = commitFileRepository
                .findByCommitIdAndDeletedAtIsNullOrderByPathAsc(commit.getId());
        for (CommitFile file : current) {
            if (StringUtils.hasText(file.getPath())) {
                existing.put(file.getPath(), file);
            }
        }

        Set<String> incomingPaths = new HashSet<>();
        if (files != null) {
            for (GitHubCommitFile file : files) {
                if (file == null || !StringUtils.hasText(file.path())) {
                    continue;
                }
                incomingPaths.add(file.path());
                CommitFile entity = existing.getOrDefault(file.path(), CommitFile.builder()
                        .commit(commit)
                        .path(file.path())
                        .build());

                entity.setCommit(commit);
                entity.setPath(file.path());
                entity.setExtension(extractExtension(file.path()));
                entity.setStatus(file.status());
                entity.setAdditions(file.additions());
                entity.setDeletions(file.deletions());
                entity.setChanges(file.changes());
                entity.setRawBlobUrl(file.rawUrl());
                entity.setDeletedAt(null);

                commitFileRepository.save(entity);
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (Map.Entry<String, CommitFile> entry : existing.entrySet()) {
            if (!incomingPaths.contains(entry.getKey())) {
                CommitFile obsolete = entry.getValue();
                obsolete.setDeletedAt(now);
                commitFileRepository.save(obsolete);
            }
        }
    }

    private void clearCommitFiles(GitCommit commit) {
        if (commit == null || commit.getId() == null) {
            return;
        }
        List<CommitFile> current = commitFileRepository
                .findByCommitIdAndDeletedAtIsNullOrderByPathAsc(commit.getId());
        if (current.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (CommitFile file : current) {
            file.setDeletedAt(now);
            commitFileRepository.save(file);
        }
    }

    private void clearPullRequestFiles(PullRequest pullRequest) {
        if (pullRequest == null || pullRequest.getId() == null) {
            return;
        }
        List<PullRequestFile> current = pullRequestFileRepository
                .findByPullRequestIdAndDeletedAtIsNullOrderByPathAsc(pullRequest.getId());
        if (current.isEmpty()) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        for (PullRequestFile file : current) {
            file.setDeletedAt(now);
            pullRequestFileRepository.save(file);
        }
    }

    private void executeInTransaction(Runnable action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.executeWithoutResult(status -> action.run());
    }

    private <T> T executeInTransaction(Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return template.execute(status -> action.get());
    }

    private User toUser(GitHubSimpleUser simpleUser) {
        if (simpleUser == null || simpleUser.id() == null || !StringUtils.hasText(simpleUser.login())) {
            return null;
        }
        return userService.upsertGitHubUser(simpleUser.id(), simpleUser.login(), null, simpleUser.avatarUrl());
    }

    private OwnerRepo resolveOwnerAndName(RepositorySyncTarget target) {
        if (target == null) {
            return null;
        }
        if (StringUtils.hasText(target.fullName()) && target.fullName().contains("/")) {
            String[] parts = target.fullName().split("/", 2);
            if (parts.length == 2 && StringUtils.hasText(parts[0]) && StringUtils.hasText(parts[1])) {
                return new OwnerRepo(parts[0], parts[1]);
            }
        }
        if (StringUtils.hasText(target.ownerLogin()) && StringUtils.hasText(target.name())) {
            return new OwnerRepo(target.ownerLogin(), target.name());
        }
        if (StringUtils.hasText(target.organizationLogin()) && StringUtils.hasText(target.name())) {
            return new OwnerRepo(target.organizationLogin(), target.name());
        }
        return null;
    }

    private RepositorySyncTarget toSyncTarget(Repository repository) {
        if (repository == null || repository.getId() == null) {
            return null;
        }

        String ownerLogin = StringUtils.hasText(repository.getOwnerLogin())
                ? repository.getOwnerLogin().trim()
                : null;
        String name = StringUtils.hasText(repository.getName()) ? repository.getName().trim() : null;
        String fullName = StringUtils.hasText(repository.getFullName()) ? repository.getFullName().trim() : null;
        Long organizationId = null;
        String organizationLogin = null;

        Organization repoOrganization = repository.getOrganization();
        if (repoOrganization != null) {
            organizationId = repoOrganization.getId();
            if (!StringUtils.hasText(ownerLogin) && StringUtils.hasText(repoOrganization.getLogin())) {
                ownerLogin = repoOrganization.getLogin().trim();
            }
            if (!StringUtils.hasText(fullName) && StringUtils.hasText(name) && StringUtils.hasText(ownerLogin)) {
                fullName = ownerLogin + "/" + name;
            }
            if (StringUtils.hasText(repoOrganization.getLogin())) {
                organizationLogin = repoOrganization.getLogin().trim();
            }
        }

        return new RepositorySyncTarget(repository.getId(), ownerLogin, name, fullName, organizationId, organizationLogin);
    }

    private Repository getActiveRepository(Long repositoryId) {
        return repositoryRepository.findByIdAndDeletedAtIsNull(repositoryId)
                .orElse(null);
    }

    private String extractExtension(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        int slashIndex = path.lastIndexOf('/');
        int dotIndex = path.lastIndexOf('.');
        if (dotIndex <= slashIndex || dotIndex == -1 || dotIndex == path.length() - 1) {
            return null;
        }
        return path.substring(dotIndex + 1);
    }

    private record OwnerRepo(String owner, String name) {
    }

    private record CommitUpsertResult(GitCommit commit, boolean hasExistingFiles) {
    }

    private record RepositorySyncTarget(
            Long id,
            String ownerLogin,
            String name,
            String fullName,
            Long organizationId,
            String organizationLogin) {
    }
}

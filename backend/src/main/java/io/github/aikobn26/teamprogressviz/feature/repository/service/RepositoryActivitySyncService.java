package io.github.aikobn26.teamprogressviz.feature.repository.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.feature.github.controller.GitHubApiException;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubCommit;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubCommitDetail;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubCommitFile;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubPullRequest;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubPullRequestFile;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubPullRequestSummary;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubRepositoryService.GitHubSimpleUser;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.Organization;
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
@Transactional
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

    public void synchronizeActivities(Organization organization, String accessToken) {
        if (organization == null || organization.getId() == null) {
            return;
        }
        List<Repository> repositories = repositoryRepository.findByOrganizationAndDeletedAtIsNull(organization);
        for (Repository repository : repositories) {
            synchronizeRepositoryInternal(repository, accessToken);
        }
    }

    public void synchronizeRepository(Long repositoryId, String accessToken) {
        if (repositoryId == null) {
            throw new ValidationException("repositoryId must not be null");
        }
        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));
        synchronizeRepositoryInternal(repository, accessToken);
    }

    public void synchronizeRepository(Repository repository, String accessToken) {
        synchronizeRepositoryInternal(repository, accessToken);
    }

    private void synchronizeRepositoryInternal(Repository repository, String accessToken) {
        if (repository == null || repository.getId() == null) {
            return;
        }
        OwnerRepo ownerRepo = resolveOwnerAndName(repository);
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
                List<GitHubPullRequestFile> files = gitHubRepositoryService.listPullRequestFiles(
                        accessToken,
                        ownerRepo.owner(),
                        ownerRepo.name(),
                        detail.number(),
                        MAX_PULL_REQUEST_FILES);
                persistPullRequest(repository, detail, files);
            }

            List<GitHubCommit> commits = gitHubRepositoryService.listCommits(
                    accessToken,
                    ownerRepo.owner(),
                    ownerRepo.name(),
                    MAX_COMMITS,
                    OffsetDateTime.now().minusDays(ACTIVITY_LOOKBACK_DAYS));

            for (GitHubCommit commit : commits) {
                persistCommit(repository, ownerRepo, accessToken, commit);
            }

            if (!commits.isEmpty() && commits.get(0) != null) {
                latestCommitSha = commits.get(0).sha();
            }

            repositorySyncStatusService.markSynced(repository, OffsetDateTime.now(), latestCommitSha);
        } catch (GitHubApiException e) {
            log.warn("Failed to synchronize repository {}: {}", repository.getFullName(), e.getMessage());
            repositorySyncStatusService.markFailure(repository, attemptStartedAt, e.getMessage());
        } catch (RuntimeException e) {
            repositorySyncStatusService.markFailure(repository, attemptStartedAt, e.getMessage());
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
        synchronizePullRequestFiles(saved, files);
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

    private void persistCommit(Repository repository,
                               OwnerRepo ownerRepo,
                               String accessToken,
                               GitHubCommit commit) {
        if (commit == null || !StringUtils.hasText(commit.sha())) {
            return;
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
        if (!hasFiles && ownerRepo != null && StringUtils.hasText(accessToken)) {
            Optional<GitHubCommitDetail> detailOpt = gitHubRepositoryService.getCommit(
                    accessToken,
                    ownerRepo.owner(),
                    ownerRepo.name(),
                    commit.sha());
            detailOpt.ifPresent(detail -> synchronizeCommitFiles(saved, detail.files()));
        }
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

    private User toUser(GitHubSimpleUser simpleUser) {
        if (simpleUser == null || simpleUser.id() == null || !StringUtils.hasText(simpleUser.login())) {
            return null;
        }
        return userService.upsertGitHubUser(simpleUser.id(), simpleUser.login(), null, simpleUser.avatarUrl());
    }

    private OwnerRepo resolveOwnerAndName(Repository repository) {
        if (repository == null) {
            return null;
        }
        if (StringUtils.hasText(repository.getFullName()) && repository.getFullName().contains("/")) {
            String[] parts = repository.getFullName().split("/", 2);
            if (parts.length == 2 && StringUtils.hasText(parts[0]) && StringUtils.hasText(parts[1])) {
                return new OwnerRepo(parts[0], parts[1]);
            }
        }
        if (repository.getOrganization() != null && StringUtils.hasText(repository.getOrganization().getLogin()) && StringUtils.hasText(repository.getName())) {
            return new OwnerRepo(repository.getOrganization().getLogin(), repository.getName());
        }
        return null;
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
}

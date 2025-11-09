package io.github.aikobn26.teamprogressviz.feature.repository.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.feature.organization.entity.Organization;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitDetailResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitFileResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitListItemResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.CommitFile;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.GitCommit;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.Repository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.CommitFileRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.GitCommitRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.RepositoryRepository;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.shared.exception.ResourceNotFoundException;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommitService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrganizationService organizationService;
    private final RepositoryRepository repositoryRepository;
    private final GitCommitRepository gitCommitRepository;
    private final CommitFileRepository commitFileRepository;

    public List<CommitListItemResponse> listCommits(User user,
                                                    Long repositoryId,
                                                    Integer limit,
                                                    Integer page) {
        Repository repository = requireAccessibleRepository(user, repositoryId);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(limit),
                Sort.by(Sort.Direction.DESC, "committedAt", "createdAt", "id"));

        Page<GitCommit> commits = gitCommitRepository.findByRepositoryIdAndDeletedAtIsNull(repository.getId(), pageable);
        return commits.getContent().stream()
                .map(this::toListItem)
                .toList();
    }

    public Mono<List<CommitListItemResponse>> listCommitsReactive(User user,
                                  Long repositoryId,
                                  Integer limit,
                                  Integer page) {
    return Mono.fromCallable(() -> listCommits(user, repositoryId, limit, page))
        .subscribeOn(Schedulers.boundedElastic());
    }

    public CommitDetailResponse getCommit(User user,
                                          Long repositoryId,
                                          String sha) {
        Repository repository = requireAccessibleRepository(user, repositoryId);
        if (!StringUtils.hasText(sha)) {
            throw new ValidationException("sha must not be blank");
        }

        GitCommit commit = gitCommitRepository
                .findByRepositoryIdAndShaAndDeletedAtIsNull(repository.getId(), sha)
                .orElseThrow(() -> new ResourceNotFoundException("Commit not found"));
        return toDetail(commit);
    }

    public Mono<CommitDetailResponse> getCommitReactive(User user,
                            Long repositoryId,
                            String sha) {
    return Mono.fromCallable(() -> getCommit(user, repositoryId, sha))
        .subscribeOn(Schedulers.boundedElastic());
    }

    public List<CommitFileResponse> listFiles(User user,
                                              Long repositoryId,
                                              String sha) {
        Repository repository = requireAccessibleRepository(user, repositoryId);
        if (!StringUtils.hasText(sha)) {
            throw new ValidationException("sha must not be blank");
        }

        GitCommit commit = gitCommitRepository
                .findByRepositoryIdAndShaAndDeletedAtIsNull(repository.getId(), sha)
                .orElseThrow(() -> new ResourceNotFoundException("Commit not found"));

        return commitFileRepository
                .findByCommitIdAndDeletedAtIsNullOrderByPathAsc(commit.getId())
                .stream()
                .map(this::toFileResponse)
                .toList();
    }

    public Mono<List<CommitFileResponse>> listFilesReactive(User user,
                                Long repositoryId,
                                String sha) {
    return Mono.fromCallable(() -> listFiles(user, repositoryId, sha))
        .subscribeOn(Schedulers.boundedElastic());
    }

    private Repository requireAccessibleRepository(User user, Long repositoryId) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (repositoryId == null) {
            throw new ValidationException("repositoryId must not be null");
        }

        Repository repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Repository not found"));
        Organization organization = repository.getOrganization();
        if (organization == null || organization.getId() == null) {
            throw new ResourceNotFoundException("Repository organization not found");
        }
        organizationService.getAccessibleOrganization(user, organization.getId());
        return repository;
    }

    private int normalizePage(Integer page) {
        if (page == null) {
            return 0;
        }
        if (page < 0) {
            throw new ValidationException("page must be 0 or greater");
        }
        return page;
    }

    private int normalizeSize(Integer limit) {
        if (limit == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (limit < 1) {
            throw new ValidationException("limit must be greater than 0");
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }

    private CommitListItemResponse toListItem(GitCommit commit) {
        Repository repository = commit.getRepository();
        String repositoryFullName = repository != null ? repository.getFullName() : null;
        return new CommitListItemResponse(
                commit.getId(),
                commit.getSha(),
                commit.getMessage(),
                repositoryFullName,
                commit.getAuthorName(),
                commit.getCommitterName(),
                commit.getCommittedAt(),
                commit.getHtmlUrl());
    }

    private CommitDetailResponse toDetail(GitCommit commit) {
        Repository repository = commit.getRepository();
        Long repoId = repository != null ? repository.getId() : null;
        String repositoryFullName = repository != null ? repository.getFullName() : null;
        return new CommitDetailResponse(
                commit.getId(),
                commit.getSha(),
                repoId,
                repositoryFullName,
                commit.getMessage(),
                commit.getHtmlUrl(),
                commit.getAuthorName(),
                commit.getAuthorEmail(),
                commit.getCommitterName(),
                commit.getCommitterEmail(),
                commit.getCommittedAt(),
                commit.getPushedAt());
    }

    private CommitFileResponse toFileResponse(CommitFile file) {
        return new CommitFileResponse(
                file.getId(),
                file.getPath(),
                file.getFilename(),
                file.getExtension(),
                file.getStatus(),
                file.getAdditions(),
                file.getDeletions(),
                file.getChanges(),
                file.getRawBlobUrl());
    }
}

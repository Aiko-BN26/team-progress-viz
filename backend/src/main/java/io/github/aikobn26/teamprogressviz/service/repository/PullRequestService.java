package io.github.aikobn26.teamprogressviz.service.repository;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.dto.response.PullRequestDetailResponse;
import io.github.aikobn26.teamprogressviz.dto.response.PullRequestFeedResponse;
import io.github.aikobn26.teamprogressviz.dto.response.PullRequestFileResponse;
import io.github.aikobn26.teamprogressviz.dto.response.PullRequestListItemResponse;
import io.github.aikobn26.teamprogressviz.entity.Organization;
import io.github.aikobn26.teamprogressviz.entity.PullRequest;
import io.github.aikobn26.teamprogressviz.entity.PullRequestFile;
import io.github.aikobn26.teamprogressviz.entity.Repository;
import io.github.aikobn26.teamprogressviz.entity.User;
import io.github.aikobn26.teamprogressviz.exception.ResourceNotFoundException;
import io.github.aikobn26.teamprogressviz.exception.ValidationException;
import io.github.aikobn26.teamprogressviz.repository.PullRequestFileRepository;
import io.github.aikobn26.teamprogressviz.repository.PullRequestRepository;
import io.github.aikobn26.teamprogressviz.repository.RepositoryRepository;
import io.github.aikobn26.teamprogressviz.service.organization.OrganizationService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PullRequestService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final OrganizationService organizationService;
    private final RepositoryRepository repositoryRepository;
    private final PullRequestRepository pullRequestRepository;
    private final PullRequestFileRepository pullRequestFileRepository;

    public List<PullRequestListItemResponse> listPullRequests(User user,
                                                              Long repositoryId,
                                                              String state,
                                                              Integer limit,
                                                              Integer page) {
        Repository repository = requireAccessibleRepository(user, repositoryId);
        Pageable pageable = PageRequest.of(normalizePage(page), normalizeSize(limit), Sort.by(Sort.Direction.DESC, "updatedAt", "createdAt"));

        Page<PullRequest> result;
        String normalizedState = normalizeState(state);
        if (normalizedState == null) {
            result = pullRequestRepository.findByRepositoryIdAndDeletedAtIsNull(repository.getId(), pageable);
        } else {
            result = pullRequestRepository
                    .findByRepositoryIdAndStateInAndDeletedAtIsNull(
                            repository.getId(),
                            List.of(normalizedState),
                            pageable);
        }

    return result.getContent().stream()
        .map(this::toListItem)
        .toList();
    }

    public PullRequestDetailResponse getPullRequest(User user,
                                                    Long repositoryId,
                                                    Integer pullNumber) {
        Repository repository = requireAccessibleRepository(user, repositoryId);
        PullRequest pullRequest = pullRequestRepository
                .findByRepositoryIdAndNumberAndDeletedAtIsNull(repository.getId(), pullNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pull request not found"));
        return toDetail(pullRequest);
    }

    public List<PullRequestFileResponse> listFiles(User user,
                                                   Long repositoryId,
                                                   Integer pullNumber) {
        Repository repository = requireAccessibleRepository(user, repositoryId);
        PullRequest pullRequest = pullRequestRepository
                .findByRepositoryIdAndNumberAndDeletedAtIsNull(repository.getId(), pullNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Pull request not found"));

        return pullRequestFileRepository
                .findByPullRequestIdAndDeletedAtIsNullOrderByPathAsc(pullRequest.getId())
                .stream()
                .map(this::toFileResponse)
                .toList();
    }

    public PullRequestFeedResponse fetchFeed(User user,
                                             Long organizationId,
                                             Long cursor,
                                             Integer limit) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        int pageSize = normalizeSize(limit);
        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "updatedAt", "id"));

        Page<PullRequest> page;
        if (cursor != null && cursor > 0) {
            page = pullRequestRepository
                    .findByRepositoryOrganizationIdAndIdLessThanAndDeletedAtIsNull(organization.getId(), cursor, pageable);
        } else {
            page = pullRequestRepository
                    .findByRepositoryOrganizationIdAndDeletedAtIsNull(organization.getId(), pageable);
        }

        List<PullRequestFeedResponse.Item> items = page.getContent().stream()
                .map(this::toFeedItem)
                .toList();

        String nextCursor = null;
        if (!items.isEmpty() && items.size() == pageSize) {
            PullRequestFeedResponse.Item last = items.get(items.size() - 1);
            nextCursor = last.id() != null ? String.valueOf(last.id()) : null;
        }

        return new PullRequestFeedResponse(items, nextCursor);
    }

    public Repository requireAccessibleRepository(User user, Long repositoryId) {
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

    private String normalizeState(String state) {
        if (!StringUtils.hasText(state) || "all".equalsIgnoreCase(state)) {
            return null;
        }
        return state.toLowerCase(Locale.ROOT);
    }

    private PullRequestListItemResponse toListItem(PullRequest pullRequest) {
    PullRequestListItemResponse.UserSummary user = toListUserSummary(pullRequest.getAuthor());
        String repositoryFullName = pullRequest.getRepository() != null ? pullRequest.getRepository().getFullName() : null;
        return new PullRequestListItemResponse(
                pullRequest.getId(),
                pullRequest.getNumber(),
                pullRequest.getTitle(),
                pullRequest.getState(),
                user,
                pullRequest.getCreatedAt(),
                pullRequest.getUpdatedAt(),
                repositoryFullName
        );
    }

    private PullRequestDetailResponse toDetail(PullRequest pullRequest) {
        String repositoryFullName = pullRequest.getRepository() != null ? pullRequest.getRepository().getFullName() : null;
    return new PullRequestDetailResponse(
                pullRequest.getId(),
                pullRequest.getNumber(),
                pullRequest.getTitle(),
                pullRequest.getBody(),
                pullRequest.getState(),
                pullRequest.getMerged(),
                pullRequest.getHtmlUrl(),
        toDetailUserSummary(pullRequest.getAuthor()),
        toDetailUserSummary(pullRequest.getMergedBy()),
                pullRequest.getAdditions(),
                pullRequest.getDeletions(),
                pullRequest.getChangedFiles(),
                pullRequest.getCreatedAt(),
                pullRequest.getUpdatedAt(),
                pullRequest.getMergedAt(),
                pullRequest.getClosedAt(),
                repositoryFullName
        );
    }

    private PullRequestFileResponse toFileResponse(PullRequestFile file) {
        return new PullRequestFileResponse(
                file.getId(),
                file.getPath(),
                file.getExtension(),
                file.getAdditions(),
                file.getDeletions(),
                file.getChanges(),
                file.getRawBlobUrl()
        );
    }

    private PullRequestFeedResponse.Item toFeedItem(PullRequest pullRequest) {
        PullRequestFeedResponse.PullRequestUser user = null;
        if (pullRequest.getAuthor() != null) {
            User author = pullRequest.getAuthor();
            user = new PullRequestFeedResponse.PullRequestUser(
                    author.getId(),
                    author.getGithubId(),
                    author.getLogin(),
                    author.getAvatarUrl()
            );
        }
        String repositoryFullName = pullRequest.getRepository() != null ? pullRequest.getRepository().getFullName() : null;
        return new PullRequestFeedResponse.Item(
                pullRequest.getId(),
                pullRequest.getNumber(),
                pullRequest.getTitle(),
                repositoryFullName,
                pullRequest.getState(),
                user,
                pullRequest.getCreatedAt(),
                pullRequest.getUpdatedAt(),
                pullRequest.getHtmlUrl()
        );
    }

    private PullRequestListItemResponse.UserSummary toListUserSummary(User author) {
        if (author == null || author.getId() == null) {
            return null;
        }
        return new PullRequestListItemResponse.UserSummary(
                author.getId(),
                author.getGithubId(),
                author.getLogin(),
                author.getAvatarUrl()
        );
    }

    private PullRequestDetailResponse.UserSummary toDetailUserSummary(User author) {
        if (author == null || author.getId() == null) {
            return null;
        }
        return new PullRequestDetailResponse.UserSummary(
                author.getId(),
                author.getGithubId(),
                author.getLogin(),
                author.getAvatarUrl()
        );
    }
}

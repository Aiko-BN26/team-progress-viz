package io.github.aikobn26.teamprogressviz.feature.organization.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubOrganization;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubOrganizationMember;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubRepository;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubOrganizationService;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.Comment;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.Organization;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.RepositorySyncStatus;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.UserOrganization;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.CommentRepository;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.OrganizationRepository;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.UserOrganizationRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.GitCommit;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.PullRequest;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.Repository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.GitCommitRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.PullRequestRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.RepositoryRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.service.RepositoryActivitySyncService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.shared.exception.ForbiddenException;
import io.github.aikobn26.teamprogressviz.shared.exception.ResourceConflictException;
import io.github.aikobn26.teamprogressviz.shared.exception.ResourceNotFoundException;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final RepositoryRepository repositoryRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final UserService userService;
    private final GitHubOrganizationService gitHubOrganizationService;
    private final RepositorySyncStatusService repositorySyncStatusService;
    private final RepositoryActivitySyncService repositoryActivitySyncService;
    private final GitCommitRepository gitCommitRepository;
    private final PullRequestRepository pullRequestRepository;
    private final CommentRepository commentRepository;

    private static final int RECENT_PULL_REQUEST_LIMIT = 10;
    private static final int RECENT_COMMIT_LIMIT = 20;
    private static final int RECENT_COMMENT_LIMIT = 20;
    private static final int SUMMARY_WINDOW_DAYS = 7;

    @Transactional(readOnly = true)
    public List<Organization> listOrganizations(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        return userOrganizationRepository.findByUserIdAndDeletedAtIsNull(user.getId()).stream()
                .map(UserOrganization::getOrganization)
                .filter(org -> org != null && !org.isDeleted())
                .toList();
    }

    @Transactional(readOnly = true)
    public Organization getAccessibleOrganization(User user, Long organizationId) {
        return requireActiveMembership(user, organizationId).getOrganization();
    }

    @Transactional(readOnly = true)
    public List<RepositorySyncStatusView> listRepositorySyncStatus(User user, Long organizationId) {
        UserOrganization membership = requireActiveMembership(user, organizationId);
        Organization organization = membership.getOrganization();
        return repositorySyncStatusService.findActiveByOrganization(organization.getId()).stream()
                .map(this::toSyncStatusView)
                .toList();
    }

    private RepositorySyncStatusView toSyncStatusView(RepositorySyncStatus status) {
        Repository repository = status.getRepository();
        Long repositoryId = repository != null ? repository.getId() : null;
        String fullName = repository != null ? resolveRepositoryFullName(repository) : null;
        return new RepositorySyncStatusView(repositoryId, fullName, status.getLastSyncedAt(),
                status.getLastSyncedCommitSha(), status.getErrorMessage());
    }

    private String resolveRepositoryFullName(Repository repository) {
        if (repository == null) {
            return null;
        }
        if (StringUtils.hasText(repository.getFullName())) {
            return repository.getFullName();
        }
        Organization organization = repository.getOrganization();
        if (organization != null && StringUtils.hasText(organization.getLogin())
                && StringUtils.hasText(repository.getName())) {
            return organization.getLogin() + "/" + repository.getName();
        }
        return repository.getName();
    }

    public OrganizationSyncResult registerOrganization(User user,
            String login,
            String defaultLinkUrl,
            String accessToken) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (!StringUtils.hasText(login)) {
            throw new ValidationException("'login' must not be blank");
        }
        if (!StringUtils.hasText(accessToken)) {
            throw new ValidationException("GitHub access token is required");
        }

        String normalizedLogin = login.trim();
        GitHubOrganization gitHubOrganization = gitHubOrganizationService.getOrganization(accessToken, normalizedLogin)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Organization not found on GitHub: " + normalizedLogin));

        Organization organization = resolveOrganization(gitHubOrganization, defaultLinkUrl);
        Organization savedOrganization = organizationRepository.save(organization);

        ensureMembership(user, savedOrganization);

        return new OrganizationSyncResult(savedOrganization, gitHubOrganization, 0);
    }

    public OrganizationSyncResult synchronizeOrganization(Long organizationId, String accessToken) {
        if (organizationId == null) {
            throw new ValidationException("organizationId must not be null");
        }
        if (!StringUtils.hasText(accessToken)) {
            throw new ValidationException("GitHub access token is required");
        }

        Organization organization = organizationRepository.findByIdAndDeletedAtIsNull(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        GitHubOrganization gitHubOrganization = gitHubOrganizationService
                .getOrganization(accessToken, organization.getLogin())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Organization not found on GitHub: " + organization.getLogin()));

        updateOrganizationFields(organization, gitHubOrganization, organization.getDefaultLinkUrl());
        Organization savedOrganization = organizationRepository.save(organization);

        List<GitHubRepository> gitHubRepositories = gitHubOrganizationService
                .listRepositories(accessToken, savedOrganization.getLogin());
        int syncedRepositories = syncRepositories(savedOrganization, gitHubRepositories);

        List<GitHubOrganizationMember> gitHubMembers = gitHubOrganizationService
                .listMembers(accessToken, savedOrganization.getLogin());
        syncOrganizationMembers(savedOrganization, gitHubMembers);

        repositoryActivitySyncService.synchronizeActivities(savedOrganization, accessToken);

        return new OrganizationSyncResult(savedOrganization, gitHubOrganization, syncedRepositories);
    }

    private Organization resolveOrganization(GitHubOrganization gitHubOrganization, String defaultLinkUrl) {
        if (gitHubOrganization.id() == null) {
            throw new ValidationException("GitHub organization id is missing");
        }

        Optional<Organization> existing = organizationRepository.findByGithubId(gitHubOrganization.id());

        if (existing.isPresent()) {
            Organization organization = existing.get();
            if (!organization.isDeleted()) {
                updateOrganizationFields(organization, gitHubOrganization, defaultLinkUrl);
                return organization;
            }
            organization.setDeletedAt(null);
            updateOrganizationFields(organization, gitHubOrganization, defaultLinkUrl);
            return organization;
        }

        Organization organization = new Organization();
        updateOrganizationFields(organization, gitHubOrganization, defaultLinkUrl);
        return organization;
    }

    private void updateOrganizationFields(Organization organization,
            GitHubOrganization gitHubOrganization,
            String defaultLinkUrl) {
        organization.setGithubId(gitHubOrganization.id());
        organization.setLogin(gitHubOrganization.login());
        organization.setName(gitHubOrganization.name());
        organization.setDescription(gitHubOrganization.description());
        organization.setAvatarUrl(gitHubOrganization.avatarUrl());
        organization.setHtmlUrl(gitHubOrganization.htmlUrl());
        if (StringUtils.hasText(defaultLinkUrl)) {
            organization.setDefaultLinkUrl(defaultLinkUrl);
        } else if (organization.getDefaultLinkUrl() == null) {
            organization.setDefaultLinkUrl(null);
        }
    }

    private void ensureMembership(User user, Organization organization) {
        Optional<UserOrganization> membershipOpt = userOrganizationRepository
                .findByUserIdAndOrganizationId(user.getId(), organization.getId());

        if (membershipOpt.isPresent()) {
            UserOrganization membership = membershipOpt.get();
            if (!membership.isDeleted()) {
                throw new ResourceConflictException("Organization already registered");
            }
            membership.setDeletedAt(null);
            membership.setJoinedAt(OffsetDateTime.now());
            membership.setRole(defaultRole(membership.getRole()));
            userOrganizationRepository.save(membership);
            return;
        }

        String role = userOrganizationRepository.existsByOrganizationIdAndDeletedAtIsNull(organization.getId())
                ? "member"
                : "admin";
        UserOrganization membership = UserOrganization.builder()
                .user(user)
                .organization(organization)
                .role(role)
                .build();
        userOrganizationRepository.save(membership);
    }

    private String defaultRole(String value) {
        return StringUtils.hasText(value) ? value : "member";
    }

    private UserOrganization requireActiveMembership(User user, Long organizationId) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }
        if (organizationId == null) {
            throw new ValidationException("organizationId must not be null");
        }

        UserOrganization membership = userOrganizationRepository
                .findByUserIdAndOrganizationIdAndDeletedAtIsNull(user.getId(), organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        Organization organization = membership.getOrganization();
        if (organization == null || organization.isDeleted()) {
            throw new ResourceNotFoundException("Organization not found");
        }
        return membership;
    }

    private void syncOrganizationMembers(Organization organization, List<GitHubOrganizationMember> gitHubMembers) {
        if (organization == null || organization.getId() == null) {
            return;
        }
        if (gitHubMembers == null || gitHubMembers.isEmpty()) {
            return;
        }

        Map<Long, UserOrganization> activeMemberships = userOrganizationRepository
                .findByOrganizationIdAndDeletedAtIsNull(organization.getId()).stream()
                .filter(membership -> membership.getUser() != null && membership.getUser().getGithubId() != null)
                .collect(Collectors.toMap(membership -> membership.getUser().getGithubId(), Function.identity()));

        for (GitHubOrganizationMember member : gitHubMembers) {
            if (member == null || member.id() == null || !StringUtils.hasText(member.login())) {
                continue;
            }

            User user = userService.upsertGitHubUser(member.id(), member.login(), null, member.avatarUrl());

            UserOrganization membership = userOrganizationRepository
                    .findByUserIdAndOrganizationId(user.getId(), organization.getId())
                    .orElseGet(() -> UserOrganization.builder()
                            .user(user)
                            .organization(organization)
                            .build());

            if (membership.isDeleted()) {
                membership.setDeletedAt(null);
                if (membership.getJoinedAt() == null) {
                    membership.setJoinedAt(OffsetDateTime.now());
                }
            }
            if (!StringUtils.hasText(membership.getRole())) {
                membership.setRole("member");
            }

            userOrganizationRepository.save(membership);
            activeMemberships.remove(user.getGithubId());
        }

        if (activeMemberships.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (UserOrganization membership : activeMemberships.values()) {
            if (membership.isDeleted()) {
                continue;
            }
            membership.setDeletedAt(now);
            userOrganizationRepository.save(membership);
        }
    }

    @Transactional(readOnly = true)
    public OrganizationDetail getOrganizationDetail(User user, Long organizationId) {
        UserOrganization membership = requireActiveMembership(user, organizationId);
        Organization organization = membership.getOrganization();

        List<UserOrganization> memberships = userOrganizationRepository
                .findByOrganizationIdAndDeletedAtIsNull(organization.getId());
        List<MemberDetail> members = memberships.stream()
                .map(this::toMemberDetail)
                .toList();

        List<Repository> repositories = repositoryRepository.findByOrganizationAndDeletedAtIsNull(organization);

        ActivitySummary activitySummary = buildActivitySummary(organization, members.size());
        PullRequestSummary pullRequestSummary = buildPullRequestSummary(organization);
        List<PullRequestDetail> pullRequestFeed = loadRecentPullRequests(organization.getId());
        List<CommitDetail> commitFeed = loadRecentCommits(organization.getId());
        List<CommentDetail> commentFeed = loadRecentComments(organization.getId());

        return new OrganizationDetail(
                organization,
                members,
                repositories,
                activitySummary,
                pullRequestSummary,
                pullRequestFeed,
                commitFeed,
                commentFeed);
    }

    private ActivitySummary buildActivitySummary(Organization organization, int memberCount) {
        if (organization == null || organization.getId() == null) {
            return new ActivitySummary(0L, 0L, 0L, memberCount);
        }

        OffsetDateTime since = OffsetDateTime.now().minusDays(SUMMARY_WINDOW_DAYS);
        long commitCount = gitCommitRepository.countRecentCommits(organization.getId(), since);
        long additions = pullRequestRepository.sumAdditionsSince(organization.getId(), since);
        long deletions = pullRequestRepository.sumDeletionsSince(organization.getId(), since);
        return new ActivitySummary(commitCount, additions, deletions, memberCount);
    }

    private PullRequestSummary buildPullRequestSummary(Organization organization) {
        if (organization == null || organization.getId() == null) {
            return new PullRequestSummary(0L, 0L, 0L);
        }

        long open = pullRequestRepository
                .countByRepositoryOrganizationIdAndStateIgnoreCaseAndDeletedAtIsNull(organization.getId(), "open");
        long closed = pullRequestRepository
                .countByRepositoryOrganizationIdAndStateIgnoreCaseAndDeletedAtIsNull(organization.getId(), "closed");
        long merged = pullRequestRepository
                .countByRepositoryOrganizationIdAndMergedIsTrueAndDeletedAtIsNull(organization.getId());
        return new PullRequestSummary(open, closed, merged);
    }

    private List<PullRequestDetail> loadRecentPullRequests(Long organizationId) {
        if (organizationId == null) {
            return List.of();
        }
        var pageable = PageRequest.of(0, RECENT_PULL_REQUEST_LIMIT,
                Sort.by(Sort.Direction.DESC, "updatedAt", "createdAt"));
        return pullRequestRepository.findByRepositoryOrganizationIdAndDeletedAtIsNull(organizationId, pageable).stream()
                .map(this::toPullRequestDetail)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<CommitDetail> loadRecentCommits(Long organizationId) {
        if (organizationId == null) {
            return List.of();
        }
        var pageable = PageRequest.of(0, RECENT_COMMIT_LIMIT, Sort.by(Sort.Direction.DESC, "committedAt", "createdAt"));
        return gitCommitRepository.findByRepositoryOrganizationIdAndDeletedAtIsNull(organizationId, pageable).stream()
                .map(this::toCommitDetail)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<CommentDetail> loadRecentComments(Long organizationId) {
        if (organizationId == null) {
            return List.of();
        }
        var pageable = PageRequest.of(0, RECENT_COMMENT_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt"));
        return commentRepository.findByOrganizationIdAndDeletedAtIsNull(organizationId, pageable).stream()
                .map(this::toCommentDetail)
                .filter(Objects::nonNull)
                .toList();
    }

    private PullRequestDetail toPullRequestDetail(PullRequest pullRequest) {
        if (pullRequest == null) {
            return null;
        }
        Repository repository = pullRequest.getRepository();
        Long repositoryId = repository != null ? repository.getId() : null;
        String repositoryFullName = repository != null ? resolveRepositoryFullName(repository) : null;

        return new PullRequestDetail(
                pullRequest.getId(),
                pullRequest.getNumber(),
                repositoryId,
                repositoryFullName,
                pullRequest.getTitle(),
                pullRequest.getState(),
                Boolean.TRUE.equals(pullRequest.getMerged()),
                pullRequest.getHtmlUrl(),
                toSimpleUser(pullRequest.getAuthor()),
                toSimpleUser(pullRequest.getMergedBy()),
                pullRequest.getAdditions(),
                pullRequest.getDeletions(),
                pullRequest.getChangedFiles(),
                pullRequest.getCreatedAt(),
                pullRequest.getUpdatedAt(),
                pullRequest.getMergedAt(),
                pullRequest.getClosedAt());
    }

    private CommitDetail toCommitDetail(GitCommit commit) {
        if (commit == null) {
            return null;
        }
        Repository repository = commit.getRepository();
        Long repositoryId = repository != null ? repository.getId() : null;
        String repositoryFullName = repository != null ? resolveRepositoryFullName(repository) : null;
        OffsetDateTime committedAt = commit.getCommittedAt() != null ? commit.getCommittedAt() : commit.getCreatedAt();

        return new CommitDetail(
                commit.getId(),
                commit.getSha(),
                commit.getMessage(),
                repositoryId,
                repositoryFullName,
                commit.getHtmlUrl(),
                commit.getAuthorName(),
                commit.getCommitterName(),
                committedAt,
                commit.getPushedAt());
    }

    private CommentDetail toCommentDetail(Comment comment) {
        if (comment == null) {
            return null;
        }
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        return new CommentDetail(
                comment.getId(),
                toSimpleUser(comment.getUser()),
                comment.getTargetType(),
                comment.getTargetId(),
                parentId,
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt());
    }

    private SimpleUser toSimpleUser(User user) {
        if (user == null) {
            return null;
        }
        return new SimpleUser(
                user.getId(),
                user.getGithubId(),
                user.getLogin(),
                user.getName(),
                user.getAvatarUrl());
    }

    public void validateDeletePermission(User user, Long organizationId) {
        requireOrganizationForDeletion(user, organizationId);
    }

    public void deleteOrganization(User user, Long organizationId) {
        Organization organization = requireOrganizationForDeletion(user, organizationId);

        List<UserOrganization> memberships = userOrganizationRepository
                .findByOrganizationIdAndDeletedAtIsNull(organizationId);
        List<Repository> repositories = repositoryRepository.findByOrganizationAndDeletedAtIsNull(organization);

        OffsetDateTime now = OffsetDateTime.now();
        organization.setDeletedAt(now);
        organizationRepository.save(organization);

        memberships.forEach(member -> {
            member.setDeletedAt(now);
            userOrganizationRepository.save(member);
        });

        repositories.forEach(repository -> {
            repository.setDeletedAt(now);
            repositoryRepository.save(repository);
            repositorySyncStatusService.markDeleted(repository);
        });
    }

    private Organization requireOrganizationForDeletion(User user, Long organizationId) {
        UserOrganization membership = requireActiveMembership(user, organizationId);

        if (!isAdmin(membership)) {
            throw new ForbiddenException("Only administrators can delete organization");
        }

        return membership.getOrganization();
    }

    private boolean isAdmin(UserOrganization membership) {
        String role = membership.getRole();
        if (!StringUtils.hasText(role)) {
            return false;
        }
        return "admin".equalsIgnoreCase(role) || "owner".equalsIgnoreCase(role);
    }

    private MemberDetail toMemberDetail(UserOrganization membership) {
        User memberUser = membership.getUser();
        Long userId = memberUser != null ? memberUser.getId() : null;
        Long githubId = memberUser != null ? memberUser.getGithubId() : null;
        String login = memberUser != null ? memberUser.getLogin() : null;
        String name = memberUser != null ? memberUser.getName() : null;
        String avatarUrl = memberUser != null ? memberUser.getAvatarUrl() : null;
        return new MemberDetail(userId, githubId, login, name, avatarUrl, defaultRole(membership.getRole()));
    }

    private int syncRepositories(Organization organization, List<GitHubRepository> gitHubRepositories) {
        if (gitHubRepositories == null || gitHubRepositories.isEmpty()) {
            return 0;
        }

        Map<Long, Repository> existing = repositoryRepository.findByOrganizationAndDeletedAtIsNull(organization)
                .stream()
                .filter(repository -> repository.getGithubId() != null)
                .collect(Collectors.toMap(Repository::getGithubId, Function.identity()));

        OffsetDateTime syncedAt = OffsetDateTime.now();
        int synced = 0;
        for (GitHubRepository gitHubRepository : gitHubRepositories) {
            if (gitHubRepository.id() == null) {
                continue;
            }
            Repository repository = existing.remove(gitHubRepository.id());
            if (repository == null) {
                repository = new Repository();
                repository.setGithubId(gitHubRepository.id());
                repository.setOrganization(organization);
            }
            updateRepositoryFields(repository, gitHubRepository);
            repository.setDeletedAt(null);
            repositoryRepository.save(repository);
            repositorySyncStatusService.markSynced(repository, syncedAt, null);
            synced++;
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (Repository repository : existing.values()) {
            repository.setDeletedAt(now);
            repositoryRepository.save(repository);
            repositorySyncStatusService.markDeleted(repository);
        }
        return synced;
    }

    private void updateRepositoryFields(Repository repository, GitHubRepository source) {
        repository.setName(source.name());
        repository.setFullName(buildFullName(repository.getOrganization(), source));
        repository.setDescription(source.description());
        repository.setHtmlUrl(source.htmlUrl());
        repository.setLanguage(source.language());
        repository.setStargazersCount(source.stargazersCount());
        repository.setForksCount(source.forksCount());
        repository.setDefaultBranch(source.defaultBranch());
        repository.setPrivateRepository(source.isPrivate());
        repository.setArchived(source.archived());
    }

    private String buildFullName(Organization organization, GitHubRepository repository) {
        if (organization == null || !StringUtils.hasText(organization.getLogin())) {
            return repository.name();
        }
        if (!StringUtils.hasText(repository.name())) {
            return organization.getLogin();
        }
        return organization.getLogin() + "/" + repository.name();
    }

    public record OrganizationSyncResult(Organization organization,
            GitHubOrganization gitHubOrganization,
            int syncedRepositories) {
    }

    public record OrganizationDetail(Organization organization,
            List<MemberDetail> members,
            List<Repository> repositories,
            ActivitySummary activitySummaryLast7Days,
            PullRequestSummary pullRequestSummary,
            List<PullRequestDetail> recentPullRequests,
            List<CommitDetail> recentCommits,
            List<CommentDetail> recentComments) {
    }

    public record MemberDetail(Long userId,
            Long githubId,
            String login,
            String name,
            String avatarUrl,
            String role) {
    }

    public record ActivitySummary(long commitCount,
            long additions,
            long deletions,
            int activeMembers) {
    }

    public record RepositorySyncStatusView(Long repositoryId,
            String repositoryFullName,
            OffsetDateTime lastSyncedAt,
            String lastSyncedCommitSha,
            String errorMessage) {
    }

    public record PullRequestSummary(long openCount,
            long closedCount,
            long mergedCount) {
    }

    public record PullRequestDetail(Long id,
            Integer number,
            Long repositoryId,
            String repositoryFullName,
            String title,
            String state,
            boolean merged,
            String htmlUrl,
            SimpleUser author,
            SimpleUser mergedBy,
            Integer additions,
            Integer deletions,
            Integer changedFiles,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime mergedAt,
            OffsetDateTime closedAt) {
    }

    public record CommitDetail(Long id,
            String sha,
            String message,
            Long repositoryId,
            String repositoryFullName,
            String htmlUrl,
            String authorName,
            String committerName,
            OffsetDateTime committedAt,
            OffsetDateTime pushedAt) {
    }

    public record CommentDetail(Long id,
            SimpleUser user,
            String targetType,
            Long targetId,
            Long parentCommentId,
            String content,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt) {
    }

    public record SimpleUser(Long userId,
            Long githubId,
            String login,
            String name,
            String avatarUrl) {
    }
}

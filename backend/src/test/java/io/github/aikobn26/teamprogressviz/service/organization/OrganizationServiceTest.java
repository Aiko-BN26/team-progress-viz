package io.github.aikobn26.teamprogressviz.service.organization;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.github.aikobn26.teamprogressviz.entity.Comment;
import io.github.aikobn26.teamprogressviz.entity.GitCommit;
import io.github.aikobn26.teamprogressviz.entity.Organization;
import io.github.aikobn26.teamprogressviz.entity.PullRequest;
import io.github.aikobn26.teamprogressviz.entity.Repository;
import io.github.aikobn26.teamprogressviz.entity.User;
import io.github.aikobn26.teamprogressviz.entity.UserOrganization;
import io.github.aikobn26.teamprogressviz.exception.ForbiddenException;
import io.github.aikobn26.teamprogressviz.exception.ResourceConflictException;
import io.github.aikobn26.teamprogressviz.github.model.GitHubOrganization;
import io.github.aikobn26.teamprogressviz.github.model.GitHubOrganizationMember;
import io.github.aikobn26.teamprogressviz.github.model.GitHubRepository;
import io.github.aikobn26.teamprogressviz.github.service.GitHubOrganizationService;
import io.github.aikobn26.teamprogressviz.repository.CommentRepository;
import io.github.aikobn26.teamprogressviz.repository.GitCommitRepository;
import io.github.aikobn26.teamprogressviz.repository.OrganizationRepository;
import io.github.aikobn26.teamprogressviz.repository.PullRequestRepository;
import io.github.aikobn26.teamprogressviz.repository.RepositoryRepository;
import io.github.aikobn26.teamprogressviz.repository.RepositorySyncStatusRepository;
import io.github.aikobn26.teamprogressviz.repository.UserOrganizationRepository;
import io.github.aikobn26.teamprogressviz.repository.UserRepository;
import io.github.aikobn26.teamprogressviz.service.repository.RepositoryActivitySyncService;
import io.github.aikobn26.teamprogressviz.service.user.UserService;

@DataJpaTest
@Import({OrganizationService.class, RepositorySyncStatusService.class, UserService.class, OrganizationServiceTest.MockConfig.class})
class OrganizationServiceTest {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

        @Autowired
        private RepositoryRepository repositoryRepository;

        @Autowired
        private RepositorySyncStatusRepository repositorySyncStatusRepository;

        @Autowired
        private UserOrganizationRepository userOrganizationRepository;

        @Autowired
        private GitHubOrganizationService gitHubOrganizationService;

        @Autowired
        private RepositoryActivitySyncService repositoryActivitySyncService;

        @Autowired
        private PullRequestRepository pullRequestRepository;

        @Autowired
        private GitCommitRepository gitCommitRepository;

        @Autowired
        private CommentRepository commentRepository;

    private User primaryUser;

    @BeforeEach
    void setUp() {
                Mockito.reset(gitHubOrganizationService);
                Mockito.reset(repositoryActivitySyncService);
                primaryUser = userRepository.save(User.builder()
                                .githubId(1_000L)
                                .login("tester")
                                .name("Tester")
                                .avatarUrl("https://avatar")
                                .build());
    }

    @Test
    void registerOrganization_createsOrganizationAndMembership() {
        when(gitHubOrganizationService.getOrganization(eq("token"), eq("octo-org")))
                .thenReturn(Optional.of(new GitHubOrganization(10L, "octo-org", "Octo Org", "org desc", "https://avatar", "https://github.com/octo-org")));
        when(gitHubOrganizationService.listMembers(eq("token"), eq("octo-org")))
                .thenReturn(List.of(toMember(primaryUser)));

        var result = organizationService.registerOrganization(primaryUser, "octo-org", null, "token");

        Organization organization = result.organization();
        assertThat(organization.getId()).isNotNull();
        assertThat(organization.getGithubId()).isEqualTo(10L);
        assertThat(result.syncedRepositories()).isZero();

        assertThat(repositoryRepository.findByOrganizationAndDeletedAtIsNull(organization)).isEmpty();
        assertThat(userOrganizationRepository.findByUserIdAndOrganizationId(primaryUser.getId(), organization.getId()))
                .get()
                .extracting(UserOrganization::getRole)
                .isEqualTo("admin");
    }

    @Test
    void registerOrganization_throwsConflictWhenMembershipAlreadyActive() {
        Organization existing = organizationRepository.save(Organization.builder()
                .githubId(20L)
                .login("existing")
                .name("Existing Org")
                .build());
        userOrganizationRepository.save(UserOrganization.builder()
                .user(primaryUser)
                .organization(existing)
                .role("member")
                .build());

        when(gitHubOrganizationService.getOrganization(eq("token"), eq("existing")))
                .thenReturn(Optional.of(new GitHubOrganization(20L, "existing", "Existing Org", null, null, null)));

        assertThatThrownBy(() -> organizationService.registerOrganization(primaryUser, "existing", null, "token"))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void registerOrganization_reactivatesSoftDeletedRecords() {
        Organization deletedOrg = organizationRepository.save(Organization.builder()
                .githubId(30L)
                .login("deleted")
                .name("Deleted Org")
                .deletedAt(OffsetDateTime.now())
                .build());

        UserOrganization deletedMembership = userOrganizationRepository.save(UserOrganization.builder()
                .user(primaryUser)
                .organization(deletedOrg)
                .role("member")
                .deletedAt(OffsetDateTime.now())
                .build());

        when(gitHubOrganizationService.getOrganization(eq("token"), eq("deleted")))
                .thenReturn(Optional.of(new GitHubOrganization(30L, "deleted", "Deleted Org", null, null, null)));

        var result = organizationService.registerOrganization(primaryUser, "deleted", null, "token");

        assertThat(result.organization().getDeletedAt()).isNull();
        assertThat(userOrganizationRepository.findById(deletedMembership.getId()))
                .get()
                .extracting(UserOrganization::getDeletedAt)
                .isNull();
    }

    @Test
    void synchronizeOrganization_syncsRepositories() {
        when(gitHubOrganizationService.getOrganization(eq("token"), eq("octo-org")))
                .thenReturn(Optional.of(new GitHubOrganization(10L, "octo-org", "Octo Org", "org desc", "https://avatar", "https://github.com/octo-org")));
        when(gitHubOrganizationService.listMembers(eq("token"), eq("octo-org")))
                .thenReturn(List.of(toMember(primaryUser)))
                .thenReturn(List.of(toMember(primaryUser)));

        var registration = organizationService.registerOrganization(primaryUser, "octo-org", null, "token");

        when(gitHubOrganizationService.listRepositories(eq("token"), eq("octo-org")))
                .thenReturn(List.of(new GitHubRepository(99L, "repo", "repo desc", "https://github.com/octo-org/repo", "Java", 42, 7, "main", true, false)));

        var syncResult = organizationService.synchronizeOrganization(registration.organization().getId(), "token");

        assertThat(syncResult.syncedRepositories()).isEqualTo(1);
        Organization persistedOrganization = organizationRepository.findById(registration.organization().getId()).orElseThrow();
        List<Repository> repositories = repositoryRepository.findByOrganizationAndDeletedAtIsNull(persistedOrganization);
        assertThat(repositories)
                .hasSize(1)
                .first()
                .extracting(Repository::getGithubId)
                .isEqualTo(99L);
    }

    @Test
    void listRepositorySyncStatus_returnsActiveStatuses() {
        when(gitHubOrganizationService.getOrganization(eq("token"), eq("octo-org")))
                .thenReturn(Optional.of(new GitHubOrganization(10L, "octo-org", "Octo Org", "org desc", "https://avatar", "https://github.com/octo-org")));

        var registration = organizationService.registerOrganization(primaryUser, "octo-org", null, "token");

        when(gitHubOrganizationService.listRepositories(eq("token"), eq("octo-org")))
                .thenReturn(List.of(new GitHubRepository(99L, "repo", "repo desc", "https://github.com/octo-org/repo", "Java", 42, 7, "main", true, false)));
        organizationService.synchronizeOrganization(registration.organization().getId(), "token");

        var statuses = organizationService.listRepositorySyncStatus(primaryUser, registration.organization().getId());

        assertThat(statuses)
                .hasSize(1)
                .first()
                .extracting(OrganizationService.RepositorySyncStatusView::repositoryFullName)
                .isEqualTo("octo-org/repo");

        assertThat(repositorySyncStatusRepository.findAll()).hasSize(1);
    }

    @Test
    void getOrganizationDetail_returnsMembersAndRepositories() {
        when(gitHubOrganizationService.getOrganization(eq("token"), eq("octo-org")))
                .thenReturn(Optional.of(new GitHubOrganization(10L, "octo-org", "Octo Org", "org desc", "https://avatar", "https://github.com/octo-org")));
        GitHubOrganizationMember primaryMember = toMember(primaryUser);
        GitHubOrganizationMember secondaryMember = new GitHubOrganizationMember(2_000L, "second", "https://avatar2", "https://github.com/second", "User", false);
        when(gitHubOrganizationService.listMembers(eq("token"), eq("octo-org")))
                .thenReturn(List.of(primaryMember))
                .thenReturn(List.of(primaryMember, secondaryMember));

        var registration = organizationService.registerOrganization(primaryUser, "octo-org", null, "token");

        when(gitHubOrganizationService.listRepositories(eq("token"), eq("octo-org")))
                .thenReturn(List.of(new GitHubRepository(99L, "repo", "repo desc", "https://github.com/octo-org/repo", "Java", 42, 7, "main", true, false)));
        organizationService.synchronizeOrganization(registration.organization().getId(), "token");

        Organization persistedOrganization = organizationRepository.findById(registration.organization().getId()).orElseThrow();
        Repository repository = repositoryRepository.findByOrganizationAndDeletedAtIsNull(persistedOrganization)
                .stream()
                .findFirst()
                .orElseThrow();
        OffsetDateTime now = OffsetDateTime.now();

        pullRequestRepository.save(PullRequest.builder()
                .number(1)
                .repository(repository)
                .githubId(1_000_000L)
                .title("Add dashboard")
                .state("open")
                .merged(false)
                .author(primaryUser)
                .htmlUrl("https://github.com/octo-org/repo/pull/1")
                .additions(120)
                .deletions(10)
                .changedFiles(5)
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build());

        gitCommitRepository.save(GitCommit.builder()
                .repository(repository)
                .sha("abc123")
                .message("feat: add dashboard")
                .htmlUrl("https://github.com/octo-org/repo/commit/abc123")
                .authorName("tester")
                .committerName("tester")
                .committedAt(now.minusHours(6))
                .pushedAt(now.minusHours(4))
                .build());

        commentRepository.save(Comment.builder()
                .user(primaryUser)
                .organization(persistedOrganization)
                .targetType("organization")
                .targetId(persistedOrganization.getId())
                .content("Please share progress")
                .build());

        var detail = organizationService.getOrganizationDetail(primaryUser, registration.organization().getId());

        assertThat(detail.organization().getLogin()).isEqualTo("octo-org");
        assertThat(detail.members()).hasSize(2);
        assertThat(detail.members())
                .extracting(io.github.aikobn26.teamprogressviz.service.organization.OrganizationService.MemberDetail::login)
                .containsExactlyInAnyOrder("tester", "second");
        assertThat(detail.repositories()).hasSize(1);
        assertThat(detail.pullRequestSummary().openCount()).isEqualTo(1L);
        assertThat(detail.recentPullRequests())
                .hasSize(1)
                .first()
                .extracting(io.github.aikobn26.teamprogressviz.service.organization.OrganizationService.PullRequestDetail::title)
                .isEqualTo("Add dashboard");
        assertThat(detail.recentCommits())
                .hasSize(1)
                .first()
                .extracting(io.github.aikobn26.teamprogressviz.service.organization.OrganizationService.CommitDetail::sha)
                .isEqualTo("abc123");
        assertThat(detail.recentComments())
                .hasSize(1)
                .first()
                .extracting(io.github.aikobn26.teamprogressviz.service.organization.OrganizationService.CommentDetail::content)
                .isEqualTo("Please share progress");
    }

    @Test
    void synchronizeOrganization_syncsMembersFromGitHub() {
        when(gitHubOrganizationService.getOrganization(eq("token"), eq("octo-org")))
                .thenReturn(Optional.of(new GitHubOrganization(10L, "octo-org", "Octo Org", "org desc", "https://avatar", "https://github.com/octo-org")));
        GitHubOrganizationMember primaryMember = toMember(primaryUser);
        GitHubOrganizationMember secondaryMember = new GitHubOrganizationMember(2_000L, "second", "https://avatar2", "https://github.com/second", "User", false);
        when(gitHubOrganizationService.listMembers(eq("token"), eq("octo-org")))
                .thenReturn(List.of(primaryMember))
                .thenReturn(List.of(primaryMember, secondaryMember));

        var registration = organizationService.registerOrganization(primaryUser, "octo-org", null, "token");

        organizationService.synchronizeOrganization(registration.organization().getId(), "token");

        assertThat(userRepository.findByGithubId(secondaryMember.id())).isPresent();
        assertThat(userOrganizationRepository.findByOrganizationIdAndDeletedAtIsNull(registration.organization().getId()))
                .extracting(UserOrganization::getUser)
                .extracting(User::getGithubId)
                .contains(secondaryMember.id());
    }

    @Test
    void deleteOrganization_softDeletesRelatedEntities() {
        when(gitHubOrganizationService.getOrganization(eq("token"), eq("octo-org")))
                .thenReturn(Optional.of(new GitHubOrganization(10L, "octo-org", "Octo Org", "org desc", "https://avatar", "https://github.com/octo-org")));
        GitHubOrganizationMember primaryMember = toMember(primaryUser);
        GitHubOrganizationMember secondaryMember = new GitHubOrganizationMember(2_000L, "second", "https://avatar2", "https://github.com/second", "User", false);
        when(gitHubOrganizationService.listMembers(eq("token"), eq("octo-org")))
                .thenReturn(List.of(primaryMember))
                .thenReturn(List.of(primaryMember, secondaryMember));

        var registration = organizationService.registerOrganization(primaryUser, "octo-org", null, "token");

        when(gitHubOrganizationService.listRepositories(eq("token"), eq("octo-org")))
                .thenReturn(List.of(new GitHubRepository(99L, "repo", "repo desc", "https://github.com/octo-org/repo", "Java", 42, 7, "main", true, false)));
        organizationService.synchronizeOrganization(registration.organization().getId(), "token");

        User nonAdmin = userRepository.findByGithubId(secondaryMember.id()).orElseThrow();
        assertThatThrownBy(() -> organizationService.validateDeletePermission(nonAdmin, registration.organization().getId()))
                .isInstanceOf(ForbiddenException.class);

        organizationService.deleteOrganization(primaryUser, registration.organization().getId());

        Organization deletedOrganization = organizationRepository.findById(registration.organization().getId()).orElseThrow();
        assertThat(deletedOrganization.getDeletedAt()).isNotNull();
        assertThat(userOrganizationRepository.findByOrganizationIdAndDeletedAtIsNull(deletedOrganization.getId())).isEmpty();
        assertThat(repositoryRepository.findByOrganizationAndDeletedAtIsNull(deletedOrganization)).isEmpty();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        GitHubOrganizationService gitHubOrganizationService() {
            return Mockito.mock(GitHubOrganizationService.class);
        }

                @Bean
                RepositoryActivitySyncService repositoryActivitySyncService() {
                        return Mockito.mock(RepositoryActivitySyncService.class);
                }
    }

        private GitHubOrganizationMember toMember(User user) {
                return new GitHubOrganizationMember(
                                user.getGithubId(),
                                user.getLogin(),
                                user.getAvatarUrl(),
                                "https://github.com/" + user.getLogin(),
                                "User",
                                false);
        }
}

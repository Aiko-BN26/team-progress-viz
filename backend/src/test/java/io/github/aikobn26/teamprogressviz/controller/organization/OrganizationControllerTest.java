package io.github.aikobn26.teamprogressviz.controller.organization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.github.aikobn26.teamprogressviz.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.entity.Organization;
import io.github.aikobn26.teamprogressviz.entity.User;
import io.github.aikobn26.teamprogressviz.github.model.GitHubOrganization;
import io.github.aikobn26.teamprogressviz.service.organization.OrganizationService;
import io.github.aikobn26.teamprogressviz.service.user.UserService;
import io.github.aikobn26.teamprogressviz.shared.properties.FrontendProperties;
import io.github.aikobn26.teamprogressviz.job.JobDescriptor;
import io.github.aikobn26.teamprogressviz.job.JobService;
import io.github.aikobn26.teamprogressviz.job.JobStatus;

@WebMvcTest(OrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(OrganizationControllerTest.MockConfig.class)
class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private JobService jobService;

    @Test
    void list_returnsUnauthorizedWhenNotAuthenticated() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsOrganizationsForAuthenticatedUser() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        var user = User.builder().id(10L).githubId(1_000L).login("octocat").build();
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var organization = Organization.builder()
                .id(20L)
                .githubId(2000L)
                .login("test-org")
                .name("Test Org")
                .avatarUrl("https://org-avatar")
                .description("description")
                .build();
        when(organizationService.listOrganizations(user)).thenReturn(List.of(organization));

        mockMvc.perform(get("/api/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20L))
                .andExpect(jsonPath("$[0].login").value("test-org"));
    }

    @Test
    void register_returnsUnauthorizedWhenTokenMissing() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"test-org\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_returnsAcceptedWhenSyncSucceeds() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.of("token"));

        var user = User.builder().id(10L).githubId(1_000L).login("octocat").build();
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var organization = Organization.builder()
                .id(33L)
                .githubId(3_300L)
                .login("test-org")
                .name("Test Org")
                .htmlUrl("https://github.com/test-org")
                .build();
        var gitHubOrganization = new GitHubOrganization(3_300L, "test-org", "Test Org", null, null, "https://github.com/test-org");
        var result = new OrganizationService.OrganizationSyncResult(organization, gitHubOrganization, 2);
        when(organizationService.registerOrganization(eq(user), eq("test-org"), eq(null), eq("token")))
                .thenReturn(result);

    var jobDescriptor = new JobDescriptor(
        "job-sync-org-1",
        "job-sync-org",
        JobStatus.QUEUED,
        OffsetDateTime.now(),
        null,
        null,
        null
    );
    when(jobService.submit(eq("job-sync-org"), any())).thenReturn(jobDescriptor);

        mockMvc.perform(post("/api/organizations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"login\":\"test-org\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.organizationId").value(33L))
        .andExpect(jsonPath("$.status").value("queued"))
        .andExpect(jsonPath("$.syncedRepositories").value(0))
        .andExpect(jsonPath("$.jobId").value("job-sync-org-1"));
    }

    @Test
    void synchronize_returnsAcceptedWhenJobIsQueued() throws Exception {
    var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
    when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
    when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.of("token"));

    var user = User.builder().id(10L).githubId(1_000L).login("octocat").build();
    when(userService.ensureUserExists(authUser)).thenReturn(user);

    var organization = Organization.builder().id(33L).githubId(3_300L).login("test-org").build();
    when(organizationService.getAccessibleOrganization(user, 33L)).thenReturn(organization);

    var jobDescriptor = new JobDescriptor(
        "job-sync-org-2",
        "job-sync-org",
        JobStatus.QUEUED,
        OffsetDateTime.now(),
        null,
        null,
        null
    );
    when(jobService.submit(eq("job-sync-org"), any())).thenReturn(jobDescriptor);

    mockMvc.perform(post("/api/organizations/33/sync"))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.jobId").value("job-sync-org-2"))
        .andExpect(jsonPath("$.status").value("queued"));
    }

    @Test
    void syncStatus_returnsStatusesForAuthenticatedUser() throws Exception {
    var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
    when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));

    var user = User.builder().id(10L).githubId(1_000L).login("octocat").build();
    when(userService.ensureUserExists(authUser)).thenReturn(user);

    var statusView = new OrganizationService.RepositorySyncStatusView(
        200L,
        "org/repo",
        OffsetDateTime.now(),
        "abc123",
        null
    );
    when(organizationService.listRepositorySyncStatus(eq(user), eq(33L))).thenReturn(List.of(statusView));

    mockMvc.perform(get("/api/organizations/33/sync/status"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].repositoryId").value(200L))
        .andExpect(jsonPath("$[0].repositoryFullName").value("org/repo"))
        .andExpect(jsonPath("$[0].lastSyncedCommitSha").value("abc123"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        GitHubOAuthService gitHubOAuthService() {
            return Mockito.mock(GitHubOAuthService.class);
        }

        @Bean
        UserService userService() {
            return Mockito.mock(UserService.class);
        }

        @Bean
        OrganizationService organizationService() {
            return Mockito.mock(OrganizationService.class);
        }

        @Bean
        FrontendProperties frontendProperties() {
            return new FrontendProperties(URI.create("http://localhost:3000"), "/success", "/error");
        }

        @Bean
        JobService jobService() {
            return Mockito.mock(JobService.class);
        }
    }
}

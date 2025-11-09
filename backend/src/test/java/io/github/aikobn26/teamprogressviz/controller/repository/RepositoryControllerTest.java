package io.github.aikobn26.teamprogressviz.controller.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.github.aikobn26.teamprogressviz.feature.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.job.model.JobDescriptor;
import io.github.aikobn26.teamprogressviz.feature.job.model.JobStatus;
import io.github.aikobn26.teamprogressviz.feature.job.service.JobService;
import io.github.aikobn26.teamprogressviz.feature.repository.controller.RepositoryController;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.Repository;
import io.github.aikobn26.teamprogressviz.feature.repository.service.PullRequestService;
import io.github.aikobn26.teamprogressviz.feature.repository.service.RepositoryActivitySyncService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.support.FrontendPropertiesTestConfig;

@WebMvcTest(RepositoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({RepositoryControllerTest.MockConfig.class, FrontendPropertiesTestConfig.class})
class RepositoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private PullRequestService pullRequestService;

    @Autowired
    private JobService jobService;

    @Test
    void syncPullRequests_returnsUnauthorizedWhenMissingAuthentication() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/repositories/60/pulls/sync"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void syncPullRequests_returnsUnauthorizedWhenTokenMissing() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar")));
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/repositories/60/pulls/sync"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void syncPullRequests_returnsAcceptedWithJobInfo() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        var repository = Repository.builder().id(60L).githubId(6_000L).name("repo").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.of("token"));
        when(userService.ensureUserExists(authUser)).thenReturn(user);
        when(pullRequestService.requireAccessibleRepository(same(user), eq(60L))).thenReturn(repository);

        var jobDescriptor = new JobDescriptor("job-1", "job-sync-prs", JobStatus.RUNNING, OffsetDateTime.now(), OffsetDateTime.now(), null, 0, null);
        when(jobService.submit(eq("job-sync-prs"), any(Runnable.class))).thenReturn(jobDescriptor);

        mockMvc.perform(post("/api/repositories/60/pulls/sync"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.status").value("running"));

        verify(pullRequestService).requireAccessibleRepository(same(user), eq(60L));
        verify(jobService).submit(eq("job-sync-prs"), any(Runnable.class));
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
        PullRequestService pullRequestService() {
            return Mockito.mock(PullRequestService.class);
        }

        @Bean
        RepositoryActivitySyncService repositoryActivitySyncService() {
            return Mockito.mock(RepositoryActivitySyncService.class);
        }

        @Bean
        JobService jobService() {
            return Mockito.mock(JobService.class);
        }
    }
}

package io.github.aikobn26.teamprogressviz.controller.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import java.net.URI;

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
import io.github.aikobn26.teamprogressviz.job.JobDescriptor;
import io.github.aikobn26.teamprogressviz.job.JobService;
import io.github.aikobn26.teamprogressviz.job.JobStatus;
import io.github.aikobn26.teamprogressviz.shared.properties.FrontendProperties;

@WebMvcTest(JobController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(JobControllerTest.MockConfig.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private JobService jobService;

    @Test
    void status_returnsUnauthorizedWhenNotAuthenticated() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/job-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void status_returnsNotFoundWhenJobMissing() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(jobService.findJob("job-1")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/jobs/job-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void status_returnsJobDetailsWhenFound() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));

        var descriptor = new JobDescriptor(
                "job-1",
                "sync",
                JobStatus.RUNNING,
                OffsetDateTime.now().minusMinutes(5),
                OffsetDateTime.now().minusMinutes(4),
                null,
                null
        );
        when(jobService.findJob("job-1")).thenReturn(Optional.of(descriptor));

        mockMvc.perform(get("/api/jobs/job-1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.type").value("sync"))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        GitHubOAuthService gitHubOAuthService() {
            return Mockito.mock(GitHubOAuthService.class);
        }

        @Bean
        JobService jobService() {
            return Mockito.mock(JobService.class);
        }

        @Bean
        FrontendProperties frontendProperties() {
            return new FrontendProperties(URI.create("http://localhost:3000"), "/success", "/error");
        }
    }
}
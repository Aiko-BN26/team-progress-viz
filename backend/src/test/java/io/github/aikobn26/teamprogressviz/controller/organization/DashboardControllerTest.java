package io.github.aikobn26.teamprogressviz.controller.organization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MockMvc;

import io.github.aikobn26.teamprogressviz.feature.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.organization.controller.DashboardController;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.DashboardResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.service.DashboardService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.support.FrontendPropertiesTestConfig;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({DashboardControllerTest.MockConfig.class, FrontendPropertiesTestConfig.class})
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private DashboardService dashboardService;

    @Test
    void getDashboard_returnsUnauthorizedWhenUnauthenticated() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/organizations/9/dashboard"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getDashboard_returnsDashboardResponse() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var statusItem = new DashboardResponse.StatusItem(20L, "octocat", "Octo Cat", "https://avatar", 480, "working", "Building features", OffsetDateTime.parse("2025-01-07T12:00:00Z"));
        var commitItem = new DashboardResponse.CommitItem(42L, "abc123", "org/repo", "Add feature", "octocat", OffsetDateTime.parse("2025-01-07T11:00:00Z"), "https://github.com/org/repo/commit/abc123");
        var commentItem = new DashboardResponse.CommentItem(77L, 20L, "octocat", "https://avatar", "Looks good", OffsetDateTime.parse("2025-01-07T10:00:00Z"));
        var response = new DashboardResponse(List.of(statusItem), List.of(commitItem), List.of(commentItem));
    when(dashboardService.fetchDashboard(eq(9L), same(user))).thenReturn(response);

        mockMvc.perform(get("/api/organizations/9/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statuses[0].status").value("working"))
                .andExpect(jsonPath("$.commits[0].sha").value("abc123"))
                .andExpect(jsonPath("$.comments[0].content").value("Looks good"));

    verify(dashboardService).fetchDashboard(9L, user);
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
        DashboardService dashboardService() {
            return Mockito.mock(DashboardService.class);
        }
    }
}

package io.github.aikobn26.teamprogressviz.controller.organization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import io.github.aikobn26.teamprogressviz.feature.organization.controller.ActivityController;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.ActivitySummaryItemResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.service.ActivityService;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitFeedResponse;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.support.FrontendPropertiesTestConfig;

@WebMvcTest(ActivityController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({ActivityControllerTest.MockConfig.class, FrontendPropertiesTestConfig.class})
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Test
    void summary_returnsUnauthorizedWhenUnauthenticated() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/organizations/10/activity/summary")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-07"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void summary_returnsDataForAuthenticatedUser() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var item = new ActivitySummaryItemResponse(20L, "octocat", "Octo Cat", "https://avatar", 5, 3, 10, 2, 480);
        when(activityService.summarize(same(user), eq(42L), eq(LocalDate.parse("2025-01-01")), eq(LocalDate.parse("2025-01-07")), eq("user")))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/organizations/42/activity/summary")
                        .param("startDate", "2025-01-01")
                        .param("endDate", "2025-01-07")
                        .param("groupBy", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(20L))
                .andExpect(jsonPath("$[0].commitCount").value(5));

        verify(activityService).summarize(same(user), eq(42L), eq(LocalDate.parse("2025-01-01")), eq(LocalDate.parse("2025-01-07")), eq("user"));
    }

    @Test
    void commitFeed_returnsFeedItems() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var feedItem = new CommitFeedResponse.Item(50L, "abc123", "org/repo", "Add feature", "octocat", "octocat", OffsetDateTime.parse("2025-01-07T12:00:00Z"), "https://github.com/org/repo/commit/abc123");
        var feed = new CommitFeedResponse(List.of(feedItem), "50");
        when(activityService.fetchCommitFeed(same(user), eq(42L), eq(10L), eq(25))).thenReturn(feed);

        mockMvc.perform(get("/api/organizations/42/git-commit/feed")
                        .param("cursor", "10")
                        .param("limit", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sha").value("abc123"))
                .andExpect(jsonPath("$.nextCursor").value("50"));

        verify(activityService).fetchCommitFeed(same(user), eq(42L), eq(10L), eq(25));
    }

    @Test
    void commitFeed_returnsUnauthorizedWhenUnauthenticated() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/organizations/42/git-commit/feed"))
                .andExpect(status().isUnauthorized());
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
        ActivityService activityService() {
            return Mockito.mock(ActivityService.class);
        }
    }
}

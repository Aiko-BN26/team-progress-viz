package io.github.aikobn26.teamprogressviz.controller.repository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;

import io.github.aikobn26.teamprogressviz.feature.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.repository.controller.PullRequestController;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.PullRequestDetailResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.PullRequestFeedResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.PullRequestFileResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.PullRequestListItemResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.service.PullRequestService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.support.FrontendPropertiesTestConfig;
import reactor.core.publisher.Mono;

@WebMvcTest(PullRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PullRequestControllerTest.MockConfig.class, FrontendPropertiesTestConfig.class})
class PullRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private PullRequestService pullRequestService;

    @Test
    void list_returnsUnauthorizedWhenUnauthenticated() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

    performAsync(get("/api/repositories/51/pulls"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsPullRequests() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var summary = new PullRequestListItemResponse.UserSummary(20L, 1_000L, "octocat", "https://avatar");
        var item = new PullRequestListItemResponse(70L, 5, "Add feature", "open", summary, OffsetDateTime.parse("2025-01-07T10:00:00Z"), OffsetDateTime.parse("2025-01-07T11:00:00Z"), "org/repo");
    when(pullRequestService.listPullRequestsReactive(same(user), eq(51L), eq("open"), eq(25), eq(1)))
        .thenReturn(Mono.just(List.of(item)));

    performAsync(get("/api/repositories/51/pulls")
            .param("state", "open")
            .param("limit", "25")
            .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Add feature"));

    verify(pullRequestService).listPullRequestsReactive(same(user), eq(51L), eq("open"), eq(25), eq(1));
    }

    @Test
    void detail_returnsPullRequestDetail() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var author = new PullRequestDetailResponse.UserSummary(20L, 1_000L, "octocat", "https://avatar");
        var detail = new PullRequestDetailResponse(70L, 5, "Add feature", "body", "open", false, "https://github.com/org/repo/pull/5", author, null, 10, 2, 3, OffsetDateTime.parse("2025-01-07T10:00:00Z"), OffsetDateTime.parse("2025-01-07T11:00:00Z"), null, null, "org/repo");
    when(pullRequestService.getPullRequestReactive(same(user), eq(51L), eq(5))).thenReturn(Mono.just(detail));

    performAsync(get("/api/repositories/51/pulls/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.htmlUrl").value("https://github.com/org/repo/pull/5"));

    verify(pullRequestService).getPullRequestReactive(same(user), eq(51L), eq(5));
    }

    @Test
    void files_returnsPullRequestFiles() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var file = new PullRequestFileResponse(1L, "src/App.java", "java", 10, 2, 12, "https://raw");
    when(pullRequestService.listFilesReactive(same(user), eq(51L), eq(5))).thenReturn(Mono.just(List.of(file)));

    performAsync(get("/api/repositories/51/pulls/5/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("src/App.java"));

    verify(pullRequestService).listFilesReactive(same(user), eq(51L), eq(5));
    }

    @Test
    void organizationFeed_returnsFeedItems() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var feedUser = new PullRequestFeedResponse.PullRequestUser(20L, 1_000L, "octocat", "https://avatar");
        var item = new PullRequestFeedResponse.Item(77L, 5, "Add feature", "org/repo", "open", feedUser, OffsetDateTime.parse("2025-01-07T10:00:00Z"), OffsetDateTime.parse("2025-01-07T11:00:00Z"), "https://github.com/org/repo/pull/5");
        var feed = new PullRequestFeedResponse(List.of(item), "77");
    when(pullRequestService.fetchFeedReactive(same(user), eq(99L), eq(20L), eq(30))).thenReturn(Mono.just(feed));

    performAsync(get("/api/organizations/99/pulls/feed")
            .param("cursor", "20")
            .param("limit", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].title").value("Add feature"))
                .andExpect(jsonPath("$.nextCursor").value("77"));

    verify(pullRequestService).fetchFeedReactive(same(user), eq(99L), eq(20L), eq(30));
    }

    private ResultActions performAsync(RequestBuilder requestBuilder) throws Exception {
    MvcResult result = mockMvc.perform(requestBuilder)
        .andExpect(request().asyncStarted())
        .andReturn();
    return mockMvc.perform(asyncDispatch(result));
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
    }
}

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
import io.github.aikobn26.teamprogressviz.feature.repository.controller.CommitController;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitDetailResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitFileResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitListItemResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.service.CommitService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.support.FrontendPropertiesTestConfig;
import reactor.core.publisher.Mono;

@WebMvcTest(CommitController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CommitControllerTest.MockConfig.class, FrontendPropertiesTestConfig.class})
class CommitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommitService commitService;

    @Test
    void list_returnsUnauthorizedWhenUnauthenticated() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

    performAsync(get("/api/repositories/44/commits"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void list_returnsCommitItems() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var item = new CommitListItemResponse(11L, "abc123", "Add feature", "org/repo", "octocat", "octocat", OffsetDateTime.parse("2025-01-07T12:00:00Z"), "https://github.com/org/repo/commit/abc123");
    when(commitService.listCommitsReactive(same(user), eq(44L), eq(10), eq(2))).thenReturn(Mono.just(List.of(item)));

    performAsync(get("/api/repositories/44/commits")
            .param("limit", "10")
            .param("page", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sha").value("abc123"));

    verify(commitService).listCommitsReactive(same(user), eq(44L), eq(10), eq(2));
    }

    @Test
    void detail_returnsCommitDetail() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var detail = new CommitDetailResponse(11L, "abc123", 44L, "org/repo", "Add feature", "https://github.com/org/repo/commit/abc123", "octocat", "octocat@example.com", "octocat", "octocat@example.com", OffsetDateTime.parse("2025-01-07T12:00:00Z"), OffsetDateTime.parse("2025-01-07T13:00:00Z"));
    when(commitService.getCommitReactive(same(user), eq(44L), eq("abc123"))).thenReturn(Mono.just(detail));

    performAsync(get("/api/repositories/44/commits/abc123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Add feature"));

    verify(commitService).getCommitReactive(same(user), eq(44L), eq("abc123"));
    }

    @Test
    void files_returnsCommitFiles() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var file = new CommitFileResponse(1L, "src/App.java", "App.java", "java", "modified", 10, 2, 12, "https://raw");
    when(commitService.listFilesReactive(same(user), eq(44L), eq("abc123"))).thenReturn(Mono.just(List.of(file)));

    performAsync(get("/api/repositories/44/commits/abc123/files"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].path").value("src/App.java"));

    verify(commitService).listFilesReactive(same(user), eq(44L), eq("abc123"));
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
        CommitService commitService() {
            return Mockito.mock(CommitService.class);
        }
    }
}

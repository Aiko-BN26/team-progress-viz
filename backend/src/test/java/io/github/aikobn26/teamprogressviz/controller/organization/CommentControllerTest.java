package io.github.aikobn26.teamprogressviz.controller.organization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aikobn26.teamprogressviz.feature.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.organization.controller.CommentController;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.request.CommentCreateRequest;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.CommentCreateResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.CommentListItemResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.service.CommentService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.support.FrontendPropertiesTestConfig;

@WebMvcTest(CommentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({CommentControllerTest.MockConfig.class, FrontendPropertiesTestConfig.class})
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private CommentService commentService;

    @Test
    void create_returnsUnauthorizedWhenUnauthenticated() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        var request = new CommentCreateRequest("organization", 1L, null, "Great work");
        mockMvc.perform(post("/api/organizations/7/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_returnsLocationAndResponseBody() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var request = new CommentCreateRequest("organization", 1L, null, "Great work");
        var response = new CommentCreateResponse(55L, OffsetDateTime.parse("2025-01-07T12:00:00Z"));
        when(commentService.createComment(same(user), eq(7L), eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/organizations/7/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/organizations/7/comments/55"))
                .andExpect(jsonPath("$.commentId").value(55L));

        verify(commentService).createComment(same(user), eq(7L), eq(request));
    }

    @Test
    void list_returnsCommentsForUser() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var comment = new CommentListItemResponse(11L, 20L, "octocat", "Octo Cat", "https://avatar", "organization", 99L, null, "Looks good", OffsetDateTime.parse("2025-01-07T12:00:00Z"), OffsetDateTime.parse("2025-01-07T12:00:00Z"));
        when(commentService.listComments(same(user), eq(7L), eq("organization"), eq(99L))).thenReturn(List.of(comment));

        mockMvc.perform(get("/api/organizations/7/comments")
                        .param("targetType", "organization")
                        .param("targetId", "99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].commentId").value(11L))
                .andExpect(jsonPath("$[0].content").value("Looks good"));

        verify(commentService).listComments(same(user), eq(7L), eq("organization"), eq(99L));
    }

    @Test
    void delete_returnsNoContentOnSuccess() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        mockMvc.perform(delete("/api/organizations/7/comments/55"))
                .andExpect(status().isNoContent());

        verify(commentService).deleteComment(same(user), eq(7L), eq(55L));
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
        CommentService commentService() {
            return Mockito.mock(CommentService.class);
        }
    }
}

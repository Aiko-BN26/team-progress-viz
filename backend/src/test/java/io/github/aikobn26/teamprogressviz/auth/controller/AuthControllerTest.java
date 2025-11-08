package io.github.aikobn26.teamprogressviz.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import io.github.aikobn26.teamprogressviz.feature.auth.controller.AuthController;
import io.github.aikobn26.teamprogressviz.feature.auth.exception.GitHubOAuthException;
import io.github.aikobn26.teamprogressviz.feature.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserOnboardingService;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.shared.properties.FrontendProperties;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private GitHubOAuthService gitHubOAuthService;

    @SuppressWarnings("removal")
    @MockBean
    private FrontendProperties frontendProperties;

    @SuppressWarnings("removal")
    @MockBean
    private UserService userService;

    @SuppressWarnings("removal")
    @MockBean
    private UserOnboardingService userOnboardingService;

    @BeforeEach
    void setUp() {
        when(userService.ensureUserExists(any(), any())).thenAnswer(invocation -> {
            Consumer<User> callback = invocation.getArgument(1);
            User user = User.builder()
                    .id(1L)
                    .githubId(1L)
                    .login("stub-user")
                    .build();
            if (callback != null) {
                callback.accept(user);
            }
            return user;
        });
        when(userOnboardingService.onboardUser(any(), anyString())).thenReturn(List.of());
    }

    @Test
    void startGitHubLogin_returnsAuthorizationUrl() throws Exception {
        when(gitHubOAuthService.createAuthorizationUrl(any())).thenReturn("https://github.com/login");

        mockMvc.perform(get("/api/auth/github/login"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizationUrl").value("https://github.com/login"));

        verify(gitHubOAuthService).createAuthorizationUrl(any());
    }

    @Test
    void startGitHubLogin_handlesOAuthException() throws Exception {
        when(gitHubOAuthService.createAuthorizationUrl(any()))
                .thenThrow(new GitHubOAuthException("service down"));

        mockMvc.perform(get("/api/auth/github/login"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("service down"));
    }

    @Test
    void handleGitHubCallback_redirectsOnSuccess() throws Exception {
        when(frontendProperties.successRedirectUrlWithStatus())
                .thenReturn("https://frontend/success?status=success");

        mockMvc.perform(get("/api/auth/github/callback")
                .param("code", "abc123")
                .param("state", "expected"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "https://frontend/success?status=success"));

        verify(gitHubOAuthService).completeAuthentication(eq("abc123"), eq("expected"), any());
    }

    @Test
    void handleGitHubCallback_redirectsOnFailure() throws Exception {
        String expectedErrorUrl = "https://frontend/error?status=error&message=redirected_on_failure";

        when(frontendProperties.errorRedirectUrl(anyString()))
                .thenReturn(expectedErrorUrl);

        doThrow(new GitHubOAuthException("bad request"))
                .when(gitHubOAuthService)
                .completeAuthentication(eq("abc123"), eq("expected"), any());

        mockMvc.perform(get("/api/auth/github/callback")
                .param("code", "abc123")
                .param("state", "expected"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", expectedErrorUrl));
    }

    @Test
    void session_returnsAuthenticatedUser() throws Exception {
        var user = new AuthenticatedUser(1L, "octocat", "Octo Cat", "https://avatar");
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(user));

        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.login").value("octocat"))
                .andExpect(jsonPath("$.name").value("Octo Cat"))
                .andExpect(jsonPath("$.avatarUrl").value("https://avatar"));
    }

    @Test
    void session_returnsUnauthorizedWhenMissing() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/auth/session"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_invalidatesSession() throws Exception {
        var session = new MockHttpSession();

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isNoContent())
                .andExpect(MockMvcResultMatchers.content().string(""));

        if (!session.isInvalid()) {
            throw new AssertionError("Session was not invalidated");
        }
    }
}

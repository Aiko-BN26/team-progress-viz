package io.github.aikobn26.teamprogressviz.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mockito;

import io.github.aikobn26.teamprogressviz.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.entity.User;
import io.github.aikobn26.teamprogressviz.job.JobService;
import io.github.aikobn26.teamprogressviz.service.user.UserService;
import io.github.aikobn26.teamprogressviz.shared.properties.FrontendProperties;

@WebMvcTest(UserController.class)
@Import(UserControllerTest.MockConfig.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Test
    void me_returnsUnauthorizedWhenSessionMissing() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void me_returnsUserWhenAuthenticated() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));

        var user = User.builder()
                .id(1L)
                .githubId(1_000L)
                .login("octocat")
                .name("Octo Cat")
                .avatarUrl("https://avatar")
                .build();
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.githubId").value(1_000L))
                .andExpect(jsonPath("$.login").value("octocat"));
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
        JobService jobService() {
            return Mockito.mock(JobService.class);
        }

        @Bean
        FrontendProperties frontendProperties() {
            return new FrontendProperties(URI.create("http://localhost:3000"), "/success", "/error");
        }
    }
}

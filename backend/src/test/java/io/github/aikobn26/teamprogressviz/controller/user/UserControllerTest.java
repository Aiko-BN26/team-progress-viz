package io.github.aikobn26.teamprogressviz.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.aikobn26.teamprogressviz.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.entity.User;
import io.github.aikobn26.teamprogressviz.job.JobService;
import io.github.aikobn26.teamprogressviz.service.user.UserOnboardingService;
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

    @Autowired
    private UserOnboardingService userOnboardingService;

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
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.of("token"));

        var user = User.builder()
                .id(1L)
                .githubId(1_000L)
                .login("octocat")
                .name("Octo Cat")
                .avatarUrl("https://avatar")
                .build();
        var onboardingJob = new UserOnboardingService.OnboardingJobResult(10L, "octo-org", "job-1");
        when(userOnboardingService.onboardUser(user, "token")).thenReturn(List.of(onboardingJob));

        when(userService.ensureUserExists(eq(authUser), any())).thenAnswer((Answer<User>) invocation -> {
            Consumer<User> callback = invocation.getArgument(1);
            if (callback != null) {
                callback.accept(user);
            }
            return user;
        });

        var result = mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.githubId").value(1_000L))
                .andExpect(jsonPath("$.login").value("octocat"))
                .andReturn();

        verify(userOnboardingService).onboardUser(user, "token");

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        Object attribute = session.getAttribute(UserOnboardingService.SESSION_ATTRIBUTE_ONBOARDING_JOBS);
        assertThat(attribute).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<UserOnboardingService.OnboardingJobResult> stored = (List<UserOnboardingService.OnboardingJobResult>) attribute;
        assertThat(stored).hasSize(1);
        assertThat(stored.get(0).jobId()).isEqualTo("job-1");
    }

    @Test
    void onboardingJobs_returnsEntriesAndClearsSessionAttribute() throws Exception {
        var authUser = new AuthenticatedUser(2_000L, "hubot", "Hubot", "https://avatar");
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));

        var user = User.builder()
                .id(2L)
                .githubId(2_000L)
                .login("hubot")
                .name("Hubot")
                .avatarUrl("https://avatar")
                .build();
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                UserOnboardingService.SESSION_ATTRIBUTE_ONBOARDING_JOBS,
                new ArrayList<>(List.of(new UserOnboardingService.OnboardingJobResult(20L, "hub-org", "job-42"))));

        mockMvc.perform(get("/api/users/me/onboarding-jobs").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].organizationId").value(20L))
                .andExpect(jsonPath("$[0].organizationLogin").value("hub-org"))
                .andExpect(jsonPath("$[0].jobId").value("job-42"));

        assertThat(session.getAttribute(UserOnboardingService.SESSION_ATTRIBUTE_ONBOARDING_JOBS))
                .isInstanceOf(List.class);
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
        UserOnboardingService userOnboardingService() {
            return Mockito.mock(UserOnboardingService.class);
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

package io.github.aikobn26.teamprogressviz.controller.organization;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.aikobn26.teamprogressviz.feature.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.organization.controller.StatusController;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.request.StatusUpdateRequest;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.StatusListItemResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.StatusUpdateResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.service.StatusService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.support.FrontendPropertiesTestConfig;

@WebMvcTest(StatusController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({StatusControllerTest.MockConfig.class, FrontendPropertiesTestConfig.class})
class StatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitHubOAuthService gitHubOAuthService;

    @Autowired
    private UserService userService;

    @Autowired
    private StatusService statusService;

    @Test
    void upsertStatus_returnsUnauthorizedWhenUnauthenticated() throws Exception {
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.empty());

        var request = new StatusUpdateRequest("working", "Shipping", 8, null, "2025-01-07");
        mockMvc.perform(post("/api/organizations/12/statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upsertStatus_returnsStatusResponse() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var request = new StatusUpdateRequest("working", "Shipping", 8, null, "2025-01-07");
        var personal = new StatusUpdateResponse.PersonalStatus(true, "working", "Shipping", OffsetDateTime.parse("2025-01-07T12:00:00Z"), 4, 8, 3, "https://github.com/org/repo/pull/1");
        var member = new StatusUpdateResponse.MemberStatus("octocat", "Octo Cat", "https://avatar", "working", "Shipping", OffsetDateTime.parse("2025-01-07T12:00:00Z"), 4, 8, 3, "https://github.com/org/repo/pull/1");
        var summary = new StatusUpdateResponse.StatusSummary(5, 2);
        var response = new StatusUpdateResponse(personal, member, summary);
        when(statusService.upsertStatus(same(user), eq(12L), eq(request))).thenReturn(response);

        mockMvc.perform(post("/api/organizations/12/statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personalStatus.submitted").value(true))
                .andExpect(jsonPath("$.summary.activeToday").value(5));

        verify(statusService).upsertStatus(same(user), eq(12L), eq(request));
    }

    @Test
    void listStatuses_returnsStatuses() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        var statusItem = new StatusListItemResponse(30L, 20L, "octocat", "Octo Cat", "https://avatar", LocalDate.parse("2025-01-07"), 480, "working", "Shipping", OffsetDateTime.parse("2025-01-07T12:00:00Z"));
        when(statusService.listStatuses(same(user), eq(12L), eq(LocalDate.parse("2025-01-07")))).thenReturn(List.of(statusItem));

        mockMvc.perform(get("/api/organizations/12/statuses")
                        .param("date", "2025-01-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statusMessage").value("Shipping"));

        verify(statusService).listStatuses(same(user), eq(12L), eq(LocalDate.parse("2025-01-07")));
    }

    @Test
    void deleteStatus_returnsNoContent() throws Exception {
        var authUser = new AuthenticatedUser(1_000L, "octocat", "Octo Cat", "https://avatar");
        var user = User.builder().id(20L).githubId(1_000L).login("octocat").build();
        when(gitHubOAuthService.getAuthenticatedUser(any())).thenReturn(Optional.of(authUser));
        when(userService.ensureUserExists(authUser)).thenReturn(user);

        mockMvc.perform(delete("/api/organizations/12/statuses/88"))
                .andExpect(status().isNoContent());

        verify(statusService).deleteStatus(same(user), eq(12L), eq(88L));
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
        StatusService statusService() {
            return Mockito.mock(StatusService.class);
        }
    }
}

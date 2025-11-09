package io.github.aikobn26.teamprogressviz.github.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.RequestBuilder;

import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.github.controller.GitHubOrganizationController;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubOrganization;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubOrganizationMember;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubRepository;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubOrganizationService;
import io.github.aikobn26.teamprogressviz.shared.properties.FrontendProperties;
import reactor.core.publisher.Mono;

@WebMvcTest(GitHubOrganizationController.class)
@AutoConfigureMockMvc(addFilters = false)
class GitHubOrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @SuppressWarnings("removal")
    @MockBean
    private GitHubOrganizationService organizationService;

    @SuppressWarnings("removal")
    @MockBean
    private GitHubOAuthService gitHubOAuthService;

    @SuppressWarnings("removal")
    @MockBean
    private FrontendProperties frontendProperties;

    @BeforeEach
    void setUpMocks() {
        when(frontendProperties.origin()).thenReturn("http://localhost:3000");
    }

    @Test
    void listOrganizations_returnsUnauthorizedWhenAccessTokenMissing() throws Exception {
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.empty());

    performAsync(get("/api/github/organizations"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void listOrganizations_returnsOrganizationsWhenAuthorized() throws Exception {
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.of("token"));
        var organizations = List.of(new GitHubOrganization(1L, "octo", "Octo", "desc", "avatar", "url"));
        when(organizationService.listOrganizationsReactive("token")).thenReturn(Mono.just(organizations));

    performAsync(get("/api/github/organizations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].login").value("octo"))
                .andExpect(jsonPath("$[0].name").value("Octo"));

        verify(organizationService).listOrganizationsReactive("token");
    }

    @Test
    void listRepositories_returnsUnauthorizedWhenAccessTokenMissing() throws Exception {
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.empty());

    performAsync(get("/api/github/organizations/octo/repositories"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void listRepositories_returnsRepositoriesWhenAuthorized() throws Exception {
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.of("token"));
        var repositories = List.of(new GitHubRepository(2L, "repo", "desc", "url", "Java", 10, 2, "main", true, false));
        when(organizationService.listRepositoriesReactive("token", "octo")).thenReturn(Mono.just(repositories));

    performAsync(get("/api/github/organizations/octo/repositories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(2L))
                .andExpect(jsonPath("$[0].name").value("repo"))
                .andExpect(jsonPath("$[0].isPrivate").value(true));

        verify(organizationService).listRepositoriesReactive(eq("token"), eq("octo"));
    }

    @Test
    void listMembers_returnsUnauthorizedWhenAccessTokenMissing() throws Exception {
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.empty());

    performAsync(get("/api/github/organizations/octo/members"))
        .andExpect(status().isUnauthorized());
    }

    @Test
    void listMembers_returnsMembersWhenAuthorized() throws Exception {
        when(gitHubOAuthService.getAccessToken(any())).thenReturn(Optional.of("token"));
        var members = List.of(new GitHubOrganizationMember(3L, "octocat", "avatar", "url", "User", false));
        when(organizationService.listMembersReactive("token", "octo")).thenReturn(Mono.just(members));

    performAsync(get("/api/github/organizations/octo/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(3L))
                .andExpect(jsonPath("$[0].login").value("octocat"))
                .andExpect(jsonPath("$[0].siteAdmin").value(false));

        verify(organizationService).listMembersReactive(eq("token"), eq("octo"));
    }
    private ResultActions performAsync(RequestBuilder requestBuilder) throws Exception {
        MvcResult result = mockMvc.perform(requestBuilder)
                .andExpect(request().asyncStarted())
                .andReturn();
        return mockMvc.perform(asyncDispatch(result));
    }
}

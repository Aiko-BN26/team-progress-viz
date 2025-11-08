package io.github.aikobn26.teamprogressviz.feature.github.service;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.aikobn26.teamprogressviz.feature.github.exception.GitHubApiException;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubOrganization;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubOrganizationMember;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubRepository;
import io.github.aikobn26.teamprogressviz.feature.github.properties.GitHubApiProperties;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GitHubOrganizationService {

    private static final String ACCEPT_HEADER = "application/vnd.github+json";
    private static final String USER_AGENT = "team-progress-viz-backend";

    private final WebClient webClient;
    private final GitHubApiProperties apiProperties;

    public List<GitHubOrganization> listOrganizations(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token must not be blank");
        }

        URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                .pathSegment("user", "orgs")
                .queryParam("per_page", 100)
                .build()
                .toUri();

        GitHubOrganizationResponse[] response = executeGet(uri, GitHubOrganizationResponse[].class, accessToken,
                "Failed to fetch GitHub organizations");

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .map(GitHubOrganizationService::toOrganization)
                .toList();
    }

    public java.util.Optional<GitHubOrganization> getOrganization(String accessToken, String organization) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token must not be blank");
        }
        if (organization == null || organization.isBlank()) {
            throw new IllegalArgumentException("organization must not be blank");
        }

        URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                .pathSegment("orgs", organization)
                .build()
                .toUri();

        GitHubOrganizationResponse response = executeGet(uri, GitHubOrganizationResponse.class, accessToken,
                "Failed to fetch GitHub organization");

        if (response == null) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.ofNullable(toOrganization(response));
    }

    public List<GitHubRepository> listRepositories(String accessToken, String organization) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token must not be blank");
        }
        if (organization == null || organization.isBlank()) {
            throw new IllegalArgumentException("organization must not be blank");
        }

        URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                                    .pathSegment("orgs", organization, "repos")
                                    .queryParam("per_page", 100)
                                    .queryParam("type", "all")
                                    .queryParam("sort", "updated")
                                    .build()
                                    .toUri();

        GitHubRepositoryResponse[] response = executeGet(uri, GitHubRepositoryResponse[].class, accessToken,
                "Failed to fetch GitHub repositories");

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .map(GitHubOrganizationService::toRepository)
                .toList();
    }

    public List<GitHubOrganizationMember> listMembers(String accessToken, String organization) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new IllegalArgumentException("GitHub access token must not be blank");
        }
        if (organization == null || organization.isBlank()) {
            throw new IllegalArgumentException("organization must not be blank");
        }

        URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                .pathSegment("orgs", organization, "members")
                .queryParam("per_page", 100)
        .queryParam("role", "all")
                .build()
                .toUri();

        GitHubMemberResponse[] response = executeGet(uri, GitHubMemberResponse[].class, accessToken,
                "Failed to fetch GitHub members");

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .map(GitHubOrganizationService::toMember)
                .toList();
    }

    private <T> T executeGet(URI uri, Class<T> responseType, String accessToken, String failureMessage) {
        try {
            return webClient.get()
                    .uri(uri)
                    .header(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                    .header(HttpHeaders.USER_AGENT, USER_AGENT)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientResponseException e) {
            String message = String.format("%s: %s", failureMessage, e.getMessage());
            throw new GitHubApiException(message, e.getStatusCode(), e);
        } catch (RuntimeException e) {
            throw new GitHubApiException(failureMessage, e);
        }
    }

    private static GitHubOrganization toOrganization(GitHubOrganizationResponse response) {
        return new GitHubOrganization(
                    response.id(),
                    response.login(),
                    response.name(),
                    response.description(),
                    response.avatarUrl(),
                    response.htmlUrl()
                );
    }

    private static GitHubRepository toRepository(GitHubRepositoryResponse response) {
        return new GitHubRepository(
                    response.id(),
                    response.name(),
                    response.description(),
                    response.htmlUrl(),
                    response.language(),
                    response.stargazersCount(),
                    response.forksCount(),
                    response.defaultBranch(),
                    response.isPrivate(),
                    response.archived()
                );
    }

    private static GitHubOrganizationMember toMember(GitHubMemberResponse response) {
        return new GitHubOrganizationMember(
                response.id(),
                response.login(),
                response.avatarUrl(),
                response.htmlUrl(),
                response.type(),
                response.siteAdmin()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubOrganizationResponse(
            Long id,
            String login,
            String name,
            String description,
            @JsonProperty("avatar_url") String avatarUrl,
            @JsonProperty("html_url") String htmlUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubRepositoryResponse(
            Long id,
            String name,
            String description,
            @JsonProperty("html_url") String htmlUrl,
            String language,
            @JsonProperty("stargazers_count") Integer stargazersCount,
            @JsonProperty("forks_count") Integer forksCount,
            @JsonProperty("default_branch") String defaultBranch,
            @JsonProperty("private") boolean isPrivate,
            boolean archived
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubMemberResponse(
        Long id,
        String login,
        @JsonProperty("avatar_url") String avatarUrl,
        @JsonProperty("html_url") String htmlUrl,
        String type,
        @JsonProperty("site_admin") boolean siteAdmin
    ) {}
}

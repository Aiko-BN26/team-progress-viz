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
import reactor.core.publisher.Mono;

@Service
@AllArgsConstructor
public class GitHubOrganizationService {

    private static final String ACCEPT_HEADER = "application/vnd.github+json";
    private static final String USER_AGENT = "team-progress-viz-backend";

    private final WebClient webClient;
    private final GitHubApiProperties apiProperties;

    public Mono<List<GitHubOrganization>> listOrganizationsReactive(String accessToken) {
        return Mono.defer(() -> {
            if (accessToken == null || accessToken.isBlank()) {
                return Mono.error(new IllegalArgumentException("GitHub access token must not be blank"));
            }

            URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                    .pathSegment("user", "orgs")
                    .queryParam("per_page", 100)
                    .build()
                    .toUri();

            return executeGetReactive(uri, GitHubOrganizationResponse[].class, accessToken,
                    "Failed to fetch GitHub organizations")
                    .defaultIfEmpty(new GitHubOrganizationResponse[0])
                    .map(response -> Arrays.stream(response)
                            .filter(Objects::nonNull)
                            .map(GitHubOrganizationService::toOrganization)
                            .toList());
        });
    }

    public List<GitHubOrganization> listOrganizations(String accessToken) {
        return listOrganizationsReactive(accessToken).blockOptional().orElse(List.of());
    }

    public Mono<java.util.Optional<GitHubOrganization>> getOrganizationReactive(String accessToken, String organization) {
        return Mono.defer(() -> {
            if (accessToken == null || accessToken.isBlank()) {
                return Mono.error(new IllegalArgumentException("GitHub access token must not be blank"));
            }
            if (organization == null || organization.isBlank()) {
                return Mono.error(new IllegalArgumentException("organization must not be blank"));
            }

            URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                    .pathSegment("orgs", organization)
                    .build()
                    .toUri();

            return executeGetReactive(uri, GitHubOrganizationResponse.class, accessToken,
                    "Failed to fetch GitHub organization")
                    .map(response -> response == null
                            ? java.util.Optional.<GitHubOrganization>empty()
                            : java.util.Optional.ofNullable(toOrganization(response)))
                    .defaultIfEmpty(java.util.Optional.empty());
        });
    }

    public java.util.Optional<GitHubOrganization> getOrganization(String accessToken, String organization) {
        return getOrganizationReactive(accessToken, organization).blockOptional().orElse(java.util.Optional.empty());
    }

    public Mono<List<GitHubRepository>> listRepositoriesReactive(String accessToken, String organization) {
        return Mono.defer(() -> {
            if (accessToken == null || accessToken.isBlank()) {
                return Mono.error(new IllegalArgumentException("GitHub access token must not be blank"));
            }
            if (organization == null || organization.isBlank()) {
                return Mono.error(new IllegalArgumentException("organization must not be blank"));
            }

            URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                    .pathSegment("orgs", organization, "repos")
                    .queryParam("per_page", 100)
                    .queryParam("type", "all")
                    .queryParam("sort", "updated")
                    .build()
                    .toUri();

            return executeGetReactive(uri, GitHubRepositoryResponse[].class, accessToken,
                    "Failed to fetch GitHub repositories")
                    .defaultIfEmpty(new GitHubRepositoryResponse[0])
                    .map(response -> Arrays.stream(response)
                            .filter(Objects::nonNull)
                            .map(GitHubOrganizationService::toRepository)
                            .toList());
        });
    }

    public List<GitHubRepository> listRepositories(String accessToken, String organization) {
        return listRepositoriesReactive(accessToken, organization).blockOptional().orElse(List.of());
    }

    public Mono<List<GitHubOrganizationMember>> listMembersReactive(String accessToken, String organization) {
        return Mono.defer(() -> {
            if (accessToken == null || accessToken.isBlank()) {
                return Mono.error(new IllegalArgumentException("GitHub access token must not be blank"));
            }
            if (organization == null || organization.isBlank()) {
                return Mono.error(new IllegalArgumentException("organization must not be blank"));
            }

            URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                    .pathSegment("orgs", organization, "members")
                    .queryParam("per_page", 100)
                    .queryParam("role", "all")
                    .build()
                    .toUri();

            return executeGetReactive(uri, GitHubMemberResponse[].class, accessToken,
                    "Failed to fetch GitHub members")
                    .defaultIfEmpty(new GitHubMemberResponse[0])
                    .map(response -> Arrays.stream(response)
                            .filter(Objects::nonNull)
                            .map(GitHubOrganizationService::toMember)
                            .toList());
        });
    }

    public List<GitHubOrganizationMember> listMembers(String accessToken, String organization) {
        return listMembersReactive(accessToken, organization).blockOptional().orElse(List.of());
    }

    private <T> Mono<T> executeGetReactive(URI uri, Class<T> responseType, String accessToken, String failureMessage) {
        return Mono.defer(() -> webClient.get()
                .uri(uri)
                .header(HttpHeaders.ACCEPT, ACCEPT_HEADER)
                .header(HttpHeaders.USER_AGENT, USER_AGENT)
                .headers(headers -> headers.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(responseType))
                .onErrorMap(throwable -> {
                    if (throwable instanceof GitHubApiException) {
                        return throwable;
                    }
                    if (throwable instanceof WebClientResponseException e) {
                        String message = String.format("%s: %s", failureMessage, e.getMessage());
                        return new GitHubApiException(message, e.getStatusCode(), e);
                    }
                    return new GitHubApiException(failureMessage, throwable);
                });
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

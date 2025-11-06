package io.github.aikobn26.teamprogressviz.github.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.aikobn26.teamprogressviz.github.exception.GitHubApiException;
import io.github.aikobn26.teamprogressviz.github.model.GitHubOrganization;
import io.github.aikobn26.teamprogressviz.github.model.GitHubRepository;
import io.github.aikobn26.teamprogressviz.github.properties.GitHubApiProperties;
import reactor.core.publisher.Mono;

class GitHubOrganizationServiceTest {

    private GitHubApiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new GitHubApiProperties(URI.create("https://api.github.com"));
    }

    @Test
    void listOrganizations_returnsMappedOrganizations() {
        ExchangeFunction stub = request -> {
            if (request.url().toString().equals("https://api.github.com/user/orgs?per_page=100")) {
                var response = ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("[{\"id\":1,\"login\":\"octo-org\",\"name\":\"Octo Org\",\"description\":\"Org description\",\"avatar_url\":\"https://avatars.githubusercontent.com/u/1\",\"html_url\":\"https://github.com/octo-org\"}]")
                        .build();
                return Mono.just(response);
            }
            return Mono.error(new IllegalStateException("Unexpected request: " + request.url()));
        };

        var service = new GitHubOrganizationService(buildClient(stub), properties);

        List<GitHubOrganization> organizations = service.listOrganizations("token-abc");

        assertThat(organizations)
                .containsExactly(new GitHubOrganization(
                        1L,
                        "octo-org",
                        "Octo Org",
                        "Org description",
                        "https://avatars.githubusercontent.com/u/1",
                        "https://github.com/octo-org"));
    }

    @Test
    void listRepositories_returnsMappedRepositories() {
        ExchangeFunction stub = request -> {
            if (request.url().toString().equals("https://api.github.com/orgs/octo-org/repos?per_page=100&type=all&sort=updated")) {
                var response = ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body("[{\"id\":99,\"name\":\"octo-repo\",\"description\":\"Repo description\",\"html_url\":\"https://github.com/octo-org/octo-repo\",\"language\":\"TypeScript\",\"stargazers_count\":42,\"forks_count\":7,\"default_branch\":\"main\",\"private\":true,\"archived\":false}]")
                        .build();
                return Mono.just(response);
            }
            return Mono.error(new IllegalStateException("Unexpected request: " + request.url()));
        };

        var service = new GitHubOrganizationService(buildClient(stub), properties);

        List<GitHubRepository> repositories = service.listRepositories("token-abc", "octo-org");

        assertThat(repositories)
                .containsExactly(new GitHubRepository(
                        99L,
                        "octo-repo",
                        "Repo description",
                        "https://github.com/octo-org/octo-repo",
                        "TypeScript",
                        42,
                        7,
                        "main",
                        true,
                        false));
    }

    @Test
    void listOrganizations_throwsWhenTokenBlank() {
        var service = new GitHubOrganizationService(buildClient(request -> Mono.never()), properties);

        assertThatThrownBy(() -> service.listOrganizations(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("access token");
    }

    @Test
    void listRepositories_throwsWhenTokenBlank() {
        var service = new GitHubOrganizationService(buildClient(request -> Mono.never()), properties);

        assertThatThrownBy(() -> service.listRepositories("", "org"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("access token");
    }

    @Test
    void listRepositories_throwsWhenOrganizationBlank() {
        var service = new GitHubOrganizationService(buildClient(request -> Mono.never()), properties);

        assertThatThrownBy(() -> service.listRepositories("token", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("organization must not be blank");
    }

    @Test
    void listOrganizations_wrapsWebClientErrors() {
        ExchangeFunction stub = request -> {
            var response = ClientResponse.create(HttpStatus.NOT_FOUND)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"message\":\"Not Found\"}")
                    .build();
            return Mono.just(response);
        };

        var service = new GitHubOrganizationService(buildClient(stub), properties);

        assertThatThrownBy(() -> service.listOrganizations("token"))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(ex -> assertThat(((GitHubApiException) ex).statusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listRepositories_wrapsWebClientErrors() {
        ExchangeFunction stub = request -> {
            var response = ClientResponse.create(HttpStatus.BAD_GATEWAY)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"message\":\"Upstream error\"}")
                    .build();
            return Mono.just(response);
        };

        var service = new GitHubOrganizationService(buildClient(stub), properties);

        assertThatThrownBy(() -> service.listRepositories("token", "octo-org"))
                .isInstanceOf(GitHubApiException.class)
                .satisfies(ex -> assertThat(((GitHubApiException) ex).statusCode()).isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    private WebClient buildClient(ExchangeFunction stub) {
        return WebClient.builder().exchangeFunction(stub).build();
    }
}

package io.github.aikobn26.teamprogressviz.feature.github.service;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.aikobn26.teamprogressviz.feature.github.exception.GitHubApiException;
import io.github.aikobn26.teamprogressviz.feature.github.properties.GitHubApiProperties;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GitHubRepositoryService {

    private static final String ACCEPT_HEADER = "application/vnd.github+json";
    private static final String USER_AGENT = "team-progress-viz-backend";

    private final WebClient webClient;
    private final GitHubApiProperties apiProperties;

    public List<GitHubPullRequestSummary> listPullRequestSummaries(String accessToken, String owner, String repository, int perPage) {
        if (!hasText(accessToken) || !hasText(owner) || !hasText(repository)) {
            throw new IllegalArgumentException("accessToken, owner, and repository must not be blank");
        }

        int size = Math.min(Math.max(perPage, 1), 100);
        URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                .pathSegment("repos", owner, repository, "pulls")
                .queryParam("state", "all")
                .queryParam("sort", "updated")
                .queryParam("direction", "desc")
                .queryParam("per_page", size)
                .build()
                .toUri();

        GitHubPullRequestSummaryResponse[] response = executeGet(uri, GitHubPullRequestSummaryResponse[].class, accessToken,
                "Failed to fetch pull requests");

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .map(item -> new GitHubPullRequestSummary(item.id(), item.number(), item.updatedAt()))
                .toList();
    }

    public Optional<GitHubPullRequest> getPullRequest(String accessToken, String owner, String repository, int number) {
        if (!hasText(accessToken) || !hasText(owner) || !hasText(repository)) {
            throw new IllegalArgumentException("accessToken, owner, and repository must not be blank");
        }

        URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                .pathSegment("repos", owner, repository, "pulls", String.valueOf(number))
                .build()
                .toUri();

        GitHubPullRequestResponse response = executeGet(uri, GitHubPullRequestResponse.class, accessToken,
                "Failed to fetch pull request detail");

        if (response == null) {
            return Optional.empty();
        }

        return Optional.of(new GitHubPullRequest(
                response.id(),
                response.number(),
                response.title(),
                response.body(),
                response.state(),
                Boolean.TRUE.equals(response.merged()),
                response.htmlUrl(),
                toSimpleUser(response.user()),
                toSimpleUser(response.mergedBy()),
                response.additions(),
                response.deletions(),
                response.changedFiles(),
                response.createdAt(),
                response.updatedAt(),
                response.mergedAt(),
                response.closedAt()));
    }

    public List<GitHubPullRequestFile> listPullRequestFiles(String accessToken, String owner, String repository, int number, int perPage) {
        if (!hasText(accessToken) || !hasText(owner) || !hasText(repository)) {
            throw new IllegalArgumentException("accessToken, owner, and repository must not be blank");
        }

        int size = Math.min(Math.max(perPage, 1), 100);
        URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                .pathSegment("repos", owner, repository, "pulls", String.valueOf(number), "files")
                .queryParam("per_page", size)
                .build()
                .toUri();

        GitHubPullRequestFileResponse[] response = executeGet(uri, GitHubPullRequestFileResponse[].class, accessToken,
                "Failed to fetch pull request files");

        if (response == null) {
            return List.of();
        }

        return Arrays.stream(response)
                .filter(Objects::nonNull)
                .map(item -> new GitHubPullRequestFile(item.filename(), item.additions(), item.deletions(), item.changes(), item.rawUrl()))
                .toList();
    }

        public List<GitHubCommit> listCommits(String accessToken, String owner, String repository, int perPage, OffsetDateTime since) {
                if (!hasText(accessToken) || !hasText(owner) || !hasText(repository)) {
                        throw new IllegalArgumentException("accessToken, owner, and repository must not be blank");
                }

                int size = Math.min(Math.max(perPage, 1), 100);
                UriComponentsBuilder builder = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                                .pathSegment("repos", owner, repository, "commits")
                                .queryParam("per_page", size);
                if (since != null) {
                        builder.queryParam("since", since.toString());
                }
                URI uri = builder.build().toUri();

                GitHubCommitResponse[] response;
                try {
                        response = executeGet(uri, GitHubCommitResponse[].class, accessToken,
                                        "Failed to fetch commits");
                } catch (GitHubApiException e) {
                        if (e.statusCode() != null && e.statusCode().value() == 409) {
                                return List.of();
                        }
                        throw e;
                }

                if (response == null) {
                        return List.of();
                }

                return Arrays.stream(response)
                                .filter(Objects::nonNull)
                                .map(this::toCommit)
                                .toList();
        }

        public Optional<GitHubCommitDetail> getCommit(String accessToken, String owner, String repository, String sha) {
                if (!hasText(accessToken) || !hasText(owner) || !hasText(repository) || !hasText(sha)) {
                        throw new IllegalArgumentException("accessToken, owner, repository, and sha must not be blank");
                }

                URI uri = UriComponentsBuilder.fromUri(apiProperties.baseUrl())
                                .pathSegment("repos", owner, repository, "commits", sha)
                                .build()
                                .toUri();

                try {
                        GitHubCommitResponse response = executeGet(uri, GitHubCommitResponse.class, accessToken,
                                        "Failed to fetch commit detail");

                        if (response == null) {
                                return Optional.empty();
                        }

                        GitHubCommit commit = toCommit(response);
                        List<GitHubCommitFile> files = response.files() == null
                                        ? List.of()
                                        : Arrays.stream(response.files())
                                                        .filter(Objects::nonNull)
                                                        .map(item -> new GitHubCommitFile(
                                                                        item.filename(),
                                                                        item.status(),
                                                                        item.additions(),
                                                                        item.deletions(),
                                                                        item.changes(),
                                                                        item.rawUrl()))
                                                        .toList();

                        return Optional.of(new GitHubCommitDetail(commit, files));
                } catch (GitHubApiException e) {
                        if (e.statusCode() != null && e.statusCode().value() == 404) {
                                return Optional.empty();
                        }
                        throw e;
                }
        }

    private GitHubCommit toCommit(GitHubCommitResponse response) {
        String authorName = null;
        String authorEmail = null;
        OffsetDateTime authoredAt = null;
        if (response.commit() != null && response.commit().author() != null) {
            authorName = response.commit().author().name();
            authorEmail = response.commit().author().email();
            authoredAt = response.commit().author().date();
        }

        String committerName = null;
        String committerEmail = null;
        OffsetDateTime committedAt = null;
        if (response.commit() != null && response.commit().committer() != null) {
            committerName = response.commit().committer().name();
            committerEmail = response.commit().committer().email();
            committedAt = response.commit().committer().date();
        }

        return new GitHubCommit(
                response.sha(),
                response.commit() != null ? response.commit().message() : null,
                response.htmlUrl(),
                authorName,
                authorEmail,
                authoredAt,
                committerName,
                committerEmail,
                committedAt);
    }

    private GitHubSimpleUser toSimpleUser(GitHubUserResponse response) {
        if (response == null || response.id() == null || !hasText(response.login())) {
            return null;
        }
        return new GitHubSimpleUser(response.id(), response.login(), response.avatarUrl(), response.htmlUrl());
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

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubPullRequestSummaryResponse(
            Long id,
            Integer number,
            @JsonProperty("updated_at") OffsetDateTime updatedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubPullRequestResponse(
            Long id,
            Integer number,
            String title,
            String body,
            String state,
            Boolean merged,
            @JsonProperty("html_url") String htmlUrl,
            GitHubUserResponse user,
            @JsonProperty("merged_by") GitHubUserResponse mergedBy,
            Integer additions,
            Integer deletions,
            @JsonProperty("changed_files") Integer changedFiles,
            @JsonProperty("created_at") OffsetDateTime createdAt,
            @JsonProperty("updated_at") OffsetDateTime updatedAt,
            @JsonProperty("merged_at") OffsetDateTime mergedAt,
            @JsonProperty("closed_at") OffsetDateTime closedAt
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubUserResponse(
            Long id,
            String login,
            @JsonProperty("avatar_url") String avatarUrl,
            @JsonProperty("html_url") String htmlUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubPullRequestFileResponse(
            String filename,
            Integer additions,
            Integer deletions,
            Integer changes,
            @JsonProperty("raw_url") String rawUrl
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubCommitResponse(
            String sha,
            @JsonProperty("html_url") String htmlUrl,
            CommitDetails commit,
            GitHubCommitFileResponse[] files
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CommitDetails(
            CommitUser author,
            CommitUser committer,
            String message
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CommitUser(
            String name,
            String email,
            OffsetDateTime date
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubCommitFileResponse(
            String filename,
            String status,
            Integer additions,
            Integer deletions,
            Integer changes,
            @JsonProperty("raw_url") String rawUrl
    ) {}

    public record GitHubPullRequestSummary(
            Long id,
            Integer number,
            OffsetDateTime updatedAt
    ) {}

    public record GitHubPullRequest(
            Long id,
            Integer number,
            String title,
            String body,
            String state,
            boolean merged,
            String htmlUrl,
            GitHubSimpleUser author,
            GitHubSimpleUser mergedBy,
            Integer additions,
            Integer deletions,
            Integer changedFiles,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime mergedAt,
            OffsetDateTime closedAt
    ) {}

    public record GitHubPullRequestFile(
            String path,
            Integer additions,
            Integer deletions,
            Integer changes,
            String rawUrl
    ) {}

    public record GitHubSimpleUser(
            Long id,
            String login,
            String avatarUrl,
            String htmlUrl
    ) {}

    public record GitHubCommit(
            String sha,
            String message,
            String htmlUrl,
            String authorName,
            String authorEmail,
            OffsetDateTime authoredAt,
            String committerName,
            String committerEmail,
            OffsetDateTime committedAt
    ) {}

    public record GitHubCommitDetail(
            GitHubCommit commit,
            List<GitHubCommitFile> files
    ) {}

    public record GitHubCommitFile(
            String path,
            String status,
            Integer additions,
            Integer deletions,
            Integer changes,
            String rawUrl
    ) {}
}

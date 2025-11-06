package io.github.aikobn26.teamprogressviz.auth.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.github.aikobn26.teamprogressviz.auth.exception.GitHubOAuthException;
import io.github.aikobn26.teamprogressviz.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.shared.properties.GitHubOAuthProperties;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class GitHubOAuthService {

    public static final String SESSION_ATTRIBUTE_USER = "AUTHENTICATED_USER";
    public static final String SESSION_ATTRIBUTE_ACCESS_TOKEN = "GITHUB_ACCESS_TOKEN";
    private static final String SESSION_ATTRIBUTE_STATE = "GITHUB_OAUTH_STATE";
    private static final int SESSION_TIMEOUT_SECONDS = 3600;

    private static final Logger log = LoggerFactory.getLogger(GitHubOAuthService.class);

    private final GitHubOAuthProperties properties;
    private final WebClient webClient;
    private final SecureRandom secureRandom = new SecureRandom();

    public String createAuthorizationUrl(HttpSession session) {
        var clientId = properties.clientId();
        var state = generateStateToken();
        session.setAttribute(SESSION_ATTRIBUTE_STATE, state);

    var scopes = List.of("read:user", "user:email", "read:org", "repo");

    var uri = UriComponentsBuilder.fromUri(properties.authorizeUrl())
        .queryParam("client_id", clientId)
        .queryParam("redirect_uri", properties.callbackUrl())
        .queryParam("scope", String.join(" ", scopes))
                .queryParam("state", state)
                .build()
                .encode()
                .toUriString();
        return uri;
    }

    public AuthenticatedUser completeAuthentication(String code, String state, HttpSession session) {
        if (code == null || code.isBlank()) {
            throw new GitHubOAuthException("Authorization code is missing");
        }
        var expectedState = (String) session.getAttribute(SESSION_ATTRIBUTE_STATE);
        session.removeAttribute(SESSION_ATTRIBUTE_STATE);
        if (expectedState == null || !expectedState.equals(state)) {
            throw new GitHubOAuthException("State parameter mismatch");
        }
        var accessToken = exchangeCodeForToken(code);
        var profile = fetchUserProfile(accessToken);
        var user = new AuthenticatedUser(profile.id(), profile.login(), profile.name(), profile.avatarUrl());
        session.setAttribute(SESSION_ATTRIBUTE_USER, user);
        session.setAttribute(SESSION_ATTRIBUTE_ACCESS_TOKEN, accessToken);
        session.setMaxInactiveInterval(SESSION_TIMEOUT_SECONDS);
        return user;
    }

    public Optional<AuthenticatedUser> getAuthenticatedUser(HttpSession session) {
        var attribute = session.getAttribute(SESSION_ATTRIBUTE_USER);
        if (attribute instanceof AuthenticatedUser user) {
            return Optional.of(user);
        }
        return Optional.empty();
    }

    public Optional<String> getAccessToken(HttpSession session) {
        var attribute = session.getAttribute(SESSION_ATTRIBUTE_ACCESS_TOKEN);
        if (attribute instanceof String token && !token.isBlank()) {
            return Optional.of(token);
        }
        return Optional.empty();
    }

    private String exchangeCodeForToken(String code) {
        var clientSecret = properties.clientSecret();
        var clientId = properties.clientId();

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("code", code);
        form.add("redirect_uri", properties.callbackUrl().toString());

        try {
            AccessTokenResponse body = webClient.post()
                    .uri(properties.tokenUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(AccessTokenResponse.class)
                    .block();

            if (body == null || body.accessToken() == null || body.accessToken().isBlank()) {
                throw new GitHubOAuthException("Failed to obtain access token from GitHub");
            }
            return body.accessToken();
        } catch (WebClientResponseException e) {
            log.error("GitHub token exchange failed: {}", e.getResponseBodyAsString(), e);
            throw new GitHubOAuthException("Failed to exchange authorization code", e);
        }
    }

    private GitHubUserProfile fetchUserProfile(String accessToken) {
        try {
            GitHubUserProfile body = webClient.get()
                    .uri(properties.userUrl())
                    .accept(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .bodyToMono(GitHubUserProfile.class)
                    .block();

            if (body == null || body.id() == null || body.login() == null) {
                throw new GitHubOAuthException("GitHub user information is incomplete");
            }
            return body;
        } catch (WebClientResponseException e) {
            log.error("GitHub user fetch failed: {}", e.getResponseBodyAsString(), e);
            throw new GitHubOAuthException("Failed to fetch GitHub user profile", e);
        }
    }

    private String generateStateToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AccessTokenResponse(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GitHubUserProfile(
            @JsonProperty("id") Long id,
            @JsonProperty("login") String login,
            @JsonProperty("name") String name,
            @JsonProperty("avatar_url") String avatarUrl) {
    }
}
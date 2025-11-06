package io.github.aikobn26.teamprogressviz.auth.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.util.UriComponentsBuilder;

import io.github.aikobn26.teamprogressviz.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.shared.properties.GitHubOAuthProperties;
import reactor.core.publisher.Mono;

class GitHubOAuthServiceTest {

    private GitHubOAuthService service;
    private MockHttpSession session;
    private GitHubOAuthProperties properties;
    @BeforeEach
    void setUp() {
        session = new MockHttpSession();
        properties = new GitHubOAuthProperties(
                "client-id",
                "client-secret",
                URI.create("https://github.com/login/oauth/authorize"),
                URI.create("https://github.com/login/oauth/access_token"),
                URI.create("https://api.github.com/user"),
                URI.create("https://app.example.com/auth/callback"));
        
        var exchangeFunction = stubSuccessExchangeFunction();
        var webClient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        service = new GitHubOAuthService(properties, webClient);
    }

    @Test
    void createAuthorizationUrl_setsStateAndBuildsUri() {
        var url = service.createAuthorizationUrl(session);

        var state = (String) session.getAttribute("GITHUB_OAUTH_STATE");
        assertThat(state).isNotBlank();
        var params = UriComponentsBuilder.fromUriString(url).build().getQueryParams();
        assertThat(params.getFirst("client_id")).isEqualTo("client-id");
        assertThat(params.getFirst("redirect_uri")).isEqualTo("https://app.example.com/auth/callback");
    assertThat(params.getFirst("scope")).isEqualTo("read:user%20user:email%20read:org%20repo");
        assertThat(params.getFirst("state")).isEqualTo(state);
    }

    @Test
    void completeAuthentication_storesUserInSession() {
        session.setAttribute("GITHUB_OAUTH_STATE", "expected-state");

        var user = service.completeAuthentication("auth-code", "expected-state", session);

        assertThat(user)
                .isEqualTo(new AuthenticatedUser(1L, "octocat", "Octo Cat", "https://avatars.com/octocat"));
    assertThat(session.getAttribute("AUTHENTICATED_USER"))
                .isEqualTo(new AuthenticatedUser(1L, "octocat", "Octo Cat", "https://avatars.com/octocat"));
    assertThat(session.getAttribute(GitHubOAuthService.SESSION_ATTRIBUTE_ACCESS_TOKEN))
        .isEqualTo("token-123");
        assertThat(session.getMaxInactiveInterval()).isEqualTo(3600);
    }

    @Test
    void completeAuthentication_throwsWhenCodeMissing() {
        assertThatThrownBy(() -> service.completeAuthentication(null, "state", session))
                .isInstanceOf(GitHubOAuthException.class)
                .hasMessageContaining("Authorization code is missing");
    }

    @Test
    void completeAuthentication_throwsWhenStateMismatch() {
        session.setAttribute("GITHUB_OAUTH_STATE", "expected");

        assertThatThrownBy(() -> service.completeAuthentication("code", "unexpected", session))
                .isInstanceOf(GitHubOAuthException.class)
                .hasMessageContaining("State parameter mismatch");
    }

    @Test
    void getAuthenticatedUser_returnsEmptyWhenAbsent() {
        assertThat(service.getAuthenticatedUser(session)).isEmpty();
    }

    @Test
    void getAuthenticatedUser_returnsUserWhenPresent() {
        var user = new AuthenticatedUser(99L, "user", "User", "avatar");
        session.setAttribute("AUTHENTICATED_USER", user);

        assertThat(service.getAuthenticatedUser(session)).contains(user);
    }

    @Test
    void getAccessToken_returnsEmptyWhenNotStored() {
        assertThat(service.getAccessToken(session)).isEmpty();
    }

    @Test
    void getAccessToken_returnsEmptyWhenTokenBlank() {
        session.setAttribute(GitHubOAuthService.SESSION_ATTRIBUTE_ACCESS_TOKEN, "   ");

        assertThat(service.getAccessToken(session)).isEmpty();
    }

    @Test
    void getAccessToken_returnsTokenWhenPresent() {
        session.setAttribute(GitHubOAuthService.SESSION_ATTRIBUTE_ACCESS_TOKEN, "token-xyz");

        assertThat(service.getAccessToken(session)).contains("token-xyz");
    }

    @Test
    void completeAuthentication_throwsWhenTokenApiReturnsError() {
        ExchangeFunction errorStub = request -> {
            if (request.url().toString().equals("https://github.com/login/oauth/access_token")) {
                var response = ClientResponse.create(HttpStatus.BAD_REQUEST)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"error\":\"bad_verification_code\"}")
                        .build();
                return Mono.just(response);
            }
            return Mono.error(new IllegalStateException("Unexpected request: " + request.url()));
        };
        
        var webClient = WebClient.builder().exchangeFunction(errorStub).build();
        var errorService = new GitHubOAuthService(properties, webClient);
        
        session.setAttribute("GITHUB_OAUTH_STATE", "expected-state");

        assertThatThrownBy(() -> errorService.completeAuthentication("auth-code", "expected-state", session))
                .isInstanceOf(GitHubOAuthException.class)
                .hasMessageContaining("Failed to exchange authorization code");
    }

    @Test
    void completeAuthentication_throwsWhenUserApiReturnsError() {
        ExchangeFunction errorStub = request -> {
            if (request.url().toString().equals("https://github.com/login/oauth/access_token")) {
                var response = ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"access_token\":\"token-123\"}")
                        .build();
                return Mono.just(response);
            }
            if (request.url().toString().equals("https://api.github.com/user")) {
                var response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"message\":\"Server Error\"}")
                        .build();
                return Mono.just(response);
            }
            return Mono.error(new IllegalStateException("Unexpected request: " + request.url()));
        };
        
        var webClient = WebClient.builder().exchangeFunction(errorStub).build();
        var errorService = new GitHubOAuthService(properties, webClient);
        
        session.setAttribute("GITHUB_OAUTH_STATE", "expected-state");

        assertThatThrownBy(() -> errorService.completeAuthentication("auth-code", "expected-state", session))
                .isInstanceOf(GitHubOAuthException.class)
                .hasMessageContaining("Failed to fetch GitHub user profile");
    }

    @Test
    void completeAuthentication_throwsWhenTokenApiResponseIsMissingToken() {
        ExchangeFunction incompleteStub = request -> {
            if (request.url().toString().equals("https://github.com/login/oauth/access_token")) {
                var response = ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"scope\":\"read:user\"}")
                        .build();
                return Mono.just(response);
            }
            return Mono.error(new IllegalStateException("Unexpected request: " + request.url()));
        };

        var webClient = WebClient.builder().exchangeFunction(incompleteStub).build();
        var errorService = new GitHubOAuthService(properties, webClient);
        
        session.setAttribute("GITHUB_OAUTH_STATE", "expected-state");

        assertThatThrownBy(() -> errorService.completeAuthentication("auth-code", "expected-state", session))
                .isInstanceOf(GitHubOAuthException.class)
                .hasMessageContaining("Failed to obtain access token from GitHub");
    }

    @Test
    void completeAuthentication_throwsWhenUserApiResponseIsIncomplete() {
        ExchangeFunction incompleteStub = request -> {
            if (request.url().toString().equals("https://github.com/login/oauth/access_token")) {
                var response = ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"access_token\":\"token-123\"}")
                        .build();
                return Mono.just(response);
            }
            if (request.url().toString().equals("https://api.github.com/user")) {
                var response = ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"id\":1,\"name\":\"Octo Cat\"}") 
                        .build();
                return Mono.just(response);
            }
            return Mono.error(new IllegalStateException("Unexpected request: " + request.url()));
        };

        var webClient = WebClient.builder().exchangeFunction(incompleteStub).build();
        var errorService = new GitHubOAuthService(properties, webClient);
        
        session.setAttribute("GITHUB_OAUTH_STATE", "expected-state");

        assertThatThrownBy(() -> errorService.completeAuthentication("auth-code", "expected-state", session))
                .isInstanceOf(GitHubOAuthException.class)
                .hasMessageContaining("GitHub user information is incomplete");
    }

    private ExchangeFunction stubSuccessExchangeFunction() {
        return request -> {
            if (request.url().toString().equals("https://github.com/login/oauth/access_token")) {
                var response = ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"access_token\":\"token-123\"}")
                        .build();
                return Mono.just(response);
            }
            if (request.url().toString().equals("https://api.github.com/user")) {
                var response = ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                        .body("{\"id\":1,\"login\":\"octocat\",\"name\":\"Octo Cat\",\"avatar_url\":\"https://avatars.com/octocat\"}")
                        .build();
                return Mono.just(response);
            }
            return Mono.error(new IllegalStateException("Unexpected request: " + request.url()));
        };
    }
}
package io.github.aikobn26.teamprogressviz.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import io.github.aikobn26.teamprogressviz.feature.auth.controller.AuthControllerExceptionHandler;
import io.github.aikobn26.teamprogressviz.feature.auth.exception.GitHubOAuthException;

class AuthControllerExceptionHandlerTest {

    private final AuthControllerExceptionHandler handler = new AuthControllerExceptionHandler();

    @Test
    void handleGitHubOAuthException_returnsInternalServerError() {
        var response = handler.handleGitHubOAuthException(new GitHubOAuthException("failure"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "failure"));
    }

    @Test
    void handleUnexpectedException_returnsGenericError() {
        var response = handler.handleUnexpectedException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Unexpected error occurred"));
    }
}

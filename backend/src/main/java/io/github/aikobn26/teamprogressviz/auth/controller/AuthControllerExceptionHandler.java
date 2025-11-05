package io.github.aikobn26.teamprogressviz.auth.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthException;

@RestControllerAdvice(assignableTypes = AuthController.class)
public class AuthControllerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthControllerExceptionHandler.class);

    @ExceptionHandler(GitHubOAuthException.class)
    public ResponseEntity<Map<String, String>> handleGitHubOAuthException(GitHubOAuthException e) {
        log.warn("GitHub OAuth flow failed: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(Exception e) {
        log.error("Unexpected error in AuthController", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unexpected error occurred"));
    }
}

package io.github.aikobn26.teamprogressviz.github.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.aikobn26.teamprogressviz.github.exception.GitHubApiException;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice(assignableTypes = GitHubOrganizationController.class)
public class GitHubControllerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GitHubControllerExceptionHandler.class);

    @ExceptionHandler(GitHubApiException.class)
    public ResponseEntity<Map<String, String>> handleGitHubApiException(GitHubApiException e) {
        log.warn("GitHub API request failed: {}", e.getMessage());
        return ResponseEntity.status(e.statusCode()).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException e) {
        log.warn("Validation error in GitHub endpoint", e);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(Exception e) {
        log.error("Unexpected error in GitHub controller", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unexpected error occurred"));
    }
}

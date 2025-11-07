package io.github.aikobn26.teamprogressviz.controller.repository;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.aikobn26.teamprogressviz.exception.ForbiddenException;
import io.github.aikobn26.teamprogressviz.exception.ResourceNotFoundException;
import io.github.aikobn26.teamprogressviz.exception.ValidationException;

@RestControllerAdvice(basePackages = "io.github.aikobn26.teamprogressviz.controller.repository")
public class RepositoryControllerExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RepositoryControllerExceptionHandler.class);

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(ValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "type", "/errors/validation",
                "title", "Validation Failed",
                "status", HttpStatus.BAD_REQUEST.value(),
                "errors", List.of(Map.of(
                        "field", "",
                        "message", e.getMessage()
                ))
        ));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ResourceNotFoundException e) {
        log.warn("Repository resource not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "type", "/errors/not-found",
                "title", "Resource Not Found",
                "status", HttpStatus.NOT_FOUND.value(),
                "detail", e.getMessage()
        ));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException e) {
        log.warn("Repository access forbidden: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "type", "/errors/forbidden",
                "title", "Forbidden",
                "status", HttpStatus.FORBIDDEN.value(),
                "detail", e.getMessage()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception e) {
        log.error("Unexpected error in repository controller", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "type", "/errors/internal-server-error",
                "title", "Internal Server Error",
                "status", HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "detail", "An unexpected error occurred"
        ));
    }
}

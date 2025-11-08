package io.github.aikobn26.teamprogressviz.controller.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.aikobn26.teamprogressviz.feature.repository.controller.RepositoryControllerExceptionHandler;
import io.github.aikobn26.teamprogressviz.shared.exception.ForbiddenException;
import io.github.aikobn26.teamprogressviz.shared.exception.ResourceNotFoundException;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;

class RepositoryControllerExceptionHandlerTest {

    private final RepositoryControllerExceptionHandler handler = new RepositoryControllerExceptionHandler();

    @Test
    void handleValidation_returnsBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleValidation(new ValidationException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("title", "Validation Failed");
    }

    @Test
    void handleNotFound_returnsNotFound() {
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(new ResourceNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("detail", "missing");
    }

    @Test
    void handleForbidden_returnsForbidden() {
        ResponseEntity<Map<String, Object>> response = handler.handleForbidden(new ForbiddenException("forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("detail", "forbidden");
    }

    @Test
    void handleUnexpected_returnsInternalServerError() {
        ResponseEntity<Map<String, Object>> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("title", "Internal Server Error");
    }
}

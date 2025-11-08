package io.github.aikobn26.teamprogressviz.controller.organization;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import io.github.aikobn26.teamprogressviz.feature.organization.controller.OrganizationControllerExceptionHandler;
import io.github.aikobn26.teamprogressviz.shared.exception.ForbiddenException;
import io.github.aikobn26.teamprogressviz.shared.exception.ResourceConflictException;
import io.github.aikobn26.teamprogressviz.shared.exception.ResourceNotFoundException;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
import jakarta.validation.ConstraintViolationException;

class OrganizationControllerExceptionHandlerTest {

    private final OrganizationControllerExceptionHandler handler = new OrganizationControllerExceptionHandler();

    @Test
    void handleValidationException_returnsBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(new ValidationException("invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("title", "Validation Failed");
    }

    @Test
    void handleConstraintViolation_returnsBadRequest() {
        ResponseEntity<Map<String, Object>> response = handler.handleConstraintViolation(new ConstraintViolationException("invalid", java.util.Set.of()));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", HttpStatus.BAD_REQUEST.value());
    }

    @Test
    void handleConflict_returnsConflictStatus() {
        ResponseEntity<Map<String, Object>> response = handler.handleConflict(new ResourceConflictException("conflict"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("detail", "conflict");
    }

    @Test
    void handleForbidden_returnsForbiddenStatus() {
        ResponseEntity<Map<String, Object>> response = handler.handleForbidden(new ForbiddenException("forbidden"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("detail", "forbidden");
    }

    @Test
    void handleNotFound_returnsNotFoundStatus() {
        ResponseEntity<Map<String, Object>> response = handler.handleNotFound(new ResourceNotFoundException("missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("detail", "missing");
    }

    @Test
    void handleUnexpected_returnsInternalServerError() {
        ResponseEntity<Map<String, Object>> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("title", "Internal Server Error");
    }
}

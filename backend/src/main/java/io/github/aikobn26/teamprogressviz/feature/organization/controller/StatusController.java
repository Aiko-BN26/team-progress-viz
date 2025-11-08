package io.github.aikobn26.teamprogressviz.feature.organization.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.request.StatusUpdateRequest;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.StatusListItemResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.StatusUpdateResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.service.StatusService;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations/{organizationId}/statuses")
@RequiredArgsConstructor
@Validated
public class StatusController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final StatusService statusService;

    @PostMapping
    public ResponseEntity<StatusUpdateResponse> upsertStatus(@PathVariable Long organizationId,
                                                             @Valid @RequestBody StatusUpdateRequest request,
                                                             HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var response = statusService.upsertStatus(user, organizationId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<StatusListItemResponse>> listStatuses(@PathVariable Long organizationId,
                                                                     @RequestParam(required = false) String date,
                                                                     HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        LocalDate targetDate = null;
        if (date != null && !date.isBlank()) {
            try {
                targetDate = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new ValidationException("Invalid date format, use YYYY-MM-DD");
            }
        }
        var statuses = statusService.listStatuses(user, organizationId, targetDate);
        return ResponseEntity.ok(statuses);
    }

    @DeleteMapping("/{statusId}")
    public ResponseEntity<Void> deleteStatus(@PathVariable Long organizationId,
                                             @PathVariable Long statusId,
                                             HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        statusService.deleteStatus(user, organizationId, statusId);
        return ResponseEntity.noContent().build();
    }
}

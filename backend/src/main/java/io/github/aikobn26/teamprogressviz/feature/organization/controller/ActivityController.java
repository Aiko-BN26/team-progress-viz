package io.github.aikobn26.teamprogressviz.feature.organization.controller;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.ActivitySummaryItemResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.service.ActivityService;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitFeedResponse;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations/{organizationId}")
@RequiredArgsConstructor
public class ActivityController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final ActivityService activityService;

    @GetMapping("/activity/summary")
    public ResponseEntity<List<ActivitySummaryItemResponse>> summary(@PathVariable Long organizationId,
                                                                      @RequestParam String startDate,
                                                                      @RequestParam String endDate,
                                                                      @RequestParam(required = false) String groupBy,
                                                                      HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        LocalDate start = parseDate(startDate, "startDate");
        LocalDate end = parseDate(endDate, "endDate");
        var response = activityService.summarize(user, organizationId, start, end, groupBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/git-commit/feed")
    public ResponseEntity<CommitFeedResponse> commitFeed(@PathVariable Long organizationId,
                                                         @RequestParam(required = false) String cursor,
                                                         @RequestParam(required = false) Integer limit,
                                                         HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        Long cursorId = null;
        if (cursor != null && !cursor.isBlank()) {
            try {
                cursorId = Long.parseLong(cursor);
            } catch (NumberFormatException e) {
                throw new ValidationException("cursor must be a numeric value");
            }
        }
        var response = activityService.fetchCommitFeed(user, organizationId, cursorId, limit);
        return ResponseEntity.ok(response);
    }

    private LocalDate parseDate(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format, use YYYY-MM-DD");
        }
    }
}

package io.github.aikobn26.teamprogressviz.feature.repository.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.PullRequestDetailResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.PullRequestFeedResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.PullRequestFileResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.PullRequestListItemResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.service.PullRequestService;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class PullRequestController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final PullRequestService pullRequestService;

    @GetMapping("/repositories/{repositoryId}/pulls")
    public ResponseEntity<List<PullRequestListItemResponse>> list(@PathVariable Long repositoryId,
                                                                  @RequestParam(required = false) String state,
                                                                  @RequestParam(required = false) Integer limit,
                                                                  @RequestParam(required = false) Integer page,
                                                                  HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var response = pullRequestService.listPullRequests(user, repositoryId, state, limit, page);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories/{repositoryId}/pulls/{pullNumber}")
    public ResponseEntity<PullRequestDetailResponse> detail(@PathVariable Long repositoryId,
                                                            @PathVariable Integer pullNumber,
                                                            HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var response = pullRequestService.getPullRequest(user, repositoryId, pullNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories/{repositoryId}/pulls/{pullNumber}/files")
    public ResponseEntity<List<PullRequestFileResponse>> files(@PathVariable Long repositoryId,
                                                               @PathVariable Integer pullNumber,
                                                               HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var response = pullRequestService.listFiles(user, repositoryId, pullNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/organizations/{organizationId}/pulls/feed")
    public ResponseEntity<PullRequestFeedResponse> organizationFeed(@PathVariable Long organizationId,
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
        var response = pullRequestService.fetchFeed(user, organizationId, cursorId, limit);
        return ResponseEntity.ok(response);
    }
}

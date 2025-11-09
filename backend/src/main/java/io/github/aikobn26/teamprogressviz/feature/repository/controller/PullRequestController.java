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
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class PullRequestController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final PullRequestService pullRequestService;

    @GetMapping("/repositories/{repositoryId}/pulls")
    public Mono<ResponseEntity<List<PullRequestListItemResponse>>> list(@PathVariable Long repositoryId,
                                                                        @RequestParam(required = false) String state,
                                                                        @RequestParam(required = false) Integer limit,
                                                                        @RequestParam(required = false) Integer page,
                                                                        HttpSession session) {
        return resolveUser(session)
                .flatMap(user -> pullRequestService.listPullRequestsReactive(user, repositoryId, state, limit, page)
                        .map(ResponseEntity::ok))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping("/repositories/{repositoryId}/pulls/{pullNumber}")
    public Mono<ResponseEntity<PullRequestDetailResponse>> detail(@PathVariable Long repositoryId,
                                                                  @PathVariable Integer pullNumber,
                                                                  HttpSession session) {
        return resolveUser(session)
                .flatMap(user -> pullRequestService.getPullRequestReactive(user, repositoryId, pullNumber)
                        .map(ResponseEntity::ok))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping("/repositories/{repositoryId}/pulls/{pullNumber}/files")
    public Mono<ResponseEntity<List<PullRequestFileResponse>>> files(@PathVariable Long repositoryId,
                                                                     @PathVariable Integer pullNumber,
                                                                     HttpSession session) {
        return resolveUser(session)
                .flatMap(user -> pullRequestService.listFilesReactive(user, repositoryId, pullNumber)
                        .map(ResponseEntity::ok))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping("/organizations/{organizationId}/pulls/feed")
    public Mono<ResponseEntity<PullRequestFeedResponse>> organizationFeed(@PathVariable Long organizationId,
                                                                          @RequestParam(required = false) String cursor,
                                                                          @RequestParam(required = false) Integer limit,
                                                                          HttpSession session) {
        return resolveUser(session)
                .flatMap(user -> pullRequestService.fetchFeedReactive(user, organizationId, parseCursor(cursor), limit)
                        .map(ResponseEntity::ok))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    private Mono<User> resolveUser(HttpSession session) {
        return Mono.defer(() -> {
            var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
            if (authenticated.isEmpty()) {
                return Mono.empty();
            }
            return Mono.fromCallable(() -> userService.ensureUserExists(authenticated.get()))
                    .subscribeOn(Schedulers.boundedElastic());
        });
    }

    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new ValidationException("cursor must be a numeric value");
        }
    }
}

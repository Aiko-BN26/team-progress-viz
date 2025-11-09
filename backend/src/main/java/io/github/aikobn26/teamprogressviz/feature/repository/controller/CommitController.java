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
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitDetailResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitFileResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.dto.response.CommitListItemResponse;
import io.github.aikobn26.teamprogressviz.feature.repository.service.CommitService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class CommitController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final CommitService commitService;

    @GetMapping("/repositories/{repositoryId}/commits")
    public Mono<ResponseEntity<List<CommitListItemResponse>>> list(@PathVariable Long repositoryId,
                                                                   @RequestParam(required = false) Integer limit,
                                                                   @RequestParam(required = false) Integer page,
                                                                   HttpSession session) {
        return resolveUser(session)
                .flatMap(user -> commitService.listCommitsReactive(user, repositoryId, limit, page)
                        .map(ResponseEntity::ok))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping("/repositories/{repositoryId}/commits/{sha}")
    public Mono<ResponseEntity<CommitDetailResponse>> detail(@PathVariable Long repositoryId,
                                                             @PathVariable String sha,
                                                             HttpSession session) {
        return resolveUser(session)
                .flatMap(user -> commitService.getCommitReactive(user, repositoryId, sha)
                        .map(ResponseEntity::ok))
                .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    @GetMapping("/repositories/{repositoryId}/commits/{sha}/files")
    public Mono<ResponseEntity<List<CommitFileResponse>>> files(@PathVariable Long repositoryId,
                                                                @PathVariable String sha,
                                                                HttpSession session) {
        return resolveUser(session)
                .flatMap(user -> commitService.listFilesReactive(user, repositoryId, sha)
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
}

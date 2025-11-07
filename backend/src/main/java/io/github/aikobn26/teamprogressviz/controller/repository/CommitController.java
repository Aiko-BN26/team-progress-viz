package io.github.aikobn26.teamprogressviz.controller.repository;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.dto.response.CommitDetailResponse;
import io.github.aikobn26.teamprogressviz.dto.response.CommitFileResponse;
import io.github.aikobn26.teamprogressviz.dto.response.CommitListItemResponse;
import io.github.aikobn26.teamprogressviz.service.repository.CommitService;
import io.github.aikobn26.teamprogressviz.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Validated
public class CommitController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final CommitService commitService;

    @GetMapping("/repositories/{repositoryId}/commits")
    public ResponseEntity<List<CommitListItemResponse>> list(@PathVariable Long repositoryId,
                                                             @RequestParam(required = false) Integer limit,
                                                             @RequestParam(required = false) Integer page,
                                                             HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var response = commitService.listCommits(user, repositoryId, limit, page);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories/{repositoryId}/commits/{sha}")
    public ResponseEntity<CommitDetailResponse> detail(@PathVariable Long repositoryId,
                                                       @PathVariable String sha,
                                                       HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var response = commitService.getCommit(user, repositoryId, sha);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/repositories/{repositoryId}/commits/{sha}/files")
    public ResponseEntity<List<CommitFileResponse>> files(@PathVariable Long repositoryId,
                                                          @PathVariable String sha,
                                                          HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var response = commitService.listFiles(user, repositoryId, sha);
        return ResponseEntity.ok(response);
    }
}

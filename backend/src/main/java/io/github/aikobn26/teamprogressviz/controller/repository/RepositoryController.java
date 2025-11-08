package io.github.aikobn26.teamprogressviz.controller.repository;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.dto.response.JobSubmissionResponse;
import io.github.aikobn26.teamprogressviz.job.JobService;
import io.github.aikobn26.teamprogressviz.service.repository.PullRequestService;
import io.github.aikobn26.teamprogressviz.service.repository.RepositoryActivitySyncService;
import io.github.aikobn26.teamprogressviz.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class RepositoryController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final PullRequestService pullRequestService;
    private final RepositoryActivitySyncService repositoryActivitySyncService;
    private final JobService jobService;

    @PostMapping("/{repositoryId}/pulls/sync")
    public ResponseEntity<JobSubmissionResponse> syncPullRequests(@PathVariable Long repositoryId,
                                                                  HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var accessToken = gitHubOAuthService.getAccessToken(session);
        if (accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var repository = pullRequestService.requireAccessibleRepository(user, repositoryId);

        var job = jobService.submit("job-sync-prs", () ->
                repositoryActivitySyncService.synchronizeRepository(repository, accessToken.get())
        );

        var response = new JobSubmissionResponse(job.id(), job.status().name().toLowerCase(Locale.ROOT));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}

package io.github.aikobn26.teamprogressviz.controller.user;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.util.Locale;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.dto.response.JobSubmissionResponse;
import io.github.aikobn26.teamprogressviz.dto.response.UserResponse;
import io.github.aikobn26.teamprogressviz.job.JobService;
import io.github.aikobn26.teamprogressviz.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final JobService jobService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var response = new UserResponse(
                user.getId(),
                user.getGithubId(),
                user.getLogin(),
                user.getName(),
                user.getAvatarUrl()
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/me")
    public ResponseEntity<JobSubmissionResponse> deleteMe(HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var job = jobService.submit("job-delete-user", () -> userService.deleteUser(user));
        session.invalidate();
        var response = new JobSubmissionResponse(job.id(), job.status().name().toLowerCase(Locale.ROOT));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}

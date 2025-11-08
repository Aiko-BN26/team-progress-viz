package io.github.aikobn26.teamprogressviz.feature.user.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.job.dto.response.JobSubmissionResponse;
import io.github.aikobn26.teamprogressviz.feature.job.service.JobService;
import io.github.aikobn26.teamprogressviz.feature.user.dto.response.UserOnboardingJobResponse;
import io.github.aikobn26.teamprogressviz.feature.user.dto.response.UserResponse;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserOnboardingService;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final JobService jobService;
    private final UserOnboardingService userOnboardingService;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var accessToken = gitHubOAuthService.getAccessToken(session);
        List<UserOnboardingService.OnboardingJobResult> onboardingJobs = new ArrayList<>();
        var user = userService.ensureUserExists(authenticated.get(), created ->
                accessToken.ifPresent(token -> onboardingJobs.addAll(userOnboardingService.onboardUser(created, token))));
        if (!onboardingJobs.isEmpty()) {
            session.setAttribute(UserOnboardingService.SESSION_ATTRIBUTE_ONBOARDING_JOBS, new ArrayList<>(onboardingJobs));
        } else {
            session.removeAttribute(UserOnboardingService.SESSION_ATTRIBUTE_ONBOARDING_JOBS);
        }
        var response = new UserResponse(
                user.getId(),
                user.getGithubId(),
                user.getLogin(),
                user.getName(),
                user.getAvatarUrl());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/onboarding-jobs")
    public ResponseEntity<List<UserOnboardingJobResponse>> onboardingJobs(HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        userService.ensureUserExists(authenticated.get());

        Object attribute = session.getAttribute(UserOnboardingService.SESSION_ATTRIBUTE_ONBOARDING_JOBS);
        if (!(attribute instanceof List<?> rawList)) {
            return ResponseEntity.ok(List.of());
        }

        List<UserOnboardingJobResponse> responses = new ArrayList<>();
        for (Object entry : rawList) {
            if (entry instanceof UserOnboardingService.OnboardingJobResult result && result.jobId() != null) {
                responses.add(new UserOnboardingJobResponse(
                        result.organizationId(),
                        result.organizationLogin(),
                        result.jobId()));
            }
        }
        return ResponseEntity.ok(responses);
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

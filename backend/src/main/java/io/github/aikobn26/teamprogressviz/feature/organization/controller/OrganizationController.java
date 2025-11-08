package io.github.aikobn26.teamprogressviz.feature.organization.controller;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.job.dto.response.JobSubmissionResponse;
import io.github.aikobn26.teamprogressviz.feature.job.service.JobService;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.request.OrganizationRegistrationRequest;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.OrganizationDetailResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.OrganizationEnsureSyncResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.OrganizationRegistrationResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.OrganizationSummaryResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.RepositorySyncStatusResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserOnboardingService;
import io.github.aikobn26.teamprogressviz.feature.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations")
@RequiredArgsConstructor
@Validated
public class OrganizationController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final OrganizationService organizationService;
    private final JobService jobService;
    private final UserOnboardingService userOnboardingService;

    @GetMapping
    public ResponseEntity<List<OrganizationSummaryResponse>> list(HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var organizations = organizationService.listOrganizations(user).stream()
                .map(org -> new OrganizationSummaryResponse(
                        org.getId(),
                        org.getGithubId(),
                        org.getLogin(),
                        org.getName(),
                        org.getAvatarUrl(),
                        org.getDescription()))
                .toList();
        return ResponseEntity.ok(organizations);
    }

    @PostMapping
    public ResponseEntity<OrganizationRegistrationResponse> register(@Valid @RequestBody OrganizationRegistrationRequest request,
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
        var result = organizationService.registerOrganization(user, request.login(), request.defaultLinkUrl(), accessToken.get());
        var job = jobService.submit("job-sync-org", () ->
                organizationService.synchronizeOrganization(result.organization().getId(), accessToken.get())
        );

        var response = new OrganizationRegistrationResponse(
                result.organization().getId(),
                result.organization().getGithubId(),
                result.organization().getLogin(),
                result.organization().getName(),
                result.organization().getHtmlUrl(),
                job.status().name().toLowerCase(Locale.ROOT),
                job.id(),
                0
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/ensure-sync")
    public ResponseEntity<OrganizationEnsureSyncResponse> ensureAndSynchronize(HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var accessToken = gitHubOAuthService.getAccessToken(session);
        if (accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var user = userService.ensureUserExists(authenticated.get());
        var onboardingJobs = userOnboardingService.onboardUser(user, accessToken.get());

        if (!onboardingJobs.isEmpty()) {
            session.setAttribute(UserOnboardingService.SESSION_ATTRIBUTE_ONBOARDING_JOBS, onboardingJobs);
        } else {
            session.removeAttribute(UserOnboardingService.SESSION_ATTRIBUTE_ONBOARDING_JOBS);
        }

        var response = new OrganizationEnsureSyncResponse(onboardingJobs.stream()
                .map(job -> new OrganizationEnsureSyncResponse.SyncJob(
                        job.organizationId(),
                        job.organizationLogin(),
                        job.jobId()))
                .toList());

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/{organizationId}/sync")
    public ResponseEntity<JobSubmissionResponse> synchronize(@PathVariable Long organizationId, HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var accessToken = gitHubOAuthService.getAccessToken(session);
        if (accessToken.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var user = userService.ensureUserExists(authenticated.get());
        organizationService.getAccessibleOrganization(user, organizationId);

        var job = jobService.submit("job-sync-org", () ->
                organizationService.synchronizeOrganization(organizationId, accessToken.get())
        );

        var response = new JobSubmissionResponse(job.id(), job.status().name().toLowerCase(Locale.ROOT));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/{organizationId}/sync/status")
    public ResponseEntity<List<RepositorySyncStatusResponse>> syncStatus(@PathVariable Long organizationId, HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        var user = userService.ensureUserExists(authenticated.get());
        var statuses = organizationService.listRepositorySyncStatus(user, organizationId).stream()
                .map(status -> new RepositorySyncStatusResponse(
                        status.repositoryId(),
                        status.repositoryFullName(),
                        status.lastSyncedAt(),
                        status.lastSyncedCommitSha(),
                        status.errorMessage()
                ))
                .toList();
        return ResponseEntity.ok(statuses);
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<OrganizationDetailResponse> detail(@PathVariable Long organizationId, HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var detail = organizationService.getOrganizationDetail(user, organizationId);
        return ResponseEntity.ok(OrganizationDetailResponse.from(detail));
    }

    @DeleteMapping("/{organizationId}")
    public ResponseEntity<JobSubmissionResponse> delete(@PathVariable Long organizationId, HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        organizationService.validateDeletePermission(user, organizationId);

        var job = jobService.submit("job-delete-org", () ->
                organizationService.deleteOrganization(user, organizationId)
        );

        var response = new JobSubmissionResponse(job.id(), job.status().name().toLowerCase(Locale.ROOT));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}

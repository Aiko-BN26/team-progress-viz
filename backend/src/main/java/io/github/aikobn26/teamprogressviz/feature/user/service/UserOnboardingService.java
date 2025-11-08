package io.github.aikobn26.teamprogressviz.feature.user.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubOrganization;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubOrganizationService;
import io.github.aikobn26.teamprogressviz.feature.job.model.JobDescriptor;
import io.github.aikobn26.teamprogressviz.feature.job.service.JobService;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.Organization;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.shared.exception.ResourceConflictException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserOnboardingService {

    public static final String SESSION_ATTRIBUTE_ONBOARDING_JOBS = "USER_ONBOARDING_JOBS";

    private static final Logger log = LoggerFactory.getLogger(UserOnboardingService.class);

    private final GitHubOrganizationService gitHubOrganizationService;
    private final OrganizationService organizationService;
    private final JobService jobService;

    public List<OnboardingJobResult> onboardUser(User user, String accessToken) {
        if (user == null || user.getId() == null) {
            return List.of();
        }
        if (!StringUtils.hasText(accessToken)) {
            return List.of();
        }

        List<GitHubOrganization> organizations = gitHubOrganizationService.listOrganizations(accessToken);
        if (organizations.isEmpty()) {
            return List.of();
        }

        Map<String, Organization> knownOrganizations = new HashMap<>();
        organizationService.listOrganizations(user).forEach(existing -> {
            if (existing != null && StringUtils.hasText(existing.getLogin())) {
                knownOrganizations.put(existing.getLogin().toLowerCase(Locale.ROOT), existing);
            }
        });

        List<PendingSyncTarget> pendingSyncTargets = new ArrayList<>();

        for (GitHubOrganization organization : organizations) {
            if (organization == null || !StringUtils.hasText(organization.login())) {
                continue;
            }

            String normalizedLogin = organization.login().trim();
            try {
                var result = organizationService.registerOrganization(
                        user,
                        normalizedLogin,
                        organization.htmlUrl(),
                        accessToken);
                Organization registered = result.organization();
                if (registered != null && registered.getId() != null) {
                    if (StringUtils.hasText(registered.getLogin())) {
                        knownOrganizations.put(registered.getLogin().toLowerCase(Locale.ROOT), registered);
                    }
                    pendingSyncTargets.add(new PendingSyncTarget(registered.getId(),
                            StringUtils.hasText(registered.getLogin()) ? registered.getLogin() : normalizedLogin));
                } else {
                    log.warn("Registered organization {} for user {} but id was null", normalizedLogin,
                            user.getGithubId());
                }
            } catch (ResourceConflictException conflict) {
                Organization existing = knownOrganizations.get(normalizedLogin.toLowerCase(Locale.ROOT));
                if (existing != null) {
                    pendingSyncTargets.add(new PendingSyncTarget(existing.getId(), existing.getLogin()));
                } else {
                    log.debug("Organization {} already registered for user {}", normalizedLogin, user.getGithubId());
                }
            } catch (Exception error) {
                log.warn("Failed to register organization {} for user {}", normalizedLogin, user.getGithubId(), error);
            }
        }

        List<OnboardingJobResult> jobs = new ArrayList<>();
        for (PendingSyncTarget target : pendingSyncTargets) {
            if (target.organizationId() == null) {
                continue;
            }
            JobDescriptor job = jobService.submit("job-sync-org",
                    () -> organizationService.synchronizeOrganization(target.organizationId(), accessToken));
            jobs.add(new OnboardingJobResult(target.organizationId(), target.organizationLogin(), job.id()));
        }

        return jobs;
    }

    public record OnboardingJobResult(Long organizationId, String organizationLogin, String jobId) {
    }

    private record PendingSyncTarget(Long organizationId, String organizationLogin) {
    }
}

package io.github.aikobn26.teamprogressviz.feature.organization.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import io.github.aikobn26.teamprogressviz.feature.organization.dto.request.StatusUpdateRequest;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.StatusListItemResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.StatusUpdateResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.ActivityDaily;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.DailyStatus;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.Organization;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.ActivityDailyRepository;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.DailyStatusRepository;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.UserOrganizationRepository;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import io.github.aikobn26.teamprogressviz.shared.exception.ForbiddenException;
import io.github.aikobn26.teamprogressviz.shared.exception.ResourceNotFoundException;
import io.github.aikobn26.teamprogressviz.shared.exception.ValidationException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class StatusService {

    private static final int MAX_STREAK_LOOKBACK_DAYS = 60;

    private final OrganizationService organizationService;
    private final DailyStatusRepository dailyStatusRepository;
    private final ActivityDailyRepository activityDailyRepository;
    private final UserOrganizationRepository userOrganizationRepository;
    private final PullRequestRepository pullRequestRepository;

    public StatusUpdateResponse upsertStatus(User user,
                                             Long organizationId,
                                             StatusUpdateRequest request) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        LocalDate targetDate = resolveDate(request.date());
        int availableMinutes = resolveAvailableMinutes(request);

        DailyStatus status = dailyStatusRepository
                .findByOrganizationIdAndUserIdAndDateAndDeletedAtIsNull(organization.getId(), user.getId(), targetDate)
                .orElseGet(() -> DailyStatus.builder()
                        .organization(organization)
                        .user(user)
                        .date(targetDate)
                        .build());

        status.setOrganization(organization);
        status.setUser(user);
        status.setDate(targetDate);
        status.setAvailableMinutes(availableMinutes);
        status.setStatusType(request.status());
        status.setStatusMessage(cleanMessage(request.statusMessage()));
        status.setDeletedAt(null);
        status = dailyStatusRepository.save(status);

        ActivityDaily activityDaily = activityDailyRepository
                .findByOrganizationIdAndUserIdAndDateAndDeletedAtIsNull(organization.getId(), user.getId(), targetDate)
                .orElseGet(() -> ActivityDaily.builder()
                        .organization(organization)
                        .user(user)
                        .date(targetDate)
                        .commitCount(0)
                        .filesChanged(0)
                        .additions(0)
                        .deletions(0)
                        .availableMinutes(0)
                        .build());
        activityDaily.setAvailableMinutes(availableMinutes);
        activityDaily.setDeletedAt(null);
        activityDaily = activityDailyRepository.save(activityDaily);

        int streakDays = calculateStreak(organization.getId(), user.getId(), targetDate);
        PullRequest latestPr = resolveLatestPullRequest(organization.getId(), user.getId());

        int activeToday = (int) dailyStatusRepository
                .countByOrganizationIdAndDateAndDeletedAtIsNull(organization.getId(), targetDate);
        int memberCount = userOrganizationRepository
                .findByOrganizationIdAndDeletedAtIsNull(organization.getId())
                .size();
        int pending = Math.max(memberCount - activeToday, 0);

        StatusUpdateResponse.PersonalStatus personal = new StatusUpdateResponse.PersonalStatus(
                true,
                status.getStatusType(),
                status.getStatusMessage(),
                status.getUpdatedAt(),
                nullSafe(activityDaily.getCommitCount()),
                toCapacityHours(availableMinutes),
                streakDays,
                latestPr != null ? latestPr.getHtmlUrl() : null
        );

        StatusUpdateResponse.MemberStatus member = new StatusUpdateResponse.MemberStatus(
                memberIdentifier(user),
                displayName(user),
                user.getAvatarUrl(),
                status.getStatusType(),
                status.getStatusMessage(),
                status.getUpdatedAt(),
                nullSafe(activityDaily.getCommitCount()),
                toCapacityHours(availableMinutes),
                streakDays,
                latestPr != null ? latestPr.getHtmlUrl() : null
        );

        StatusUpdateResponse.StatusSummary summary = new StatusUpdateResponse.StatusSummary(activeToday, pending);

        return new StatusUpdateResponse(personal, member, summary);
    }

    @Transactional(readOnly = true)
    public List<StatusListItemResponse> listStatuses(User user, Long organizationId, LocalDate date) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        LocalDate targetDate = date != null ? date : LocalDate.now();

        return dailyStatusRepository.findByOrganizationIdAndDateAndDeletedAtIsNull(organization.getId(), targetDate).stream()
                .sorted(Comparator.comparing(DailyStatus::getUpdatedAt).reversed())
                .map(status -> {
                    User owner = status.getUser();
                    return new StatusListItemResponse(
                            status.getId(),
                            owner != null ? owner.getId() : null,
                            owner != null ? owner.getLogin() : null,
                            owner != null ? owner.getName() : null,
                            owner != null ? owner.getAvatarUrl() : null,
                            status.getDate(),
                            status.getAvailableMinutes(),
                            status.getStatusType(),
                            status.getStatusMessage(),
                            status.getUpdatedAt()
                    );
                })
                .toList();
    }

    public void deleteStatus(User user, Long organizationId, Long statusId) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        DailyStatus status = dailyStatusRepository
                .findByIdAndOrganizationIdAndDeletedAtIsNull(statusId, organization.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Status not found"));

        boolean sameUser = status.getUser() != null && Objects.equals(status.getUser().getId(), user.getId());
        if (!sameUser && !hasAdminPrivilege(user, organization)) {
            throw new ForbiddenException("You do not have permission to delete this status");
        }

        OffsetDateTime now = OffsetDateTime.now();
        status.setDeletedAt(now);
        dailyStatusRepository.save(status);

        Long targetUserId = status.getUser() != null ? status.getUser().getId() : null;
        if (targetUserId == null) {
            return;
        }

        activityDailyRepository
                .findByOrganizationIdAndUserIdAndDateAndDeletedAtIsNull(organization.getId(), targetUserId, status.getDate())
                .ifPresent(activity -> {
                    activity.setAvailableMinutes(0);
                    activityDailyRepository.save(activity);
                });
    }

    private int calculateStreak(Long organizationId, Long userId, LocalDate date) {
        int streak = 0;
        LocalDate cursor = date;
        for (int i = 0; i < MAX_STREAK_LOOKBACK_DAYS; i++) {
            LocalDate target = cursor.minusDays(i);
            boolean exists = dailyStatusRepository
                    .findByOrganizationIdAndUserIdAndDateAndDeletedAtIsNull(organizationId, userId, target)
                    .isPresent();
            if (!exists) {
                break;
            }
            streak++;
        }
        return streak;
    }

    private PullRequest resolveLatestPullRequest(Long organizationId, Long userId) {
        return pullRequestRepository
                .findFirstByRepositoryOrganizationIdAndAuthorIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, userId)
                .orElse(null);
    }

    private int resolveAvailableMinutes(StatusUpdateRequest request) {
        Integer minutes = request.availableMinutes();
        if (minutes != null) {
            if (minutes < 0) {
                throw new ValidationException("availableMinutes must be 0 or greater");
            }
            return minutes;
        }
        Integer capacityHours = request.capacityHours();
        if (capacityHours == null) {
            return 0;
        }
        if (capacityHours < 0) {
            throw new ValidationException("capacityHours must be 0 or greater");
        }
        return Math.toIntExact(Math.min((long) capacityHours * 60, Integer.MAX_VALUE));
    }

    private LocalDate resolveDate(String date) {
        if (!StringUtils.hasText(date)) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Invalid date format, use YYYY-MM-DD");
        }
    }

    private String cleanMessage(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }
        String trimmed = message.trim();
        return trimmed.length() > 2000 ? trimmed.substring(0, 2000) : trimmed;
    }

    private String memberIdentifier(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getLogin())) {
            return user.getLogin();
        }
        return user.getId() != null ? "user-" + user.getId() : null;
    }

    private String displayName(User user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getName())) {
            return user.getName();
        }
        if (StringUtils.hasText(user.getLogin())) {
            return user.getLogin();
        }
        return "Member";
    }

    private int toCapacityHours(Integer availableMinutes) {
        if (availableMinutes == null || availableMinutes <= 0) {
            return 0;
        }
        return (int) Math.round(availableMinutes / 60.0);
    }

    private boolean hasAdminPrivilege(User user, Organization organization) {
        if (user == null || user.getId() == null || organization == null || organization.getId() == null) {
            return false;
        }
        return userOrganizationRepository
                .findByUserIdAndOrganizationIdAndDeletedAtIsNull(user.getId(), organization.getId())
                .map(UserOrganization::getRole)
                .map(role -> {
                    if (!StringUtils.hasText(role)) {
                        return false;
                    }
                    String normalized = role.toLowerCase(Locale.ROOT);
                    return "admin".equals(normalized) || "owner".equals(normalized);
                })
                .orElse(false);
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }
}

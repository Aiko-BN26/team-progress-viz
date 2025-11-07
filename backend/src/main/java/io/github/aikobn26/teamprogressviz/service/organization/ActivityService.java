package io.github.aikobn26.teamprogressviz.service.organization;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.aikobn26.teamprogressviz.dto.response.ActivitySummaryItemResponse;
import io.github.aikobn26.teamprogressviz.dto.response.CommitFeedResponse;
import io.github.aikobn26.teamprogressviz.entity.ActivityDaily;
import io.github.aikobn26.teamprogressviz.entity.GitCommit;
import io.github.aikobn26.teamprogressviz.entity.Organization;
import io.github.aikobn26.teamprogressviz.entity.User;
import io.github.aikobn26.teamprogressviz.exception.ValidationException;
import io.github.aikobn26.teamprogressviz.repository.ActivityDailyRepository;
import io.github.aikobn26.teamprogressviz.repository.GitCommitRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private static final int DEFAULT_FEED_LIMIT = 20;
    private static final int MAX_FEED_LIMIT = 100;

    private final OrganizationService organizationService;
    private final ActivityDailyRepository activityDailyRepository;
    private final GitCommitRepository gitCommitRepository;

    public List<ActivitySummaryItemResponse> summarize(User user,
                                                        Long organizationId,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        String groupBy) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        if (startDate == null || endDate == null) {
            throw new ValidationException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("endDate must not be before startDate");
        }

        List<ActivityDaily> activities = activityDailyRepository
                .findByOrganizationIdAndDateBetweenAndDeletedAtIsNull(organization.getId(), startDate, endDate);

        String normalizedGroup = groupBy == null ? "user" : groupBy.toLowerCase(Locale.ROOT);
        if (!"user".equals(normalizedGroup)) {
            throw new ValidationException("Unsupported groupBy value: " + groupBy);
        }

        Map<Long, List<ActivityDaily>> grouped = activities.stream()
                .filter(activity -> activity.getUser() != null && activity.getUser().getId() != null)
                .collect(Collectors.groupingBy(activity -> activity.getUser().getId()));

        return grouped.values().stream()
                .map(entries -> entries.stream()
                        .sorted(Comparator.comparing(ActivityDaily::getDate).reversed())
                        .reduce(this::mergeActivity)
                        .map(this::toSummary)
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ActivitySummaryItemResponse::userId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    public CommitFeedResponse fetchCommitFeed(User user,
                                              Long organizationId,
                                              Long cursor,
                                              Integer limit) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        int pageSize = normalizeLimit(limit);

        Pageable pageable = PageRequest.of(0, pageSize, Sort.by(Sort.Direction.DESC, "id"));
        Page<GitCommit> page;
        if (cursor != null && cursor > 0) {
            page = gitCommitRepository
                    .findByRepositoryOrganizationIdAndIdLessThanAndDeletedAtIsNull(organization.getId(), cursor, pageable);
        } else {
            page = gitCommitRepository
                    .findByRepositoryOrganizationIdAndDeletedAtIsNull(organization.getId(), pageable);
        }

    List<CommitFeedResponse.Item> items = page.getContent().stream()
                .map(this::toCommitItem)
                .toList();

        String nextCursor = null;
    if (!items.isEmpty() && items.size() == pageSize) {
            CommitFeedResponse.Item last = items.get(items.size() - 1);
            nextCursor = last.id() != null ? String.valueOf(last.id()) : null;
        }

        return new CommitFeedResponse(items, nextCursor);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_FEED_LIMIT;
        }
        if (limit < 1) {
            throw new ValidationException("limit must be greater than 0");
        }
        return Math.min(limit, MAX_FEED_LIMIT);
    }

    private ActivityDaily mergeActivity(ActivityDaily left, ActivityDaily right) {
        ActivityDaily merged = ActivityDaily.builder()
                .organization(left.getOrganization())
                .user(left.getUser())
                .date(left.getDate())
                .commitCount(sum(left.getCommitCount(), right.getCommitCount()))
                .filesChanged(sum(left.getFilesChanged(), right.getFilesChanged()))
                .additions(sum(left.getAdditions(), right.getAdditions()))
                .deletions(sum(left.getDeletions(), right.getDeletions()))
                .availableMinutes(sum(left.getAvailableMinutes(), right.getAvailableMinutes()))
                .build();
        return merged;
    }

    private ActivitySummaryItemResponse toSummary(ActivityDaily activity) {
        User owner = activity.getUser();
        return new ActivitySummaryItemResponse(
                owner != null ? owner.getId() : null,
                owner != null ? owner.getLogin() : null,
                owner != null ? owner.getName() : null,
                owner != null ? owner.getAvatarUrl() : null,
                asLong(activity.getCommitCount()),
                asLong(activity.getFilesChanged()),
                asLong(activity.getAdditions()),
                asLong(activity.getDeletions()),
                asLong(activity.getAvailableMinutes())
        );
    }

    private CommitFeedResponse.Item toCommitItem(GitCommit commit) {
        String repositoryFullName = commit.getRepository() != null ? commit.getRepository().getFullName() : null;
        return new CommitFeedResponse.Item(
                commit.getId(),
                commit.getSha(),
                repositoryFullName,
                commit.getMessage(),
                commit.getAuthorName(),
                commit.getCommitterName(),
                commit.getCommittedAt(),
                commit.getHtmlUrl()
        );
    }

    private int sum(Integer left, Integer right) {
        int a = left == null ? 0 : left;
        int b = right == null ? 0 : right;
        return a + b;
    }

    private long asLong(Integer value) {
        return value == null ? 0L : value.longValue();
    }
}

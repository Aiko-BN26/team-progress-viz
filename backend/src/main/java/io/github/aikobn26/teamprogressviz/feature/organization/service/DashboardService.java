package io.github.aikobn26.teamprogressviz.feature.organization.service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.aikobn26.teamprogressviz.feature.organization.dto.response.DashboardResponse;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.Comment;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.DailyStatus;
import io.github.aikobn26.teamprogressviz.feature.organization.entity.Organization;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.CommentRepository;
import io.github.aikobn26.teamprogressviz.feature.organization.repository.DailyStatusRepository;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.GitCommit;
import io.github.aikobn26.teamprogressviz.feature.repository.repository.GitCommitRepository;
import io.github.aikobn26.teamprogressviz.feature.user.entity.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int STATUS_LIMIT = 100;
    private static final int COMMIT_LIMIT = 10;
    private static final int COMMENT_LIMIT = 10;

    private final OrganizationService organizationService;
    private final DailyStatusRepository dailyStatusRepository;
    private final GitCommitRepository gitCommitRepository;
    private final CommentRepository commentRepository;

    public DashboardResponse fetchDashboard(Long organizationId, User user) {
        Organization organization = organizationService.getAccessibleOrganization(user, organizationId);
        LocalDate today = LocalDate.now();

        List<DashboardResponse.StatusItem> statuses = dailyStatusRepository
                .findByOrganizationIdAndDateAndDeletedAtIsNull(organization.getId(), today).stream()
                .limit(STATUS_LIMIT)
                .map(this::toStatusItem)
                .toList();

        List<DashboardResponse.CommitItem> commits = gitCommitRepository
                .findByRepositoryOrganizationIdAndDeletedAtIsNull(
                        organization.getId(),
                        PageRequest.of(0, COMMIT_LIMIT, Sort.by(Sort.Direction.DESC, "committedAt", "createdAt")))
                .getContent()
                .stream()
                .map(this::toCommitItem)
                .collect(Collectors.toList());

        List<DashboardResponse.CommentItem> comments = commentRepository
                .findByOrganizationIdAndDeletedAtIsNull(
                        organization.getId(),
                        PageRequest.of(0, COMMENT_LIMIT, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(this::toCommentItem)
                .collect(Collectors.toList());

        return new DashboardResponse(statuses, commits, comments);
    }

    private DashboardResponse.StatusItem toStatusItem(DailyStatus status) {
        var owner = status.getUser();
        return new DashboardResponse.StatusItem(
                owner != null ? owner.getId() : null,
                owner != null ? owner.getLogin() : null,
                owner != null ? owner.getName() : null,
                owner != null ? owner.getAvatarUrl() : null,
                status.getAvailableMinutes(),
                status.getStatusType(),
                status.getStatusMessage(),
                status.getUpdatedAt()
        );
    }

    private DashboardResponse.CommitItem toCommitItem(GitCommit commit) {
        String repositoryFullName = commit.getRepository() != null ? commit.getRepository().getFullName() : null;
        return new DashboardResponse.CommitItem(
                commit.getId(),
                commit.getSha(),
                repositoryFullName,
                commit.getMessage(),
                commit.getAuthorName(),
                commit.getCommittedAt(),
                commit.getHtmlUrl()
        );
    }

    private DashboardResponse.CommentItem toCommentItem(Comment comment) {
        var owner = comment.getUser();
        return new DashboardResponse.CommentItem(
                comment.getId(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getLogin() : null,
                owner != null ? owner.getAvatarUrl() : null,
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}

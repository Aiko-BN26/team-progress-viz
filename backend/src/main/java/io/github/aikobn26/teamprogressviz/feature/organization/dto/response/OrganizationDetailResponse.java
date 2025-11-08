package io.github.aikobn26.teamprogressviz.feature.organization.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import io.github.aikobn26.teamprogressviz.feature.organization.entity.Organization;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService.ActivitySummary;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService.CommentDetail;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService.CommitDetail;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService.MemberDetail;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService.OrganizationDetail;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService.PullRequestDetail;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService.PullRequestSummary;
import io.github.aikobn26.teamprogressviz.feature.organization.service.OrganizationService.SimpleUser;
import io.github.aikobn26.teamprogressviz.feature.repository.entity.Repository;

public record OrganizationDetailResponse(
        OrganizationInfo organization,
        List<Member> members,
        List<RepositorySummary> repositories,
        ActivitySummaryResponse activitySummaryLast7Days,
        PullRequestSummaryResponse pullRequestSummary,
        List<PullRequestResponse> recentPullRequests,
        List<CommitResponse> recentCommits,
        List<CommentResponse> recentComments
) {

    public static OrganizationDetailResponse from(OrganizationDetail detail) {
        Organization organization = detail.organization();
        ActivitySummary summary = detail.activitySummaryLast7Days();
        PullRequestSummary pullRequestSummary = detail.pullRequestSummary();

        OrganizationInfo organizationInfo = new OrganizationInfo(
                organization.getId(),
                organization.getGithubId(),
                organization.getLogin(),
                organization.getName(),
                organization.getDescription(),
                organization.getAvatarUrl(),
                organization.getHtmlUrl(),
                organization.getDefaultLinkUrl()
        );

        List<Member> memberResponses = detail.members().stream()
                .map(OrganizationDetailResponse::mapMember)
                .toList();

        List<RepositorySummary> repositoryResponses = detail.repositories().stream()
                .map(OrganizationDetailResponse::mapRepository)
                .toList();

        ActivitySummaryResponse activityResponse = summary == null
                ? new ActivitySummaryResponse(0L, 0L, 0L, 0)
                : new ActivitySummaryResponse(
                        summary.commitCount(),
                        summary.additions(),
                        summary.deletions(),
                        summary.activeMembers()
                );

        PullRequestSummaryResponse pullRequestSummaryResponse = mapPullRequestSummary(pullRequestSummary);

        List<PullRequestDetail> pullRequests = detail.recentPullRequests() == null
                ? List.of()
                : detail.recentPullRequests();
        List<PullRequestResponse> pullRequestResponses = pullRequests.stream()
                .map(OrganizationDetailResponse::mapPullRequest)
                .filter(Objects::nonNull)
                .toList();

        List<CommitDetail> commits = detail.recentCommits() == null
                ? List.of()
                : detail.recentCommits();
        List<CommitResponse> commitResponses = commits.stream()
                .map(OrganizationDetailResponse::mapCommit)
                .filter(Objects::nonNull)
                .toList();

        List<CommentDetail> comments = detail.recentComments() == null
                ? List.of()
                : detail.recentComments();
        List<CommentResponse> commentResponses = comments.stream()
                .map(OrganizationDetailResponse::mapComment)
                .filter(Objects::nonNull)
                .toList();

        return new OrganizationDetailResponse(
                organizationInfo,
                memberResponses,
                repositoryResponses,
                activityResponse,
                pullRequestSummaryResponse,
                pullRequestResponses,
                commitResponses,
                commentResponses);
    }

    private static Member mapMember(MemberDetail member) {
        return new Member(
                member.userId(),
                member.githubId(),
                member.login(),
                member.name(),
                member.avatarUrl(),
                member.role()
        );
    }

    private static RepositorySummary mapRepository(Repository repository) {
        return new RepositorySummary(
                repository.getId(),
                repository.getGithubId(),
                repository.getFullName(),
                repository.getHtmlUrl(),
                repository.getLanguage(),
                repository.getStargazersCount(),
                repository.getForksCount(),
                repository.getUpdatedAt()
        );
    }

        private static PullRequestSummaryResponse mapPullRequestSummary(PullRequestSummary summary) {
                if (summary == null) {
                        return new PullRequestSummaryResponse(0L, 0L, 0L);
                }
                return new PullRequestSummaryResponse(
                                summary.openCount(),
                                summary.closedCount(),
                                summary.mergedCount()
                );
        }

        private static PullRequestResponse mapPullRequest(PullRequestDetail pullRequest) {
                if (pullRequest == null) {
                        return null;
                }
                return new PullRequestResponse(
                                pullRequest.id(),
                                pullRequest.number(),
                                pullRequest.repositoryId(),
                                pullRequest.repositoryFullName(),
                                pullRequest.title(),
                                pullRequest.state(),
                                pullRequest.merged(),
                                pullRequest.htmlUrl(),
                                mapUser(pullRequest.author()),
                                mapUser(pullRequest.mergedBy()),
                                pullRequest.additions(),
                                pullRequest.deletions(),
                                pullRequest.changedFiles(),
                                pullRequest.createdAt(),
                                pullRequest.updatedAt(),
                                pullRequest.mergedAt(),
                                pullRequest.closedAt()
                );
        }

        private static CommitResponse mapCommit(CommitDetail commit) {
                if (commit == null) {
                        return null;
                }
                return new CommitResponse(
                                commit.id(),
                                commit.sha(),
                                commit.message(),
                                commit.repositoryId(),
                                commit.repositoryFullName(),
                                commit.htmlUrl(),
                                commit.authorName(),
                                commit.committerName(),
                                commit.committedAt(),
                                commit.pushedAt()
                );
        }

        private static CommentResponse mapComment(CommentDetail comment) {
                if (comment == null) {
                        return null;
                }
                return new CommentResponse(
                                comment.id(),
                                mapUser(comment.user()),
                                comment.targetType(),
                                comment.targetId(),
                                comment.parentCommentId(),
                                comment.content(),
                                comment.createdAt(),
                                comment.updatedAt()
                );
        }

        private static UserSummary mapUser(SimpleUser user) {
                if (user == null) {
                        return null;
                }
                return new UserSummary(
                                user.userId(),
                                user.githubId(),
                                user.login(),
                                user.name(),
                                user.avatarUrl()
                );
        }

    public record OrganizationInfo(
            Long id,
            Long githubId,
            String login,
            String name,
            String description,
            String avatarUrl,
            String htmlUrl,
            String defaultLinkUrl
    ) {}

    public record Member(
            Long userId,
            Long githubId,
            String login,
            String name,
            String avatarUrl,
            String role
    ) {}

    public record RepositorySummary(
            Long id,
            Long githubId,
            String fullName,
            String htmlUrl,
            String language,
            Integer stargazersCount,
            Integer forksCount,
            OffsetDateTime updatedAt
    ) {}

    public record ActivitySummaryResponse(
            long commitCount,
            long additions,
            long deletions,
            int activeMembers
    ) {}

    public record PullRequestSummaryResponse(
            long openCount,
            long closedCount,
            long mergedCount
    ) {}

    public record PullRequestResponse(
            Long id,
            Integer number,
            Long repositoryId,
            String repositoryFullName,
            String title,
            String state,
            boolean merged,
            String htmlUrl,
            UserSummary author,
            UserSummary mergedBy,
            Integer additions,
            Integer deletions,
            Integer changedFiles,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt,
            OffsetDateTime mergedAt,
            OffsetDateTime closedAt
    ) {}

    public record CommitResponse(
            Long id,
            String sha,
            String message,
            Long repositoryId,
            String repositoryFullName,
            String htmlUrl,
            String authorName,
            String committerName,
            OffsetDateTime committedAt,
            OffsetDateTime pushedAt
    ) {}

    public record CommentResponse(
            Long id,
            UserSummary user,
            String targetType,
            Long targetId,
            Long parentCommentId,
            String content,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record UserSummary(
            Long userId,
            Long githubId,
            String login,
            String name,
            String avatarUrl
    ) {}
}

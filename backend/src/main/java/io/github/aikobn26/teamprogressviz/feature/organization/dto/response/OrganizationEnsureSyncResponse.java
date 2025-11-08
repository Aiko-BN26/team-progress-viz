package io.github.aikobn26.teamprogressviz.feature.organization.dto.response;

import java.util.List;

public record OrganizationEnsureSyncResponse(List<SyncJob> jobs) {

    public record SyncJob(Long organizationId, String organizationLogin, String jobId) {
    }
}

package io.github.aikobn26.teamprogressviz.feature.organization.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "organization.sync")
public class OrganizationSyncProperties {

    private boolean fetchCommitDetails = true;

    private boolean fetchPullRequestDetails = true;

    public boolean isFetchCommitDetails() {
        return fetchCommitDetails;
    }

    public void setFetchCommitDetails(boolean fetchCommitDetails) {
        this.fetchCommitDetails = fetchCommitDetails;
    }

    public boolean isFetchPullRequestDetails() {
        return fetchPullRequestDetails;
    }

    public void setFetchPullRequestDetails(boolean fetchPullRequestDetails) {
        this.fetchPullRequestDetails = fetchPullRequestDetails;
    }
}

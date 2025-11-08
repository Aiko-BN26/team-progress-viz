package io.github.aikobn26.teamprogressviz.dto.response;

public record PullRequestFileResponse(
        Long id,
        String path,
        String extension,
        Integer additions,
        Integer deletions,
        Integer changes,
        String rawBlobUrl
) {
}

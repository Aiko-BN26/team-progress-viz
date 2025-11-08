package io.github.aikobn26.teamprogressviz.dto.response;

public record CommitFileResponse(
        Long id,
        String path,
        String filename,
        String extension,
        String status,
        Integer additions,
        Integer deletions,
        Integer changes,
        String rawBlobUrl
) {}

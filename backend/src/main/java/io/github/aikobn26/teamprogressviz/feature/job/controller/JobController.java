package io.github.aikobn26.teamprogressviz.feature.job.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.job.dto.response.JobStatusResponse;
import io.github.aikobn26.teamprogressviz.feature.job.service.JobService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final GitHubOAuthService gitHubOAuthService;
    private final JobService jobService;

    @GetMapping("/{jobId}")
    public ResponseEntity<JobStatusResponse> status(@PathVariable String jobId, HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return jobService.findJob(jobId)
                .map(descriptor -> new JobStatusResponse(
                        descriptor.id(),
                        descriptor.type(),
                        descriptor.status(),
                        descriptor.createdAt(),
                        descriptor.startedAt(),
            descriptor.finishedAt(),
            descriptor.progress(),
                        descriptor.errorMessage()
                ))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }
}

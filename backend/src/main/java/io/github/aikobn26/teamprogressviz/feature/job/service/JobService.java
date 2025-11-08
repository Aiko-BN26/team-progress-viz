package io.github.aikobn26.teamprogressviz.feature.job.service;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.github.aikobn26.teamprogressviz.feature.job.model.AsyncJobExecutor;
import io.github.aikobn26.teamprogressviz.feature.job.model.JobDescriptor;
import io.github.aikobn26.teamprogressviz.feature.job.model.JobStatus;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JobService {

    private static final Logger log = LoggerFactory.getLogger(JobService.class);

    private final AsyncJobExecutor asyncJobExecutor;
    private final ConcurrentHashMap<String, JobState> jobs = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public JobDescriptor submit(String prefix, Runnable task) {
        String type = prefix;
        String id = buildJobId(prefix);
        JobState initial = JobState.queued(id, type);
        jobs.put(id, initial);

        asyncJobExecutor.execute(() -> runJob(id, task));

        return toDescriptor(initial);
    }

    public Optional<JobDescriptor> findJob(String jobId) {
        return Optional.ofNullable(jobs.get(jobId)).map(this::toDescriptor);
    }

    private void runJob(String jobId, Runnable task) {
        update(jobId, JobState::running);
        try {
            task.run();
            update(jobId, JobState::succeeded);
        } catch (Exception e) {
            log.error("Job {} failed", jobId, e);
            update(jobId, state -> state.failed(e.getMessage()));
        }
    }

    private void update(String jobId, Function<JobState, JobState> updater) {
        jobs.computeIfPresent(jobId, (id, state) -> updater.apply(state));
    }

    private JobDescriptor toDescriptor(JobState state) {
        return new JobDescriptor(
                state.id,
                state.type,
                state.status,
                state.createdAt,
                state.startedAt,
                state.finishedAt,
                state.errorMessage
        );
    }

    private String buildJobId(String prefix) {
        long seq = sequence.incrementAndGet();
        String safePrefix = prefix == null || prefix.isBlank() ? "job" : prefix;
        return safePrefix + "-" + seq + "-" + UUID.randomUUID();
    }

    private record JobState(
            String id,
            String type,
            JobStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            String errorMessage
    ) {
        private static JobState queued(String id, String type) {
            OffsetDateTime now = OffsetDateTime.now();
            return new JobState(id, type, JobStatus.QUEUED, now, null, null, null);
        }

        private JobState running() {
            return new JobState(id, type, JobStatus.RUNNING, createdAt, OffsetDateTime.now(), null, null);
        }

        private JobState succeeded() {
            OffsetDateTime finished = OffsetDateTime.now();
            OffsetDateTime startedTime = startedAt == null ? finished : startedAt;
            return new JobState(id, type, JobStatus.SUCCEEDED, createdAt, startedTime, finished, null);
        }

        private JobState failed(String message) {
            OffsetDateTime finished = OffsetDateTime.now();
            OffsetDateTime startedTime = startedAt == null ? finished : startedAt;
            return new JobState(id, type, JobStatus.FAILED, createdAt, startedTime, finished, message);
        }
    }
}

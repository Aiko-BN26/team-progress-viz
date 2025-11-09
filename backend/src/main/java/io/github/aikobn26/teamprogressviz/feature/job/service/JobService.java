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
        return submit(prefix, context -> task.run());
    }

    public JobDescriptor submit(String prefix, JobTask task) {
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

    public void updateProgress(String jobId, int progress) {
        update(jobId, state -> state.withProgress(progress));
    }

    public interface JobTask {
        void run(JobContext context);
    }

    public interface JobContext {
        String jobId();

        void updateProgress(int progress);
    }

    private void runJob(String jobId, JobTask task) {
        update(jobId, JobState::running);
        try {
            JobContext context = new DefaultJobContext(jobId);
            task.run(context);
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
                state.progress,
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
            String errorMessage,
            int progress
    ) {
        private static JobState queued(String id, String type) {
            OffsetDateTime now = OffsetDateTime.now();
            return new JobState(id, type, JobStatus.QUEUED, now, null, null, null, 0);
        }

        private JobState running() {
            return new JobState(id, type, JobStatus.RUNNING, createdAt, OffsetDateTime.now(), null, null, progress);
        }

        private JobState succeeded() {
            OffsetDateTime finished = OffsetDateTime.now();
            OffsetDateTime startedTime = startedAt == null ? finished : startedAt;
            return new JobState(id, type, JobStatus.SUCCEEDED, createdAt, startedTime, finished, null, 100);
        }

        private JobState failed(String message) {
            OffsetDateTime finished = OffsetDateTime.now();
            OffsetDateTime startedTime = startedAt == null ? finished : startedAt;
            return new JobState(id, type, JobStatus.FAILED, createdAt, startedTime, finished, message, progress);
        }

        private JobState withProgress(int newProgress) {
            int safeProgress = Math.max(0, Math.min(100, newProgress));
            return new JobState(id, type, status, createdAt, startedAt, finishedAt, errorMessage, safeProgress);
        }
    }

    private class DefaultJobContext implements JobContext {
        private final String jobId;

        private DefaultJobContext(String jobId) {
            this.jobId = jobId;
        }

        @Override
        public String jobId() {
            return jobId;
        }

        @Override
        public void updateProgress(int progress) {
            JobService.this.updateProgress(jobId, progress);
        }
    }
}

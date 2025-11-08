package io.github.aikobn26.teamprogressviz.feature.job.exception;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(String jobId) {
        super("Job not found: " + jobId);
    }
}

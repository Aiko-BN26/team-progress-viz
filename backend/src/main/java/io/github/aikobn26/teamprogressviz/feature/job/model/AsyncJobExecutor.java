package io.github.aikobn26.teamprogressviz.feature.job.model;

import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AsyncJobExecutor {

    private final TaskExecutor jobExecutor;

    public void execute(Runnable task) {
        jobExecutor.execute(task);
    }
}

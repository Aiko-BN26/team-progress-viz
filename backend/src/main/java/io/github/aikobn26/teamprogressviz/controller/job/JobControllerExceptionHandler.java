package io.github.aikobn26.teamprogressviz.controller.job;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import io.github.aikobn26.teamprogressviz.exception.JobNotFoundException;

@RestControllerAdvice(assignableTypes = JobController.class)
public class JobControllerExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleJobNotFound(JobNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "type", "/errors/not-found",
                "title", "Resource Not Found",
                "status", HttpStatus.NOT_FOUND.value(),
                "detail", e.getMessage()
        ));
    }
}

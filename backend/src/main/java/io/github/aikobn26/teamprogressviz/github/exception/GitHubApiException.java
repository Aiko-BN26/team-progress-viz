package io.github.aikobn26.teamprogressviz.github.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

public class GitHubApiException extends RuntimeException {

    private final HttpStatusCode statusCode;

    public GitHubApiException(String message) {
        super(message);
        this.statusCode = HttpStatus.BAD_GATEWAY;
    }

    public GitHubApiException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = HttpStatus.BAD_GATEWAY;
    }

    public GitHubApiException(String message, HttpStatusCode statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode != null ? statusCode : HttpStatus.BAD_GATEWAY;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }
}

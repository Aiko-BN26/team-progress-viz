package io.github.aikobn26.teamprogressviz.auth.exception;

public class GitHubOAuthException extends RuntimeException {

    public GitHubOAuthException(String message) {
        super(message);
    }

    public GitHubOAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}

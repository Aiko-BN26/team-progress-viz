package io.github.aikobn26.teamprogressviz.controller.organization;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.dto.request.CommentCreateRequest;
import io.github.aikobn26.teamprogressviz.dto.response.CommentCreateResponse;
import io.github.aikobn26.teamprogressviz.dto.response.CommentListItemResponse;
import io.github.aikobn26.teamprogressviz.service.organization.CommentService;
import io.github.aikobn26.teamprogressviz.service.user.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/organizations/{organizationId}/comments")
@RequiredArgsConstructor
@Validated
public class CommentController {

    private final GitHubOAuthService gitHubOAuthService;
    private final UserService userService;
    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentCreateResponse> create(@PathVariable Long organizationId,
                                                        @Valid @RequestBody CommentCreateRequest request,
                                                        HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        CommentCreateResponse response = commentService.createComment(user, organizationId, request);
        URI location = URI.create(String.format("/api/organizations/%d/comments/%d", organizationId, response.commentId()));
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CommentListItemResponse>> list(@PathVariable Long organizationId,
                                                              @RequestParam(required = false) String targetType,
                                                              @RequestParam(required = false) Long targetId,
                                                              HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        var response = commentService.listComments(user, organizationId, targetType, targetId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long organizationId,
                                       @PathVariable Long commentId,
                                       HttpSession session) {
        var authenticated = gitHubOAuthService.getAuthenticatedUser(session);
        if (authenticated.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = userService.ensureUserExists(authenticated.get());
        commentService.deleteComment(user, organizationId, commentId);
        return ResponseEntity.noContent().build();
    }
}

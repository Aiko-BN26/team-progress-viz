package io.github.aikobn26.teamprogressviz.auth.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.auth.exception.GitHubOAuthException;
import io.github.aikobn26.teamprogressviz.auth.model.AuthenticatedUser;
import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.shared.properties.FrontendProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
public class AuthController {


    private final GitHubOAuthService gitHubOAuthService;
    private final FrontendProperties frontendProperties;

    /**
     * GitHubログインを開始し、Authorization Url を返します。
     * @param session HTTPセッション
     * @return Authorization Url を含むレスポンス 
     */
    @GetMapping("/github/login")
    public ResponseEntity<Map<String, String>> startGitHubLogin(HttpSession session) {
        var authorizationUrl = gitHubOAuthService.createAuthorizationUrl(session);
        return ResponseEntity.ok(Map.of("authorizationUrl", authorizationUrl));
    }

    /**
     * GitHubからのコールバックを処理します。
     * @param code 認可コード
     * @param state CSRF
     * @param session HTTPセッション 
     * @param response レスポンス
     */
    @GetMapping("/github/callback")
    public void handleGitHubCallback
    (
        @RequestParam String code,
        @RequestParam(required = false) String state,
        HttpSession session,
        HttpServletResponse response
    ) throws IOException {
        try {
            gitHubOAuthService.completeAuthentication(code, state, session);
            response.sendRedirect(frontendProperties.successRedirectUrlWithStatus());
        } catch (GitHubOAuthException e) {
            response.sendRedirect(frontendProperties.errorRedirectUrl("認証に失敗しました"));
        }
    }

    /**
     * 現在のセッションの認証済みユーザー情報を取得します。
     * @param session HTTPセッション
     * @return 認証済みユーザー情報
     */
    @GetMapping("/session")
    public ResponseEntity<AuthenticatedUser> session(HttpSession session) {
        return gitHubOAuthService
                .getAuthenticatedUser(session)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * 現在のセッションをログアウトします。
     * @param session HTTPセッション
     * @return レスポンスエンティティ
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}

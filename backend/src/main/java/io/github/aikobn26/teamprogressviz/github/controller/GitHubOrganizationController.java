package io.github.aikobn26.teamprogressviz.github.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.github.model.GitHubOrganization;
import io.github.aikobn26.teamprogressviz.github.model.GitHubRepository;
import io.github.aikobn26.teamprogressviz.github.service.GitHubOrganizationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/github")
@Validated
@AllArgsConstructor
public class GitHubOrganizationController {

    private final GitHubOrganizationService organizationService;
    private final GitHubOAuthService gitHubOAuthService;

    @GetMapping("/organizations")
    public ResponseEntity<List<GitHubOrganization>> listOrganizations(HttpSession session) {
        var token = gitHubOAuthService.getAccessToken(session);
        if (token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var organizations = organizationService.listOrganizations(token.get());
        return ResponseEntity.ok(organizations);
    }

    @GetMapping("/organizations/{organization}/repositories")
    public ResponseEntity<List<GitHubRepository>> listRepositories(
            @PathVariable("organization") @NotBlank String organization,
            HttpSession session) {
        var token = gitHubOAuthService.getAccessToken(session);
        if (token.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var repositories = organizationService.listRepositories(token.get(), organization);
        return ResponseEntity.ok(repositories);
    }
}

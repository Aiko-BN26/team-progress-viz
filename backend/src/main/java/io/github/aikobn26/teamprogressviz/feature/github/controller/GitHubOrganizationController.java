package io.github.aikobn26.teamprogressviz.feature.github.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.aikobn26.teamprogressviz.feature.auth.service.GitHubOAuthService;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubOrganization;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubOrganizationMember;
import io.github.aikobn26.teamprogressviz.feature.github.model.GitHubRepository;
import io.github.aikobn26.teamprogressviz.feature.github.service.GitHubOrganizationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/github")
@Validated
@AllArgsConstructor
public class GitHubOrganizationController {

    private final GitHubOrganizationService organizationService;
    private final GitHubOAuthService gitHubOAuthService;

    @GetMapping("/organizations")
    public Mono<ResponseEntity<List<GitHubOrganization>>> listOrganizations(HttpSession session) {
        return Mono.defer(() -> {
            var token = gitHubOAuthService.getAccessToken(session);
            if (token.isEmpty()) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            return organizationService.listOrganizationsReactive(token.get())
                    .map(ResponseEntity::ok);
        });
    }

    @GetMapping("/organizations/{organization}/repositories")
    public Mono<ResponseEntity<List<GitHubRepository>>> listRepositories(
            @PathVariable("organization") @NotBlank String organization,
            HttpSession session) {
        return Mono.defer(() -> {
            var token = gitHubOAuthService.getAccessToken(session);
            if (token.isEmpty()) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            return organizationService.listRepositoriesReactive(token.get(), organization)
                    .map(ResponseEntity::ok);
        });
    }

    @GetMapping("/organizations/{organization}/members")
    public Mono<ResponseEntity<List<GitHubOrganizationMember>>> listMembers(
            @PathVariable("organization") @NotBlank String organization,
            HttpSession session) {
        return Mono.defer(() -> {
            var token = gitHubOAuthService.getAccessToken(session);
            if (token.isEmpty()) {
                return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
            }
            return organizationService.listMembersReactive(token.get(), organization)
                    .map(ResponseEntity::ok);
        });
    }
}

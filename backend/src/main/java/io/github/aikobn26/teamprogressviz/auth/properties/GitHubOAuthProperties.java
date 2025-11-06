package io.github.aikobn26.teamprogressviz.auth.properties;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "github.oauth")
@Validated
public record GitHubOAuthProperties
(
    
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotNull(message="github.authorize-url must be provided") URI authorizeUrl,
        @NotNull(message="github.token-url must be provided") URI tokenUrl,
        @NotNull(message="github.user-url must be provided") URI userUrl,
        @NotNull(message="github.callback-url must be provided") URI callbackUrl
) {}

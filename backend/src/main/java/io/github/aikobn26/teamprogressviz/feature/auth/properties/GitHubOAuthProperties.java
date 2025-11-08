package io.github.aikobn26.teamprogressviz.feature.auth.properties;

import java.net.URI;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;

@ConfigurationProperties(prefix = "github.oauth")
@Validated
public record GitHubOAuthProperties
(
    
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        URI authorizeUrl,
        URI tokenUrl,
        URI userUrl,
        URI callbackUrl
) 
{

    public GitHubOAuthProperties {
        Objects.requireNonNull(authorizeUrl, "github.oauth.authorize-url must be provided");
        Objects.requireNonNull(tokenUrl, "github.oauth.token-url must be provided");
        Objects.requireNonNull(userUrl, "github.oauth.user-url must be provided");
        Objects.requireNonNull(callbackUrl, "github.oauth.callback-url must be provided");
    }
}

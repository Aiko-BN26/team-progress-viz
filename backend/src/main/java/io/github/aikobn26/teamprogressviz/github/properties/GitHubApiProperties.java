package io.github.aikobn26.teamprogressviz.github.properties;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotNull;

@ConfigurationProperties(prefix = "github.api")
@Validated
public record GitHubApiProperties(
    @NotNull(message = "github.api.base-url must be provided") URI baseUrl
) {}

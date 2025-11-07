package io.github.aikobn26.teamprogressviz.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record OrganizationRegistrationRequest(
        @NotBlank(message = "login is required")
        @Size(max = 255, message = "login must be 255 characters or fewer")
        String login,
        @Size(max = 2048, message = "defaultLinkUrl must be 2048 characters or fewer")
        String defaultLinkUrl
) {}

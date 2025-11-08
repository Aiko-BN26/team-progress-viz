package io.github.aikobn26.teamprogressviz.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotBlank(message = "status is required")
        String status,
        @Size(max = 2000, message = "statusMessage must be 2000 characters or less")
        String statusMessage,
        @Min(value = 0, message = "capacityHours must be 0 or greater")
        Integer capacityHours,
        @Min(value = 0, message = "availableMinutes must be 0 or greater")
        Integer availableMinutes,
        String date
) {
}

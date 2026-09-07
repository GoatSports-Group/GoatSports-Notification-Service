package iuh.fit.notificationservice.presentation.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateNotificationRequest(
        @NotNull UUID receiverId,
        @NotBlank String title,
        @NotBlank String content,
        @NotBlank String type,
        @NotBlank String sourceService,
        @NotBlank String referenceType,
        @NotNull UUID referenceId
) {
}

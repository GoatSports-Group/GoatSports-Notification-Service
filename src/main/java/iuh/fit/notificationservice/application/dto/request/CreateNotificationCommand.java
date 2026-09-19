package iuh.fit.notificationservice.application.dto.request;

import java.util.UUID;

public record CreateNotificationCommand(
    UUID eventId,
    UUID receiverId,
    String title,
    String content,
    String type,
    String sourceService,
    String referenceType,
    UUID referenceId
) {}

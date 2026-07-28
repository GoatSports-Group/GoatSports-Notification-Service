package iuh.fit.notificationservice.application.dto.request;

import java.util.UUID;

public record DeleteNotificationCommand(
    UUID notificationId,
    UUID requesterId
) {}

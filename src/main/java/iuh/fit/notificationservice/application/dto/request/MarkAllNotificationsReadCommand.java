package iuh.fit.notificationservice.application.dto.request;

import java.util.UUID;

public record MarkAllNotificationsReadCommand(
    UUID receiverId
) {}

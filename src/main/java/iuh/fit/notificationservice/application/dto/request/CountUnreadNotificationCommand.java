package iuh.fit.notificationservice.application.dto.request;

import java.util.UUID;

public record CountUnreadNotificationCommand(
    UUID receiverId
) {}

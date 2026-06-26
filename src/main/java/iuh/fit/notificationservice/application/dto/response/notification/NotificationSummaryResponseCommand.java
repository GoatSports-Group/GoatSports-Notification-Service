package iuh.fit.notificationservice.application.dto.response.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationSummaryResponseCommand(
    UUID notificationId,
    String title,
    String type,
    String status,
    LocalDateTime createdAt
) {}

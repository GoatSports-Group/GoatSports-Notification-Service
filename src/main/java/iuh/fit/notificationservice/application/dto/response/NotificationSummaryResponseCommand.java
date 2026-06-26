package iuh.fit.notificationservice.application.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationSummaryResponseCommand(
    UUID notificationId,
    String title,
    String type,
    String status,
    LocalDateTime createdAt
) {}

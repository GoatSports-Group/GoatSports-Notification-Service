package iuh.fit.notificationservice.domain.entity;

import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private UUID notificationId;
    private UUID receiverId;
    private String title;
    private String content;
    private NotificationType type;
    private NotificationStatus status;
    private UUID referenceId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}

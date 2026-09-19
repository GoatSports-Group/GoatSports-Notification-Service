package iuh.fit.notificationservice.domain.entity;

import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.domain.enums.NotificationReferenceType;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import iuh.fit.notificationservice.domain.enums.SourceService;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {
    private UUID notificationId;
    private UUID eventId;
    private UUID receiverId;
    private String title;
    private String content;
    private NotificationType type;
    private NotificationStatus status;
    private SourceService sourceService;
    private NotificationReferenceType referenceType;
    private UUID referenceId;
    private LocalDateTime readAt;
}

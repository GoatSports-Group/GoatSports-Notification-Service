package iuh.fit.notificationservice.infrastructure.persistence.entity;

import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.domain.enums.NotificationReferenceType;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import iuh.fit.notificationservice.domain.enums.SourceService;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID notificationId;
    /** Dedupe domain event: cùng eventId chỉ sinh một notification. */
    @Column(name = "event_id", unique = true)
    private UUID eventId;
    @Column(name = "receiver_id", nullable = false)
    private UUID receiverId;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, length = 2000)
    private String content;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "source_service", nullable = false)
    private SourceService sourceService;
    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private NotificationReferenceType referenceType;
    @Column(name = "reference_id")
    private UUID referenceId;
    @Column(name = "read_at")
    private LocalDateTime readAt;
}

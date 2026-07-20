package iuh.fit.notificationservice.infrastructure.persistence.mapper;

import iuh.fit.notificationservice.domain.entity.Notification;
import iuh.fit.notificationservice.infrastructure.persistence.entity.NotificationEntity;

public class NotificationMapper {

    public static NotificationEntity toEntity(Notification domain) {
        if (domain == null) return null;

        NotificationEntity entity = NotificationEntity.builder()
                .notificationId(domain.getNotificationId())
                .receiverId(domain.getReceiverId())
                .title(domain.getTitle())
                .content(domain.getContent())
                .type(domain.getType())
                .status(domain.getStatus())
                .referenceId(domain.getReferenceId())
                .readAt(domain.getReadAt() != null ? domain.getReadAt() : null)
                .build();
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }

    public static Notification toDomain(NotificationEntity entity) {
        if (entity == null) return null;

        return Notification.builder()
                .notificationId(entity.getNotificationId())
                .receiverId(entity.getReceiverId())
                .title(entity.getTitle())
                .content(entity.getContent())
                .type(entity.getType())
                .status(entity.getStatus())
                .referenceId(entity.getReferenceId())
                .readAt(entity.getReadAt() != null ? entity.getReadAt() : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt() : null)
                .build();
    }
}

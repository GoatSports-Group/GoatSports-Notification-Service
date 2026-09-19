package iuh.fit.notificationservice.infrastructure.persistence.repository.jpa;

import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, UUID>,
        JpaSpecificationExecutor<NotificationEntity> {

    long countByReceiverIdAndStatus(UUID receiverId, NotificationStatus status);

    @Modifying
    @Query("""
            UPDATE NotificationEntity n
            SET n.status = :status, n.readAt = :readAt
            WHERE n.receiverId = :receiverId AND n.status = :unreadStatus
            """)
    void markAllReadByReceiverId(
            @Param("receiverId") UUID receiverId,
            @Param("status") NotificationStatus status,
            @Param("unreadStatus") NotificationStatus unreadStatus,
            @Param("readAt") LocalDateTime readAt
    );

    boolean existsByEventId(UUID eventId);
}

package iuh.fit.notificationservice.infrastructure.persistence.repository.jpa;

import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.infrastructure.persistence.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, UUID>,
        JpaSpecificationExecutor<NotificationEntity> {

    long countByReceiverIdAndStatus(UUID receiverId, NotificationStatus status);
}

package iuh.fit.notificationservice.application.port.persistence;

import iuh.fit.notificationservice.application.dto.PageFilter;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.domain.entity.Notification;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification) throws ServiceException;

    Optional<Notification> findById(UUID id);

    PageResult<Notification> findByReceiverId(PageFilter filter, UUID receiverId);

    long countUnread(UUID receiverId);

    void markAllReadByReceiverId(UUID receiverId);

    void deleteById(UUID id) throws ServiceException;
}

package iuh.fit.notificationservice.infrastructure.decorator;

import iuh.fit.notificationservice.application.dto.event.OwnerApplicationProgressChangedEvent;
import iuh.fit.notificationservice.application.dto.request.*;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationCatchUp;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.application.service.impl.NotificationServiceImpl;
import iuh.fit.notificationservice.domain.entity.Notification;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Primary
@RequiredArgsConstructor
public class NotificationDecorator implements NotificationService {

    private final NotificationServiceImpl notificationServiceImpl;

    @Override
    @Transactional(readOnly = true)
    public long countUnreadNotification(CountUnreadNotificationCommand command) {
        return notificationServiceImpl.countUnreadNotification(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public Notification createNotification(CreateNotificationCommand command) throws ServiceException {
        return notificationServiceImpl.createNotification(command);
    }

    @Override
    public void notifyUser(NotificationResponseCommand notification) {
        notificationServiceImpl.notifyUser(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Notification> getAllNotifications(GetNotificationsCommand command) {
        return notificationServiceImpl.getAllNotifications(command);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationCatchUp getNotificationsSince(GetNotificationsSinceCommand command) {
        return notificationServiceImpl.getNotificationsSince(command);
    }

    @Override
    @Transactional(rollbackFor = ServiceException.class)
    public Notification markNotificationRead(MarkNotificationReadCommand command) throws ServiceException {
        return notificationServiceImpl.markNotificationRead(command);
    }

    @Override
    public void markAllNotificationsRead(MarkAllNotificationsReadCommand command) {
        notificationServiceImpl.markAllNotificationsRead(command);
    }

    @Override
    public void deleteNotification(DeleteNotificationCommand command) throws ServiceException {
        notificationServiceImpl.deleteNotification(command);
    }

    @Override
    public void publishOwnerApplicationProgress(OwnerApplicationProgressChangedEvent event) {
        notificationServiceImpl.publishOwnerApplicationProgress(event);
    }
}

package iuh.fit.notificationservice.application.service;

import iuh.fit.notificationservice.application.dto.request.CountUnreadNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.DeleteNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.GetNotificationsCommand;
import iuh.fit.notificationservice.application.dto.request.MarkAllNotificationsReadCommand;
import iuh.fit.notificationservice.application.dto.request.MarkNotificationReadCommand;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.domain.entity.Notification;

public interface NotificationService {
    long countUnreadNotification(CountUnreadNotificationCommand command);

    Notification createNotification(CreateNotificationCommand command) throws ServiceException;

    void notifyUser(NotificationResponseCommand notification);

    PageResult<Notification> getAllNotifications(GetNotificationsCommand command);

    Notification markNotificationRead(MarkNotificationReadCommand command) throws ServiceException;

    void markAllNotificationsRead(MarkAllNotificationsReadCommand command);

    void deleteNotification(DeleteNotificationCommand command) throws ServiceException;
}

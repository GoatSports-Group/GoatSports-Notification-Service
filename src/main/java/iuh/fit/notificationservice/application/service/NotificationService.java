package iuh.fit.notificationservice.application.service;

import iuh.fit.notificationservice.application.dto.request.CountUnreadNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.GetNotificationsCommand;
import iuh.fit.notificationservice.application.dto.request.MarkNotificationReadCommand;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.domain.entity.Notification;

public interface NotificationService {
    long countUnreadNotification(CountUnreadNotificationCommand command);

    Notification createNotification(CreateNotificationCommand command) throws ServiceException;

    PageResult<Notification> getAllNotifications(GetNotificationsCommand command);

    Notification markNotificationRead(MarkNotificationReadCommand command) throws ServiceException;
}

package iuh.fit.notificationservice.application.service.impl;

import iuh.fit.notificationservice.application.dto.request.CountUnreadNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.GetNotificationsCommand;
import iuh.fit.notificationservice.application.dto.request.MarkNotificationReadCommand;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.domain.entity.Notification;
import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.domain.enums.NotificationType;

import java.time.LocalDateTime;

public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketPublisher webSocketPublisher;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationWebSocketPublisher webSocketPublisher) {
        this.notificationRepository = notificationRepository;
        this.webSocketPublisher = webSocketPublisher;
    }

    @Override
    public long countUnreadNotification(CountUnreadNotificationCommand command) {
        return notificationRepository.countUnread(command.receiverId());
    }

    @Override
    public Notification createNotification(CreateNotificationCommand command) throws ServiceException {
        NotificationType type;
        try {
            type = NotificationType.valueOf(command.type());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Loại thông báo không hợp lệ: " + command.type());
        }

        Notification notification = Notification.builder()
                .receiverId(command.receiverId())
                .title(command.title())
                .content(command.content())
                .type(type)
                .status(NotificationStatus.UNREAD)
                .referenceId(command.referenceId())
                .build();

        Notification saved = notificationRepository.save(notification);

        webSocketPublisher.publish(saved);

        return saved;
    }

    @Override
    public PageResult<Notification> getAllNotifications(GetNotificationsCommand command) {
        return notificationRepository.findByReceiverId(command.filter(), command.receiverId());
    }

    @Override
    public Notification markNotificationRead(MarkNotificationReadCommand command) throws ServiceException {
        Notification notification = notificationRepository.findById(command.notificationId())
                .orElseThrow(() -> new ServiceException("Thông báo không tồn tại."));

        if (notification.getStatus() == NotificationStatus.READ) {
            throw new ServiceException("Thông báo đã được đọc.");
        }

        notification.setStatus(NotificationStatus.READ);
        notification.setReadAt(LocalDateTime.now());

        return notificationRepository.save(notification);
    }
}

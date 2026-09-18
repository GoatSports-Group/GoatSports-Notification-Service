package iuh.fit.notificationservice.application.service.impl;

import iuh.fit.notificationservice.application.dto.event.OwnerApplicationProgressChangedEvent;
import iuh.fit.notificationservice.application.dto.request.CountUnreadNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.DeleteNotificationCommand;
import iuh.fit.notificationservice.application.dto.request.GetNotificationsCommand;
import iuh.fit.notificationservice.application.dto.request.MarkAllNotificationsReadCommand;
import iuh.fit.notificationservice.application.dto.request.MarkNotificationReadCommand;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.mapper.NotificationMapper;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.domain.entity.Notification;
import iuh.fit.notificationservice.domain.enums.NotificationReferenceType;
import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import iuh.fit.notificationservice.domain.enums.SourceService;

import java.time.LocalDateTime;

public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationWebSocketPublisher webSocketPublisher;
    private final NotificationMapper notificationMapper;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            NotificationWebSocketPublisher webSocketPublisher,
            NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.webSocketPublisher = webSocketPublisher;
        this.notificationMapper = notificationMapper;
    }

    @Override
    public long countUnreadNotification(CountUnreadNotificationCommand command) {
        return notificationRepository.countUnread(command.receiverId());
    }

    @Override
    public Notification createNotification(CreateNotificationCommand command) throws ServiceException {
        NotificationType type;
        SourceService sourceService;
        NotificationReferenceType referenceType;
        try {
            type = NotificationType.valueOf(command.type());
            sourceService = SourceService.valueOf(command.sourceService());
            referenceType = NotificationReferenceType.valueOf(command.referenceType());
        } catch (IllegalArgumentException e) {
            throw new ServiceException("Loại thông báo không hợp lệ: " + command.type());
        }

        Notification notification = Notification.builder()
                .receiverId(command.receiverId())
                .title(command.title())
                .content(command.content())
                .type(type)
                .status(NotificationStatus.UNREAD)
                .sourceService(sourceService)
                .referenceType(referenceType)
                .referenceId(command.referenceId())
                .build();

        Notification saved = notificationRepository.save(notification);
        webSocketPublisher.publish(notificationMapper.toNotificationResponseCommand(saved));
        return saved;
    }

    @Override
    public void notifyUser(NotificationResponseCommand notification) {
        webSocketPublisher.publish(notification);
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

    @Override
    public void markAllNotificationsRead(MarkAllNotificationsReadCommand command) {
        notificationRepository.markAllReadByReceiverId(command.receiverId());
    }

    @Override
    public void deleteNotification(DeleteNotificationCommand command) throws ServiceException {
        Notification notification = notificationRepository.findById(command.notificationId())
                .orElseThrow(() -> new ServiceException("Thông báo không tồn tại."));

        if (!notification.getReceiverId().equals(command.requesterId())) {
            throw new ServiceException("Bạn không có quyền xóa thông báo này.");
        }

        notificationRepository.deleteById(command.notificationId());
    }

    @Override
    public void publishOwnerApplicationProgress(OwnerApplicationProgressChangedEvent event) {
        webSocketPublisher.publishOwnerApplicationProgress(event);
    }
}

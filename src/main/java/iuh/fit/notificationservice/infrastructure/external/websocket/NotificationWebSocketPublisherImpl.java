package iuh.fit.notificationservice.infrastructure.external.websocket;

import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.mapper.NotificationMapper;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import iuh.fit.notificationservice.domain.entity.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketPublisherImpl implements NotificationWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    @Override
    public void publish(Notification notification) {
        log.info("Đẩy thông báo đến websocket: ID={}, Title={}", notification.getNotificationId(), notification.getTitle());
        try {
            NotificationResponseCommand response = NotificationMapper.toNotificationResponseCommand(notification);
            messagingTemplate.convertAndSend("/topic/admin/notifications", response);
            log.info("Gửi thông báo thành công: ID={}, Title={}", notification.getNotificationId(), notification.getTitle());
        } catch (Exception e) {
            log.error("Lỗi đẩy thông báo đến websocket: {}", e.getMessage(), e);
        }
    }
}

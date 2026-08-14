package iuh.fit.notificationservice.infrastructure.external.websocket;

import iuh.fit.notificationservice.application.dto.event.OwnerApplicationProgressChangedEvent;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import iuh.fit.notificationservice.infrastructure.external.client.AuthClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketPublisherImpl implements NotificationWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final AuthClient authClient;

    @Override
    public void publish(NotificationResponseCommand notification) {
        log.info("Đẩy thông báo đến websocket: ID={}, Title={}", notification.notificationId(), notification.title());
        try {
            String adminId = authClient.getAdmin();

            if (notification.receiverId() != null && notification.receiverId().toString().equalsIgnoreCase(adminId)) {
                messagingTemplate.convertAndSend("/topic/admin/notifications", notification);
            } else {
                messagingTemplate.convertAndSend("/topic/user/notifications/" + notification.receiverId(), notification);
            }
            
            log.info("Gửi thông báo thành công: ID={}, Title={}", notification.notificationId(), notification.title());
        } catch (Exception e) {
            log.error("Lỗi đẩy thông báo đến websocket: {}", e.getMessage(), e);
        }
    }

    @Override
    public void publishOwnerApplicationProgress(OwnerApplicationProgressChangedEvent event) {
        String destination = "/topic/user/owner-application-progress/" + event.userId();
        messagingTemplate.convertAndSend(destination, event);
        log.info(
                "Đã gửi cập nhật tiến trình đơn {} đến người dùng {}",
                event.ownerApplicationId(),
                event.userId()
        );
    }
}

package iuh.fit.notificationservice.infrastructure.external.websocket;

import iuh.fit.notificationservice.application.dto.event.OwnerApplicationProgressChangedEvent;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import iuh.fit.notificationservice.infrastructure.external.client.AuthClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketPublisherImpl implements NotificationWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;
    private final AuthClient authClient;

    @Override
    public void publish(NotificationResponseCommand notification) {
        if (notification.receiverId() == null) {
            log.warn("Skipping WebSocket notification {} because receiverId is missing", notification.notificationId());
            return;
        }

        log.info("Publishing WebSocket notification: ID={}, Title={}", notification.notificationId(), notification.title());
        try {
            if (isAdminReceiver(notification.receiverId().toString())) {
                messagingTemplate.convertAndSend("/topic/admin/notifications", notification);
            } else {
                messagingTemplate.convertAndSend("/topic/user/notifications/" + notification.receiverId(), notification);
            }

            log.info("WebSocket notification sent: ID={}, Title={}", notification.notificationId(), notification.title());
        } catch (Exception e) {
            log.error("Failed to publish WebSocket notification: {}", e.getMessage(), e);
        }
    }

    // Mã admin gần như không đổi: trước đây MỖI thông báo gọi HTTP sang auth-service một lần (gửi cho
    // 1.000 người = 1.000 request). Giữ 5 phút; lỗi thì không cache để lần sau hỏi lại.
    private static final Duration ADMIN_ID_TTL = Duration.ofMinutes(5);
    private volatile String cachedAdminId;
    private volatile Instant adminIdExpiresAt = Instant.EPOCH;

    private boolean isAdminReceiver(String receiverId) {
        try {
            String adminId = adminId();
            return adminId != null && receiverId.equalsIgnoreCase(adminId);
        } catch (Exception e) {
            log.warn("Could not resolve the admin account; using the user notification topic: {}", e.getMessage());
            return false;
        }
    }

    private String adminId() {
        if (Instant.now().isAfter(adminIdExpiresAt)) {
            cachedAdminId = authClient.getAdmin();
            adminIdExpiresAt = Instant.now().plus(ADMIN_ID_TTL);
        }
        return cachedAdminId;
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

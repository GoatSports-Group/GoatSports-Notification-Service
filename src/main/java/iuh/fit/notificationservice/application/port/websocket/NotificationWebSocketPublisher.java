package iuh.fit.notificationservice.application.port.websocket;

import iuh.fit.notificationservice.application.dto.event.OwnerApplicationProgressChangedEvent;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;

import java.util.UUID;

public interface NotificationWebSocketPublisher {
    void publish(NotificationResponseCommand notification);

    void publishOwnerApplicationProgress(OwnerApplicationProgressChangedEvent event);

    /** Phiên của người dùng vừa bị thu hồi (bị khóa / yêu cầu xóa tài khoản): mọi tab đang mở đăng xuất ngay. */
    void publishSessionRevoked(UUID userId, String reason);
}

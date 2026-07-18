package iuh.fit.notificationservice.application.port.websocket;

import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;

public interface NotificationWebSocketPublisher {
    void publish(NotificationResponseCommand notification);
}

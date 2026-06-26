package iuh.fit.notificationservice.application.port.websocket;

import iuh.fit.notificationservice.domain.entity.Notification;

public interface NotificationWebSocketPublisher {
    void publish(Notification notification);
}

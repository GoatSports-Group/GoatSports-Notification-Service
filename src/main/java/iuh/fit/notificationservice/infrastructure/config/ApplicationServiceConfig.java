package iuh.fit.notificationservice.infrastructure.config;

import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import iuh.fit.notificationservice.application.service.impl.NotificationServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationServiceConfig {

    @Bean
    public NotificationServiceImpl notificationService(
            NotificationRepository notificationRepository,
            NotificationWebSocketPublisher webSocketPublisher
    ) {
        return new NotificationServiceImpl(notificationRepository, webSocketPublisher);
    }

}

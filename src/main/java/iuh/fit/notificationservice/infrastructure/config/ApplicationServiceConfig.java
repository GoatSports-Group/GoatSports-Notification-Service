package iuh.fit.notificationservice.infrastructure.config;

import iuh.fit.notificationservice.application.mapper.NotificationMapper;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import iuh.fit.notificationservice.application.service.impl.NotificationServiceImpl;
import iuh.fit.notificationservice.application.port.client.RecipientDirectoryPort;
import iuh.fit.notificationservice.application.port.persistence.BroadcastRepository;
import iuh.fit.notificationservice.application.port.scheduling.BroadcastDeliveryScheduler;
import iuh.fit.notificationservice.application.service.BroadcastService;
import iuh.fit.notificationservice.application.service.impl.BroadcastServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ApplicationServiceConfig {

    @Bean
    public NotificationServiceImpl notificationService(
            NotificationRepository notificationRepository,
            NotificationWebSocketPublisher webSocketPublisher,
            NotificationMapper notificationMapper
    ) {
        return new NotificationServiceImpl(notificationRepository, webSocketPublisher, notificationMapper);
    }

    @Bean
    public BroadcastService broadcastService(
            BroadcastRepository broadcastRepository,
            NotificationRepository notificationRepository,
            RecipientDirectoryPort recipientDirectory,
            NotificationWebSocketPublisher webSocketPublisher,
            NotificationMapper notificationMapper,
            BroadcastDeliveryScheduler deliveryScheduler
    ) {
        return new BroadcastServiceImpl(broadcastRepository, notificationRepository, recipientDirectory,
                webSocketPublisher, notificationMapper, deliveryScheduler, Clock.systemDefaultZone());
    }
}

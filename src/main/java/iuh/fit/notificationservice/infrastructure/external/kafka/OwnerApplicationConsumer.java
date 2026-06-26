package iuh.fit.notificationservice.infrastructure.external.kafka;

import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import iuh.fit.notificationservice.infrastructure.external.kafka.dto.OwnerApplicationEvent;
import iuh.fit.notificationservice.infrastructure.external.kafka.dto.OwnerApplicationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerApplicationConsumer {

    private final NotificationService notificationService;

    @Value("${app.notification.admin-receiver-id:00000000-0000-0000-0000-000000000000}")
    private String adminReceiverIdStr;

    @KafkaListener(topics = "${spring.kafka.topic.owner-application}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(OwnerApplicationEvent event) {
        log.info("Nhận sự kiện kafka: {}", event);
        try {
            if (event.getEventType() == OwnerApplicationEventType.SUBMITTED) {
                UUID adminReceiverId = UUID.fromString(adminReceiverIdStr);

                CreateNotificationCommand command = new CreateNotificationCommand(
                        adminReceiverId,
                        "Có đơn đăng ký chủ sân mới",
                        event.getData().getEmail() + " vừa gửi đơn đăng ký chủ sân.",
                        NotificationType.OWNER_APPLICATION.name(),
                        event.getData().getUserId()
                );

                notificationService.createNotification(command);
                log.info("Tạo thông báo thành công");
            } else {
                log.info("Bỏ qua event này [{}] (chỉ SUBMITTED)", event.getEventType());
            }
        } catch (Exception e) {
            log.error("Thất bại trong khi xử lý sự kiện Kafka: {}", e.getMessage(), e);
        }
    }
}

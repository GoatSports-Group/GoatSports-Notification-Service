package iuh.fit.notificationservice.infrastructure.external.kafka;

import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.application.util.BaseUtil;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import iuh.fit.notificationservice.infrastructure.external.client.AuthClient;
import iuh.fit.notificationservice.infrastructure.external.kafka.dto.OwnerApplicationEvent;
import iuh.fit.notificationservice.infrastructure.external.kafka.dto.OwnerApplicationEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OwnerApplicationConsumer {

    private final NotificationService notificationService;

    private final AuthClient authClient;

    @KafkaListener(
            topics = "${spring.kafka.topic.owner-application}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consume(OwnerApplicationEvent event) {
        log.info("Nhận sự kiện kafka: {}", event);
        try {
            if (event.getEventType() == OwnerApplicationEventType.SUBMITTED) {
                consumeSubmitted(event);
            } else if (event.getEventType() == OwnerApplicationEventType.APPROVED) {
                consumeApproved(event);
            } else if (event.getEventType() == OwnerApplicationEventType.REJECTED) {
                consumeRejected(event);
            }
        } catch (Exception e) {
            log.error("Thất bại trong khi xử lý sự kiện Kafka: {}", e.getMessage(), e);
        }
    }

    private void consumeSubmitted(OwnerApplicationEvent event) throws ServiceException {
        UUID adminReceiverId = BaseUtil.uuid(getAdminReceiverId());

        CreateNotificationCommand command = new CreateNotificationCommand(
                adminReceiverId,
                "Có đơn đăng ký chủ sân mới",
                event.getData().getEmail() + " vừa gửi đơn đăng ký chủ sân.",
                NotificationType.OWNER_APPLICATION.name(),
                event.getData().getUserId()
        );

        notificationService.createNotification(command);
        log.info("Tạo thông báo thành công");
    }

    private void consumeApproved(OwnerApplicationEvent event) throws ServiceException {
        UUID adminReceiverId = BaseUtil.uuid(getAdminReceiverId());

        CreateNotificationCommand command = new CreateNotificationCommand(
                event.getData().getUserId(),
                "Yêu cầu đăng ký chủ sân được phê duyệt",
                "Đơn đăng ký làm chủ sân của bạn đã được chấp nhận.",
                NotificationType.OWNER_APPLICATION.name(),
                adminReceiverId
        );

        notificationService.createNotification(command);
        log.info("Tạo thông báo phê duyệt thành công");
    }

    private void consumeRejected(OwnerApplicationEvent event) throws ServiceException {
        UUID adminReceiverId = BaseUtil.uuid(getAdminReceiverId());

        CreateNotificationCommand command = new CreateNotificationCommand(
                event.getData().getUserId(),
                "Yêu cầu đăng ký chủ sân bị từ chối",
                "Đơn đăng ký làm chủ sân của bạn đã bị từ chối. Lý do: " + event.getData().getRejectReason(),
                NotificationType.OWNER_APPLICATION.name(),
                adminReceiverId
        );

        notificationService.createNotification(command);
        log.info("Tạo thông báo từ chối thành công");
    }

    private String getAdminReceiverId() {
        return authClient.getAdmin();
    }
}

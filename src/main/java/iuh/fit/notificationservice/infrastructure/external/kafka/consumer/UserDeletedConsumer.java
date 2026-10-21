package iuh.fit.notificationservice.infrastructure.external.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** auth-service vừa ẩn danh hóa một tài khoản (hết 30 ngày chờ xóa): xóa thông báo của người đó. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "user-deleted.consumer-enabled", havingValue = "true", matchIfMissing = true)
public class UserDeletedConsumer {
    private final ObjectMapper objectMapper;
    private final NotificationRepository notificationRepository;

    @KafkaListener(topics = "${user-deleted.topic:user-deleted-topic}", groupId = "notification-service-user-deleted",
            containerFactory = "stringListenerContainerFactory")
    @Transactional
    public void consume(String message) throws Exception {
        UUID userId = UUID.fromString(objectMapper.readTree(message).path("userId").asText());
        notificationRepository.deleteByReceiverId(userId);
        log.info("Deleted notifications of deleted user {}", userId);
    }
}

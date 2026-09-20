package iuh.fit.notificationservice.infrastructure.external.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "club.events",
        name = "notification-consumer-enabled",
        havingValue = "true"
)
public class ClubNotificationConsumer {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @KafkaListener(
            topics = "${club.events.topic}",
            groupId = "${club.events.notification-group-id}",
            containerFactory = "stringListenerContainerFactory"
    )
    public void consume(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            String eventType = envelope.path("eventType").asText();
            if (!"club.activity.created.v1".equals(eventType)) return;

            UUID eventId = uuid(envelope, "eventId");
            if (eventId == null) {
                log.warn("Bỏ qua club event không có eventId: {}", eventType);
                return;
            }
            if (notificationRepository.existsByEventId(eventId)) {
                log.debug("Bỏ qua notification trùng eventId {}", eventId);
                return;
            }

            JsonNode payload = envelope.path("payload");
            UUID receiverId = uuid(payload, "receiverId");
            if (receiverId == null) {
                log.warn("Club event {} không có receiverId, bỏ qua", eventType);
                return;
            }

            String clubName = text(payload, "clubName", "Câu lạc bộ");
            String title = text(payload, "title", "hoạt động mới");

            notificationService.createNotification(new CreateNotificationCommand(
                    eventId,
                    receiverId,
                    "Hoạt động mới của " + clubName,
                    clubName + " vừa thêm hoạt động \"" + title + "\".",
                    "CLUB",
                    "CLUB",
                    "CLUB_ACTIVITY",
                    uuid(payload, "activityId")
            ));
        } catch (Exception exception) {
            log.error("Không xử lý được club event cho notification: {}", exception.getMessage(), exception);
        }
    }

    private UUID uuid(JsonNode node, String field) {
        String raw = node.path(field).asText(null);
        if (raw == null || raw.isBlank() || "null".equals(raw)) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String text(JsonNode node, String field, String fallback) {
        String raw = node.path(field).asText(null);
        return raw == null || raw.isBlank() ? fallback : raw;
    }
}

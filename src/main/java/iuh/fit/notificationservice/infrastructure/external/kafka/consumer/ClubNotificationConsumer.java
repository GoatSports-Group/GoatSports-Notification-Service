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

    private static final java.util.Set<String> SUPPORTED = java.util.Set.of(
            "club.activity.created.v1",
            "club.membership.requested.v1",
            "club.membership.approved.v1",
            "club.membership.rejected.v1",
            "club.invitation.sent.v1",
            "club.invitation.accepted.v1",
            "club.invitation.declined.v1",
            "club.disbanded.v1",
            "club.ownership.transferred.v1");

    @KafkaListener(
            topics = "${club.events.topic}",
            groupId = "${club.events.notification-group-id}",
            containerFactory = "stringListenerContainerFactory"
    )
    public void consume(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            if (envelope != null && envelope.isTextual()) {
                envelope = objectMapper.readTree(envelope.asText());
            }
            if (envelope == null || !envelope.isObject()) {
                log.warn("Bỏ qua club event không phải JSON object");
                return;
            }
            String eventType = envelope.path("eventType").asText();
            if (!SUPPORTED.contains(eventType)) return;

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
            notificationService.createNotification(switch (eventType) {
                case "club.activity.created.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Hoạt động mới của " + clubName,
                        clubName + " vừa thêm hoạt động \"" + text(payload, "title", "hoạt động mới") + "\".",
                        "CLUB", "CLUB", "CLUB_ACTIVITY", uuid(payload, "clubId"));

                case "club.membership.requested.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Yêu cầu tham gia " + clubName,
                        requesterLabel(payload) + " muốn tham gia " + clubName + ". Vào duyệt giúp nhé.",
                        "CLUB", "CLUB", "CLUB_MEMBERSHIP_REQUEST", uuid(payload, "clubId"));

                case "club.membership.approved.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Đã được duyệt vào " + clubName,
                        "Yêu cầu tham gia " + clubName + " của bạn đã được chấp nhận.",
                        "CLUB", "CLUB", "CLUB", uuid(payload, "clubId"));

                case "club.membership.rejected.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Yêu cầu tham gia bị từ chối",
                        clubName + " đã từ chối yêu cầu tham gia của bạn.",
                        "CLUB", "CLUB", "CLUB_PUBLIC", uuid(payload, "clubId"));

                case "club.invitation.sent.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Lời mời tham gia " + clubName,
                        clubName + " đã mời bạn tham gia câu lạc bộ.",
                        "CLUB", "CLUB", "CLUB_INVITATION", uuid(payload, "clubId"));

                case "club.invitation.accepted.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Lời mời đã được chấp nhận",
                        "Người bạn mời đã tham gia " + clubName + ".",
                        "CLUB", "CLUB", "CLUB", uuid(payload, "clubId"));

                case "club.invitation.declined.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Lời mời bị từ chối",
                        "Người bạn mời đã từ chối tham gia " + clubName + ".",
                        "CLUB", "CLUB", "CLUB", uuid(payload, "clubId"));

                case "club.disbanded.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        clubName + " đã giải tán",
                        "Chủ câu lạc bộ đã giải tán " + clubName
                                + ". Bạn không còn là thành viên của câu lạc bộ này.",
                        "CLUB", "CLUB", "CLUB_DISBANDED", uuid(payload, "clubId"));

                case "club.ownership.transferred.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Bạn là chủ mới của " + clubName,
                        "Quyền sở hữu " + clubName + " đã được chuyển cho bạn.",
                        "CLUB", "CLUB", "CLUB", uuid(payload, "clubId"));

                default -> throw new IllegalStateException("Loại sự kiện chưa hỗ trợ: " + eventType);
            });
        } catch (Exception exception) {
            log.error("Không xử lý được club event cho notification: {}", exception.getMessage(), exception);
        }
    }

    /** Người xin vào có thể chưa có tên hiển thị; khi đó client tự tra theo requesterId. */
    private String requesterLabel(JsonNode payload) {
        String name = payload.path("requesterName").asText(null);
        return name == null || name.isBlank() ? "Một người chơi" : name;
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

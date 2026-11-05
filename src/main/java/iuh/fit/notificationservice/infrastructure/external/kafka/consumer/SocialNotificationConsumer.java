package iuh.fit.notificationservice.infrastructure.external.kafka.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.infrastructure.external.client.AuthClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Thong bao cong dong tu social-service. social-service da loc nguoi nhan (khong gui cho chinh minh,
 * khong gui qua quan he chan); o day chi dung noi dung va dedupe theo eventId.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "social.events", name = "notification-consumer-enabled", havingValue = "true")
public class SocialNotificationConsumer {
    private static final String FALLBACK_NAME = "Một người chơi";

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final AuthClient authClient;

    @KafkaListener(
            topics = "${social.events.topic}",
            groupId = "${social.events.notification-group-id}",
            containerFactory = "stringListenerContainerFactory"
    )
    public void consume(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            if (envelope != null && envelope.isTextual()) envelope = objectMapper.readTree(envelope.asText());
            if (envelope == null || !envelope.isObject()) return;

            String eventType = envelope.path("eventType").asText();
            UUID eventId = uuid(envelope, "eventId");
            JsonNode payload = envelope.path("payload");
            UUID receiverId = uuid(payload, "receiverId");
            UUID actorId = uuid(envelope, "actorId");
            if (eventId == null || receiverId == null || actorId == null) {
                log.warn("Bỏ qua social event thiếu eventId/receiverId/actorId: {}", eventType);
                return;
            }
            if (notificationRepository.existsByEventId(eventId)) return;

            CreateNotificationCommand command = eventType.startsWith("social.moderation.")
                    ? moderation(eventType, eventId, receiverId, uuid(envelope, "aggregateId"), payload)
                    : toCommand(eventType, eventId, receiverId, actorId, payload);
            if (command != null) notificationService.createNotification(command);
        } catch (Exception exception) {
            log.error("Không xử lý được social event cho notification: {}", exception.getMessage(), exception);
        }
    }

    private CreateNotificationCommand toCommand(String eventType, UUID eventId, UUID receiverId, UUID actorId,
                                                JsonNode payload) {
        String snippet = payload.path("snippet").asText("");
        UUID postId = uuid(payload, "postId");
        return switch (eventType) {
            case "social.post.liked.v1" -> post(eventId, receiverId, postId,
                    actorName(actorId) + " đã thích bài viết của bạn",
                    quoteOr(snippet, "Bài viết của bạn vừa có thêm một lượt thích."));
            case "social.post.commented.v1" -> post(eventId, receiverId, postId,
                    actorName(actorId) + " đã bình luận về bài viết của bạn",
                    quoteOr(snippet, "Vào xem bình luận mới nhé."));
            case "social.comment.replied.v1" -> post(eventId, receiverId, postId,
                    actorName(actorId) + " đã trả lời bình luận của bạn",
                    quoteOr(snippet, "Vào xem câu trả lời nhé."));
            case "social.post.shared.v1" -> post(eventId, receiverId, postId,
                    actorName(actorId) + " đã chia sẻ bài viết của bạn",
                    quoteOr(snippet, "Bài viết của bạn đang được lan tỏa trong cộng đồng."));
            case "social.post.mentioned.v1" -> post(eventId, receiverId, postId,
                    actorName(actorId) + " đã nhắc đến bạn trong một bài viết",
                    quoteOr(snippet, "Vào xem bài viết nhé."));
            case "social.post.tagged.v1" -> post(eventId, receiverId, postId,
                    actorName(actorId) + ("TOURNAMENT".equals(payload.path("kind").asText())
                            ? " đã khoe thành tích giải đấu của CLB và gắn thẻ bạn"
                            : " đã khoe trận đấu với bạn lên Cộng đồng"),
                    quoteOr(snippet, "Vào xem bài viết nhé."));
            case "social.comment.mentioned.v1" -> post(eventId, receiverId, postId,
                    actorName(actorId) + " đã nhắc đến bạn trong một bình luận",
                    quoteOr(snippet, "Vào xem bình luận nhé."));
            case "social.friend.requested.v1" -> friendship(eventId, receiverId, payload,
                    actorName(actorId) + " muốn kết bạn với bạn",
                    "Vào Cộng đồng › Bạn bè để chấp nhận hoặc từ chối.");
            case "social.friend.accepted.v1" -> friendship(eventId, receiverId, payload,
                    actorName(actorId) + " đã chấp nhận lời mời kết bạn",
                    "Giờ hai bạn có thể xem bài chỉ dành cho bạn bè và nhắn tin cho nhau.");
            case "social.user.followed.v1" -> {
                String name = actorName(actorId);
                yield new CreateNotificationCommand(eventId, receiverId,
                        name + " đã bắt đầu theo dõi bạn",
                        "Xem các bài viết của " + name + " trong Cộng đồng.",
                        "COMMUNITY", "SOCIAL", "COMMUNITY_USER", actorId);
            }
            default -> null;
        };
    }

    /**
     * Kiểm duyệt: không nêu tên quản trị viên. Quyết định mở trang "Kiểm duyệt nội dung" (tham chiếu là quyết định),
     * kết quả khiếu nại mở cùng trang (tham chiếu là khiếu nại).
     */
    private CreateNotificationCommand moderation(String eventType, UUID eventId, UUID receiverId, UUID aggregateId,
                                                 JsonNode payload) {
        String action = payload.path("actionType").asText("");
        String what = targetLabel(payload.path("targetType").asText(""));
        return switch (eventType) {
            case "social.moderation.decided.v1" -> {
                String title = switch (action) {
                    case "HIDE_CONTENT" -> what + " của bạn đã bị ẩn";
                    case "REMOVE_CONTENT" -> what + " của bạn đã bị gỡ";
                    case "WARNING" -> "Bạn nhận một cảnh cáo từ quản trị viên";
                    case "RESTORE_CONTENT" -> what + " của bạn đã được hiển thị lại";
                    default -> null;
                };
                if (title == null) yield null;
                String reason = payload.path("reason").asText("");
                String content = (reason.isBlank() ? "" : "Lý do: " + reason + ". ")
                        + (payload.path("appealable").asBoolean(false)
                        ? "Nếu thấy chưa đúng, bạn có thể khiếu nại trong Cài đặt › Kiểm duyệt nội dung."
                        : "Cảm ơn bạn đã đồng hành cùng cộng đồng GOAT Sports.");
                yield new CreateNotificationCommand(eventId, receiverId, title, content,
                        "CONTENT_MODERATION", "SOCIAL", "CONTENT_REPORT", aggregateId);
            }
            case "social.moderation.appeal-reviewed.v1" -> {
                boolean approved = "APPROVED".equals(payload.path("decision").asText(""));
                String content = !approved
                        ? "Quản trị viên giữ nguyên quyết định kiểm duyệt sau khi xem xét."
                        : "WARNING".equals(action) ? "Cảnh cáo đã được rút lại." : what + " đã được hiển thị lại.";
                yield new CreateNotificationCommand(eventId, receiverId,
                        approved ? "Khiếu nại của bạn đã được chấp nhận" : "Khiếu nại của bạn không được chấp nhận",
                        content, "CONTENT_MODERATION", "SOCIAL", "CONTENT_APPEAL", aggregateId);
            }
            default -> null;
        };
    }

    private static String targetLabel(String targetType) {
        return switch (targetType) {
            case "COMMENT" -> "Bình luận";
            case "MESSAGE" -> "Tin nhắn";
            case "USER" -> "Tài khoản";
            default -> "Bài viết";
        };
    }

    private CreateNotificationCommand friendship(UUID eventId, UUID receiverId, JsonNode payload, String title,
                                                 String content) {
        return new CreateNotificationCommand(eventId, receiverId, title, content,
                "FRIENDSHIP", "SOCIAL", "FRIENDSHIP", uuid(payload, "friendshipId"));
    }

    private CreateNotificationCommand post(UUID eventId, UUID receiverId, UUID postId, String title, String content) {
        return new CreateNotificationCommand(eventId, receiverId, title, content,
                "COMMUNITY", "SOCIAL", "POST", postId);
    }

    /** Thong bao van phai toi khi auth-service cham: loi tra cuu chi lam mat ten, khong lam mat thong bao. */
    private String actorName(UUID actorId) {
        try {
            AuthClient.UserView user = authClient.getUserById(actorId).data();
            if (user == null) return FALLBACK_NAME;
            if (user.fullName() != null && !user.fullName().isBlank()) return user.fullName();
            return user.username() == null || user.username().isBlank() ? FALLBACK_NAME : user.username();
        } catch (Exception exception) {
            log.warn("Không tra được tên người dùng {}: {}", actorId, exception.getMessage());
            return FALLBACK_NAME;
        }
    }

    private static String quoteOr(String snippet, String fallback) {
        return snippet == null || snippet.isBlank() ? fallback : "“" + snippet + "”";
    }

    private static UUID uuid(JsonNode node, String field) {
        String raw = node.path(field).asText(null);
        if (raw == null || raw.isBlank() || "null".equals(raw)) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}

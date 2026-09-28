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
            "club.ownership.transferred.v1",
            "tournament.team.invited.v1",
            "tournament.team.answered.v1",
            "tournament.registration.payment-due.v1",
            "tournament.registration.confirmed.v1",
            "tournament.registration.closed.v1",
            "tournament.match.rated.v1");

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
            String tournamentName = text(payload, "tournamentName", "giải đấu");
            String teamName = text(payload, "teamName", "đội của bạn");
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

                // Giai dau: nguoi nhan mo trang giai (/tournaments/{id}) de xac nhan, dong phi hoac xem ly do.
                case "tournament.team.invited.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Lời mời vào đội " + teamName,
                        "Bạn được mời vào đội " + teamName + " dự " + tournamentName
                                + ". Vào trang giải để nhận lời hoặc từ chối trước hạn đăng ký.",
                        "TOURNAMENT", "CLUB", "TOURNAMENT", uuid(payload, "tournamentId"));

                case "tournament.team.answered.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "true".equals(payload.path("accepted").asText()) ? "Đồng đội đã nhận lời" : "Đồng đội đã từ chối",
                        ("true".equals(payload.path("accepted").asText())
                                ? "Một người bạn mời đã vào đội " : "Một người bạn mời đã từ chối vào đội ")
                                + teamName + " (" + tournamentName + ").",
                        "TOURNAMENT", "CLUB", "TOURNAMENT", uuid(payload, "tournamentId"));

                case "tournament.registration.payment-due.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Đóng lệ phí để giữ suất",
                        "Đăng ký " + tournamentName + " đã đủ điều kiện. Hãy đóng "
                                + text(payload, "amount", "lệ phí") + " trước " + text(payload, "deadline", "hạn")
                                + ", quá hạn suất sẽ được trả lại.",
                        "TOURNAMENT", "CLUB", "TOURNAMENT", uuid(payload, "tournamentId"));

                case "tournament.registration.confirmed.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Đăng ký giải đã được xác nhận",
                        teamName + " đã chính thức có suất tại " + tournamentName + ".",
                        "TOURNAMENT", "CLUB", "TOURNAMENT", uuid(payload, "tournamentId"));

                case "tournament.registration.closed.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        "Đăng ký " + tournamentName + " đã đóng",
                        text(payload, "reason", "Đăng ký của bạn không còn hiệu lực."),
                        "TOURNAMENT", "CLUB", "TOURNAMENT", uuid(payload, "tournamentId"));

                // Mot tran giai co ket qua: moi nguoi thi dau biet minh duoc cong/tru bao nhieu ELO.
                case "tournament.match.rated.v1" -> new CreateNotificationCommand(
                        eventId, receiverId,
                        matchRatedTitle(payload),
                        text(payload, "matchTitle", tournamentName) + " · gặp " + text(payload, "opponentName", "đối thủ")
                                + ". ELO " + text(payload, "oldElo", "?") + " → " + text(payload, "newElo", "?") + ".",
                        "TOURNAMENT", "CLUB", "TOURNAMENT", uuid(payload, "tournamentId"));

                default -> throw new IllegalStateException("Loại sự kiện chưa hỗ trợ: " + eventType);
            });
        } catch (Exception exception) {
            log.error("Không xử lý được club event cho notification: {}", exception.getMessage(), exception);
        }
    }

    /** "Thắng 21–15 · +24 ELO" / "Thua 15–21 · −18 ELO" / "Hòa 18–18 · +0 ELO". */
    static String matchRatedTitle(JsonNode payload) {
        String outcome = switch (payload.path("outcome").asText("")) {
            case "WIN" -> "Thắng";
            case "LOSS" -> "Thua";
            default -> "Hòa";
        };
        int delta = payload.path("delta").asInt(0);
        String change = delta < 0 ? "−" + Math.abs(delta) : "+" + delta;
        String score = payload.path("score").asText("");
        return outcome + (score.isBlank() ? "" : " " + score) + " · " + change + " ELO";
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

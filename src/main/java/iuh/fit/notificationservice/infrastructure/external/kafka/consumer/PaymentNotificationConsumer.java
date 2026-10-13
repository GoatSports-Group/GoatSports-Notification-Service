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

import java.text.NumberFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * PAY-07: biến payment/refund event thành notification inbox.
 * Dedupe bằng eventId (NOT-03) để consumer retry không sinh thông báo trùng.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "payment.events",
        name = "notification-consumer-enabled",
        havingValue = "true"
)
public class PaymentNotificationConsumer {
    static final String REFUND_REMINDER = "reminder.refund-pending.v1";
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;

    @KafkaListener(
            topics = "${payment.events.topic}",
            groupId = "${payment.events.notification-group-id}",
            containerFactory = "stringListenerContainerFactory"
    )
    public void consume(String message) {
        try {
            JsonNode envelope = objectMapper.readTree(message);
            String eventType = envelope.path("eventType").asText();
            UUID eventId = uuid(envelope, "eventId");
            if (eventId == null) {
                log.warn("Bỏ qua payment event không có eventId: {}", eventType);
                return;
            }
            if (notificationRepository.existsByEventId(eventId)) {
                log.debug("Bỏ qua notification trùng eventId {}", eventId);
                return;
            }

            JsonNode payload = envelope.path("payload");
            NotificationContent content = REFUND_REMINDER.equals(eventType)
                    ? reminderContent(payload)
                    : contentFor(eventType);
            if (content == null) return;

            UUID receiverId = uuid(payload, "payerId");
            if (receiverId == null) {
                log.warn("Payment event {} không có payerId, bỏ qua", eventType);
                return;
            }

            notificationService.createNotification(new CreateNotificationCommand(
                    eventId,
                    receiverId,
                    content.title(),
                    content.body(),
                    content.type(),
                    "PAYMENT",
                    content.referenceType(),
                    // Sự kiện hoàn tiền không có referenceId: trỏ về chính khoản hoàn để người chơi mở đúng mục.
                    eventType.startsWith("refund.") || REFUND_REMINDER.equals(eventType)
                            ? uuid(payload, "refundId") : uuid(payload, "referenceId")
            ));
        } catch (Exception exception) {
            log.error("Không xử lý được payment event cho notification: {}", exception.getMessage(), exception);
        }
    }

    private NotificationContent contentFor(String eventType) {
        return switch (eventType) {
            case "payment.succeeded.v1" -> new NotificationContent(
                    "Thanh toán thành công",
                    "Giao dịch của bạn đã được xác nhận.",
                    "PAYMENT", "PAYMENT");
            case "payment.failed.v1" -> new NotificationContent(
                    "Thanh toán thất bại",
                    "Giao dịch không thành công. Vui lòng thử lại.",
                    "PAYMENT", "PAYMENT");
            case "payment.expired.v1" -> new NotificationContent(
                    "Phiên thanh toán hết hạn",
                    "Bạn chưa hoàn tất thanh toán trong thời gian giữ chỗ.",
                    "PAYMENT", "PAYMENT");
            case "payment.cancelled.v1" -> new NotificationContent(
                    "Thanh toán đã huỷ",
                    "Giao dịch đã được huỷ.",
                    "PAYMENT", "PAYMENT");
            case "refund.succeeded.v1" -> new NotificationContent(
                    "Hoàn tiền thành công",
                    "Tiền hoàn đã được chuyển về tài khoản ngân hàng của bạn.",
                    "REFUND", "REFUND");
            case "refund.failed.v1" -> new NotificationContent(
                    "Chuyển tiền hoàn chưa thành công",
                    "Vào Cài đặt › Ngân hàng & hoàn tiền để kiểm tra tài khoản và thử lại.",
                    "REFUND", "REFUND");
            case "refund.awaiting-bank-account.v1" -> new NotificationContent(
                    "Liên kết ngân hàng để nhận hoàn tiền",
                    "Bạn có một khoản hoàn tiền đang chờ. Liên kết tài khoản ngân hàng, tiền sẽ được chuyển tự động.",
                    "REFUND", "REFUND");
            case "refund.manual-review.v1" -> new NotificationContent(
                    "Hoàn tiền đang được rà soát",
                    "Yêu cầu hoàn tiền của bạn đang được kiểm tra thủ công.",
                    "REFUND", "REFUND");
            default -> null;
        };
    }

    private NotificationContent reminderContent(JsonNode payload) {
        String amount = NumberFormat.getIntegerInstance(VI).format(payload.path("amount").asDouble()) + " đ";
        return switch (payload.path("status").asText()) {
            case "AWAITING_CLAIM" -> new NotificationContent(
                    "Bạn còn khoản hoàn tiền chưa nhận",
                    "Khoản hoàn " + amount + " đang chờ bạn. Vào Cài đặt › Ngân hàng & hoàn tiền và bấm Nhận hoàn tiền.",
                    "REFUND", "REFUND");
            case "AWAITING_BANK_ACCOUNT" -> new NotificationContent(
                    "Liên kết ngân hàng để nhận " + amount,
                    "Khoản hoàn " + amount + " vẫn đang chờ. Liên kết tài khoản ngân hàng, tiền sẽ được chuyển tự động.",
                    "REFUND", "REFUND");
            case "FAILED" -> new NotificationContent(
                    "Khoản hoàn " + amount + " chưa chuyển được",
                    "Kiểm tra tài khoản ngân hàng trong Cài đặt › Ngân hàng & hoàn tiền rồi bấm Thử lại.",
                    "REFUND", "REFUND");
            default -> null;
        };
    }

    private UUID uuid(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) return null;
        try {
            return UUID.fromString(value.asText());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record NotificationContent(String title, String body, String type, String referenceType) {}
}

package iuh.fit.notificationservice.application.dto.request;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lấy bù thông báo sau mốc (createdAt, notificationId). {@code afterId} null nghĩa là lần gọi đầu sau khi
 * kết nối lại: {@code after} là thông báo mới nhất client đang có.
 */
public record GetNotificationsSinceCommand(
    UUID receiverId,
    LocalDateTime after,
    UUID afterId,
    int limit
) {}

package iuh.fit.notificationservice.application.dto.request.broadcast;

import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.domain.entity.Broadcast;
import iuh.fit.notificationservice.domain.enums.BroadcastAudience;

import java.util.UUID;

public record SendBroadcastCommand(String title, String content, String audience, String idempotencyKey, UUID sentBy) {
    public static final int IDEMPOTENCY_KEY_MAX = 100;

    public BroadcastAudience validate() throws ServiceException {
        if (sentBy == null) throw new ServiceException("Bạn phải đăng nhập để gửi thông báo.");
        if (idempotencyKey == null || idempotencyKey.isBlank() || idempotencyKey.length() > IDEMPOTENCY_KEY_MAX) {
            throw new ServiceException("Thiếu hoặc sai Idempotency-Key.");
        }
        String trimmedTitle = title == null ? "" : title.trim();
        String trimmedContent = content == null ? "" : content.trim();
        if (trimmedTitle.isEmpty() || trimmedTitle.length() > Broadcast.TITLE_MAX) {
            throw new ServiceException("Tiêu đề từ 1 đến " + Broadcast.TITLE_MAX + " ký tự.");
        }
        if (trimmedContent.isEmpty() || trimmedContent.length() > Broadcast.CONTENT_MAX) {
            throw new ServiceException("Nội dung từ 1 đến " + Broadcast.CONTENT_MAX + " ký tự.");
        }
        return parseAudience(audience);
    }

    public static BroadcastAudience parseAudience(String value) throws ServiceException {
        try {
            return BroadcastAudience.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ServiceException("Đối tượng nhận không hợp lệ: " + value);
        }
    }
}

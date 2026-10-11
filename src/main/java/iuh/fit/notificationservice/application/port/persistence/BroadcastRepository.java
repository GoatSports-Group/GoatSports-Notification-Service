package iuh.fit.notificationservice.application.port.persistence;

import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.domain.entity.Broadcast;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BroadcastRepository {
    Broadcast create(Broadcast broadcast);

    Optional<Broadcast> findById(UUID broadcastId);

    Optional<Broadcast> findByIdempotencyKey(String idempotencyKey);

    PageResult<Broadcast> findPage(int page, int size);

    /**
     * Giành quyền gửi (cập nhật có điều kiện, an toàn khi chạy nhiều instance): thành công khi thông báo còn
     * SENDING và chưa ai giữ, hoặc người giữ đã im lặng từ trước {@code staleBefore}. Tăng attemptCount.
     */
    boolean claim(UUID broadcastId, LocalDateTime now, LocalDateTime staleBefore);

    /** Ghi mốc sau mỗi lô và gia hạn quyền gửi. */
    void checkpoint(UUID broadcastId, UUID lastRecipientId, int deliveredCount, LocalDateTime now);

    void markSent(UUID broadcastId, LocalDateTime completedAt);

    void markFailed(UUID broadcastId, String reason, LocalDateTime completedAt);

    /** Thả quyền gửi sau một lần lỗi để lần quét kế tiếp thử lại. */
    void release(UUID broadcastId, String reason);

    List<UUID> findStalled(LocalDateTime staleBefore);
}

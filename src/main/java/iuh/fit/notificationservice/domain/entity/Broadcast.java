package iuh.fit.notificationservice.domain.entity;

import iuh.fit.notificationservice.domain.enums.BroadcastAudience;
import iuh.fit.notificationservice.domain.enums.BroadcastStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Thông báo hệ thống admin gửi tới một nhóm người dùng. Mỗi người nhận một bản ghi Notification riêng
 * (đọc/xóa độc lập); gửi theo lô, lưu mốc người nhận cuối cùng để chạy tiếp được sau khi service khởi động lại.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Broadcast extends BaseEntity {
    public static final int TITLE_MAX = 150;
    public static final int CONTENT_MAX = 2000;

    private UUID broadcastId;
    private String title;
    private String content;
    private BroadcastAudience audience;
    private BroadcastStatus status;
    private UUID sentBy;
    private String idempotencyKey;
    private int deliveredCount;
    private UUID lastRecipientId;
    private int attemptCount;
    private LocalDateTime claimedAt;
    private LocalDateTime completedAt;
    private String failureReason;

    /**
     * Mỗi (thông báo hệ thống, người nhận) chỉ sinh đúng một Notification, kể cả khi một lô bị gửi lại
     * sau sự cố: dùng làm event_id (đã unique) của bảng notifications.
     */
    public UUID notificationEventId(UUID receiverId) {
        return UUID.nameUUIDFromBytes(("broadcast:" + broadcastId + ":" + receiverId).getBytes(StandardCharsets.UTF_8));
    }
}

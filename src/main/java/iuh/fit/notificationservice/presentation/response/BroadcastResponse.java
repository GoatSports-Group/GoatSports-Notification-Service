package iuh.fit.notificationservice.presentation.response;

import iuh.fit.notificationservice.domain.entity.Broadcast;
import iuh.fit.notificationservice.domain.enums.BroadcastAudience;
import iuh.fit.notificationservice.domain.enums.BroadcastStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record BroadcastResponse(
        UUID broadcastId,
        String title,
        String content,
        BroadcastAudience audience,
        BroadcastStatus status,
        UUID sentBy,
        int deliveredCount,
        String failureReason,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {
    public static BroadcastResponse from(Broadcast broadcast) {
        return new BroadcastResponse(broadcast.getBroadcastId(), broadcast.getTitle(), broadcast.getContent(),
                broadcast.getAudience(), broadcast.getStatus(), broadcast.getSentBy(), broadcast.getDeliveredCount(),
                broadcast.getFailureReason(), broadcast.getCreatedAt(), broadcast.getCompletedAt());
    }
}

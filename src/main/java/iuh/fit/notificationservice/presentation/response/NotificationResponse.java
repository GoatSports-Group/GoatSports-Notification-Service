package iuh.fit.notificationservice.presentation.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationResponse {
    private UUID notificationId;
    private UUID receiverId;
    private String title;
    private String content;
    private String type;
    private String status;
    private String sourceService;
    private String referenceType;
    private UUID referenceId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}

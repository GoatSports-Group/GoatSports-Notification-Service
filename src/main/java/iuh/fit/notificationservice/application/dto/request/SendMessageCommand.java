package iuh.fit.notificationservice.application.dto.request;

import iuh.fit.notificationservice.domain.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageCommand {
    private UUID roomId;
    private UUID senderId;
    private String senderName;
    private String senderAvatar;
    private String content;
    private MessageType type;
    private String metadata;
}

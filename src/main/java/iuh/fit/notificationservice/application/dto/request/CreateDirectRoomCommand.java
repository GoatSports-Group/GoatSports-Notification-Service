package iuh.fit.notificationservice.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDirectRoomCommand {
    private UUID currentUserId;
    private UUID targetUserId;
    private String targetUserName;
    private String targetUserAvatar;
}

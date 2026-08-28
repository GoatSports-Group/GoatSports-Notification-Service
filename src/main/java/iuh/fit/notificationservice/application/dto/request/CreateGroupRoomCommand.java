package iuh.fit.notificationservice.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGroupRoomCommand {
    private String name;
    private String avatarUrl;
    private UUID creatorId;
    private List<UUID> participantIds;
}

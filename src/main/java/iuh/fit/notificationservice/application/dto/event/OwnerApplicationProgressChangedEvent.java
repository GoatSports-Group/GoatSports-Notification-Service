package iuh.fit.notificationservice.application.dto.event;

import java.util.UUID;

public record OwnerApplicationProgressChangedEvent(
        String type,
        UUID userId,
        UUID ownerApplicationId
) { }

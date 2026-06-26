package iuh.fit.notificationservice.presentation.converter;

import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.presentation.response.NotificationResponse;

public class NotificationConverter {

    public static NotificationResponse toResponse(NotificationResponseCommand command) {
        if (command == null) return null;

        return NotificationResponse.builder()
                .notificationId(command.notificationId())
                .receiverId(command.receiverId())
                .title(command.title())
                .content(command.content())
                .type(command.type())
                .status(command.status())
                .referenceId(command.referenceId())
                .createdAt(command.createdAt())
                .readAt(command.readAt())
                .build();
    }
}

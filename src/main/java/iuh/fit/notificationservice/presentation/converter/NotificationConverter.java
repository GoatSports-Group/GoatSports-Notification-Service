package iuh.fit.notificationservice.presentation.converter;

import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.presentation.response.NotificationResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationConverter {

    NotificationResponse toResponse(NotificationResponseCommand command);

    NotificationResponseCommand toResponseCommand(NotificationResponse response);
}

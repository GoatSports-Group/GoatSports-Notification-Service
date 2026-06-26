package iuh.fit.notificationservice.application.dto.request;

import iuh.fit.notificationservice.application.dto.PageFilter;
import java.util.UUID;

public record GetNotificationsCommand(
    PageFilter filter,
    UUID receiverId
) {}

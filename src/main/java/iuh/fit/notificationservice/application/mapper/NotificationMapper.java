package iuh.fit.notificationservice.application.mapper;

import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationSummaryResponseCommand;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.domain.entity.Notification;

public class NotificationMapper {

    public static NotificationResponseCommand toNotificationResponseCommand(Notification notification) {
        if (notification == null) return null;

        return new NotificationResponseCommand(
                notification.getNotificationId(),
                notification.getReceiverId(),
                notification.getTitle(),
                notification.getContent(),
                notification.getType().name(),
                notification.getStatus().name(),
                notification.getReferenceId(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    public static NotificationSummaryResponseCommand toNotificationSummaryResponseCommand(Notification notification) {
        if (notification == null) return null;

        return new NotificationSummaryResponseCommand(
                notification.getNotificationId(),
                notification.getTitle(),
                notification.getType().name(),
                notification.getStatus().name(),
                notification.getCreatedAt()
        );
    }

    public static PageResult<NotificationResponseCommand> toNotificationResponseCommand(PageResult<Notification> pageResult) {
        if (pageResult == null) return null;

        return new PageResult<>(
                pageResult.items().stream().map(NotificationMapper::toNotificationResponseCommand).toList(),
                pageResult.total(),
                pageResult.page(),
                pageResult.pageSize(),
                pageResult.totalPages()
        );
    }
}

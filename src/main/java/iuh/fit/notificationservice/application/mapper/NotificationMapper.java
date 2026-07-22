package iuh.fit.notificationservice.application.mapper;

import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationSummaryResponseCommand;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.domain.entity.Notification;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponseCommand toNotificationResponseCommand(Notification notification);

    NotificationSummaryResponseCommand toNotificationSummaryResponseCommand(Notification notification);

    List<NotificationResponseCommand> toNotificationResponseCommandList(List<Notification> notifications);

    default PageResult<NotificationResponseCommand> toNotificationResponseCommand(PageResult<Notification> pageResult) {
        if (pageResult == null) return null;

        return new PageResult<>(
                toNotificationResponseCommandList(pageResult.items()),
                pageResult.total(),
                pageResult.page(),
                pageResult.pageSize(),
                pageResult.totalPages()
        );
    }
}

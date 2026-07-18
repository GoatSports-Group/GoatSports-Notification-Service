package iuh.fit.notificationservice.presentation.controller.internal;

import iuh.fit.notificationservice.application.dto.event.OwnerApplicationEvent;
import iuh.fit.notificationservice.application.dto.event.type.OwnerApplicationEventType;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.mapper.NotificationMapper;
import iuh.fit.notificationservice.application.port.security.CurrentUserProvider;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.application.util.BaseUtil;
import iuh.fit.notificationservice.presentation.converter.NotificationConverter;
import iuh.fit.notificationservice.presentation.response.NotificationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class NotificationInternalController {
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/owner-applications")
    public ResponseEntity<NotificationResponse> createOwnerApplicationNotification(
            @RequestBody OwnerApplicationEvent event
    ) throws ServiceException
    {
        CreateNotificationCommand command = new CreateNotificationCommand(
            event.getEventType() == OwnerApplicationEventType.SUBMITTED
                    ? currentUserProvider.getAdminId() : event.getData().getUserId(),

            BaseUtil.getTitleForOwnerApplication(event.getEventType()),

            event.getEventType() == OwnerApplicationEventType.REJECTED
                    ? BaseUtil.getContentForOwnerApplication(event.getEventType())
                    + " Lý do: " + event.getData().getRejectReason()
                    :  BaseUtil.getContentForOwnerApplication(event.getEventType()),

            "OWNER_APPLICATION",

            event.getEventType() == OwnerApplicationEventType.SUBMITTED
                    ? event.getData().getUserId() : currentUserProvider.getAdminId()
        );

        NotificationResponseCommand responseCommand = NotificationMapper.toNotificationResponseCommand(
                notificationService.createNotification(command)
        );

        NotificationResponse response = NotificationConverter.toResponse(responseCommand);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> notifyUser(@RequestBody NotificationResponse notification) {
        notificationService.notifyUser(NotificationConverter.toResponseCommand(notification));

        return ResponseEntity.ok().build();
    }
}
package iuh.fit.notificationservice.presentation.controller.internal;

import iuh.fit.notificationservice.application.dto.event.OwnerApplicationEvent;
import iuh.fit.notificationservice.application.dto.event.OwnerApplicationProgressChangedEvent;
import iuh.fit.notificationservice.application.dto.event.type.OwnerApplicationEventType;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.mapper.NotificationMapper;
import iuh.fit.notificationservice.application.port.security.CurrentUserProvider;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.application.util.BaseUtil;
import iuh.fit.notificationservice.domain.entity.Notification;
import iuh.fit.notificationservice.presentation.converter.NotificationConverter;
import iuh.fit.notificationservice.presentation.response.NotificationResponse;
import iuh.fit.notificationservice.presentation.request.CreateNotificationRequest;
import iuh.fit.shared.annotation.AuditLog;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/internal/notifications")
@RequiredArgsConstructor
public class NotificationInternalController {
    private final NotificationService notificationService;
    private final CurrentUserProvider currentUserProvider;
    private final NotificationMapper notificationMapper;
    private final NotificationConverter notificationConverter;

    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(
            @Valid @RequestBody CreateNotificationRequest request
    ) throws ServiceException {
        CreateNotificationCommand command = new CreateNotificationCommand(
                request.eventId(),
                request.receiverId(),
                request.title(),
                request.content(),
                request.type(),
                request.sourceService(),
                request.referenceType(),
                request.referenceId()
        );
        Notification created = notificationService.createNotification(command);
        if (created == null) return ResponseEntity.ok().build(); // eventId đã xử lý trước đó
        NotificationResponseCommand result = notificationMapper.toNotificationResponseCommand(created);
        return ResponseEntity.status(HttpStatus.CREATED).body(notificationConverter.toResponse(result));
    }

    /**
     * Cảnh báo vận hành gửi admin nền tảng (audit-service: dịch vụ ngừng, đăng nhập thất bại liên tục). Người gửi không
     * cần biết mã admin; cùng eventId chỉ tạo một thông báo.
     */
    // ponytail: gửi admin chính (authClient.getAdmin()); nhiều admin thì cần API "mọi admin" ở auth-service.
    @PostMapping("/admin")
    public ResponseEntity<Void> notifyAdmin(@Valid @RequestBody AdminAlertRequest request) throws ServiceException {
        notificationService.createNotification(new CreateNotificationCommand(
                request.eventId(), currentUserProvider.getAdminId(), request.title(), request.content(),
                "SYSTEM", request.sourceService(), request.referenceType(), request.referenceId()));
        return ResponseEntity.ok().build();
    }

    public record AdminAlertRequest(
            UUID eventId,
            @NotBlank String title,
            @NotBlank String content,
            @NotBlank String sourceService,
            /** SYSTEM_ALERT (sự cố dịch vụ) hoặc SECURITY_ALERT (đăng nhập thất bại liên tục). */
            @Pattern(regexp = "SYSTEM_ALERT|SECURITY_ALERT") String referenceType,
            @NotNull UUID referenceId
    ) {
    }

    @PostMapping("/owner-applications")
    @AuditLog(action = "CREATE_NOTIFICATION", description = "Tạo thông báo tới người dùng #{#event.data.userId}")
    public ResponseEntity<NotificationResponse> createOwnerApplicationNotification(
            @RequestBody OwnerApplicationEvent event
    ) throws ServiceException
    {
        CreateNotificationCommand command = new CreateNotificationCommand(
            null,

            event.getEventType() == OwnerApplicationEventType.SUBMITTED
                    ? currentUserProvider.getAdminId() : event.getData().getUserId(),

            BaseUtil.getTitleForOwnerApplication(event.getEventType()),

            event.getEventType() == OwnerApplicationEventType.REJECTED
                    ? BaseUtil.getContentForOwnerApplication(event.getEventType())
                        + " Lý do: " + event.getData().getRejectReason()
                    :  BaseUtil.getContentForOwnerApplication(event.getEventType()),

            "OWNER_APPLICATION",
            "VENUE",
            "OWNER_APPLICATION",
            event.getData().getOwnerApplicationId()
        );

        NotificationResponseCommand responseCommand = notificationMapper.toNotificationResponseCommand(
                notificationService.createNotification(command)
        );

        NotificationResponse response = notificationConverter.toResponse(responseCommand);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/notify")
    public ResponseEntity<Void> notifyUser(@RequestBody NotificationResponse notification) {
        notificationService.notifyUser(notificationConverter.toResponseCommand(notification));

        return ResponseEntity.ok().build();
    }

    @PostMapping("/owner-application-progress")
    public ResponseEntity<Void> publish(
            @RequestBody OwnerApplicationProgressChangedEvent event
    ) {
        notificationService.publishOwnerApplicationProgress(event);
        return ResponseEntity.ok().build();
    }
}

package iuh.fit.notificationservice.presentation.controller.internal;

import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * auth-service báo phiên của một người dùng vừa bị thu hồi: đẩy qua websocket để tab đang ngồi yên cũng đăng xuất
 * ngay, không đợi tới thao tác kế tiếp mới bị gateway chặn. Lý do trùng mã lỗi gateway trả về.
 */
@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class SessionInternalController {
    private final NotificationWebSocketPublisher webSocketPublisher;

    public record SessionRevokedRequest(
            @NotNull @Pattern(regexp = "ACCOUNT_BLOCKED|ACCOUNT_DELETION_PENDING|ACCOUNT_INACTIVE|SESSION_REPLACED") String reason) {
    }

    @PostMapping("/{userId}/session-revoked")
    public ResponseEntity<Void> sessionRevoked(@PathVariable UUID userId, @Valid @RequestBody SessionRevokedRequest request) {
        webSocketPublisher.publishSessionRevoked(userId, request.reason());
        return ResponseEntity.ok().build();
    }
}

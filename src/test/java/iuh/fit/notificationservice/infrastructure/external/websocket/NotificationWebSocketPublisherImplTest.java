package iuh.fit.notificationservice.infrastructure.external.websocket;

import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.infrastructure.external.client.AuthClient;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationWebSocketPublisherImplTest {

    @Test
    void userNotificationIsStillDeliveredWhenAdminLookupFails() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AuthClient authClient = mock(AuthClient.class);
        when(authClient.getAdmin()).thenThrow(new IllegalStateException("auth service unavailable"));

        NotificationWebSocketPublisherImpl publisher = new NotificationWebSocketPublisherImpl(
                messagingTemplate,
                authClient
        );
        UUID receiverId = UUID.randomUUID();
        NotificationResponseCommand notification = notification(receiverId);

        publisher.publish(notification);

        verify(messagingTemplate).convertAndSend(
                "/topic/user/notifications/" + receiverId,
                notification
        );
    }

    @Test
    void adminNotificationUsesAdminTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        AuthClient authClient = mock(AuthClient.class);
        UUID receiverId = UUID.randomUUID();
        when(authClient.getAdmin()).thenReturn(receiverId.toString());

        NotificationWebSocketPublisherImpl publisher = new NotificationWebSocketPublisherImpl(
                messagingTemplate,
                authClient
        );
        NotificationResponseCommand notification = notification(receiverId);

        publisher.publish(notification);

        verify(messagingTemplate).convertAndSend("/topic/admin/notifications", notification);
    }

    /** Tài khoản bị khóa: tab đang mở của chính người đó nhận lệnh đăng xuất trên topic phiên riêng. */
    @Test
    void sessionRevocationGoesToTheUsersSessionTopic() {
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        NotificationWebSocketPublisherImpl publisher = new NotificationWebSocketPublisherImpl(
                messagingTemplate,
                mock(AuthClient.class)
        );
        UUID userId = UUID.randomUUID();

        publisher.publishSessionRevoked(userId, "ACCOUNT_BLOCKED");

        verify(messagingTemplate).convertAndSend("/topic/user/session/" + userId,
                (Object) java.util.Map.of("type", "SESSION_REVOKED", "reason", "ACCOUNT_BLOCKED"));
    }

    private static NotificationResponseCommand notification(UUID receiverId) {
        return new NotificationResponseCommand(
                UUID.randomUUID(),
                receiverId,
                "Club membership request",
                "A player wants to join your club",
                "INFO",
                "UNREAD",
                "CLUB_SERVICE",
                "CLUB_MEMBERSHIP_REQUEST",
                UUID.randomUUID(),
                LocalDateTime.now(),
                null
        );
    }
}

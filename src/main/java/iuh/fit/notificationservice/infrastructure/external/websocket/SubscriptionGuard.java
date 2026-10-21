package iuh.fit.notificationservice.infrastructure.external.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.util.List;

/**
 * Mỗi phiên chỉ nghe được kênh mang đúng userId của mình; /topic/admin/notifications chỉ cho ADMIN; topic lạ bị từ chối.
 */
@Slf4j
public class SubscriptionGuard implements ChannelInterceptor {
    static final String ADMIN_TOPIC = "/topic/admin/notifications";
    static final List<String> PERSONAL_PREFIXES = List.of(
            "/topic/user/notifications/", "/topic/user/session/", "/topic/user/owner-application-progress/");

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || !StompCommand.SUBSCRIBE.equals(accessor.getCommand())) return message;

        String destination = accessor.getDestination();
        GatewayHandshake.UserPrincipal user =
                accessor.getUser() instanceof GatewayHandshake.UserPrincipal principal ? principal : null;
        if (destination != null && user != null && allowed(destination, user)) return message;

        log.warn("Từ chối subscribe {} cho người dùng {}", destination, user == null ? null : user.userId());
        throw new IllegalArgumentException("Bạn không thể nghe kênh này.");
    }

    static boolean allowed(String destination, GatewayHandshake.UserPrincipal user) {
        if (ADMIN_TOPIC.equals(destination)) return user.roles().contains("ADMIN");
        return PERSONAL_PREFIXES.stream()
                .anyMatch(prefix -> destination.equals(prefix + user.userId()));
    }
}

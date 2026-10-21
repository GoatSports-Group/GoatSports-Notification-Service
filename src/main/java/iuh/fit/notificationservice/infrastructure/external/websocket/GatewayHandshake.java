package iuh.fit.notificationservice.infrastructure.external.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Danh tính của phiên websocket lấy từ header gateway gắn lúc bắt tay (JwtFilter đã kiểm X-Gateway-Secret, gateway đã
 * xóa X-User-* do client tự gửi). Không có X-User-Id thì từ chối bắt tay: chỉ người đã đăng nhập mới nghe thông báo.
 */
@Slf4j
public class GatewayHandshake extends DefaultHandshakeHandler implements HandshakeInterceptor {
    private static final String USER_ATTRIBUTE = "goatsports.user";

    public record UserPrincipal(UUID userId, Set<String> roles) implements Principal {
        @Override
        public String getName() {
            return userId.toString();
        }
    }

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) return false;
        String rawId = servletRequest.getServletRequest().getHeader("X-User-Id");
        try {
            UUID userId = UUID.fromString(rawId == null ? "" : rawId.trim());
            attributes.put(USER_ATTRIBUTE,
                    new UserPrincipal(userId, roles(servletRequest.getServletRequest().getHeader("X-User-Roles"))));
            return true;
        } catch (IllegalArgumentException invalid) {
            log.warn("Từ chối bắt tay WebSocket: thiếu hoặc sai X-User-Id");
            return false;
        }
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request, @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler, Exception exception) {
        // Không cần dọn gì.
    }

    @Override
    protected Principal determineUser(@NonNull ServerHttpRequest request, @NonNull WebSocketHandler wsHandler,
                                      @NonNull Map<String, Object> attributes) {
        return attributes.get(USER_ATTRIBUTE) instanceof UserPrincipal user ? user : null;
    }

    /** "ROLE_ADMIN, VENUE_OWNER" -> {ADMIN, VENUE_OWNER}. */
    static Set<String> roles(String raw) {
        if (raw == null || raw.isBlank()) return Set.of();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(role -> role.startsWith("ROLE_") ? role.substring(5) : role)
                .collect(Collectors.toUnmodifiableSet());
    }
}

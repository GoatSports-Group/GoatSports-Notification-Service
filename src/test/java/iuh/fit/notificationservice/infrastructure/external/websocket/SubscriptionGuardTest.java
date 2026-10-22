package iuh.fit.notificationservice.infrastructure.external.websocket;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/** Biết userId của người khác không còn đủ để nghe thông báo của họ. */
class SubscriptionGuardTest {
    private final SubscriptionGuard guard = new SubscriptionGuard();
    private final MessageChannel channel = mock(MessageChannel.class);
    private final UUID me = UUID.randomUUID();

    private Message<byte[]> subscribe(String destination, GatewayHandshake.UserPrincipal user) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSubscriptionId("sub-1");
        accessor.setUser(user);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    void aUserListensOnlyToTheirOwnChannels() {
        GatewayHandshake.UserPrincipal player = new GatewayHandshake.UserPrincipal(me, Set.of("PLAYER"));

        assertThat(guard.preSend(subscribe("/topic/user/notifications/" + me, player), channel)).isNotNull();
        assertThat(guard.preSend(subscribe("/topic/user/session/" + me, player), channel)).isNotNull();
        assertThatThrownBy(() -> guard.preSend(subscribe("/topic/user/notifications/" + UUID.randomUUID(), player), channel))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> guard.preSend(subscribe("/topic/user/notifications/" + me + "/extra", player), channel))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void theAdminChannelIsForAdminsAndUnknownTopicsAreRefused() {
        GatewayHandshake.UserPrincipal player = new GatewayHandshake.UserPrincipal(me, Set.of("PLAYER"));
        GatewayHandshake.UserPrincipal admin = new GatewayHandshake.UserPrincipal(UUID.randomUUID(), GatewayHandshake.roles("ROLE_ADMIN"));

        assertThatThrownBy(() -> guard.preSend(subscribe("/topic/admin/notifications", player), channel))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(guard.preSend(subscribe("/topic/admin/notifications", admin), channel)).isNotNull();
        assertThatThrownBy(() -> guard.preSend(subscribe("/topic/anything", admin), channel))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aSessionWithoutAGatewayIdentityCannotSubscribe() {
        assertThatThrownBy(() -> guard.preSend(subscribe("/topic/user/notifications/" + me, null), channel))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

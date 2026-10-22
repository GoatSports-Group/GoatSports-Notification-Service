package iuh.fit.notificationservice.infrastructure.external.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.service.NotificationService;
import iuh.fit.notificationservice.infrastructure.external.client.AuthClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SocialNotificationConsumerTest {
    @Mock NotificationService notificationService;
    @Mock NotificationRepository notificationRepository;
    @Mock AuthClient authClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private SocialNotificationConsumer consumer() {
        return new SocialNotificationConsumer(objectMapper, notificationService, notificationRepository, authClient);
    }

    private String envelope(String eventType, UUID eventId, UUID actorId, UUID receiverId, UUID postId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "producer": "social-service",
                  "actorId": "%s",
                  "payload": { "receiverId": "%s", "postId": "%s", "snippet": "Còn thiếu 2 thủ môn" }
                }
                """.formatted(eventId, eventType, actorId, receiverId, postId);
    }

    private CreateNotificationCommand captured() throws Exception {
        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        return captor.getValue();
    }

    @Test
    void commentNotificationNamesTheActorAndLinksThePost() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(authClient.getUserById(actor)).thenReturn(
                new AuthClient.Envelope<>(new AuthClient.UserView("Trần Quốc Bảo", "bao.tq")));

        consumer().consume(envelope("social.post.commented.v1", UUID.randomUUID(), actor, UUID.randomUUID(), postId));

        CreateNotificationCommand command = captured();
        assertThat(command.title()).isEqualTo("Trần Quốc Bảo đã bình luận về bài viết của bạn");
        assertThat(command.content()).contains("Còn thiếu 2 thủ môn");
        assertThat(command.type()).isEqualTo("COMMUNITY");
        assertThat(command.referenceType()).isEqualTo("POST");
        assertThat(command.referenceId()).isEqualTo(postId);
    }

    @Test
    void followLinksTheFollowerAndSurvivesAnAuthOutage() throws Exception {
        UUID actor = UUID.randomUUID();
        when(authClient.getUserById(actor)).thenThrow(new RuntimeException("auth down"));

        consumer().consume(envelope("social.user.followed.v1", UUID.randomUUID(), actor, UUID.randomUUID(),
                UUID.randomUUID()));

        CreateNotificationCommand command = captured();
        assertThat(command.title()).startsWith("Một người chơi");
        assertThat(command.referenceType()).isEqualTo("COMMUNITY_USER");
        assertThat(command.referenceId()).isEqualTo(actor);
    }

    @Test
    void taggedInATournamentHighlightSaysWhatWasShownOff() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(authClient.getUserById(actor)).thenReturn(new AuthClient.Envelope<>(new AuthClient.UserView("Minh Đạt", null)));
        String tagged = """
                {"eventId":"%s","eventType":"social.post.tagged.v1","actorId":"%s",
                 "payload":{"receiverId":"%s","postId":"%s","snippet":"Vô địch Giải Phong Trào","kind":"TOURNAMENT"}}
                """.formatted(UUID.randomUUID(), actor, UUID.randomUUID(), postId);

        consumer().consume(tagged);

        CreateNotificationCommand command = captured();
        assertThat(command.title()).isEqualTo("Minh Đạt đã khoe thành tích giải đấu của CLB và gắn thẻ bạn");
        assertThat(command.content()).contains("Vô địch Giải Phong Trào");
        assertThat(command.referenceType()).isEqualTo("POST");
        assertThat(command.referenceId()).isEqualTo(postId);
    }

    @Test
    void duplicateEventIsIgnored() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(true);

        consumer().consume(envelope("social.post.liked.v1", eventId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID()));

        verify(notificationService, never()).createNotification(any());
    }

    @Test
    void mentionAndFriendRequestBecomeTheirOwnNotifications() throws Exception {
        UUID actor = UUID.randomUUID();
        when(authClient.getUserById(actor)).thenReturn(new AuthClient.Envelope<>(new AuthClient.UserView("Lê Thu Hà", null)));

        consumer().consume(envelope("social.post.mentioned.v1", UUID.randomUUID(), actor, UUID.randomUUID(), UUID.randomUUID()));
        String friendRequest = """
                {"eventId":"%s","eventType":"social.friend.requested.v1","actorId":"%s",
                 "payload":{"receiverId":"%s","friendshipId":"%s"}}
                """.formatted(UUID.randomUUID(), actor, UUID.randomUUID(), UUID.randomUUID());
        consumer().consume(friendRequest);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService, Mockito.times(2)).createNotification(captor.capture());
        assertThat(captor.getAllValues().get(0).title()).isEqualTo("Lê Thu Hà đã nhắc đến bạn trong một bài viết");
        assertThat(captor.getAllValues().get(0).referenceType()).isEqualTo("POST");
        assertThat(captor.getAllValues().get(1).type()).isEqualTo("FRIENDSHIP");
        assertThat(captor.getAllValues().get(1).referenceType()).isEqualTo("FRIENDSHIP");
    }

    @Test
    void commentMentionLinksThePost() throws Exception {
        UUID actor = UUID.randomUUID();
        UUID postId = UUID.randomUUID();
        when(authClient.getUserById(actor)).thenReturn(new AuthClient.Envelope<>(new AuthClient.UserView("Phạm Khoa", null)));

        consumer().consume(envelope("social.comment.mentioned.v1", UUID.randomUUID(), actor, UUID.randomUUID(), postId));

        CreateNotificationCommand command = captured();
        assertThat(command.title()).isEqualTo("Phạm Khoa đã nhắc đến bạn trong một bình luận");
        assertThat(command.referenceId()).isEqualTo(postId);
    }
}

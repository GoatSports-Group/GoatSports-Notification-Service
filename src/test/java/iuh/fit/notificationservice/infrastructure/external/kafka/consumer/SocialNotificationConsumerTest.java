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
        when(authClient.getUserById(actor)).thenReturn(objectMapper.readTree(
                "{\"data\":{\"fullName\":\"Trần Quốc Bảo\",\"username\":\"bao.tq\"}}"));

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
    void duplicateEventIsIgnored() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(true);

        consumer().consume(envelope("social.post.liked.v1", eventId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID()));

        verify(notificationService, never()).createNotification(any());
    }
}

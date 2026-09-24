package iuh.fit.notificationservice.infrastructure.external.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubNotificationConsumerTest {
    @Mock NotificationService notificationService;
    @Mock NotificationRepository notificationRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ClubNotificationConsumer consumer() {
        return new ClubNotificationConsumer(objectMapper, notificationService, notificationRepository);
    }

    private String envelope(String eventType, UUID eventId, UUID receiverId, UUID clubId) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "producer": "club-service",
                  "payload": {
                    "receiverId": "%s",
                    "clubId": "%s",
                    "clubName": "GOAT Weekend Club"
                  }
                }
                """.formatted(eventId, eventType, receiverId, clubId);
    }

    @Test
    void membershipRequestRoutesOwnerToManagerWorkspace() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(envelope("club.membership.requested.v1", eventId, UUID.randomUUID(), clubId));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().referenceType()).isEqualTo("CLUB_MEMBERSHIP_REQUEST");
        assertThat(captor.getValue().referenceId()).isEqualTo(clubId);
    }

    @Test
    void approvedRequestRoutesPlayerToTheirClubhouse() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(envelope("club.membership.approved.v1", eventId, UUID.randomUUID(), clubId));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().referenceType()).isEqualTo("CLUB");
        assertThat(captor.getValue().referenceId()).isEqualTo(clubId);
    }

    @Test
    void rejectedRequestRoutesPlayerToPublicClubDetails() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(envelope("club.membership.rejected.v1", eventId, UUID.randomUUID(), clubId));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().referenceType()).isEqualTo("CLUB_PUBLIC");
        assertThat(captor.getValue().referenceId()).isEqualTo(clubId);
    }

    @Test
    void consumesLegacyDoubleEncodedClubEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);
        String doubleEncoded = objectMapper.writeValueAsString(
                envelope("club.membership.requested.v1", eventId, receiverId, clubId)
        );

        consumer().consume(doubleEncoded);

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().receiverId()).isEqualTo(receiverId);
        assertThat(captor.getValue().referenceType()).isEqualTo("CLUB_MEMBERSHIP_REQUEST");
    }

    @Test
    void disbandedClubNotifiesMembersAndPointsAtClubLanding() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(envelope("club.disbanded.v1", eventId, UUID.randomUUID(), clubId));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().referenceType()).isEqualTo("CLUB_DISBANDED");
        assertThat(captor.getValue().referenceId()).isEqualTo(clubId);
        assertThat(captor.getValue().title()).contains("đã giải tán");
    }

    @Test
    void ownershipTransferNotifiesTheNewOwner() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume(envelope("club.ownership.transferred.v1", eventId, UUID.randomUUID(), clubId));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().referenceType()).isEqualTo("CLUB");
        assertThat(captor.getValue().title()).contains("chủ mới");
    }

    @Test
    void teamInvitationRoutesInviteeToTournamentPage() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID tournamentId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume("""
                {"eventId": "%s", "eventType": "tournament.team.invited.v1", "producer": "club-service",
                 "payload": {"receiverId": "%s", "tournamentId": "%s", "tournamentName": "GOAT Cup",
                             "teamName": "Đôi Sấm Sét"}}
                """.formatted(eventId, receiverId, tournamentId));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("TOURNAMENT");
        assertThat(captor.getValue().referenceType()).isEqualTo("TOURNAMENT");
        assertThat(captor.getValue().referenceId()).isEqualTo(tournamentId);
        assertThat(captor.getValue().receiverId()).isEqualTo(receiverId);
        assertThat(captor.getValue().title()).contains("Đôi Sấm Sét");
    }

    @Test
    void paymentDueTellsAmountAndDeadline() throws Exception {
        UUID eventId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(false);

        consumer().consume("""
                {"eventId": "%s", "eventType": "tournament.registration.payment-due.v1", "producer": "club-service",
                 "payload": {"receiverId": "%s", "tournamentId": "%s", "tournamentName": "GOAT Cup",
                             "amount": "150.000 đ", "deadline": "20:00 25/09/2026"}}
                """.formatted(eventId, UUID.randomUUID(), UUID.randomUUID()));

        ArgumentCaptor<CreateNotificationCommand> captor = ArgumentCaptor.forClass(CreateNotificationCommand.class);
        verify(notificationService).createNotification(captor.capture());
        assertThat(captor.getValue().content()).contains("150.000 đ").contains("20:00 25/09/2026");
    }
}

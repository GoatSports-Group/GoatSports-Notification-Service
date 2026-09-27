package iuh.fit.notificationservice.application.service.impl;

import iuh.fit.notificationservice.application.dto.request.CreateNotificationCommand;
import iuh.fit.notificationservice.application.dto.response.notification.NotificationResponseCommand;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.mapper.NotificationMapper;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import iuh.fit.notificationservice.domain.entity.Notification;
import iuh.fit.notificationservice.domain.enums.NotificationReferenceType;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationWebSocketPublisher webSocketPublisher;
    @Mock NotificationMapper notificationMapper;

    @Test
    void clubDisbandedNotificationIsPersistedAndPublishedRealtime() throws ServiceException {
        UUID eventId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        UUID clubId = UUID.randomUUID();
        CreateNotificationCommand command = new CreateNotificationCommand(
                eventId,
                receiverId,
                "Câu lạc bộ đã giải tán",
                "Bạn không còn là thành viên của câu lạc bộ này.",
                "CLUB",
                "CLUB",
                "CLUB_DISBANDED",
                clubId);
        NotificationResponseCommand response = new NotificationResponseCommand(
                UUID.randomUUID(), receiverId, command.title(), command.content(),
                "CLUB", "UNREAD", "CLUB", "CLUB_DISBANDED", clubId, null, null);

        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(notificationMapper.toNotificationResponseCommand(any(Notification.class)))
                .thenReturn(response);

        Notification saved = service().createNotification(command);

        assertThat(saved.getType()).isEqualTo(NotificationType.CLUB);
        assertThat(saved.getReferenceType()).isEqualTo(NotificationReferenceType.CLUB_DISBANDED);
        assertThat(saved.getReceiverId()).isEqualTo(receiverId);
        assertThat(saved.getReferenceId()).isEqualTo(clubId);
        verify(notificationRepository).save(saved);
        verify(webSocketPublisher).publish(response);
    }

    @Test
    void invalidReferenceTypeReportsTheActualInvalidField() {
        CreateNotificationCommand command = new CreateNotificationCommand(
                UUID.randomUUID(), UUID.randomUUID(), "Tiêu đề", "Nội dung",
                "CLUB", "CLUB", "UNKNOWN_REFERENCE", UUID.randomUUID());

        assertThatThrownBy(() -> service().createNotification(command))
                .isInstanceOf(ServiceException.class)
                .hasMessage("Loại tham chiếu không hợp lệ: UNKNOWN_REFERENCE");
    }

    @Test
    void repeatedEventIdCreatesNoSecondNotification() throws ServiceException {
        UUID eventId = UUID.randomUUID();
        when(notificationRepository.existsByEventId(eventId)).thenReturn(true);

        assertThat(service().createNotification(new CreateNotificationCommand(
                eventId, UUID.randomUUID(), "Hết giờ giữ sân — đang hoàn tiền", "Nội dung",
                "BOOKING", "VENUE", "BOOKING", UUID.randomUUID()))).isNull();

        verify(notificationRepository, org.mockito.Mockito.never()).save(any());
        org.mockito.Mockito.verifyNoInteractions(webSocketPublisher);
    }

    private NotificationServiceImpl service() {
        return new NotificationServiceImpl(notificationRepository, webSocketPublisher, notificationMapper);
    }
}

package iuh.fit.notificationservice.infrastructure.persistence;

import iuh.fit.notificationservice.domain.enums.BroadcastAudience;
import iuh.fit.notificationservice.domain.enums.BroadcastStatus;
import iuh.fit.notificationservice.domain.enums.NotificationReferenceType;
import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import iuh.fit.notificationservice.domain.enums.SourceService;
import iuh.fit.notificationservice.infrastructure.persistence.entity.BroadcastEntity;
import iuh.fit.notificationservice.infrastructure.persistence.entity.NotificationEntity;
import iuh.fit.notificationservice.infrastructure.persistence.repository.jpa.JpaBroadcastRepository;
import iuh.fit.notificationservice.infrastructure.persistence.repository.jpa.JpaNotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Các câu JPQL của thông báo hệ thống chạy trên H2 (chế độ PostgreSQL). */
@DataJpaTest
@ActiveProfiles("test")
class BroadcastPersistenceTest {
    @Autowired private JpaBroadcastRepository broadcasts;
    @Autowired private JpaNotificationRepository notifications;

    @Test
    void onlyOneWorkerCanClaimUntilTheLeaseGoesStale() {
        UUID id = broadcasts.saveAndFlush(BroadcastEntity.builder()
                .title("Bảo trì").content("Tối nay").audience(BroadcastAudience.ALL).status(BroadcastStatus.SENDING)
                .sentBy(UUID.randomUUID()).idempotencyKey("key").build()).getBroadcastId();
        LocalDateTime now = LocalDateTime.of(2026, 10, 10, 10, 0);

        assertThat(broadcasts.claim(id, BroadcastStatus.SENDING, now, now.minusMinutes(2))).isEqualTo(1);
        assertThat(broadcasts.claim(id, BroadcastStatus.SENDING, now.plusMinutes(1), now.minusMinutes(1))).isZero();
        assertThat(broadcasts.findStalled(BroadcastStatus.SENDING, now.minusMinutes(1))).isEmpty();

        // Người giữ im lặng quá hạn: tiến trình khác giành được và gửi tiếp.
        assertThat(broadcasts.findStalled(BroadcastStatus.SENDING, now.plusMinutes(3))).containsExactly(id);
        assertThat(broadcasts.claim(id, BroadcastStatus.SENDING, now.plusMinutes(5), now.plusMinutes(3))).isEqualTo(1);
        assertThat(broadcasts.findById(id).orElseThrow().getAttemptCount()).isEqualTo(2);

        broadcasts.finish(id, BroadcastStatus.SENT, now.plusMinutes(6), null);
        assertThat(broadcasts.claim(id, BroadcastStatus.SENDING, now.plusMinutes(30), now.plusMinutes(29))).isZero();
    }

    @Test
    void findsWhichEventIdsAlreadyHaveANotification() {
        UUID delivered = UUID.randomUUID();
        notifications.saveAndFlush(NotificationEntity.builder()
                .eventId(delivered).receiverId(UUID.randomUUID()).title("t").content("c")
                .type(NotificationType.SYSTEM).status(NotificationStatus.UNREAD).sourceService(SourceService.ADMIN)
                .referenceType(NotificationReferenceType.SYSTEM).build());

        assertThat(notifications.findExistingEventIds(List.of(delivered, UUID.randomUUID()))).containsExactly(delivered);
    }
}

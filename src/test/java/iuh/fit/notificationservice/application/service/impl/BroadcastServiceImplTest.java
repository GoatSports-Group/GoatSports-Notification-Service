package iuh.fit.notificationservice.application.service.impl;

import iuh.fit.notificationservice.application.dto.request.broadcast.SendBroadcastCommand;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.dto.PageFilter;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.mapper.NotificationMapper;
import iuh.fit.notificationservice.application.port.client.RecipientDirectoryPort;
import iuh.fit.notificationservice.application.port.persistence.BroadcastRepository;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import iuh.fit.notificationservice.domain.entity.Broadcast;
import iuh.fit.notificationservice.domain.entity.Notification;
import iuh.fit.notificationservice.domain.enums.BroadcastStatus;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import iuh.fit.notificationservice.domain.enums.SourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BroadcastServiceImplTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-10T03:00:00Z"), ZoneId.of("UTC"));
    private static final UUID ADMIN = UUID.randomUUID();

    private final FakeBroadcasts broadcasts = new FakeBroadcasts();
    private final FakeNotifications notifications = new FakeNotifications();
    private final FakeDirectory directory = new FakeDirectory();
    private final List<UUID> pushed = new ArrayList<>();
    private final List<UUID> scheduled = new ArrayList<>();
    private BroadcastServiceImpl service;

    @BeforeEach
    void setUp() {
        NotificationWebSocketPublisher publisher = mock(NotificationWebSocketPublisher.class, invocation -> {
            if (invocation.getMethod().getName().equals("publish")) pushed.add(UUID.randomUUID());
            return null;
        });
        service = new BroadcastServiceImpl(broadcasts, notifications, directory, publisher,
                mock(NotificationMapper.class), scheduled::add, CLOCK);
    }

    @Test
    void sendingTwiceWithTheSameKeyCreatesOneBroadcastAndSchedulesItOnce() throws ServiceException {
        Broadcast first = service.send(command("key-1"));
        Broadcast replay = service.send(command("key-1"));

        assertThat(replay.getBroadcastId()).isEqualTo(first.getBroadcastId());
        assertThat(broadcasts.items).hasSize(1);
        assertThat(scheduled).containsExactly(first.getBroadcastId());
        assertThat(first.getStatus()).isEqualTo(BroadcastStatus.SENDING);
        assertThat(first.getTitle()).isEqualTo("Bảo trì hệ thống");
    }

    @Test
    void rejectsUnknownAudienceAndOverlongTitle() {
        assertThatThrownBy(() -> service.send(new SendBroadcastCommand("t", "c", "ADMIN", "k", ADMIN)))
                .isInstanceOf(ServiceException.class);
        assertThatThrownBy(() -> service.send(new SendBroadcastCommand("x".repeat(151), "c", "ALL", "k", ADMIN)))
                .isInstanceOf(ServiceException.class);
    }

    @Test
    void deliversToEveryRecipientInBatchesAndMarksSent() throws ServiceException {
        directory.add(BroadcastServiceImpl.BATCH_SIZE * 2 + 3);
        Broadcast broadcast = service.send(command("key-2"));

        service.deliver(broadcast.getBroadcastId());

        Broadcast stored = broadcasts.items.get(broadcast.getBroadcastId());
        assertThat(stored.getStatus()).isEqualTo(BroadcastStatus.SENT);
        assertThat(stored.getDeliveredCount()).isEqualTo(1003);
        assertThat(notifications.saved).hasSize(1003);
        assertThat(pushed).hasSize(1003);
        assertThat(directory.calls).isEqualTo(3);
        Notification sample = notifications.saved.getFirst();
        assertThat(sample.getType()).isEqualTo(NotificationType.SYSTEM);
        assertThat(sample.getSourceService()).isEqualTo(SourceService.ADMIN);
        assertThat(sample.getReferenceId()).isEqualTo(broadcast.getBroadcastId());
    }

    @Test
    void resumesFromTheCheckpointWithoutDuplicatingAlreadyDeliveredNotifications() throws ServiceException {
        directory.add(BroadcastServiceImpl.BATCH_SIZE + 10);
        Broadcast broadcast = service.send(command("key-3"));
        directory.failAfterCalls = 1;

        service.deliver(broadcast.getBroadcastId());

        Broadcast interrupted = broadcasts.items.get(broadcast.getBroadcastId());
        assertThat(interrupted.getStatus()).isEqualTo(BroadcastStatus.SENDING);
        assertThat(interrupted.getClaimedAt()).isNull();
        assertThat(interrupted.getFailureReason()).contains("đã tới 500 người");
        assertThat(service.findStalled()).containsExactly(broadcast.getBroadcastId());

        directory.failAfterCalls = Integer.MAX_VALUE;
        service.deliver(broadcast.getBroadcastId());

        assertThat(broadcasts.items.get(broadcast.getBroadcastId()).getStatus()).isEqualTo(BroadcastStatus.SENT);
        assertThat(notifications.saved).hasSize(510);
        assertThat(new HashSet<>(notifications.saved.stream().map(Notification::getReceiverId).toList())).hasSize(510);
    }

    @Test
    void secondWorkerDoesNothingWhileTheFirstHoldsTheClaim() throws ServiceException {
        directory.add(3);
        Broadcast broadcast = service.send(command("key-4"));
        broadcasts.claim(broadcast.getBroadcastId(), LocalDateTime.now(CLOCK), LocalDateTime.now(CLOCK).minusMinutes(2));

        service.deliver(broadcast.getBroadcastId());

        assertThat(notifications.saved).isEmpty();
        assertThat(service.findStalled()).isEmpty();
    }

    @Test
    void givesUpAfterTheMaximumNumberOfAttempts() throws ServiceException {
        directory.add(3);
        directory.failAfterCalls = 0;
        Broadcast broadcast = service.send(command("key-5"));

        for (int attempt = 0; attempt < BroadcastServiceImpl.MAX_ATTEMPTS; attempt++) {
            service.deliver(broadcast.getBroadcastId());
        }

        assertThat(broadcasts.items.get(broadcast.getBroadcastId()).getStatus()).isEqualTo(BroadcastStatus.FAILED);
        assertThat(service.findStalled()).isEmpty();
    }

    private static SendBroadcastCommand command(String key) {
        return new SendBroadcastCommand("  Bảo trì hệ thống ", "Hệ thống bảo trì lúc 23:00.", "ALL", key, ADMIN);
    }

    /** Mô phỏng UPDATE có điều kiện của JpaBroadcastRepository. */
    private static final class FakeBroadcasts implements BroadcastRepository {
        final Map<UUID, Broadcast> items = new HashMap<>();

        @Override
        public Broadcast create(Broadcast broadcast) {
            broadcast.setBroadcastId(UUID.randomUUID());
            broadcast.setCreatedAt(LocalDateTime.now(CLOCK));
            items.put(broadcast.getBroadcastId(), broadcast);
            return broadcast;
        }

        @Override
        public Optional<Broadcast> findById(UUID broadcastId) {
            return Optional.ofNullable(items.get(broadcastId));
        }

        @Override
        public Optional<Broadcast> findByIdempotencyKey(String key) {
            return items.values().stream().filter(item -> item.getIdempotencyKey().equals(key)).findFirst();
        }

        @Override
        public PageResult<Broadcast> findPage(int page, int size) {
            return new PageResult<>(List.copyOf(items.values()), items.size(), page, size, 1);
        }

        @Override
        public boolean claim(UUID id, LocalDateTime now, LocalDateTime staleBefore) {
            Broadcast item = items.get(id);
            boolean free = item.getClaimedAt() == null || item.getClaimedAt().isBefore(staleBefore);
            if (item.getStatus() != BroadcastStatus.SENDING || !free) return false;
            item.setClaimedAt(now);
            item.setAttemptCount(item.getAttemptCount() + 1);
            return true;
        }

        @Override
        public void checkpoint(UUID id, UUID lastRecipientId, int deliveredCount, LocalDateTime now) {
            Broadcast item = items.get(id);
            item.setLastRecipientId(lastRecipientId);
            item.setDeliveredCount(deliveredCount);
            item.setClaimedAt(now);
        }

        @Override
        public void markSent(UUID id, LocalDateTime completedAt) {
            finish(id, BroadcastStatus.SENT, null, completedAt);
        }

        @Override
        public void markFailed(UUID id, String reason, LocalDateTime completedAt) {
            finish(id, BroadcastStatus.FAILED, reason, completedAt);
        }

        private void finish(UUID id, BroadcastStatus status, String reason, LocalDateTime completedAt) {
            Broadcast item = items.get(id);
            item.setStatus(status);
            item.setFailureReason(reason);
            item.setCompletedAt(completedAt);
            item.setClaimedAt(null);
        }

        @Override
        public void release(UUID id, String reason) {
            items.get(id).setClaimedAt(null);
            items.get(id).setFailureReason(reason);
        }

        @Override
        public List<UUID> findStalled(LocalDateTime staleBefore) {
            return items.values().stream()
                    .filter(item -> item.getStatus() == BroadcastStatus.SENDING)
                    .filter(item -> item.getClaimedAt() == null || item.getClaimedAt().isBefore(staleBefore))
                    .map(Broadcast::getBroadcastId).toList();
        }
    }

    private static final class FakeNotifications implements NotificationRepository {
        final List<Notification> saved = new ArrayList<>();
        private final Set<UUID> eventIds = new HashSet<>();

        @Override
        public List<Notification> saveAllNew(List<Notification> batch) {
            List<Notification> fresh = batch.stream().filter(item -> eventIds.add(item.getEventId())).toList();
            saved.addAll(fresh);
            return fresh;
        }

        @Override public Notification save(Notification notification) { throw new UnsupportedOperationException(); }
        @Override public boolean existsByEventId(UUID eventId) { return eventIds.contains(eventId); }
        @Override public Optional<Notification> findById(UUID id) { return Optional.empty(); }
        @Override public PageResult<Notification> findByReceiverId(PageFilter filter, UUID receiverId) { return null; }
        @Override public long countUnread(UUID receiverId) { return 0; }
        @Override public void markAllReadByReceiverId(UUID receiverId) { }
        @Override public void deleteById(UUID id) { }
        @Override public void deleteByReceiverId(UUID receiverId) { }
    }

    /** Keyset theo thứ tự byte không dấu như PostgreSQL (so chuỗi UUID). */
    private static final class FakeDirectory implements RecipientDirectoryPort {
        private final List<UUID> recipients = new ArrayList<>();
        int calls;
        int failAfterCalls = Integer.MAX_VALUE;

        void add(int count) {
            IntStream.range(0, count).forEach(index -> recipients.add(UUID.randomUUID()));
            recipients.sort(Comparator.comparing(UUID::toString));
        }

        @Override
        public List<UUID> findRecipientIds(Set<String> roles, UUID after, int limit) {
            if (calls++ >= failAfterCalls) throw new IllegalStateException("auth-service tạm lỗi");
            return recipients.stream()
                    .filter(id -> after == null || id.toString().compareTo(after.toString()) > 0)
                    .limit(limit).toList();
        }

        @Override
        public long countRecipients(Set<String> roles) {
            return recipients.size();
        }
    }
}

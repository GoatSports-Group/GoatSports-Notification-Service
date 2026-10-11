package iuh.fit.notificationservice.application.service.impl;

import iuh.fit.notificationservice.application.dto.request.broadcast.SendBroadcastCommand;
import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.exception.ServiceException;
import iuh.fit.notificationservice.application.mapper.NotificationMapper;
import iuh.fit.notificationservice.application.port.client.RecipientDirectoryPort;
import iuh.fit.notificationservice.application.port.persistence.BroadcastRepository;
import iuh.fit.notificationservice.application.port.persistence.NotificationRepository;
import iuh.fit.notificationservice.application.port.scheduling.BroadcastDeliveryScheduler;
import iuh.fit.notificationservice.application.port.websocket.NotificationWebSocketPublisher;
import iuh.fit.notificationservice.application.service.BroadcastService;
import iuh.fit.notificationservice.domain.entity.Broadcast;
import iuh.fit.notificationservice.domain.entity.Notification;
import iuh.fit.notificationservice.domain.enums.BroadcastAudience;
import iuh.fit.notificationservice.domain.enums.BroadcastStatus;
import iuh.fit.notificationservice.domain.enums.NotificationReferenceType;
import iuh.fit.notificationservice.domain.enums.NotificationStatus;
import iuh.fit.notificationservice.domain.enums.NotificationType;
import iuh.fit.notificationservice.domain.enums.SourceService;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BroadcastServiceImpl implements BroadcastService {
    static final int BATCH_SIZE = 500;
    /** Người gửi im lặng quá thời gian này (service chết giữa chừng) thì tiến trình khác được gửi tiếp. */
    static final Duration LEASE = Duration.ofMinutes(2);
    static final int MAX_ATTEMPTS = 5;
    private static final int MAX_PAGE_SIZE = 50;

    private final BroadcastRepository broadcastRepository;
    private final NotificationRepository notificationRepository;
    private final RecipientDirectoryPort recipientDirectory;
    private final NotificationWebSocketPublisher webSocketPublisher;
    private final NotificationMapper notificationMapper;
    private final BroadcastDeliveryScheduler deliveryScheduler;
    private final Clock clock;

    public BroadcastServiceImpl(BroadcastRepository broadcastRepository, NotificationRepository notificationRepository,
                                RecipientDirectoryPort recipientDirectory, NotificationWebSocketPublisher webSocketPublisher,
                                NotificationMapper notificationMapper, BroadcastDeliveryScheduler deliveryScheduler,
                                Clock clock) {
        this.broadcastRepository = broadcastRepository;
        this.notificationRepository = notificationRepository;
        this.recipientDirectory = recipientDirectory;
        this.webSocketPublisher = webSocketPublisher;
        this.notificationMapper = notificationMapper;
        this.deliveryScheduler = deliveryScheduler;
        this.clock = clock;
    }

    @Override
    public Broadcast send(SendBroadcastCommand command) throws ServiceException {
        BroadcastAudience audience = command.validate();
        Optional<Broadcast> replay = broadcastRepository.findByIdempotencyKey(command.idempotencyKey());
        if (replay.isPresent()) return replay.get();

        Broadcast created = broadcastRepository.create(Broadcast.builder()
                .title(command.title().trim())
                .content(command.content().trim())
                .audience(audience)
                .status(BroadcastStatus.SENDING)
                .sentBy(command.sentBy())
                .idempotencyKey(command.idempotencyKey())
                .build());
        deliveryScheduler.schedule(created.getBroadcastId());
        return created;
    }

    @Override
    public PageResult<Broadcast> getBroadcasts(int page, int size) {
        return broadcastRepository.findPage(Math.max(page, 0), Math.max(1, Math.min(size, MAX_PAGE_SIZE)));
    }

    @Override
    public long countAudience(String audience) throws ServiceException {
        return recipientDirectory.countRecipients(SendBroadcastCommand.parseAudience(audience).roles());
    }

    @Override
    public void deliver(UUID broadcastId) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!broadcastRepository.claim(broadcastId, now, now.minus(LEASE))) return;
        Broadcast broadcast = broadcastRepository.findById(broadcastId).orElse(null);
        if (broadcast == null) return;

        UUID after = broadcast.getLastRecipientId();
        int delivered = broadcast.getDeliveredCount();
        try {
            while (true) {
                List<UUID> recipients = recipientDirectory.findRecipientIds(broadcast.getAudience().roles(), after, BATCH_SIZE);
                if (recipients.isEmpty()) break;
                // Người đã nhận ở lần chạy trước (lô bị ngắt giữa chừng) không bị tạo hay đẩy lại.
                List<Notification> created = notificationRepository.saveAllNew(
                        recipients.stream().map(receiverId -> notificationFor(broadcast, receiverId)).toList());
                created.forEach(item -> webSocketPublisher.publish(notificationMapper.toNotificationResponseCommand(item)));
                after = recipients.getLast();
                delivered += recipients.size();
                broadcastRepository.checkpoint(broadcastId, after, delivered, LocalDateTime.now(clock));
                if (recipients.size() < BATCH_SIZE) break;
            }
            broadcastRepository.markSent(broadcastId, LocalDateTime.now(clock));
        } catch (RuntimeException exception) {
            String reason = "Gửi chưa xong (đã tới " + delivered + " người): " + exception.getMessage();
            if (broadcast.getAttemptCount() >= MAX_ATTEMPTS) {
                broadcastRepository.markFailed(broadcastId, reason, LocalDateTime.now(clock));
            } else {
                broadcastRepository.release(broadcastId, reason);
            }
        }
    }

    @Override
    public List<UUID> findStalled() {
        return broadcastRepository.findStalled(LocalDateTime.now(clock).minus(LEASE));
    }

    private static Notification notificationFor(Broadcast broadcast, UUID receiverId) {
        return Notification.builder()
                .eventId(broadcast.notificationEventId(receiverId))
                .receiverId(receiverId)
                .title(broadcast.getTitle())
                .content(broadcast.getContent())
                .type(NotificationType.SYSTEM)
                .status(NotificationStatus.UNREAD)
                .sourceService(SourceService.ADMIN)
                .referenceType(NotificationReferenceType.SYSTEM)
                .referenceId(broadcast.getBroadcastId())
                .build();
    }
}

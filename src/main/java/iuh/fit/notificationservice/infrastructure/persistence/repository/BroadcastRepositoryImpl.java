package iuh.fit.notificationservice.infrastructure.persistence.repository;

import iuh.fit.notificationservice.application.dto.response.base.PageResult;
import iuh.fit.notificationservice.application.port.persistence.BroadcastRepository;
import iuh.fit.notificationservice.domain.entity.Broadcast;
import iuh.fit.notificationservice.domain.enums.BroadcastStatus;
import iuh.fit.notificationservice.infrastructure.persistence.mapper.BroadcastEntityMapper;
import iuh.fit.notificationservice.infrastructure.persistence.repository.jpa.JpaBroadcastRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BroadcastRepositoryImpl implements BroadcastRepository {
    private final JpaBroadcastRepository jpaRepository;
    private final BroadcastEntityMapper mapper;

    @Override
    public Broadcast create(Broadcast broadcast) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(broadcast)));
    }

    @Override
    public Optional<Broadcast> findById(UUID broadcastId) {
        return jpaRepository.findById(broadcastId).map(mapper::toDomain);
    }

    @Override
    public Optional<Broadcast> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(mapper::toDomain);
    }

    @Override
    public PageResult<Broadcast> findPage(int page, int size) {
        Page<Broadcast> result = jpaRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(mapper::toDomain);
        return new PageResult<>(result.getContent(), result.getTotalElements(), page, size, result.getTotalPages());
    }

    @Override
    @Transactional
    public boolean claim(UUID broadcastId, LocalDateTime now, LocalDateTime staleBefore) {
        return jpaRepository.claim(broadcastId, BroadcastStatus.SENDING, now, staleBefore) == 1;
    }

    @Override
    @Transactional
    public void checkpoint(UUID broadcastId, UUID lastRecipientId, int deliveredCount, LocalDateTime now) {
        jpaRepository.checkpoint(broadcastId, lastRecipientId, deliveredCount, now);
    }

    @Override
    @Transactional
    public void markSent(UUID broadcastId, LocalDateTime completedAt) {
        jpaRepository.finish(broadcastId, BroadcastStatus.SENT, completedAt, null);
    }

    @Override
    @Transactional
    public void markFailed(UUID broadcastId, String reason, LocalDateTime completedAt) {
        jpaRepository.finish(broadcastId, BroadcastStatus.FAILED, completedAt, truncate(reason));
    }

    @Override
    @Transactional
    public void release(UUID broadcastId, String reason) {
        jpaRepository.release(broadcastId, truncate(reason));
    }

    @Override
    public List<UUID> findStalled(LocalDateTime staleBefore) {
        return jpaRepository.findStalled(BroadcastStatus.SENDING, staleBefore);
    }

    private static String truncate(String reason) {
        return reason == null || reason.length() <= 500 ? reason : reason.substring(0, 500);
    }
}
